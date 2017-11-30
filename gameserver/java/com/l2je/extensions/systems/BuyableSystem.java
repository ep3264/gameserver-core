package com.l2je.extensions.systems;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author HandOfGod
 * @date Nov 29, 2017 12:56:11 AM 
 */
public abstract class BuyableSystem
{
	protected boolean buy(L2PcInstance player, final int itemId, final int itemCount, final String desctription)
	{
		ItemInstance item = player.getInventory().getItemByItemId(itemId);
		if (item == null || item.getCount() < itemCount)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return false;
		}
		if (!player.destroyItem(desctription, item, itemCount, player, true))
		{
			return false;
		}
		return true;
	}
}
