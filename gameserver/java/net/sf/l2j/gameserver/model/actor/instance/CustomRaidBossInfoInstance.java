/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.actor.instance;

/**
 * @author user
 *
 */

import java.util.Calendar;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import custom.raidboss.RaidBossInfo;

public class CustomRaidBossInfoInstance extends L2NpcInstance
{
	private static final int[] RBOSSES =
	{	
		25325,25199,25235,25248,25220,
		25523,25109,25202,25054,25229,
		25244,25249,25266,25276,25282,
		25524,25205,25143,25245,25293,
		25126,25450,25299,25309,25302,
		25312,25319,25527,25305,25315,	
	};
	private static final int[] EBOSSES =
	{
		29001, 
		29006, 
		29014,
		29022,		
		29020,
		29019,
		29028,
		29045,
	};
	
	public CustomRaidBossInfoInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(getHtmlPath(getNpcId(), val));
		html.replace("%objectId%", getObjectId());
		if (val != 0)
		{
			String bossesList="";
			if (val == 1)
			{
				bossesList=generateBossList();
			}
			else if (val == 2)
			{
				bossesList=generateEpicBossList();
			}
			html.replace("%bosslist%",bossesList);			
		}
		player.sendPacket(html);
	}
	
	private static String generateBossList()
	{
		final StringBuilder sb = new StringBuilder();
		
		for (int rboss : RBOSSES)
		{
			
			long delay = RaidBossInfo.getInstance().getRespawnTime(rboss)- Calendar.getInstance().getTimeInMillis();
			String name = NpcTable.getInstance().getTemplate(rboss).getName();
			String level =Byte.toString(NpcTable.getInstance().getTemplate(rboss).getLevel());
			
			if (delay <= 0)
			{
				sb.append("<font color=\"FFFFFF\">" + name +" "+level+ "</font><font color=\"FFFF00\"> Жив!</font><br1>");
			}/*
			else if (delay < 0)
			{
				sb.append("<font color=\"FF0000\">&nbsp;" + name + "&nbsp; Мертв.</font><br1>");
			} */
			else
			{				
				sb.append("<font color=\"FF0000\">" + name +" "+level+ "</font> " + ConverTime(delay) + "<br1>");
			}
		}
		return sb.toString();		
	}
	private static String generateEpicBossList()
	{
		final StringBuilder sb = new StringBuilder();
		
		for (int rboss : EBOSSES)
		{
			
			long delay = RaidBossInfo.getInstance().getEpicRespawnDate(rboss)- Calendar.getInstance().getTimeInMillis();
			String name = NpcTable.getInstance().getTemplate(rboss).getName().toUpperCase();
			String level =Byte.toString(NpcTable.getInstance().getTemplate(rboss).getLevel());
			
			if (delay <= 0)
			{
				sb.append("<font color=\"FFFFFF\">" + name +" "+level+ "</font><font color=\"FFFF00\"> Жив!</font><br1>");
			}/*
			else if (delay < 0)
			{
				sb.append("<font color=\"FF0000\">&nbsp;" + name + "&nbsp; Мертв.</font><br1>");
			} */
			else
			{				
				sb.append("<font color=\"FF0000\">" + name +" "+level+ "</font> " + ConverTime(delay) + "<br1>");
			}
		}
		return sb.toString();		
	}
	private static String ConverTime(long mseconds)
	{
		long remainder = mseconds;
		
		long hours = (long) Math.ceil((mseconds / (60 * 60 * 1000)));
		remainder = mseconds - (hours * 60 * 60 * 1000);
		
		long minutes = (long) Math.ceil((remainder / (60 * 1000)));
		remainder = remainder - (minutes * (60 * 1000));
		
		long seconds = (long) Math.ceil((remainder / 1000));
		
		return hours + ":" + minutes + ":" + seconds;
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;
		
		if (val == 0)
			filename = "data/html/mods/rbinfo/" + npcId + ".htm";
		else
			filename = "data/html/mods/rbinfo/" + npcId + "-" + val + ".htm";
			
		
		return filename;		
	}
}