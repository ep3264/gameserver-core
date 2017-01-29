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
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import custom.auction.Auction;
import custom.auction.AuctionConfig;
import custom.auction.AuctionItem;

/**
 * @author user
 */
public class CustomL2AuctioneerInstance extends L2NpcInstance
{
	
	/**
	 * @param objectId
	 * @param template
	 */
	public CustomL2AuctioneerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		if (AuctionConfig.AUCTION_ENABLE)
		{
			showWindow(player, Auction.getInstance().showHeadPage(player, 1, 0));
		}
		else
		{
			String html = HtmCache.getInstance().getHtm("data/html/mods/auction/Disable.htm");
			showWindow(player, html);
		}			
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;
		if (val == 0)
			filename = "data/html/mods/auction/" + npcId + ".htm";
		else
			filename = "data/html/mods/auction/" + npcId + "-" + val + ".htm";
		
		return filename;
	}
	
	public void showWindow(L2PcInstance player, String text)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setHtml(text);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		int page = 1;
		int type = 0;
		if (currentCommand.startsWith("auction"))
		{
			if (st.hasMoreTokens())
			{
				currentCommand = st.nextToken();
				if (currentCommand.startsWith("create_product"))
				{
					try
					{
						page = Integer.parseInt(st.nextToken());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					showWindow(player, Auction.getInstance().showCreateProductPage(player, page));
					
				}
				else if (currentCommand.startsWith("page"))
				{
					try
					{
						page = Integer.parseInt(st.nextToken());
						type = Integer.parseInt(st.nextToken());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					showWindow(player, Auction.getInstance().showHeadPage(player, page, type));		
				}
				else if (currentCommand.startsWith("show"))
				{
					int itemId = 0;
					try
					{
						itemId = Integer.parseInt(st.nextToken());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					AuctionItem auctionItem = Auction.getInstance()._products.get(itemId);
					if (auctionItem != null)
					{
						showWindow(player, Auction.getInstance().showItem(player, auctionItem));
					}
					else
					{
						player.sendMessage("Такой предмет на аукционе отсутствует.");
						showWindow(player, Auction.getInstance().showCreateProductPage(player, 1));
					}
				}
				else if (currentCommand.startsWith("accept_buy"))
				{
					int itemId = 0;
					try
					{
						itemId = Integer.parseInt(st.nextToken());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					AuctionItem auctionItem = Auction.getInstance()._products.get(itemId);
					if (auctionItem != null)
					{
						if (!auctionItem.alreadySell)
						{
							if (player.getObjectId() != auctionItem.trader_objId)
							{
								Auction.getInstance().buyItem(player, auctionItem);
							}
							else
							{
								Auction.getInstance().removeFromSale(player, auctionItem);
							}
						}
						else
						{
							player.sendMessage("Предмет уже был продан.");
						}
					}
					else
					{
						player.sendMessage("Такой предмет на аукционе отсутствует.");
					}
				}				
				else if (currentCommand.equals("chose"))
				{
					int itemId = 0;
					try
					{
						itemId = Integer.parseInt(st.nextToken());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					ItemInstance itemInstance = player.getInventory().getItemByObjectId(itemId);
					if (Auction.getInstance().allowedItem(itemInstance))
					{
						showWindow(player, Auction.getInstance().showChoseProductPage(player, itemInstance));
						
					}
					else
					{
						player.sendMessage("В вашем инвентаре нет этого предмета.");
						showWindow(player, Auction.getInstance().showCreateProductPage(player, 1));
					}
				}
				else if (currentCommand.equals("chose_accept"))
				{
					int itemId = 0;
					int price = 0;
					int priceItem = 0;
					try
					{
						itemId = Integer.parseInt(st.nextToken());
						price = Integer.parseInt(st.nextToken());
						StringBuffer str = new StringBuffer();
						while(st.hasMoreTokens())
						{
							str.append(st.nextToken());
							str.append(' ');
						}
						str.deleteCharAt(str.length()-1);
						priceItem = Auction.getInstance().getRewardId(str.toString());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					ItemInstance itemInstance = player.getInventory().getItemByObjectId(itemId);
					if (!Auction.getInstance().allowedItem(itemInstance))
					{
						player.sendMessage("В вашем инвентаре нет этого предмета.");
						showWindow(player, Auction.getInstance().showCreateProductPage(player, 1));
					}
					if (price <= 0)
					{
						player.sendMessage("Цена должна быть больше нуля.");
						showWindow(player, Auction.getInstance().showCreateProductPage(player, 1));
					}
					Auction.getInstance().choseAccept(player, itemInstance, priceItem, price);
					showWindow(player, Auction.getInstance().showHeadPage(player, page, type));		
				}
			}
		}
	}
	
}
