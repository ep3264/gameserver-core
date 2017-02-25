package com.l2je.protection.hwid;

import com.l2je.protection.ProtectionConfig;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.network.L2GameClient;

public class HWIDManager
{
	protected static final Logger _log = Logger.getLogger(HWIDManager.class.getName());
	private BannedHwids _bannedHwids = new BannedHwids();
	private HashSet<Hwid> _hwids = new HashSet<>();
	private ArrayList<Hwid> _newHwids = new ArrayList<>();
	private static final String INSERT_HWIDS = "INSERT INTO `protection_hwids`(`login`, `ip`, `mac`, `hdd`) VALUES (?,?,?,?)";
	private static final String CREATE_HWIDS_TABLE = "create table `protection_hwids` (`login` varchar(32) not null, `ip` varchar(24), `mac` varchar(128), `hdd` varchar(128), CONSTRAINT PK_h PRIMARY KEY (`login`,`hdd`,`ip`, `mac`));";
	private static final String INSERT_BANNED_HWIDS = "INSERT INTO `protection_banned_hwids` (`ip`, `mac`, `hdd`) VALUES (?,?,?)";
	private static final String CREATE_BANNED_HWIDS_TABLE = "create table `protection_banned_hwids` ( `ip` varchar(24), `mac` varchar(128), `hdd` varchar(128), CONSTRAINT PK_bh PRIMARY KEY (`ip`, `mac`, `hdd`));";
	
	private static class SingletonHolder
	{
		protected static final HWIDManager _instance = new HWIDManager();
	}
	
	public static final HWIDManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public HWIDManager()
	{
		loadBannedHwids();
		loadHwids();
		_log.info("HWIDManager: Ready.");
	}
	
	private static Hwid byteToHwid(L2GameClient client, byte[] data)
	{
		String mac = "N/A";
		String hdd = "N/A";
		String ip = client.getIp();
		if (data.length > 17)
		{
			byte bytes[] = Arrays.copyOfRange(data, 16, data.length);
			try
			{
				String stringByte = new String(bytes, "US-ASCII");
				StringTokenizer st = new StringTokenizer(stringByte, "###");
				while (st.hasMoreTokens())
				{
					String temp = st.nextToken();
					if (temp.startsWith("@@@:"))
					{
						hdd = temp;
					}
					else
					{
						mac = temp;
					}
				}
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			_log.warning("Bad HWID. Account:" + client.getAccountName());
		}
		return new Hwid(client.getAccountName(), ip, mac, hdd);
	}
	
	public void initSession(L2GameClient client, byte[] data)
	{
		if (ProtectionConfig.HWID)
		{
			Hwid hwidToAdd = byteToHwid(client, data);
			client.setHwid(hwidToAdd);
			if (!_hwids.contains(hwidToAdd))
			{
				_hwids.add(hwidToAdd);
				_newHwids.add(hwidToAdd);
			}
			
			boolean ip = ProtectionConfig.HWID_BAN_IP && _bannedHwids.containsIp(hwidToAdd.getIP());
			boolean mac = ProtectionConfig.HWID_BAN_MAC && !hwidToAdd.getMac().equals("N/A") && _bannedHwids.containsMac(hwidToAdd.getMac());
			boolean hdd = ProtectionConfig.HWID_BAN_HDD && !hwidToAdd.getHdd().equals("N/A") && _bannedHwids.containsHdd(hwidToAdd.getHdd());
			if (hdd || mac || ip)
			{
				client.setBanHwid();
				_log.warning("Attempting to log in Banned HWID: " + hwidToAdd);
				return;
			}
		}
		checkIp(client);
		checkMac(client);
	}
	
	/**
	 * Проверка привязки по ip
	 * @param client
	 */
	private static void checkIp(L2GameClient client)
	{
		String ipBlock = client.getAccountData().getString("ip", "0");
		if (!ipBlock.equals("0"))
		{
			if (!ipBlock.equals(client.getIp()))
			{
				client.closeNow();
				StringBuilder sb = new StringBuilder();
				StringUtil.append(sb, "Bad ip for:", client.getAccountName(), " ", client.getConnection().getInetAddress().getHostAddress(), ". Must be - " + ipBlock);
				_log.warning(sb.toString());
			}
		}
	}
	/**
	 * Проверка привязки по ip
	 * @param client
	 */
	private static void checkMac(L2GameClient client)
	{
		String macBlock = client.getAccountData().getString("mac", "0");
		if (!macBlock.equals("0"))
		{
			if (!macBlock.equals(client.getHwid().getMac()))
			{
				client.closeNow();
				StringBuilder sb = new StringBuilder();
				StringUtil.append(sb, "Bad mac for:", client.getAccountName(), " ", client.getHwid().getMac(), ". Must be - " + macBlock);
				_log.warning(sb.toString());
			}
		}
	}
	
	public void banHwid(L2GameClient client)
	{
		Hwid banHWid = client.getHwid();
		if (banHWid != null)
		{
			_bannedHwids.add(banHWid);
			client.setBanHwid();
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement stm = con.prepareStatement(INSERT_BANNED_HWIDS);
				stm.setString(1, banHWid.getIP());
				stm.setString(2, banHWid.getMac());
				stm.setString(3, banHWid.getHdd());
				stm.execute();
				stm.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			_log.warning("HWID has Banned : " + banHWid);
		}
		else
		{
			_log.warning("Error ban, HWID is null, account : " + client.getAccountName() + " Ip: " + client.getIp());
		}
	}
	
	public void saveHwidsToDB()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statementInsert = con.prepareStatement(INSERT_HWIDS);
			for (Hwid hwid : _newHwids)
			{
				statementInsert.setString(1, hwid.getLogin());
				statementInsert.setString(2, hwid.getIP());
				statementInsert.setString(3, hwid.getMac());
				statementInsert.setString(4, hwid.getHdd());
				statementInsert.execute();
				statementInsert.clearParameters();
			}
			statementInsert.close();
			_log.info("HWIDManager: Save " + _newHwids.size() + " new hwid(s)");
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void reloadBannedHwids()
	{
		_bannedHwids.clear();
		loadBannedHwids();
	}
	
	private void loadBannedHwids()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stm = con.prepareStatement("select * from protection_banned_hwids"); ResultSet rs = stm.executeQuery())
		{
			while (rs.next())
			{
				_bannedHwids.add(rs.getString(1), rs.getString(2), rs.getString(3));
			}
		}
		catch (Exception e)
		{
			if (e.getClass().getSimpleName().equals("MySQLSyntaxErrorException"))
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement(CREATE_BANNED_HWIDS_TABLE);)
				{
					stmt.execute();
				}
				catch (Exception ex)
				{
					_log.warning(ex.toString());
				}
			}
		}
		_log.info("HWIDManager: Loaded " + _bannedHwids.size() + " banned hwid(s)");
	}
	
	private void loadHwids()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stm = con.prepareStatement("select * from protection_hwids"); ResultSet rs = stm.executeQuery())
		{
			while (rs.next())
			{
				_hwids.add(new Hwid(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
				
			}
		}
		catch (Exception e)
		{
			if (e.getClass().getSimpleName().equals("MySQLSyntaxErrorException"))
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement(CREATE_HWIDS_TABLE);)
				{
					stmt.execute();
				}
				catch (Exception ex)
				{
					_log.warning(ex.toString());
				}
			}
		}
		_log.info("HWIDManager: Loaded " + _hwids.size() + "  hwid(s)");
	}
	
}
