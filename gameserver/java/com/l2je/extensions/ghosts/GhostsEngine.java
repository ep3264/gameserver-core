package com.l2je.extensions.ghosts;

import com.l2je.extensions.ghosts.ai.GhostAITask;
import com.l2je.extensions.ghosts.generators.GhostGenarator;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Author: HandOfGod<br>
 * Date: 18 февр. 2016 г.<br>
 * Time: 20:00:21<br>
 */

public class GhostsEngine
{
	protected static final Logger _log = Logger.getLogger(GhostsEngine.class.getName());
	private final static int GHOST_MAX_NUMBER = 3; 
	private GhostGenarator genarator = new GhostGenarator();
	private Shouts shouts = null;
	private Dao dao = new Dao();
	boolean isTableChanged = false;
	protected ArrayList<L2PcInstance> ghosts = new ArrayList<>();
	
	
	private static class SingletonHolder
	{
		protected static final GhostsEngine _instance = new GhostsEngine();
	}
	
	public static GhostsEngine getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected GhostsEngine()
	{
		
	}
	
	public void init() {
		dao.loadGhosts(ghosts);
		while (ghosts.size() < GHOST_MAX_NUMBER) {
			if (!isTableChanged) {
				isTableChanged = true;
			}
			L2PcInstance ghost = genarator.genarate();
			ghosts.add(ghost);
		}
		dao.saveGhosts(ghosts, isTableChanged);
		
		if (ghosts.size() > 0)
		{
//			ThreadPool.scheduleAtFixedRate(new GhostsResurrector(), 60000, 12000);
		} 
		int x =147700;
		int y = -55510;
		int z = -2736;
		Location loc = new Location(x, y, z);
		long startTime = 60000;
		for (L2PcInstance player : ghosts) {
			ThreadPool.scheduleAtFixedRate(new GhostAITask(player, new Location(loc)), startTime, 10000);
			startTime += 1000;
		}
		
		if(Config.ENABLE_GHOSTS_SHOUTS){
			loadShout();
		}
	}
	
	public void shutdown() {
		dao.saveGhosts(ghosts, isTableChanged);
	}
	
	
	public void addGhost(L2PcInstance player)
	{
		if (!isTableChanged)
			isTableChanged = true;
		ghosts.add(player);
	}
	
	public void deleteGhost(L2PcInstance player)
	{
		if (!isTableChanged)
			isTableChanged = true;
		ghosts.remove(player);
	}
	

	private void loadShout() {
		if(Config.ENABLE_GHOSTS_SHOUTS){
			_log.info("GhostsShouts: Activated.");
			shouts = new Shouts(ghosts); 
			ThreadPool.scheduleAtFixedRate(shouts, Shouts.getIntialDelay(), Shouts.getShoutRndTime());
		}
	}
	
	protected class GhostsResurrector implements Runnable
	{
		private ScheduledFuture<?> _task = null;
		
		public GhostsResurrector() {
		}
		
		@Override
		public void run()
		{
			for (L2PcInstance ghost : ghosts)
			{
				if (ghost.isDead())
				{
					synchronized (ghost)
					{
						ghost.doRevive();
					}					
				}
			}			
			_task = null;
		}
		
		public void cancel()
		{
			if (_task != null)
			{
				_task.cancel(false);
			}
			_task = null;
		}
	}
}
