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
	private L2PcInstance ghost;
	private Location startLoc;
	
	public GhostAITask(L2PcInstance ghost, Location startLoc)
	{
		super();
		this.ghost = ghost;
		this.startLoc = startLoc;
	}
	
	@Override
	public void run()
	{
		randomMove(34, 55);
	}
	
	public void randomMove(int min_range, int max_range)
	{
		int x = ghost.getX() + (Rnd.get(100) > 50 ? 1 : -1) * Rnd.get(min_range, max_range);
		int y = ghost.getY() + (Rnd.get(100) > 50 ? 1 : -1) * Rnd.get(min_range, max_range);
		final int z = ghost.getZ();
		if (x > startLoc.getX() + 500 || y > startLoc.getY() + 500) {
			x = startLoc.getX();
			y = startLoc.getY();
		}
		ghost.moveToLocation(x, y, z, 0);
		ghost.broadcastPacket(new MoveToLocation(ghost));
	}
}
