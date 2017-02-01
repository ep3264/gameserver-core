package com.l2je.custom.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemIcons;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @className:custom.auction.Auction.java
 * @author evgeny64 Official Website: http://l2je.com
 * @date 24 янв. 2017 г. 19:59:42
 */
public class Auction
{
	private static class SingletonHolder
	{
		protected static final Auction _instance = new Auction();
	}
	
	public static final Auction getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected static final Logger _log = Logger.getLogger(Auction.class.getName());
	private ConcurrentHashMap<Integer, AuctionItem> _products = new ConcurrentHashMap<>();
	
	public Auction()
	{
	}
	public AuctionItem getItemById(int id)
	{
		return _products.get(id);
	}
	public void init()
	{
		AuctionInventory.getInstance().restore();
		restoreAuction();
	}
	
	public void enterTheGame(L2PcInstance player)
	{
		try
		{
			Iterator<AuctionItem> iterator = this._products.values().iterator();
			while (iterator.hasNext())
			{
				AuctionItem aItem = iterator.next();
				if (aItem.trader_objId == player.getObjectId() && aItem.alreadySell)
				{
					sendPayment(player, aItem);
				}
				else if (aItem.isOverdue())
				{
					refundItem(player, aItem);
				}
			}
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void refundItem(L2PcInstance player, AuctionItem item)
	{
		player.sendMessage("Один из ваших товаров на Аукционе не был продан за выделенный срок.");
		if (AuctionConfig.AUCTION_LOG)
		{
			_log.info("Auction: Item refund: " + item.trader_objId);
		}
		transferItem(AuctionInventory.getInstance(), player.getInventory(), item.getObjectId(), player);
		this.removeItem(item);
	}
	
	public String getMainPage(L2PcInstance player, int page, int type)
	{
		String html = HtmCache.getInstance().getHtm("data/html/mods/auction/Auction.htm");
		ArrayList<AuctionItem> auctionItems = getItems(player, type);
		html = html.replace("%products%", getPage(auctionItems, page));
		html = html.replace("%pages%", getPagesNum(player, page, type, auctionItems.size()));
		return html;
	}
	
	public String getItemInformationPage(L2PcInstance player, AuctionItem item)
	{
		String html = HtmCache.getInstance().getHtm("data/html/mods/auction/ShowItemInfo.htm");
		html = html.replace("%information%", item.getAcceptPage(player.getObjectId() == item.trader_objId));
		return html;
	}
	
	public String getCreateProductPage(L2PcInstance player, int page)
	{
		String html = HtmCache.getInstance().getHtm("data/html/mods/auction/CreateProduct.htm");
		html = html.replace("%page%", this.getPageAddProduct(player, page));
		return html;
	}
	
	public String getChoseProductPage(L2PcInstance player, ItemInstance item)
	{
		String html = HtmCache.getInstance().getHtm("data/html/mods/auction/ChoseProduct.htm");
		if (AuctionConfig.AUCTION_PERCENTAGE)
		{
			html = html.replace("%item_a%", String.valueOf(AuctionConfig.AUCTION_GET_PERCENT));
			html = html.replace("%price_a%", "% от стоимости предмета.");
			html = html.replace("%price%", String.valueOf(AuctionConfig.AUCTION_GET_PERCENT));
			html = html.replace("%item%", "% от стоимости предмета.");
		}
		else
		{
			html = html.replace("%item_a%", ItemTable.getInstance().getTemplate(AuctionConfig.AUCTION_AUGMENT_PRICE[0]).getName());
			html = html.replace("%price_a%", String.valueOf(AuctionConfig.AUCTION_AUGMENT_PRICE[1]));
			html = html.replace("%item%", ItemTable.getInstance().getTemplate(AuctionConfig.AUCTION_PRICE[0]).getName());
			html = html.replace("%price%", String.valueOf(AuctionConfig.AUCTION_PRICE[1]));
		}
		html = html.replace("%page%", getChoseAddProduct(item));
		return html;
	}
	public void acceptBuy(L2PcInstance player, int itemId)
	{
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
	public boolean allowedItem(ItemInstance item)
	{
		return item != null && (item.isSellable() || item.isAugmented() && AuctionConfig.ALLOW_AUGMENT_ITEMS) && !this._products.containsKey(item.getObjectId()) && !item.isStackable();
	}
	public int getRewardId(String name)
	{
		int[] arr = AuctionConfig.AUCTION_ALLOWED_ITEM_ID;
		for (int i = 0; i < arr.length; ++i)
		{
			int id = arr[i];
			if (ItemTable.getInstance().getTemplate(id).getName().equals(name))
			{
				return id;
			}
		}
		return 57;
	}
	private ArrayList<AuctionItem> getItems(L2PcInstance player, int type)
	{
		ArrayList<AuctionItem> auctionItems = new ArrayList<>();
		Iterator<AuctionItem> iterator = _products.values().iterator();
		while (iterator.hasNext())
		{
			AuctionItem auctionItem = iterator.next();
			if (auctionItem != null && checkItem(player, auctionItem, type))
			{
				auctionItems.add(auctionItem);
			}
		}
		return auctionItems;
	}

	private String getPage(ArrayList<AuctionItem> auctionItems, int page)
	{
		if (auctionItems.isEmpty())
		{
			return "Товары отсутствуют.";
		}
		StringBuffer stringBuffer =  new StringBuffer();
		String string="";
		try
		{
			if (this._products.size() <= AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE)
			{
				page = 1;
			}
			for (int i = (page-1) * AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE; i < page * AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE && i < auctionItems.size(); ++i)
			{
				AuctionItem auctionItem = auctionItems.get(i);
				stringBuffer.append(auctionItem.getItemInfo(true));
			}
			string = stringBuffer.toString();
			if(string.isEmpty())
			{
				return "Товары отсутствуют.";
			}
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return string;
	}
	
	private static String getPagesNum(L2PcInstance player, int page, int type, int countItems)
	{
		StringBuffer stringBuffer = new StringBuffer("<table width=270><tr><td align=center><table><tr>");
		int countPages = (int) Math.ceil((double) countItems / (double) AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE);
		
		for (int i = 1; i <= countPages; ++i)
		{
			if (i != page)
			{
				stringBuffer.append("<td width=15 align=center><a action=\"bypass -h npc_%objectId%_auction page ");
				stringBuffer.append(i);
				stringBuffer.append(" ");
				stringBuffer.append(type);
				stringBuffer.append("\">");
				stringBuffer.append(i);
				stringBuffer.append("</a></td>");
			}
			else
			{
				stringBuffer.append("<td width=15 align=center>");
				stringBuffer.append(i);
				stringBuffer.append("</td>");
			}
		}
		stringBuffer.append("</tr></table></td></tr></table>");
		return stringBuffer.toString();
	}
	
	private String getPageAddProduct(L2PcInstance player, int page)
	{	
		StringBuffer stringBuffer = new StringBuffer();
		String string= "";
		try
		{
			ArrayList<ItemInstance> quaItems = new ArrayList<>();
			List<ItemInstance> items = player.getInventory().getItems();			
			for (ItemInstance item : items)
			{
				if (this.allowedItem(item))
				{
					quaItems.add(item);
				}
			}			
			if (quaItems.size() <= AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE)
			{
				page = 1;
			}
			
			for (int i = (page -1) * AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE ; i < page * AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE && i < quaItems.size(); ++i)
			{
				ItemInstance itemInstance = quaItems.get(i);
				if (itemInstance != null)
				{
					stringBuffer.append(getItemInfoForAddProduct(itemInstance, true));
				}
			}
			stringBuffer.append(getPagesForAddProduct(quaItems.size(), page));
			string = stringBuffer.toString();
			if (string.isEmpty())
			{
				return "Товары отсутствуют.";
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return string;
	}
	
	private static String getItemInfoForAddProduct(ItemInstance item, boolean urlSell)
	{
		StringBuffer str = new StringBuffer("<table width=300><tr>");
		str.append("<td width=32><img src=\"");
		str.append(ItemIcons.getInstance().getIcon(item.getItemId()));
		str.append("\" width=32 height=32 align=left></td>");
		str.append( "<td width=250><table width=250>");
		str.append("<tr><td> ");
		str.append(item.getName());
		str.append(" "); 
		if(item.getEnchantLevel() > 0)
		{
			str.append("<font color=LEVEL> +");
			str.append(item.getEnchantLevel());
			str.append("</font>") ;
		}
		str.append("</td></tr>");
		str.append( getAugment(item));
		if (urlSell)
		{
			str.append("<tr><td><a action=\"bypass -h npc_%objectId%_auction chose ");
			str.append( item.getObjectId());
			str.append("\">Продать</a></td></tr>");
		}
		
		str.append("</table></td></tr></table>");
		return str.toString();
	}
	
	static String getAugment(ItemInstance item)
	{
		StringBuffer augmentInfo = new StringBuffer();
		if (AuctionConfig.ALLOW_AUGMENT_ITEMS)
		{
			if (item.getAugmentation() != null && item.getAugmentation().getSkill() != null)
			{
				L2Skill skill = item.getAugmentation().getSkill();
				String type;
				if (skill.isChance())
				{
					type = "Chance";
				}
				else if (skill.isActive())
				{
					type = "Active";
				}
				else
				{
					type = "Passive";
				}				
				augmentInfo.append("<tr><td><font color=603ca9>Аугмент:</font> <font color=3caa3c>");
				augmentInfo.append(skill.getName());
				augmentInfo.append(" - ");
				augmentInfo.append(skill.getLevel());
				augmentInfo.append(" level</font> <font color=00ff00>[");
				augmentInfo.append(type);
				augmentInfo.append( "]</font></td></tr>");
			}
			else
			{
				augmentInfo.append("<tr><td><font color=603ca9>Аугмент:</font> <font color=3caa3c>нет</font></td></tr>");
			}
		}
		
		return augmentInfo.toString();
	}
	
	private static String getPagesForAddProduct(int size, int page)
	{
		StringBuffer str =  new StringBuffer("<table width=270><tr><td align=center><table><tr>");
		
		for (int i = 1; i <= Math.ceil((double) size / (double) AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE); ++i)
		{
			if (i != page)
			{
				str.append( "<td width=12 align=center><a action=\"bypass -h npc_%objectId%_auction create_product ");
				str.append( i);
				str.append("\">");
				str.append(i);
				str.append("</a></td>");
			}
			else
			{
				str.append("<td width=12 align=center>");
				str.append(i );
				str.append("</td>");
			}
		}
		str.append( "</tr></table></td></tr></table>");
		return str.toString();
	}	

	private static String getChoseAddProduct(ItemInstance item)
	{
		StringBuffer str = new StringBuffer();
		str.append("<table width=250><tr><td width=40 align=right><img src=\"");		
		str.append( ItemIcons.getInstance().getIcon(item.getItemId()));
		str.append("\" width=32 height=32 align=right></td><td width=230><table width=230><tr><td> ");
		str.append(item.getName());
		str.append(" ");
		if(item.getEnchantLevel() > 0)
		{
			str.append("<font color=LEVEL> +");
			str.append( item.getEnchantLevel());
			str.append("</font>"); 		
		}
		str.append( "</td></tr>");
		str.append(getAugment(item));
		str.append( "</table></td></tr></table><br><table width=300><tr><tr><td align=\"right\">Валюта: </td><td align=\"left\"><combobox width=100 var=\"reward\" list=\"");
		str.append(getAvailablePrice());
		str.append("\"></td></tr><tr><td align=\"right\">Цена: </td><td align=\"left\"><edit var=\"count\" width=100 height=10></td></tr></tr></table><br><table width=300><tr><td align=center width=132><button value=\"Выставить на продажу\" action=\"bypass -h npc_%objectId%_auction chose_accept ");
		str.append(item.getObjectId());
		str.append(" $count $reward\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>");
		str.append("<td align=center width=132><button value=\"Отказаться\" action=\"bypass -h npc_%objectId%_auction page 1 0\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td></tr></table>");
		return str.toString();
	}
	
	private static String getAvailablePrice()
	{
		StringBuffer rewards = new StringBuffer();
		int[] arr = AuctionConfig.AUCTION_ALLOWED_ITEM_ID;
		
		for (int i = 0; i < arr.length; ++i)
		{
			int id = arr[i];
			if (i == 0)
			{
				rewards.append(ItemTable.getInstance().getTemplate(id).getName());
			}
			else
			{
				StringUtil.append(rewards, ";", ItemTable.getInstance().getTemplate(id).getName());
			}
		}
		
		return rewards.toString();
	}
	
	private static boolean checkItem(L2PcInstance player, AuctionItem item, int type)
	{
		boolean ok = !item.alreadySell;
		if (ok)
		{
			Item itm = ItemTable.getInstance().getTemplate(item.getId());
			ok = ok && itm != null;
			if (ok)
			{
				switch (type)
				{
					case 1:
						ok = itm.getType2() == 0;
						break;
					case 2:
						ok = itm.getType2() == 2 || itm.getType2() == 1;
						break;
					case 3:
						ok = item.trader_objId == player.getObjectId();
				}
			}
		}
		
		return ok;
	}
	
	private void restoreAuction()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM `item_auction`");
			rset = statement.executeQuery();
			
			int count;
			for (count = 0; rset.next(); ++count)
			{
				int trader_objId = rset.getInt("trader_objId");
				int objId = rset.getInt("objId");
				int id_price = rset.getInt("id_price");
				int price = rset.getInt("price");
				long addTime = rset.getLong("addTime");
				boolean alreadySell = rset.getBoolean("alreadySell");
				AuctionItem item = new AuctionItem(trader_objId, id_price, price, addTime, objId, alreadySell);
				this.storeItem(item, false);
			}
			
			rset.close();
			statement.close();
			_log.info("Auction: Loaded " + count + " items in Auction.");
		}
		catch (Exception e)
		{
			_log.info("Auction: Could not restore Auction items. Error: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void storeItem(AuctionItem item, boolean insertToBD) throws SQLException
	{
		this._products.put(Integer.valueOf(item.getObjectId()), item);
		Connection con = null;
		PreparedStatement statement = null;
		if (insertToBD)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("INSERT INTO `item_auction` (objId, trader_objId, id_price, price, addTime, alreadySell) values(?,?,?,?,?,?)");
				statement.setInt(1, item.getObjectId());
				statement.setInt(2, item.trader_objId);
				statement.setInt(3, item.id_price);
				statement.setInt(4, item.price);
				statement.setLong(5, item.addTime);
				statement.setBoolean(6, item.alreadySell);
				statement.execute();
				statement.close();
			}
			catch (Exception ex)
			{
				_log.info("Auction: Could not store Item: " + ex.getLocalizedMessage());
				ex.printStackTrace();
			}
			finally
			{
				con.close();
			}
		}
		
	}
	
	private void removeItem(AuctionItem item)
	{
		try
		{
			Connection con = null;
			PreparedStatement statement = null;
			boolean log = true;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM `item_auction` where objId = ?");
				statement.setInt(1, item.getObjectId());
				statement.execute();
				statement.close();
				if (log)
				{
					_log.info("Auction: Item removed: " + item.getObjectId());
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				_log.info("Auction: Could remove Item: " + ex.getLocalizedMessage());
			}
			finally
			{
				con.close();
			}
			
			this._products.remove(Integer.valueOf(item.getObjectId()));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	private static boolean payAuctioneer(L2PcInstance player, ItemInstance item, int id_price, int price)
	{
		int auction_priceId;
		int auction_priceCount;
		if (AuctionConfig.AUCTION_PERCENTAGE)
		{
			auction_priceId = id_price;
			auction_priceCount = price / 100 * AuctionConfig.AUCTION_GET_PERCENT;
		}
		else
		{
			auction_priceId = item.isAugmented() ? AuctionConfig.AUCTION_AUGMENT_PRICE[0] : AuctionConfig.AUCTION_PRICE[0];
			auction_priceCount = item.isAugmented() ? AuctionConfig.AUCTION_AUGMENT_PRICE[1] : AuctionConfig.AUCTION_PRICE[1];
		}
		if (!haveCountItem(player, auction_priceId, auction_priceCount))
		{
			player.sendMessage("У вас недостаточно средств для оплаты аукциона.");
			return false;
		}
		if (player.destroyItemByItemId("Auction add product.", auction_priceId, auction_priceCount, (L2Object) null, true))
		{
			return true;
		}
		return false;
	}
	public void choseAccept(L2PcInstance player, ItemInstance item, int id_price, int price)
	{
		if(payAuctioneer(player,item,id_price,price))	
		{		
			AuctionItem sellitem = new AuctionItem(player.getObjectId(), id_price, price, System.currentTimeMillis(), item);
			transferItem(player.getInventory(), AuctionInventory.getInstance(), item.getObjectId(), player);
			try
			{
				storeItem(sellitem, true);
			}
			catch (SQLException e)
			{				
				e.printStackTrace();
			}
			StringBuffer message = new StringBuffer();
			StringUtil.append(message, "Auctioneer: ",item.getName()," выставлен на продажу!");
			Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", message.toString()));
		}
	}
	
	private static ItemInstance transferItem(ItemContainer src, ItemContainer dst, int objId, L2PcInstance player)
	{
		ItemInstance item = src.getItemByObjectId(objId);
		if (item != null)
		{
			src.transferItem("Auction", objId, item.getCount(), dst, player, (L2Object) null);
			InventoryUpdate iu = new InventoryUpdate();
			SystemMessage sm;
			if (src.getOwnerId() == player.getObjectId())
			{
				iu.addRemovedItem(item);
				sm = (SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1)).addItemName(item);
			}
			else
			{
				iu.addNewItem(item);
				sm = (SystemMessage.getSystemMessage(SystemMessageId.OBTAINED_S1_S2)).addNumber(item.getCount()).addItemName(item);
			}
			
			player.sendPacket(iu);
			player.sendPacket(sm);
		}
		
		return item;
	}
	
	private static boolean haveCountItem(L2PcInstance player, int id, int count)
	{
		return getItemCount(player, id) >= count;
	}
	
	private static int getItemCount(L2PcInstance player, int item_id)
	{
		return player.getInventory().getItemByItemId(item_id) != null ? player.getInventory().getItemByItemId(item_id).getCount() : 0;
	}
	
	private void buyItem(L2PcInstance player, AuctionItem item)
	{
		if (!haveCountItem(player, item.id_price, item.price))
		{
			player.sendMessage("У вас недостаточно средств для оплаты товара.");
		}
		else
		{
			try
			{
				player.destroyItemByItemId("Auction buy item.", item.id_price, item.price, (L2Object) null, true);
				transferItem(AuctionInventory.getInstance(), player.getInventory(), item.getObjectId(), player);
				sendPayment(item);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
		}
	}
	
	private void sendPayment(L2PcInstance player, AuctionItem item)
	{
		player.sendMessage("Один из ваших товаров на Аукционе был продан.");
		if (AuctionConfig.AUCTION_LOG)
		{
			_log.info("Auction: Item sold: " + item.trader_objId);
		}
		
		player.addItem("Auction payment.", item.id_price, item.price, (L2Object) null, true);
		this.removeItem(item);
	}
	
	private void sendPayment(AuctionItem item)
	{
		L2PcInstance player = L2World.getInstance().getPlayer(item.trader_objId);
		if (player == null)
		{
			setSendPayment(item, true);
		}
		else
		{
			sendPayment(player, item);
		}
		
	}
	
	private static void setSendPayment(AuctionItem item, boolean insertToBD)
	{
		Connection con = null;
		if (insertToBD)
		{
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement e = con.prepareStatement("UPDATE `item_auction` set `alreadySell` = ? where objId = ?");
				e.setBoolean(1, true);
				e.setInt(2, item.getObjectId());
				e.execute();
				e.close();
				if (AuctionConfig.AUCTION_LOG)
				{
					_log.info("Auction: Item payment=true: " + item.getObjectId());
				}
			}
			catch (Exception ex)
			{
				_log.info("Auction: Could update Item: " + ex.getLocalizedMessage());
				ex.printStackTrace();
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		item.alreadySell = true;
	}
	

	
	private void removeFromSale(L2PcInstance player, AuctionItem item)
	{
		transferItem(AuctionInventory.getInstance(), player.getInventory(), item.getObjectId(), player);
		this.removeItem(item);
		if (AuctionConfig.AUCTION_LOG)
		{
			_log.info("Auction: remove from sale item objId: " + item.getId() + " player objId: " + player.getObjectId());
		}
	}
}
