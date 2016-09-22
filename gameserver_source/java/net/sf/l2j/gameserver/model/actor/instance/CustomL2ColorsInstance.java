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

import java.util.Arrays;
import java.util.HashSet;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import custom.colors.ColorsManager;

/**
 * @author Redist
 */
public class CustomL2ColorsInstance extends L2NpcInstance
{
	private static final int PRICE = 5;
	private final static int COL = 9213;
	public static final HashSet<String> ALLOWED_COLORS = new HashSet<>(Arrays.asList("009900", "007fff", "ff00ff", "ffff00", "ff0000", "ff9900", "93db70", "9f9f9f", "00ffff"));
	
	public CustomL2ColorsInstance(int objectId, NpcTemplate template)
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
			filename = "data/html/mods/colors/" + npcId + ".htm";
		else
			filename = "data/html/mods/colors/" + npcId + "-" + val + ".htm";
			
		return filename;		
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			
			showChatWindow(player, val);
		}
		else if (command.startsWith("SetNameColor"))
		{
			int color = 0xFFFFFF;
			try
			{
				String choice = command.substring(13);
				if (ALLOWED_COLORS.contains(choice))
				{
					color = Integer.decode("0x" + choice);
				}
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			if (pay(player))
			{
				setNameColor(player, color);
			}
			showChatWindow(player, 1);
		}
		else if (command.startsWith("SetTitleColor"))
		{
			int color = 0xFFFFFF;
			try
			{
				String choice = command.substring(14);
				if (ALLOWED_COLORS.contains(choice))
				{
					color = Integer.decode("0x" + choice);
				}
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			catch (NumberFormatException nfe)
			{
			}
			if (pay(player))
			{
				setTitleColor(player, color);
			}
			showChatWindow(player, 2);
		}
	}
	
	private static boolean pay(L2PcInstance player)
	{
		ItemInstance item = player.getInventory().getItemByItemId(COL);
		if (item == null || item.getCount() < PRICE)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return false;
		}
		if (!player.destroyItem("Color Manager", item, PRICE, player, true))
		{
			return false;
		}
		return true;
	}
	
	private static void setNameColor(L2PcInstance player, int color)
	{
		player.getAppearance().setNameColor(color);
		ColorsManager.getInstance().storeNameColor(player);
		player.broadcastUserInfo();
	}
	
	private static void setTitleColor(L2PcInstance player, int color)
	{
		player.getAppearance().setTitleColor(color);
		ColorsManager.getInstance().storeTitleColor(player);
		player.broadcastUserInfo();
	}
	
}
