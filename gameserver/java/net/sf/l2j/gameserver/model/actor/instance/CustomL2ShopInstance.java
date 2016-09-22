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

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import custom.casino.Roulette;

/**
 * @author redist
 *
 */

/**
 * @author user
 */
public class CustomL2ShopInstance extends L2NpcInstance
{
	public CustomL2ShopInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(getHtmlPath(getNpcId(), val));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;		
		if (val == 0)
			filename = "data/html/mods/shop/" + npcId + ".htm";
		else
			filename = "data/html/mods/shop/" + npcId + "-" + val + ".htm";			
		
		return filename;	
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		if (currentCommand.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
			}
			showChatWindow(player, val);
		}
		else if (currentCommand.startsWith("Roulette"))
		{
			int val = 1;
			try
			{
				val = Integer.parseInt(st.nextToken());
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			Roulette.getInstance().twist(player, val);
			showChatWindow(player, 1);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}
