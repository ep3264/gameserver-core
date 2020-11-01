package net.sf.l2j.gameserver.model.actor.instance;

import com.l2je.extensions.auction.Auction;
import com.l2je.extensions.auction.AuctionConfig;
import com.l2je.extensions.auction.AuctionItem;

import java.util.StringTokenizer;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;


/**
 * @author dbg Official Website: http://l2je.com
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
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		if (AuctionConfig.AUCTION_ENABLE)
		{
			showWindow(player, Auction.getInstance().getMainPage(player, 1, 0));
		}
		else
		{
			String html = HtmCache.getInstance().getHtm("data/html/mods/auction/Disable.htm");
			showWindow(player, html);
		}
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
					showWindow(player, Auction.getInstance().getCreateProductPage(player, page));
					
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
					showWindow(player, Auction.getInstance().getMainPage(player, page, type));
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
					AuctionItem auctionItem = Auction.getInstance().getItemById(itemId);
					if (auctionItem != null)
					{
						showWindow(player, Auction.getInstance().getItemInformationPage(player, auctionItem));
					}
					else
					{
						player.sendMessage("Такой предмет на аукционе отсутствует.");
						showWindow(player, Auction.getInstance().getCreateProductPage(player, 1));
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
					Auction.getInstance().acceptBuy(player, itemId);
					
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
						showWindow(player, Auction.getInstance().getChoseProductPage(player, itemInstance));
						
					}
					else
					{
						player.sendMessage("В вашем инвентаре нет этого предмета.");
						showWindow(player, Auction.getInstance().getCreateProductPage(player, 1));
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
						StringBuilder str = new StringBuilder();
						while (st.hasMoreTokens())
						{
							str.append(st.nextToken());
							str.append(' ');
						}
						str.deleteCharAt(str.length() - 1);
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
						showWindow(player, Auction.getInstance().getCreateProductPage(player, 1));
					}
					if (price <= 0)
					{
						player.sendMessage("Цена должна быть больше нуля.");
						showWindow(player, Auction.getInstance().getCreateProductPage(player, 1));
						return;
					}
					Auction.getInstance().choseAccept(player, itemInstance, priceItem, price);
					showWindow(player, Auction.getInstance().getMainPage(player, page, type));
				}
			}
		}
	}
	
}
