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
	private BannedHWIDs _bannedHWIDs = new BannedHWIDs();
	private HashSet<HardwareID> _hardwareIDs = new HashSet<>();
	private ArrayList<HardwareID> _newHWIDs = new ArrayList<>();
	
	private static final String CREATE_HWIDS_TABLE = "create table `protection_hwids` (`login` varchar(32) not null, `ip` varchar(24), `mac` varchar(128), `hdd` varchar(128), CONSTRAINT PK_h PRIMARY KEY (`login`,`hdd`,`ip`, `mac`)) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_bin";
	private static final String INSERT_HWIDS = "INSERT INTO `protection_hwids`(`login`, `ip`, `mac`, `hdd`) VALUES (?,?,?,?)";
	private static final String SELECT_HWIDS = "select * from protection_hwids";
	
	private static final String CREATE_BANNED_IPS_TABLE = "create table `protection_banned_ips` ( `ip` varchar(24) PRIMARY KEY) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_bin;";
	private static final String CREATE_BANNED_MACS_TABLE = "create table `protection_banned_macs` (`mac` varchar(128) PRIMARY KEY) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_bin;";
	private static final String CREATE_BANNED_HDDS_TABLE = "create table `protection_banned_hdds` ( `hdd` varchar(128) PRIMARY KEY) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_bin;";
	private static final String INSERT_BANNED_IPS = "INSERT INTO `protection_banned_ips` (`ip`) VALUES (?)";
	private static final String INSERT_BANNED_MACS = "INSERT INTO `protection_banned_macs` (`mac`) VALUES (?)";
	private static final String INSERT_BANNED_HDDS = "INSERT INTO `protection_banned_hdds` (`hdd`) VALUES (?)";
	private static final String SELECT_BANNED_IPS = "select * from protection_banned_ips";
	private static final String SELECT_BANNED_MACS = "select * from protection_banned_macs";
	private static final String SELECT_BANNED_HDDS = "select * from protection_banned_hdds";
	
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
		loadBannedHWIDs();
		loadAllHWIDs();
		_log.info("HWIDManager: Ready.");
	}
	
	private static HardwareID byteToHWID(L2GameClient client, byte[] data)
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
		return new HardwareID(client.getAccountName(), ip, mac, hdd);
	}
	
	public void initSession(L2GameClient client, byte[] data)
	{
		if (ProtectionConfig.HWID)
		{
			HardwareID hardwareID = byteToHWID(client, data);
			client.setHwid(hardwareID);
			synchronized (this)
			{
				if (!_hardwareIDs.contains(hardwareID))
				{
					_hardwareIDs.add(hardwareID);
					_newHWIDs.add(hardwareID);
				}
			}

			if ((!hardwareID.getHDD().equals("N/A") && _bannedHWIDs.containsHdd(hardwareID.getHDD())) 
				|| (!hardwareID.getMAC().equals("N/A") && _bannedHWIDs.containsMac(hardwareID.getMAC())) 
				|| _bannedHWIDs.containsIp(hardwareID.getIP()))
			{
				client.setBanHWID();
				_log.warning("Attempting to log in, Banned HWID: " + hardwareID);
				return;
			}
			checkMac(client);
		}
		checkIp(client);		
	}
	
	/**
	 * Проверка привязки по ip
	 * @param client
	 */
	private static void checkIp(L2GameClient client)
	{
		String bindIP = client.getAccountData().getString("ip", "0");
		if (!bindIP.equals("0"))
		{
			if (!bindIP.equals(client.getIp()))
			{
				client.closeNow();
				StringBuilder sb = new StringBuilder();
				StringUtil.append(sb, "Bad ip for:", client.getAccountName(), " ", client.getConnection().getInetAddress().getHostAddress(), ". Must be - " + bindIP);
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
		String bindMAC = client.getAccountData().getString("mac", "0");
		if (!bindMAC.equals("0"))
		{
			if (!bindMAC.equals(client.getHWID().getMAC()))
			{
				client.closeNow();
				StringBuilder sb = new StringBuilder();
				StringUtil.append(sb, "Bad mac for:", client.getAccountName(), " ", client.getHWID().getMAC(), ". Must be - " + bindMAC);
				_log.warning(sb.toString());
			}
		}
	}
	
	public void banIP(L2GameClient client)
	{
		String ip = client.getIp();
		client.setBanHWID();
		_bannedHWIDs.addIP(ip);
		_log.warning("IP has Banned : " + ip);
		if (!saveBannedHwidToDB(INSERT_BANNED_IPS, ip))
		{
			_log.warning("Error saving banned ip to db, account : " + client.getAccountName() + " Ip: " + client.getIp());
		}
	}
	
	public void banMAC(L2GameClient client)
	{
		String mac = client.getHWID().getMAC();
		client.setBanHWID();
		_bannedHWIDs.addMAC(mac);
		_log.warning("MAC has Banned : " + mac);
		if (!saveBannedHwidToDB(INSERT_BANNED_MACS, mac))
		{
			_log.warning("Error saving banned mac to db, account : " + client.getAccountName() + " MAC: " + mac);
		}
	}
	
	public void banHDD(L2GameClient client)
	{
		String hdd = client.getHWID().getHDD();
		client.setBanHWID();
		_bannedHWIDs.addHDD(hdd);
		_log.warning("HDD has Banned : " + hdd);
		if (!saveBannedHwidToDB(INSERT_BANNED_HDDS, hdd))
		{
			_log.warning("Error saving banned hdd in db, account : " + client.getAccountName() + " HDD: " + hdd);
		}
	}
	
	/**
	 * Сохранить забаненый hwid в бд
	 * @param query
	 * @param value
	 * @return
	 */
	private static boolean saveBannedHwidToDB(String query, String value)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stm = con.prepareStatement(query);
			stm.setString(1, value);
			stm.execute();
			stm.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * Загрузить все вариации Hardware Id + Account name
	 */
	private void loadAllHWIDs()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stm = con.prepareStatement(SELECT_HWIDS); ResultSet rs = stm.executeQuery())
		{
			while (rs.next())
			{
				_hardwareIDs.add(new HardwareID(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
				
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
		_log.info("HWIDManager: Loaded " + _hardwareIDs.size() + "  hwid(s)");
	}
	
	/**
	 * Сохранить если появились новые вариации HardwareId + Account name
	 */
	public void saveHwidsToDB()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statementInsert = con.prepareStatement(INSERT_HWIDS);
			for (HardwareID hwid : _newHWIDs)
			{
				statementInsert.setString(1, hwid.getLogin());
				statementInsert.setString(2, hwid.getIP());
				statementInsert.setString(3, hwid.getMAC());
				statementInsert.setString(4, hwid.getHDD());
				statementInsert.execute();
				statementInsert.clearParameters();
			}
			statementInsert.close();
			_log.info("HWIDManager: Save " + _newHWIDs.size() + " new hwid(s)");
			_newHWIDs.clear();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void reloadBannedHwids()
	{
		_bannedHWIDs.clear();
		loadBannedHWIDs();
	}
	
	private void loadBannedHWIDs()
	{
		loadBannedIps();
		loadBannedMacs();
		loadBannedHdds();
	}
	
	private void loadBannedIps()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stm = con.prepareStatement(SELECT_BANNED_IPS); ResultSet rs = stm.executeQuery())
		{
			while (rs.next())
			{
				_bannedHWIDs.addIP(rs.getString(1));
			}
		}
		catch (Exception e)
		{
			if (e.getClass().getSimpleName().equals("MySQLSyntaxErrorException"))
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement(CREATE_BANNED_IPS_TABLE);)
				{
					stmt.execute();
				}
				catch (Exception ex)
				{
					_log.warning(ex.toString());
				}
			}
		}
		_log.info("HWIDManager: Loaded " + _bannedHWIDs.countIPs() + " banned ip(s)");
	}
	
	private void loadBannedMacs()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stm = con.prepareStatement(SELECT_BANNED_MACS); ResultSet rs = stm.executeQuery())
		{
			while (rs.next())
			{
				_bannedHWIDs.addMAC(rs.getString(1));
			}
		}
		catch (Exception e)
		{
			if (e.getClass().getSimpleName().equals("MySQLSyntaxErrorException"))
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement(CREATE_BANNED_MACS_TABLE);)
				{
					stmt.execute();
				}
				catch (Exception ex)
				{
					_log.warning(ex.toString());
				}
			}
		}
		_log.info("HWIDManager: Loaded " + _bannedHWIDs.countMACs() + " banned mac(s)");
	}
	
	private void loadBannedHdds()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stm = con.prepareStatement(SELECT_BANNED_HDDS); ResultSet rs = stm.executeQuery())
		{
			while (rs.next())
			{
				_bannedHWIDs.addHDD(rs.getString(1));
			}
		}
		catch (Exception e)
		{
			if (e.getClass().getSimpleName().equals("MySQLSyntaxErrorException"))
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement(CREATE_BANNED_HDDS_TABLE);)
				{
					stmt.execute();
				}
				catch (Exception ex)
				{
					_log.warning(ex.toString());
				}
			}
		}
		_log.info("HWIDManager: Loaded " + _bannedHWIDs.countHDDs() + " banned hdd(s)");
	}
	
}
