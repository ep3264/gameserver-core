package com.l2je.extensions.auction;

import java.util.logging.Logger;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.item.instance.ItemIcons;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * @className:custom.auction.AuctionItem.java
 * @author dbg Official Website: http://l2je.com
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
	
	public String getAcceptPage(boolean owner)
	{
		StringBuilder sb = new StringBuilder();
		getItemInfo(sb, false);
		getTextForAcceptPage(sb, owner);
		getButtonsForAcceptPage(sb,owner);
		return sb.toString();
	}
	
	public void getItemInfo(StringBuilder sb, boolean head)
	{
		StringUtil.append(sb, "<table width=300><tr><td width=32><img src=\"", ItemIcons.getInstance().getIcon(this.item.getItemId()), "\" width=32 height=32 align=left></td><td width=250><table width=250><tr><td>");
		if (head)
		{
			StringUtil.append(sb, "<a action=\"bypass -h npc_%objectId%_auction show ", item.getObjectId(), "\">", ItemTable.getInstance().getTemplate(this.item.getItemId()).getName(), "</a>");
		}
		else
		{
			sb.append(ItemTable.getInstance().getTemplate(item.getItemId()).getName());
		}
		if (item.getEnchantLevel() > 0)
		{
			StringUtil.append(sb, "<font color=LEVEL> +", item.getEnchantLevel(), "</font>");
		}
		StringUtil.append(sb, "<font color=ff0000> Продавец: ", CharNameTable.getInstance().getPlayerName(Integer.valueOf(trader_objId)), "</font></td></tr>");
		if (head)
		{
			sb.append("<tr><td><font color=603ca9>Цена:</font> <font color=3caa3c>");
			getPrice(sb);
			sb.append("</font></td></tr>");
		}
		 Auction.getAugment(sb, item);
		 sb.append("</table></td></tr></table>");
	}
	
	public void getTextForAcceptPage(StringBuilder sb, boolean owner)
	{
		sb.append("<table width=270><tr><td width=270>");
		if (!owner)
		{
			sb.append("Вы уверены, что хотите купить этот предмет по цене <font color=LEVEL>");
			getPrice(sb);
			sb.append("</font>?");
		}
		else
		{
			sb.append("Это ваш предмет, в данный момент в продаже по цене <font color=LEVEL>");
			getPrice(sb);
			sb.append("</font>. Хотите снять его с продажи?");
		}
		
		sb.append("</td></tr></table>");
	}
	
	public void getButtonsForAcceptPage(StringBuilder sb, boolean owner)
	{		
		StringUtil.append(sb, "<table width=290><tr><td width=270><table width=290><tr>");
		if (!owner)
		{
			StringUtil.append(sb, "<td align=center><button value=\"Купить\" action=\"bypass -h npc_%objectId%_auction accept_buy ",
				item.getObjectId(),
				"\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>");
		}
		else
		{
			StringUtil.append(sb, "<td align=center><button value=\"Забрать\" action=\"bypass -h npc_%objectId%_auction accept_buy ",
				item.getObjectId(),
				"\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td>");
		}
		sb.append("<td align=center><button value=\"Назад\" action=\"bypass -h npc_%objectId%_auction page 1 0\" width=135 height=24 back=\"L2UI_CH3.bigbutton3_down\" fore=\"L2UI_CH3.bigbutton3\"></td></tr></table>");
	}
	
	/**
	 *
	 * @return  Вышло ли врямя отведенное на продажу
	 */
	public boolean isOverdue()
	{
		return this.getDeleteTime() < System.currentTimeMillis();
	}
	
	public long getDeleteTime()
	{
		return this.addTime + 86400000L * ((AuctionConfig.AUCTION_COUNT_DAY_FOR_DELETE_ITEM > 366) ? 366L : AuctionConfig.AUCTION_COUNT_DAY_FOR_DELETE_ITEM);
	}
	
	public void getPrice(StringBuilder sb)
	{		
		StringUtil.append(sb, price, " ", ItemTable.getInstance().getTemplate(id_price).getName());
	}
	
	public int getId()
	{
		return this.item == null ? 0 : this.item.getItemId();
	}
	
	public int getObjectId()
	{
		return objId;
	}
}
