/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2je.extensions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author HandOfGod
 */
public class ColorManager
{
	private static class SingletonHolder
	{
		protected static final ColorManager _instance = new ColorManager();
	}
	
	public static ColorManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static final String INSERT_NAME_COLOR = "INSERT INTO character_colors (char_id, name_color, title_color) VALUES (?,?,?)" + "ON DUPLICATE KEY UPDATE name_color=VALUES(name_color)";
	private static final String INSERT_TITLE_COLOR = "INSERT INTO character_colors (char_id, name_color, title_color) VALUES (?,?,?)" + "ON DUPLICATE KEY UPDATE title_color=VALUES(title_color)";
	private static final String SELECT_COLOR = "SELECT name_color, title_color FROM character_colors WHERE char_id=?";
	public static final HashSet<String> ALLOWED_COLORS = new HashSet<>(Arrays.asList("009900", "007fff", "ff00ff", "ffff00", "ff0000", "ff9900", "93db70", "9f9f9f", "00ffff"));
	
	public void restoreColors(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(SELECT_COLOR);
			statement.setInt(1, player.getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				player.getAppearance().setNameColor(rset.getInt("name_color"));
				player.getAppearance().setTitleColor(rset.getInt("title_color"));
			}
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void storeNameColor(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(INSERT_NAME_COLOR);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, player.getAppearance().getNameColor());
			statement.setInt(3, player.getAppearance().getTitleColor());
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public void storeTitleColor(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(INSERT_TITLE_COLOR);
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, player.getAppearance().getNameColor());
			statement.setInt(3, player.getAppearance().getTitleColor());
			statement.execute();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private static boolean payDyeing(L2PcInstance player)
	{
		ItemInstance item = player.getInventory().getItemByItemId(Config.COLOR_CHANGE_ITEM);
		if (item == null || item.getCount() < Config.COLOR_CHANGE_PRICE)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return false;
		}
		if (!player.destroyItem("Color Manager", item, Config.COLOR_CHANGE_PRICE, player, true))
		{
			return false;
		}
		return true;
	}
	
	
	public void setTitleColor(L2PcInstance player, String command)
	{
		int color = 0xFFFFFF;
		try
		{
			String choice = command.substring(14);
			if (ALLOWED_COLORS.contains(choice))
			{
				color = Integer.decode("0x" + choice);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	
		setTitleColor(player, color);
	}
	
	public void setTitleColor(L2PcInstance player, int color)
	{
		if (payDyeing(player))
		{
			player.getAppearance().setTitleColor(color);
			storeTitleColor(player);
			player.broadcastUserInfo();
		}
	}
	
	public void setNameColor(L2PcInstance player, String command)
	{
		int color = 0xFFFFFF;
		try
		{
			String choice = command.substring(13);
			if (ALLOWED_COLORS.contains(choice))
			{
				color = Integer.decode("0x" + choice);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	
		setNameColor(player, color);
	}
	
	
	public void setNameColor(L2PcInstance player, int color)
	{
		if (payDyeing(player))
		{
			player.getAppearance().setNameColor(color);
			storeNameColor(player);
			player.broadcastUserInfo();
		}
	}
}
