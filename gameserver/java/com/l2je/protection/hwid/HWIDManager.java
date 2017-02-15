package com.l2je.protection.hwid;

import com.l2je.protection.ProtectionConfig;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.network.L2GameClient;

public class HWIDManager
{
	protected static final Logger _log = Logger.getLogger(HWIDManager.class.getName());
	private HashSet<String> _bannedHwids = new HashSet<>();
	private HashSet<Hwid> _hwids = new HashSet<>();
	private LinkedList<Hwid> _newHwids = new LinkedList<>();
	
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
	
	public void initSession(L2GameClient client, byte[] data)
	{
		if (ProtectionConfig.HWID)
		{
			if (data.length > 17)
			{
				byte bytes[] = Arrays.copyOfRange(data, 16, data.length);
				String hwidHdd = new String(bytes, StandardCharsets.UTF_8);
				String ip = client.getConnection().getInetAddress().getHostAddress();
				client.setHWID(hwidHdd);
				Hwid hwidToAdd = new Hwid(client.getAccountName(), hwidHdd, ip);
				if (!_hwids.contains(hwidToAdd))
				{
					_hwids.add(hwidToAdd);
					_newHwids.add(hwidToAdd);
				}
				if (_bannedHwids.contains(hwidHdd))
				{
					client.banByHwid();
					_log.warning("Banned HWID: " + client.getHWid() + ". Account:" + client.getAccountName());
				}
				ipCheck(client);
			}
			else
			{
				_log.warning("Bad HWID. Account:" + client.getAccountName());
			}
			
		}
	}
	
	/**
	 * Проверка привязки по ip
	 * @param client
	 */
	private static void ipCheck(L2GameClient client)
	{
		String ipBlock = client.getAccountData().getString("ip", "0");
		if (!ipBlock.equals("0"))
		{
			if (!ipBlock.equals(client.getConnection().getInetAddress().getHostAddress()))
			{
				client.closeNow();
				StringBuffer sb = new StringBuffer();
				StringUtil.append(sb, "Bad ip for:", client.getAccountName(), " ", client.getConnection().getInetAddress().getHostAddress(), ". Must be - " + ipBlock);
				_log.warning(sb.toString());
			}
		}
	}
	
	public void banHwid(L2GameClient client)
	{
		_bannedHwids.add(client.getHWid());
		client.banByHwid();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement stm = con.prepareStatement("INSERT INTO `banned_hwids`(`HWID`) VALUES ('" + client.getHWid() + "')");
			stm.execute();
			stm.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		_log.warning("HWID has Banned : " + client.getHWid() + ". Account:" + client.getAccountName());
	}
	
	public void saveHwidsToDB()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statementInsert = con.prepareStatement("INSERT INTO `a_hwids`(`login`, `HWID`,`ip`) VALUES (?,?,?)");
			for (Hwid hwid : _newHwids)
			{
				statementInsert.setString(1, hwid.getLogin());
				statementInsert.setString(2, hwid.getHwid());
				statementInsert.setString(3, hwid.getIP());
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
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stm = con.prepareStatement("select * from banned_hwids"); ResultSet rs = stm.executeQuery())
		{
			while (rs.next())
			{
				_bannedHwids.add(rs.getString(1));
				
			}
		}
		catch (Exception e)
		{
			if (e.getClass().getSimpleName().equals("MySQLSyntaxErrorException"))
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement("create table `banned_hwids` (`HWID` varchar(64) PRIMARY KEY)");)
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
		try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stm = con.prepareStatement("select * from a_hwids"); ResultSet rs = stm.executeQuery())
		{
			while (rs.next())
			{
				_hwids.add(new Hwid(rs.getString(1), rs.getString(2), rs.getString(3)));
				
			}
		}
		catch (Exception e)
		{
			if (e.getClass().getSimpleName().equals("MySQLSyntaxErrorException"))
			{
				try (Connection con = L2DatabaseFactory.getInstance().getConnection(); PreparedStatement stmt = con.prepareStatement("create table `a_hwids` (`login` varchar(32) not null,`HWID` varchar(64), `ip` varchar(24) CONSTRAINT PK_ah PRIMARY KEY (`login`,`HWID`,`ip`))");)
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
