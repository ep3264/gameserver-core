package com.l2je.extensions.casino;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author dbg Official Website: http://l2je.com
 * @date 11 июн. 2017 г. 20:10:07
 */
public class MiddleMode extends Mode
{
	/*
	 * (non-Javadoc)
	 * @see com.l2je.extensions.casino.Mode#go(net.sf.l2j.gameserver.model.actor. instance.L2PcInstance)
	 */
	@Override
	public void run(L2PcInstance player, int count)
	{
		ItemInstance item = player.getInventory().getItemByItemId(BLODDY);
		if (item == null || item.getCount() < 1)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return;
		}
		if (!player.destroyItem("Roulette", item, count, player, true))
		{
			return;
		}
		int chance = Rnd.get(1000);
		String description = "RouletteReward[mode:" + this + "]";
		// EW
		if (chance <= 150)
		{
			player.addItem(description, 6577, 1, player, true);
		}
		else if (chance <= 300)
		{
			player.addItem(description, 6578, 1, player, true);
		}
		else if (chance <= 400)
		{
			player.addItem(description, 8732, 1, player, true);
		}
		else if (chance <= 450)
		{
			player.addItem(description, 8742, 1, player, true);
		}
		else if (chance <= 475)
		{
			player.addItem(description, 8752, 1, player, true);
		}
		else if (chance <= 485)
		{
			player.addItem(description, 8762, 1, player, true);
		}
		else if (chance <= 600)// cp potion
		{
			player.addItem(description, 5592, 15, player, true);
		}
		else if (chance <= 650)// bloody
		{
			player.addItem(description, BLODDY, 1, player, true);
		}
		else if (chance <= 654)
		{// col
			player.addItem(description, COL, 1, player, true);
		}
		else if (chance <= 657)
		{// Jester Hat
			player.addItem(description, 8562, 1, player, true);
		}
		else if (chance <= 1000)
		{// loose
			player.addItem(description, 57, 1, player, true);
		}
		
	}
	
}
