package com.l2je.extensions.ghosts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.LoginServerThread;
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
		
		public GhostsResurrector()
		{
			
		}
		
		@Override
		public void run()
		{
			for (L2PcInstance ghost : _ghosts)
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
	
	protected static final Logger _log = Logger.getLogger(GhostsPlayers.class.getName());	
	// private static final String CLEAR_OFFLINE_TABLE = "DELETE FROM fake_players";
	private static final String SELECT_GHOSTS = "SELECT * FROM ghosts_players";
	private static final String SAVE_GHOST = "INSERT INTO `ghosts_players` (`obj_Id`) VALUES (?)";
	// private static final String DELETE_GHOST = "DELETE FROM `ghosts_players` WHERE `obj_Id`=?";
	private static final String DELETE_GHOSTS = "TRUNCATE TABLE ghosts_players";
	
	protected ArrayList<L2PcInstance> _ghosts = new ArrayList<>();
	boolean _changeTable = false;
	
	private static class SingletonHolder
	{
		protected static final GhostsPlayers _instance = new GhostsPlayers();
	}
	
	public static GhostsPlayers getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private L2PcInstance getRandomGhost()
	{
		Random random = new Random();
		if (_ghosts.size() > 0)
		{
			return _ghosts.get(random.nextInt(_ghosts.size()));
		}
		return null;
	}
	
	public String getRandomGhostName()
	{
		L2PcInstance l2PcInstance = getRandomGhost();
		if (l2PcInstance == null)
		{
			return "donater";
		}
		return l2PcInstance.getName();
	}
	
	protected GhostsPlayers()
	{
	}
	
	public void addGhost(L2PcInstance player)
	{
		if (!_changeTable)
			_changeTable = true;
		_ghosts.add(player);
	}
	
	public void deleteGhost(L2PcInstance player)
	{
		if (!_changeTable)
			_changeTable = true;
		_ghosts.remove(player);
	}
	
	public void saveGhosts()
	{
		if (!_changeTable)
		{
			_log.info("GhostsPlayers: Nothing to save.");
			return;
		}
		_log.info("GhostsPlayers: Save ghosts.");
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement st = con.prepareStatement(DELETE_GHOSTS);
			st.execute();
			st.close();
			PreparedStatement statementInsert = con.prepareStatement(SAVE_GHOST);
			for (L2PcInstance ghost : _ghosts)
			{
				statementInsert.setInt(1, ghost.getObjectId());
				statementInsert.execute();
				statementInsert.clearParameters();
			}
			statementInsert.close();
			_log.info("GhostsPlayers: Saved " + (_ghosts.size()) + " ghost(s)");
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void loadGhosts()
	{
		_log.info("GhostsPlayers: Activated.");
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement stm = con.prepareStatement(SELECT_GHOSTS);
			ResultSet rs = stm.executeQuery();
			
			while (rs.next())
			{
				L2PcInstance player = null;
				
				try
				{
					
					L2GameClient client = new L2GameClient(null);
					player = L2PcInstance.restore(rs.getInt("obj_Id"));
					player.setIsGhost(true);
					client.setActiveChar(player);
					client.setAccountName(player.getAccountName());
					client.setState(GameClientState.IN_GAME);
					player.setOnlineStatus(true, false);
					player.setClient(client);
					_ghosts.add(player);
					player.spawnMe(player.getX(), player.getY(), player.getZ());
					
					LoginServerThread.getInstance().addGameServerLogin(player.getAccountName(), client);
					
					if (Config.GHOSTS_PLAYERS_SIT)
					{
						int random = Rnd.get(100);
						
						if (random <= 50)
						{
							player.sitDown();
						}
					}
					player.broadcastUserInfo();
				}
				catch (Exception e)
				{
					_log.info("Ghosts Players Engine: Error loading player: " + player); // , e);
					e.printStackTrace();
					if (player != null)
					{
						player.deleteMe();
					}
				}
			}
			rs.close();
			stm.close();
			if (_ghosts.size() > 0)
			{
				ThreadPool.scheduleAtFixedRate(new GhostsResurrector(), 60000, 12000);
			}			
			_log.info("Loaded: " + _ghosts.size() + " Ghosts Players.");
		}
		catch (Exception e)
		{
			_log.info("Ghosts Players Engine : Error while loading player: " + e.toString());
		}
		finally
		{
			try
			{
				if (con != null)
				{
					con.close();
				}
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
}
