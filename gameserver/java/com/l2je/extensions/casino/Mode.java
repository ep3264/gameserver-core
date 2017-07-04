package com.l2je.extensions.casino;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author dbg Official Website: http://l2je.com
 * @date 11 июн. 2017 г. 20:32:10
 */
public abstract class Mode
{
	protected final static int PRICE_ADENA = 100000000;
	protected final static int COL = 9213;
	protected final static int BLODDY = 4358;
	
	@Override
	public String toString()
	{
		return this.getClass().getName();
	}
	
	public abstract void run(L2PcInstance player, int mode);
	
	public void run(L2PcInstance player)
	{
		run(player, 1);
	}
	
}
