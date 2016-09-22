package custom.events;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.Broadcast;

public final class CustomUtil
{
	public static void sendHtml(L2PcInstance player, String file)
	{
		sendHtml(player, file, new HashMap<String, String>());
	}
	
	public static void sendHtml(L2PcInstance player, String file, Map<String, String> variables)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(file);
		for (Map.Entry<String, String> entry : variables.entrySet())
		{
			html.replace(entry.getKey(), entry.getValue());
		}
		player.sendPacket(html);
	}
	
	public static void sendScreenMessage(L2PcInstance player, int time, int position, String message)
	{
		player.sendPacket(new ExShowScreenMessage(1, 0, position, false, 0, 0, 0, false, 2000, true, message));
	}
	
	public static void autoLogout(L2PcInstance player, int time)
	{
		new AutoLogoutCountDown(player, time);
	}
	
	public static class AutoLogoutCountDown implements Runnable
	{
		private L2PcInstance _player;
		
		public AutoLogoutCountDown(L2PcInstance player, int delay)
		{
			if ((_player = player) == null)
			{
				throw new NullPointerException("Trying to create AutoLogoutCountDown thread for null player");
			}
			ThreadPool.schedule(this, delay);
		}
		
		@Override
		public synchronized void run()
		{
			if (_player != null)
			{
				_player.logout(false);
			}
		}
	}
	
	public static void sendCaToAll(String message)
	{
		Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", message));
	}
	
	public static void sendCaToPlayer(L2PcInstance player, String message)
	{
		player.sendPacket(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", message));
	}
	
	public static void onStartUp()
	{
		/*spawnFence(187087,-177494,-3576,2,999,999,3);
		spawnFence(186550,-177137,-3565,2,200,200,3);
		spawnFence(186527,-177861,-3558,2,200,200,3);
		spawnFence(187615,-176942,-3576,2,200,200,3);*/
	}
}