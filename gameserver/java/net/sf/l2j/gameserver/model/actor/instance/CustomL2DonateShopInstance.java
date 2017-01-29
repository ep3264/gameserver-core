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

import com.l2je.custom.PremiumAccount;

import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author user
 */
public class CustomL2DonateShopInstance extends L2NpcInstance
{
	
	/**
	 * @param objectId
	 * @param template
	 */
	public CustomL2DonateShopInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;
		if (val == 0)
			filename = "data/html/mods/donate_shop/" + npcId + ".htm";
		else
			filename = "data/html/mods/donate_shop/" + npcId + "-" + val + ".htm";
		
		return filename;
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
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		if (currentCommand.startsWith("Chat"))
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
		else if (currentCommand.startsWith("Premium"))
		{
			if (player.getPremiumService() > 0)
			{
				SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
				player.sendMessage("Премиум уже активирован до: " + String.valueOf(format.format(player.getPremiumService())));
				return;
			}
			if (st.hasMoreTokens())
			{
				String input = st.nextToken();
				try
				{
					int days = Integer.valueOf(input);
					if (pay(player, days))
					{
						PremiumAccount.addPremiumServices(player, player.getAccountName(), days);
						player.sendMessage("Вы активировали премиум аккаунт на " + days + " дней.");
					}
					
				}
				catch (NumberFormatException e)
				{
					player.sendMessage("Неверный формат, введите количество дней.");
				}
				showPaWindow(player);
			}
			else
			{
				showPaWindow(player);
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	private static boolean pay(L2PcInstance player, int countDays)
	{
		ItemInstance item = player.getInventory().getItemByItemId(Config.PREMIUM_ITEM);
		if (item == null || item.getCount() < Config.PREMIUM_PRICE*countDays)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return false;
		}
		if (!player.destroyItem("Premium Manager", item, Config.PREMIUM_PRICE*countDays, player, true))
		{
			return false;
		}
		return true;
	}
	private void showPaWindow(L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(getHtmlPath(getNpcId(), 1));
		html.replace("%objectId%", getObjectId());
		html.replace("%price%", Config.PREMIUM_PRICE);
		String itemName = ItemTable.getInstance().getTemplate(Config.PREMIUM_ITEM).getName();
		html.replace("%item%", itemName);
		player.sendPacket(html);
	}
}
