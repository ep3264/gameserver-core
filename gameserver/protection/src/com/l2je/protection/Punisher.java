package com.l2je.protection;



import com.l2je.protection.ProtectionProperties;
import com.l2je.protection.hwid.HWIDManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;

/**
 * Developers: Redist Team<br>
 * <br>
 * <br>
 * Author: Redist<br>
 * Date: 29 февр. 2016 г.<br>
 * Time: 19:27:39<br>
 * <br>
 */
public class Punisher
{
	protected static final Logger _log = Logger.getLogger(Punisher.class.getName());
	
	private static class SingletonHolder
	{
		protected static final Punisher _instance = new Punisher();
	}
	
	private final int PUNISH_DURATION = 15;
	private static final String INSERT_ILLEGAL_ACTION =
		"INSERT INTO illegal_action"
		+ " (account_name, char_name, ip, hwid, reason, description) "	
		+ "VALUES (?,?,?,?,?,?)";	
	public static final Punisher getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static void saveToDb(L2GameClient client,byte reason,String descr)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(INSERT_ILLEGAL_ACTION);
			statement.setString(1, client.getAccountName());
			String char_name = "N/A";
			L2PcInstance player=null;
			if((player=client.getActiveChar())!=null)
			{
				char_name=player.getName();
			}
			statement.setString(2, char_name);
			statement.setString(3, client.getConnection().getInetAddress().getHostAddress());			
			statement.setString(4, client.getHWid());
			statement.setInt(5, reason);
			statement.setString(6, descr);
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{		
			e.printStackTrace();
		}
	}
	
	/*  1-2 - client recv func hack
	 *  3-5 - client send func hack 
	 * 0x6 - adrenalin bot
	 * reason 0x10 - <8 bytes packet  
	 * 0x11 - buffer 
	 * 0x12 - many unknow packet
	 * */
	public void illegalAction(L2GameClient client, byte reason)
	{
		if (reason >= 0x0 && reason < 0x10)
		{			
			String description = "";
			if(reason == 6)
			{
				description = "Adrenalin bot, titile XAAAA";
			}
			saveToDb(client,reason,description);
			String descr = "Punisher: Client " + client.toString() + " hack client and will disconnected for 60 sec. Reason: " + Byte.toString(reason);
			_log.warning(descr);
		
		}
		else
		{
			if (ProtectionProperties.ON_HACK_ATTEMP.equals("hwidban") && client.getHWid() != null)
			{
				HWIDManager.getInstance().banHwid(client);
			}
			else if (ProtectionProperties.ON_HACK_ATTEMP.equals("jail") && client.getActiveChar() != null)
			{
				client.getActiveChar().setPunishLevel(L2PcInstance.PunishLevel.JAIL, PUNISH_DURATION);
			}
			else if (ProtectionProperties.ON_HACK_ATTEMP.equals("acc") && client.getActiveChar() != null)
			{
				client.getActiveChar().setPunishLevel(L2PcInstance.PunishLevel.ACC, 0);
			}
			_log.warning("Punisher: Client " + client.toString() + " send bad packets and will " + ProtectionProperties.ON_HACK_ATTEMP + "ed. Reason: " + Byte.toString(reason));
			
		}
	}
}
