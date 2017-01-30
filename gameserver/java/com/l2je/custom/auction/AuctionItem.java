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
		StringBuffer itemInfo = new StringBuffer();
		itemInfo.append("<table width=300><tr><td width=32><img src=\"");
		itemInfo.append(ItemIcons.getInstance().getIcon(this.item.getItemId()));
		itemInfo.append("\" width=32 height=32 align=left></td><td width=250><table width=250>");	 
		itemInfo.append("<tr><td>");
		if(head)
		{
			itemInfo.append("<a action=\"bypass -h npc_%objectId%_auction show ");
			itemInfo.append(item.getObjectId());
			itemInfo.append("\">");
			itemInfo.append(ItemTable.getInstance().getTemplate(this.item.getItemId()).getName());
			itemInfo.append("</a>");
		}
		else 
		{
			itemInfo.append(ItemTable.getInstance().getTemplate(item.getItemId()).getName());
		}
		if(item.getEnchantLevel() > 0 )
		{
			itemInfo.append("<font color=LEVEL> +");
			itemInfo.append(item.getEnchantLevel());
			itemInfo.append("</font>");
		}
		itemInfo.append("<font color=ff0000> Продавец: ");
		itemInfo.append(CharNameTable.getInstance().getPlayerName(Integer.valueOf(trader_objId)));
		itemInfo.append("</font>");
		itemInfo.append("</td></tr>");		
		if (head)
		{
			itemInfo.append("<tr><td><font color=603ca9>Цена:</font> <font color=3caa3c>");
			itemInfo.append(getPrice());
			itemInfo.append("</font></td></tr>");
		}
		itemInfo.append(Auction.getAugment(item));
		itemInfo.append("</table></td></tr></table>");
		return itemInfo.toString();
	}
	
	public String getAcceptPage(boolean owner)
	{
		StringBuffer stringBuffer= new StringBuffer(getItemInfo(false));		
		stringBuffer.append(getTextForAcceptPage(owner));
		stringBuffer.append(getButtonsForAcceptPage(owner));
		return stringBuffer.toString();
	}
	
	public String getButtonsForAcceptPage(boolean owner)
	{
		StringBuffer stringBuffer= new StringBuffer("<table width=290><tr><td width=270>");
		stringBuffer.append("<table width=290><tr>");
		if (!owner)
		{
			stringBuffer.append("<td align=center><button value=\"Купить\" action=\"bypass -h npc_%objectId%_auction accept_buy ");
			stringBuffer.append(item.getObjectId());
			stringBuffer.append("\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>");
		}
		else
		{
			stringBuffer.append("<td align=center><button value=\"Забрать\" action=\"bypass -h npc_%objectId%_auction accept_buy ");
			stringBuffer.append(item.getObjectId());
			stringBuffer.append("\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>");
		}		
		stringBuffer.append("<td align=center><button value=\"Назад\" action=\"bypass -h npc_%objectId%_auction page 1 0\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td></tr></table>");
		return stringBuffer.toString();
	}
	
	public String getTextForAcceptPage(boolean owner)
	{
		StringBuffer str = new StringBuffer("<table width=270><tr><td width=270>");
		if (!owner)
		{
			str.append("Вы уверены, что хотите купить этот предмет по цене <font color=LEVEL>");
			str.append(this.getPrice());
			str.append("</font>?");
		}
		else
		{
		str.append( "Это ваш предмет, в данный момент в продаже по цене <font color=LEVEL>");
		str.append(this.getPrice());
		str.append("</font>. Хотите снять его с продажи?");
		}
		
		str.append("</td></tr></table>");
		return str.toString();
	}
	
	// Вышло время
	public boolean isOverdue()
	{
		return this.getDeleteTime() < System.currentTimeMillis();
	}
	
	public long getDeleteTime()
	{
		return this.addTime + 86400000 * AuctionConfig.AUCTION_COUNT_DAY_FOR_DELETE_ITEM;
	}
	
	public String getPrice()
	{
		StringBuffer buffer = new StringBuffer(price);
		buffer.append(" ");
		buffer.append(ItemTable.getInstance().getTemplate(id_price).getName());
		return buffer.toString();
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
