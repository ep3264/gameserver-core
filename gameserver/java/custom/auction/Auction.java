package custom.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
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
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

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
	public ConcurrentHashMap<Integer, AuctionItem> _products = new ConcurrentHashMap<>();
	
	
	public Auction()
	{
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
					AuctionItem aItem =  iterator.next();
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

	public String showHeadPage(L2PcInstance player, int page, int type)
	{
		String html = HtmCache.getInstance().getHtm("data/html/mods/auction/Auction.htm");
		html = html.replace("%products%",getPage(player, page, type));
		html = html.replace("%pages%", getPages(player, page, type));
		return html;
	}
	
	public String showItem(L2PcInstance player, AuctionItem item)
	{
		String html = HtmCache.getInstance().getHtm("data/html/mods/auction/ShowItemInfo.htm");
		html = html.replace("%information%", item.getAcceptPage(player.getObjectId() == item.trader_objId));
		return html;
	}
	
	public String showCreateProductPage(L2PcInstance player, int page)
	{
		String html = HtmCache.getInstance().getHtm("data/html/mods/auction/CreateProduct.htm");
		html = html.replace("%page%", this.getPageAddProduct(player, page));
		return html;
	}
	
	public String showChoseProductPage(L2PcInstance player, ItemInstance item)
	{
		String html = HtmCache.getInstance().getHtm("data/html/mods/auction/ChoseProduct.htm");
		if (AuctionConfig.AUCTION_PERCENTAGE)
		{			
			html = html.replace("%item_a%",  String.valueOf(AuctionConfig.AUCTION_GET_PERCENT));
			html = html.replace("%price_a%","% от стоимости предмета.");
			html = html.replace("%price%",  String.valueOf(AuctionConfig.AUCTION_GET_PERCENT));
			html = html.replace("%item%", "% от стоимости предмета.");
		}
		else
		{
			html = html.replace("%item_a%", ItemTable.getInstance().getTemplate(AuctionConfig.AUCTION_AUGMENT_PRICE[0]).getName());
			html = html.replace("%price_a%", String.valueOf(AuctionConfig.AUCTION_AUGMENT_PRICE[1]));
			html = html.replace("%item%",  ItemTable.getInstance().getTemplate(AuctionConfig.AUCTION_PRICE[0]).getName());
			html = html.replace("%price%", String.valueOf(AuctionConfig.AUCTION_PRICE[1]));
		}
		html = html.replace("%page%", this.getChoseAddProduct(item));
		return html;
	}
	
	public String getPage(L2PcInstance player, int page, int type)
	{
		String str = "";
		try
		{
			if (this._products.size() <= AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE)
			{
				page = 1;
			}
			LinkedList<AuctionItem> auctionItems = new LinkedList<>();
			Iterator<AuctionItem> iterator = this._products.values().iterator();
			while (iterator.hasNext())
			{
				AuctionItem auctionItem = iterator.next();
				if (auctionItem != null && this.checkItem(player, auctionItem, type))
				{
					auctionItems.add(auctionItem);
				}
			}
			for (int i = page * AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE - AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE; i < page * AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE && i < auctionItems.size(); ++i)
			{
				AuctionItem auctionItem = auctionItems.get(i);
				str = str + auctionItem.getItemInfo(true);
			}
			if (str.isEmpty())
			{
				str = "Товары отсутствуют.";
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return str;
	}
	
	public String getPages(L2PcInstance player, int page, int type)
	{
		String str = "<table width=270><tr><td align=center><table><tr>";
		int count = 0;
		Iterator<AuctionItem> start = this._products.values().iterator();
		while (start.hasNext())
		{
			AuctionItem maxPage = start.next();
			if (this.checkItem(player, maxPage, type))
			{
				++count;
			}
		}
		int var9 = page - 3 > 0 ? page - 3 : 1;
		int var10 = (int) Math.ceil((double) count / (double) AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE);
		
		for (int i = var9; i <= var10 && i <= page + 3; ++i)
		{
			if (i != page)
			{
				if (i < page + 3)
				{
					str = str + "<td width=15 align=center><a action=\"bypass -h npc_%objectId%_auction page " + i + " " + type + "\">" + i + "</a></td>";
				}
				else if (i < var10)
				{
					if (i < var10 - 1)
					{
						str = str + "<td width=15 align=center><a action=\"bypass -h npc_%objectId%_auction page " + (var10 - 1) + " " + type + "\">" + (var10 - 1) + "</a></td>";
					}
					
					str = str + "<td width=15 align=center><a action=\"bypass -h npc_%objectId%_auction page " + var10 + " " + type + "\">" + var10 + "</a></td>";
				}
			}
			else
			{
				str = str + "<td width=15 align=center>" + i + "</td>";
			}
		}
		
		str = str + "</tr></table></td></tr></table>";
		return str;
	}
	
	public String getPageAddProduct(L2PcInstance player, int page)
	{
		String str = "";
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
			
			for (int i = page * AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE - AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE; i < page * AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE && i < quaItems.size(); ++i)
			{
				ItemInstance itemInstance = quaItems.get(i);
				if (itemInstance != null)
				{
					str = str + this.getItemInfoForAddProduct(itemInstance, true);
				}
			}
			str = str + this.getPagesForAddProduct(quaItems.size(), page);
			if (str.isEmpty())
			{
				str = "Товары отсутствуют.";
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return str;
	}
	
	public String getItemInfoForAddProduct(ItemInstance item, boolean urlBuy)
	{
		String str = "<table width=300><tr>";
		str = str + "<td width=32><img src=\"" + ItemIcons.getInstance().getIcon(item.getItemId()) + "\" width=32 height=32 align=left></td>";
		str = str + "<td width=250><table width=250>";
		str = str + "<tr><td> " + item.getName() + " " + (item.getEnchantLevel() > 0 ? "<font color=LEVEL> +" + item.getEnchantLevel() + "</font>" : "") + "</td></tr>";
		str = str + getAugment(item);
		if (urlBuy)
		{
			str = str + "<tr><td><a action=\"bypass -h npc_%objectId%_auction chose " + item.getObjectId() + "\">Продать</a></td></tr>";
		}
		
		str = str + "</table></td></tr></table>";
		return str;
	}
	
	public static String getAugment(ItemInstance item)
	{
		String augmentInfo = "";
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
				
				augmentInfo = "<tr><td><font color=603ca9>Аугмент:</font> <font color=3caa3c>" + skill.getName() + " - " + skill.getLevel() + " level</font> <font color=00ff00>[" + type + "]</font></td></tr>";
			}
			else
			{
				augmentInfo = "<tr><td><font color=603ca9>Аугмент:</font> <font color=3caa3c>нет</font></td></tr>";
			}
		}
		
		return augmentInfo;
	}
	
	public String getPagesForAddProduct(int size, int page)
	{
		String str = "<table width=270><tr><td align=center><table><tr>";
		
		for (int i = 1; i <= Math.ceil((double) size / (double) AuctionConfig.AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE); ++i)
		{
			if (i != page)
			{
				str = str + "<td width=12 align=center><a action=\"bypass -h npc_%objectId%_auction create_product " + i + "\">" + i + "</a></td>";
			}
			else
			{
				str = str + "<td width=12 align=center>" + i + "</td>";
			}
		}
		str = str + "</tr></table></td></tr></table>";
		return str;
	}
	
	public boolean allowedItem(ItemInstance item)
	{
		 return item != null && (item.isSellable() || item.isAugmented() && AuctionConfig.ALLOW_AUGMENT_ITEMS) && !this._products.containsKey(item.getObjectId()) && !item.isStackable();
	}
	
	public String getChoseAddProduct(ItemInstance item)
	{
		String str = "";
		str = str + "<table width=250><tr>";
		str = str + "<td width=40 align=right><img src=\"" + ItemIcons.getInstance().getIcon(item.getItemId()) + "\" width=32 height=32 align=right></td>";
		str = str + "<td width=230><table width=230><tr><td> " + item.getName() + " " + (item.getEnchantLevel() > 0 ? "<font color=LEVEL> +" + item.getEnchantLevel() + "</font>" : "") + "</td></tr>";
		str = str + getAugment(item);
		str = str + "</table></td></tr></table><br>";
		str = str + "<table width=300><tr>";
		str = str + "<tr><td align=\"right\">Валюта: </td><td align=\"left\"><combobox width=100 var=\"reward\" list=\"" + this.getAvailablePrice() + "\"></td></tr>";
		str = str + "<tr><td align=\"right\">Цена: </td><td align=\"left\"><edit var=\"count\" width=100 height=10></td></tr>";
		str = str + "</tr></table><br>";
		str = str + "<table width=300><tr>";
		str = str + "<td align=center width=132><button value=\"Выставить на продажу\" action=\"bypass -h npc_%objectId%_auction chose_accept " + item.getObjectId() + " $count $reward\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>";
		str = str + "<td align=center width=132><button value=\"Отказаться\" action=\"bypass -h Quest Auction_Ro0TT page 1\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>";
		str = str + "</tr></table>";
		return str;
	}
	
	public String getAvailablePrice()
	{
		String rewards = "";
		int[] arr = AuctionConfig.AUCTION_ALLOWED_ITEM_ID;
		
		for (int i = 0; i < arr.length; ++i)
		{
			int id = arr[i];
			if (rewards.isEmpty())
			{
				rewards = rewards + ItemTable.getInstance().getTemplate(id).getName();
			}
			else
			{
				rewards = rewards + ";" + ItemTable.getInstance().getTemplate(id).getName();
			}
		}
		
		return rewards;
	}
	
	public boolean checkItem(L2PcInstance player, AuctionItem item, int type)
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
	
	public void restoreAuction()
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
	
	public void choseAccept(L2PcInstance player, ItemInstance item, int id_price, int price)
	{
		AuctionItem sellitem = new AuctionItem(player.getObjectId(), id_price, price, System.currentTimeMillis(), item);
		transferItem(player.getInventory(), AuctionInventory.getInstance(), item.getObjectId(), player);
		try
		{
			this.storeItem(sellitem, true);
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		
		player.destroyItemByItemId("Auction add product.", auction_priceId, auction_priceCount, (L2Object) null, true);
	}
	
	public static ItemInstance transferItem(ItemContainer src, ItemContainer dst, int objId, L2PcInstance player)
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
	
	public static boolean haveCountItem(L2PcInstance player, int id, int count)
	{
		return getItemCount(player, id) >= count;
	}
	
	public static int getItemCount(L2PcInstance player, int item_id)
	{
		return player.getInventory().getItemByItemId(item_id) != null ? player.getInventory().getItemByItemId(item_id).getCount() : 0;
	}
	
	public void buyItem(L2PcInstance player, AuctionItem item)
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
	
	public void sendPayment(L2PcInstance player, AuctionItem item)
	{
		player.sendMessage("Один из ваших товаров на Аукционе был продан.");
		if (AuctionConfig.AUCTION_LOG)
		{
			_log.info("Auction: Item sold: " + item.trader_objId);
		}
		
		player.addItem("Auction payment.", item.id_price, item.price, (L2Object) null, true);
		this.removeItem(item);
	}
	
	public void sendPayment(AuctionItem item)
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		item.alreadySell = true;
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
	public void removeFromSale(L2PcInstance player, AuctionItem item)
	{
		transferItem(AuctionInventory.getInstance(), player.getInventory(), item.getObjectId(), player);
		this.removeItem(item);
		if (AuctionConfig.AUCTION_LOG)
		{
			_log.info("Auction: remove from sale item objId: " + item.getId() + " player objId: " + player.getObjectId());
		}
	}
}
