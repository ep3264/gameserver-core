package com.l2je.extensions.ghosts.ai;

import java.util.logging.Logger;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MoveToLocation;

/**
 * @author HandOfGod
 * @date Nov 26, 2017 12:48:33 AM
 */
public class GhostAITask implements Runnable
{
	protected static final Logger _log = Logger.getLogger(GhostAITask.class.getName());
	public L2PcInstance ghost;
	
	public GhostAITask(L2PcInstance ghost)
	{
		super();
		this.ghost = ghost;
	}
	
	@Override
	public void run()
	{
		randomMove(34, 55);
	}
	
	public void randomMove(int min_range, int max_range)
	{
		L2PcInstance l2PcInstance = ghost;
		Location loc = new Location(l2PcInstance.getX() + (Rnd.get(100) > 50 ? 1 : -1) * Rnd.get(min_range, max_range), l2PcInstance.getY() + (Rnd.get(100) > 50 ? 1 : -1) * Rnd.get(min_range, max_range), l2PcInstance.getZ());
		l2PcInstance.moveToLocation(loc.getX(), loc.getY(), loc.getZ(), 0);
		l2PcInstance.broadcastPacket(new MoveToLocation(l2PcInstance));
	}
}
