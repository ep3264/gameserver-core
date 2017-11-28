package com.l2je.extensions.ghosts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.L2GameClient.GameClientState;

/**
 * @author sector
 * @date Nov 25, 2017 11:51:34 PM 
 */
public class Dao
{
	protected static final Logger _log = Logger.getLogger(Dao.class.getName());	
	private static final String DELETE_GHOST = "DELETE FROM `ghosts_players` WHERE `obj_Id`=?";
	private static final String SELECT_GHOSTS = "SELECT * FROM ghosts_players";
	private static final String SAVE_GHOST = "INSERT INTO `ghosts_players` (`obj_Id`) VALUES (?)";
	private static final String DELETE_GHOSTS = "TRUNCATE TABLE ghosts_players";
	
	public void loadGhosts(ArrayList<L2PcInstance> ghosts)
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
					player = L2PcInstance.restore(rs.getInt("obj_Id"));
					player.setIsGhost(true);
					player.setOnlineStatus(true, false);
					ghosts.add(player);
					player.spawnMe(player.getX(), player.getY(), player.getZ());
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
			_log.info("Loaded: " + ghosts.size() + " Ghosts Players.");
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
	
	public void saveGhosts(ArrayList<L2PcInstance> ghosts, boolean isTableChanged)
	{
		if (!isTableChanged)
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
			for (L2PcInstance ghost : ghosts)
			{
				statementInsert.setInt(1, ghost.getObjectId());
				statementInsert.execute();
				statementInsert.clearParameters();
			}
			statementInsert.close();
			_log.info("GhostsPlayers: Saved " + (ghosts.size()) + " ghost(s)");
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
