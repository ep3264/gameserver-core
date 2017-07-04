package com.l2je.extensions.casino;

import java.util.logging.Logger;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author dbg Official Website: http://l2je.com
 * @date 11 июн. 2017 г. 20:10:29
 */
public class HighMode extends Mode
{
	protected static final Logger _log = Logger.getLogger(HighMode.class.getName());
	
	@Override
	public void run(L2PcInstance player, int count)
	{
		ItemInstance item = player.getInventory().getItemByItemId(COL);
		if (item == null || item.getCount() < 1)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return;
		}
		if (!player.destroyItem("Roulette", item, count, player, true))
		{
			return;
		}
		int val = Rnd.get(1000);
		String description = "RouletteReward[mode:" + this + "]";
		
		if (val == 777)
		{
			_log.info(player.getClient().toString() + "earned AQ");
			Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", player.getName() + " Выиграл AQ"));
			player.addItem(description, 6660, 1, player, true);
			
		}
		// EW
		else if (val <= 150)
		{
			player.addItem(description, 6577, 1, player, true);
		}
		else if (val <= 300)
		{
			player.addItem(description, 6578, 1, player, true);
		}
		else if (val <= 400)// LS
		{
			player.addItem(description, 8732, 1, player, true);
		}
		else if (val <= 500)// mid ls
		{
			player.addItem(description, 8742, 1, player, true);
		}
		else if (val <= 575)// high ls
		{
			player.addItem(description, 8752, 1, player, true);
		}
		else if (val <= 625)// top ls
		{
			player.addItem(description, 8762, 1, player, true);
		}
		else if (val <= 775) // cp potion
		{
			player.addItem(description, 5592, 50, player, true);
		}
		else if (val <= 850)// bloody
		{
			player.addItem(description, BLODDY, 4, player, true);
		}
		else if (val <= 855)
		{// col
			player.addItem(description, COL, 1, player, true);
		}
		else if (val <= 862)
		{// Jester Hat
			player.addItem(description, 8562, 1, player, true);
		}
		else if (val <= 868) // mask
		{
			player.addItem(description, 8552, 1, player, true);
		}
		else if (val <= 950) // adena
		{
			player.addItem(description, 57, 100000000, player, true);
		}
		else if (val <= 1000)// loose
		{
			player.addItem(description, 57, 1, player, true);
		}
	}	
}
