package net.sf.l2j.gameserver.model.item.instance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;


/**
 * @className:net.sf.l2j.gameserver.model.item.instance.ItemIcons.java 
 * @author dbg
 * Official Website: http://l2je.com 
 * @date 26 янв. 2017 г. 21:07:47 
 */
public class ItemIcons
{
	private static ItemIcons _instance = null;

	protected static final Logger _log = Logger.getLogger(ItemIcons.class.getName());

	public static ItemIcons getInstance()
	{
		if (_instance==null)
			_instance = new ItemIcons();
		return _instance;
	}
	
	HashMap<Integer, String> _items;
	
	public String getIcon(int itemId)
	{
		return _items.containsKey(itemId) ? _items.get(itemId) : "";
	}
	
	public ItemIcons()
	{
		_instance = this;
		_items = new HashMap<>();
		java.sql.Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT itemId,icon FROM `item_icons`");
			rset = statement.executeQuery();
			int itemId;
			String icon;
			int count = 0;
			while(rset.next())
			{
				itemId = rset.getInt("itemId");
				icon = rset.getString("icon");
				_items.put(itemId,icon);
				count++;
			}
			_log.info("Loaded " + count + " Item Icons.");
			statement.close();
			rset.close();
		}
		catch(final Exception e)
		{
			e.printStackTrace();

		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
