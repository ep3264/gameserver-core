package com.l2je.custom.auction;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.item.instance.ItemIcons;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * @className:custom.auction.AuctionItem.java
 * @author evgeny64 Official Website: http://l2je.com
 * @date 26 янв. 2017 г. 19:56:07
 */
public class AuctionItem
{
	protected static final Logger _log = Logger.getLogger(AuctionItem.class.getName());
	public final int trader_objId;
	public final int id_price;
	public final int price;
	public final long addTime;
	private final int objId;
	private final ItemInstance item;
	public boolean alreadySell = false;
	
	public AuctionItem(int trader_objId, int id_price, int price, long addTime, ItemInstance item)
	{
		this.trader_objId = trader_objId;
		this.id_price = id_price;
		this.price = price;
		this.addTime = addTime;
		this.objId = item.getObjectId();
		this.item = item;
	}
	
	public AuctionItem(int trader_objId, int id_price, int price, long addTime, int objId, boolean alreadySell)
	{
		this.trader_objId = trader_objId;
		this.id_price = id_price;
		this.price = price;
		this.addTime = addTime;
		this.alreadySell = alreadySell;
		this.objId = objId;
		this.item = AuctionInventory.getInstance().getItemByObjectId(objId);
		if (this.item == null && !alreadySell)
		{
			_log.info("Auction: item in Auction, with trader_objid = " + trader_objId + " is null.");
		}
	}
	
	public String getItemInfo(boolean head)
	{
		String str = "<table width=300><tr><td width=32><img src=\"" + ItemIcons.getInstance().getIcon(this.item.getItemId()) + "\" width=32 height=32 align=left></td>" + "<td width=250>" + "<table width=250>";
		String itemName = (head ? "<a action=\"bypass -h npc_%objectId%_auction show " + this.item.getObjectId() + "\">" + ItemTable.getInstance().getTemplate(this.item.getItemId()).getName() + "</a>" : ItemTable.getInstance().getTemplate(this.item.getItemId()).getName()) + " ";
		String enchant = this.item.getEnchantLevel() > 0 ? "<font color=LEVEL> +" + this.item.getEnchantLevel() + "</font>" : "";
		String name = "<font color=ff0000> Продавец: " + CharNameTable.getInstance().getPlayerName(Integer.valueOf(this.trader_objId)) + "</font>";
		str = str + "<tr><td>" + itemName + enchant + name + "</td></tr>";
		if (head)
		{
			str = str + "<tr><td><font color=603ca9>Цена:</font> <font color=3caa3c>" + this.getPrice() + "</font></td></tr>";
		}
		str = str + Auction.getAugment(this.item);
		str = str + "</table></td></tr></table>";
		return str;
	}
	
	public String getAcceptPage(boolean me)
	{
		String str = this.getItemInfo(false);
		str = str + this.getTextForAcceptPage(me);
		str = str + this.getButtonsForAcceptPage(me);
		return str;
	}
	
	public String getButtonsForAcceptPage(boolean owner)
	{
		String str = "<table width=290><tr><td width=270>";
		str = str + "<table width=290><tr>";
		if (!owner)
		{
			str = str + "<td align=center><button value=\"Купить\" action=\"bypass -h npc_%objectId%_auction accept_buy " + this.item.getObjectId() + "\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>";
		}
		else
		{
			str = str + "<td align=center><button value=\"Забрать\" action=\"bypass -h npc_%objectId%_auction accept_buy " + this.item.getObjectId() + "\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>";
		}
		
		str = str + "<td align=center><button value=\"Назад\" action=\"bypass -h npc_%objectId%_auction page 1 0\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>";
		str = str + "</tr></table>";
		return str;
	}
	
	public String getTextForAcceptPage(boolean me)
	{
		String str = "<table width=270><tr><td width=270>";
		if (!me)
		{
			str = str + "Вы уверены, что хотите купить этот предмет по цене <font color=LEVEL>" + this.getPrice() + "</font>?";
		}
		else
		{
			str = str + "Это ваш предмет, в данный момент в продаже по цене <font color=LEVEL>" + this.getPrice() + "</font>. Хотите снять его с продажи?";
		}
		
		str = str + "</td></tr></table>";
		return str;
	}
	
	// Вышло время
	public boolean isOverdue()
	{
		return this.getDeleteTime() < System.currentTimeMillis();
	}
	
	public long getDeleteTime()
	{
		return this.addTime + (long) (86400000 * AuctionConfig.AUCTION_COUNT_DAY_FOR_DELETE_ITEM);
	}
	
	public String getPrice()
	{
		return this.price + " " + ItemTable.getInstance().getTemplate(this.id_price).getName();
	}
	
	public int getId()
	{
		return this.item == null ? 0 : this.item.getItemId();
	}
	
	public int getObjectId()
	{
		return this.objId;
	}
}
