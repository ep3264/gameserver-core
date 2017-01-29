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

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Redist
 */
public class CustomL2NamesInstance extends L2NpcInstance
{	
	public CustomL2NamesInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(getHtmlPath(getNpcId(), val));
		html.replace("%objectId%", getObjectId());
		html.replace("%price%", Config.NAME_CHANGE_PRICE);
		String itemName = ItemTable.getInstance().getTemplate(Config.NAME_CHANGE_ITEM).getName();
		html.replace("%item%", itemName);
		player.sendPacket(html);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;
		if (val == 0)
			filename = "data/html/mods/names/" + npcId + ".htm";
		else
			filename = "data/html/mods/names/" + npcId + "-" + val + ".htm";
		
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
			
			catch (NumberFormatException nfe)
			{
			}
			
			showChatWindow(player, val);
		}
		else if (command.startsWith("SetName"))
		{
			String name = "";
			try
			{
				name = command.substring(8);
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			setName(player, name);
			showChatWindow(player, 0);
		}
	}
	
	private static boolean checkCondition(L2PcInstance player, String name)
	{
		ItemInstance item = player.getInventory().getItemByItemId(Config.NAME_CHANGE_ITEM);
		if (item == null || item.getCount() < Config.NAME_CHANGE_PRICE)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return false;
		}
		if (!StringUtil.isValidName(name, "^[A-Za-z0-9\\~\\{\\}\\^_\\-\\[\\]\\.\\(\\)<>@]{3,16}$"))
		{
			player.sendMessage("Введите правильное имя, 3-16 символов, допустимые символы A-z^_~-{}.[]()<>@");
			return false;
		}
		if (CharNameTable.getInstance().getPlayerObjectId(name) > 0)
		{
			player.sendMessage("Персонаж с таким именем уже существует.");
			return false;
		}
		if (!player.destroyItem("Name Manager", item, Config.NAME_CHANGE_PRICE, player, true))
		{
			return false;
		}
		return true;
	}
	
	private static void setName(L2PcInstance player, String name)
	{
		try
		{
			if (checkCondition(player, name))
			{
				player.setName(name);
				CharNameTable.getInstance().updatePlayerData(player, false);
				player.broadcastUserInfo();
				player.store();
				player.sendMessage("Смена имени прошла успешно.");
			}
		}
		catch (Exception e)
		{
			player.sendMessage("Ошибка, попробуйте позже...");
		}
	}
}
