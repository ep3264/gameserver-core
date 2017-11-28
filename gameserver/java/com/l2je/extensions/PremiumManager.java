package com.l2je.extensions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.lang.Language;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author user
 */
public class PremiumManager
{
	private static final String HTML_PATH = "data/html/mods/Premium.htm";
	private static final String HTML_RU_PATH = "data/html-ru/mods/Premium.htm";
	
	/**
	 * @param activeChar игрок
	 * @return информация о состоянии премиум аккаунта
	 */
	public static String getPremiumEndDate(L2PcInstance activeChar)
	{
		long date = activeChar.getPremiumService();
		if (date > 0)
		{
			SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			return String.valueOf(format.format(date));
		}
		return "n/a";
	}
	
	/**
	 * Купить премиум
	 * @param player
	 * @param countDays количество дней
	 * @return успешна ли покупка
	 */
	public static boolean buy(L2PcInstance player, int countDays)
	{
		ItemInstance item = player.getInventory().getItemByItemId(Config.PREMIUM_ITEM);
		if (item == null || item.getCount() < Config.PREMIUM_PRICE * countDays)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return false;
		}
		if (!player.destroyItem("Premium Manager", item, Config.PREMIUM_PRICE * countDays, player, true))
		{
			return false;
		}
		return true;
	}
	
	public static void onBypassFeedback(L2PcInstance player, String command)
	{
		int countDays = 1;
		try
		{
			countDays = Integer.parseInt(command);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (buy(player, countDays))
		{
			addPremiumServices(player, player.getAccountName(), countDays);
			player.sendMessage("Премиум активирован");
			Menu.getInstance().showChatWindow(player);
		}
		else
		{
			showChatWindow(player);
		}
		
	}
	
	public static void showChatWindow(L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		if (player.getLang() == Language.RU)
			html.setFile(HTML_RU_PATH);
		else
			html.setFile(HTML_PATH);
		html.replace("%price%", Config.PREMIUM_PRICE);
		String itemName = ItemTable.getInstance().getTemplate(Config.PREMIUM_ITEM).getName();
		html.replace("%item%", itemName);
		player.sendPacket(html);
	}
	
	/**
	 * Добавляет премиум на аккаунт выбранного игрока
	 * @param player
	 * @param AccName
	 * @param days
	 */
	public static void addPremiumServices(L2PcInstance player, String AccName, int days)
	{
		if (player == null)
			return;
		
		Calendar finishtime = Calendar.getInstance();
		finishtime.set(Calendar.SECOND, 0);
		finishtime.add(Calendar.DAY_OF_MONTH, days);
		
		if (!player.getAccountName().equalsIgnoreCase(AccName))
		{
			player = null;
			for (L2PcInstance pc : World.getInstance().getAllPlayers())
				if (pc.getAccountName().equalsIgnoreCase(AccName))
				{
					player = pc;
					break;
				}
			
			if (player == null || player.getClient() == null)
			{
				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement stm = con.prepareStatement("REPLACE account_data VALUES (?,'premium',?)");
					stm.setString(1, AccName.toLowerCase());
					stm.setString(2, String.valueOf(finishtime.getTimeInMillis()));
					stm.execute();
					stm.close();
					con.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			player.setPremiumService(finishtime.getTimeInMillis());
			player.getClient().storeData();
		}
		
	}
}
