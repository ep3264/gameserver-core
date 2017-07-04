package com.l2je.extensions.casino;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author dbg Official Website: http://l2je.com
 * @date 11 июн. 2017 г. 20:07:07
 */
public class LowMode extends Mode
{
	@Override
	public void run(L2PcInstance player, int count)
	{
		if (!player.reduceAdena("Roulette", PRICE_ADENA * count, player.getCurrentFolkNPC(), true))
		{
			return;
		}
		int val = Rnd.get(1000);
		String description = "RouletteReward[mode:" + this + "]";
		// EW
		if (val <= 250)
		{
			player.addItem(description, 6577, 1, player, true);
		}
		else if (val <= 500)
		{
			player.addItem(description, 6578, 1, player, true);
		}
		else if (val <= 600)
		{ // cp potion
			player.addItem(description, 5592, 10, player, true);
		}
		else if (val <= 700)
		{// adena
			player.addItem(description, 57, 100000000, player, true);
		}
		else if (val <= 730)
		{// bloody
			player.addItem(description, BLODDY, 1, player, true);
		}
		else if (val == 731)
		{// col
			player.addItem(description, COL, 1, player, true);
		}
		else if (val <= 1000)
		{// loose
			player.addItem(description, 57, 1, player, true);
		}
		
	}
	
}
