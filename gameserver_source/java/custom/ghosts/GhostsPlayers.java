package custom.ghosts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GameClient.GameClientState;




import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

/**
 * Developers: Redist Team<br>
 * <br>
 * <br>
 * Author: Redist<br>
 * Date: 18 февр. 2016 г.<br>
 * Time: 20:00:21<br>
 * <br>
 */
public class GhostsPlayers
{
	protected class GhostsResurrector implements Runnable
	{
		private ScheduledFuture<?> _task = null;
		ArrayList<L2PcInstance> _ghosts;
		
		
		public GhostsResurrector(ArrayList<L2PcInstance> ghosts)
		{
			_ghosts = ghosts;				
		}
		
		@Override
		public synchronized void run()
		{
			for(L2PcInstance ghost :_ghosts)
			 {
				if(ghost.isDead())
				{
					ghost.doRevive();			
				}
			 }
			_task = null;			
		}
		
		public synchronized void cancel()
		{
			if (_task != null)
			{
				_task.cancel(false);
			}
			_task = null;
		}
	}
	protected static final Logger _log = Logger.getLogger(GhostsPlayers.class.getName());
	
	//private static final String CLEAR_OFFLINE_TABLE = "DELETE FROM fake_players";
	private static final String SELECT_GHOSTS = "SELECT * FROM ghosts_players";
    private LinkedList<Location> locs = new LinkedList<>();
    private ArrayList<L2PcInstance> ghosts = new ArrayList<>(100);
	private static class SingletonHolder
	{
		protected static final GhostsPlayers _instance = new GhostsPlayers();
	}
	
	public static GhostsPlayers getInstance()
	{		
		return SingletonHolder._instance;
	}
		
	
	protected GhostsPlayers()
	{		
	}
     
	private void spawnGhosts()
	{
		 Collections.shuffle(locs);
		 int i =0;
		 if(ghosts.size()>1){
		 ThreadPool.scheduleAtFixedRate(new GhostsResurrector(ghosts), 60000, 60000);
		 }
		 for(Location loc :locs)
		 {
			 ghosts.get(i).spawnMe(loc.getX(),loc.getY(),loc.getZ());
			 i++;
		 }
	}
	public void loadGhosts()
	{
		_log.info("GhostsPlayers: Activated.");
		Connection con = null;
		int nPlayers = 0;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement(SELECT_GHOSTS);
			ResultSet rs = stm.executeQuery();

			while(rs.next())
			{
				L2PcInstance player = null;
				
				try
				{
					
					L2GameClient client = new L2GameClient(null);
					player = L2PcInstance.restore(rs.getInt("obj_Id"));					
					player.setIsGhost(true);
					client.setActiveChar(player);
					//client.setAccountName(player.getAccountName());
					client.setState(GameClientState.IN_GAME);
					player.setClient(client);					
					locs.add(new Location(player.getX(), player.getY(), player.getZ()));
					ghosts.add(player);
					//player.spawnMe(player.getX(), player.getY(), player.getZ());
					
					//LoginServerThread.getInstance().addGameServerLogin(player.getAccountName(), client);
					
					if(Config.GHOSTS_PLAYERS_SIT)
					{
						int _random = Rnd.get(100);
						
						if (_random <= 50)
						{
							player.sitDown();
						}
					}
					//player.setOnlineStatus(true);
					//player.restoreEffects();
					player.broadcastUserInfo();
					nPlayers++;
				}
				catch(Exception e)
				{
					_log.info("Ghosts Players Engine: Error loading player: " + player); //, e);
					e.printStackTrace();
					if(player != null)
					{
						player.deleteMe();
					}
				}
			}
			rs.close();
			stm.close();
			spawnGhosts();
			_log.info("Loaded: " + nPlayers + " Ghosts Players.");
		}
		catch(Exception e)
		{
			_log.info("Ghosts Players Engine : Error while loading player: "+e.toString());
		}
		finally
		{
			try
			{
				if(con!=null){
				con.close();
				}
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
