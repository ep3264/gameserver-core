package com.l2je.extensions;

import com.l2je.extensions.acp.AcpManager;
import com.l2je.extensions.events.EventManager;
import com.l2je.protection.ProtectionConfig;

import net.sf.l2j.commons.lang.Language;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author dbg Official Website: http://l2je.com
 * @date 14 февр. 2017 г. 10:57:13
 */
public class Menu
{
	private static class SingletonHolder
	{
		protected static final Menu _instance = new Menu();
	}
	
	public static final Menu getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static final String HTML_PATH = "data/html/commands/menu/Menu.htm";
	private static final String HTML_RU_PATH = "data/html-ru/commands/menu/Menu.htm";
	
	public void showChatWindow(L2PcInstance activeChar)
	{
		boolean ru = (activeChar.getLang() == Language.RU);
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		if (ru)
			html.setFile(HTML_RU_PATH);
		else
			html.setFile(HTML_PATH);
		
		html.basicReplace("%lang%", activeChar.getLang().toString());
		String premiumInfo = ru ? "<td><font color=\"ff0000\">Не активирован</font></td><td><button value=\"Активировать\" action=\"bypass -h menu_premium\" width=75 height=20 back=\"L2UI_CH3.Btn1_normalDisable\" fore=\"L2UI_CH3.Btn1_normalDisable\"></td>" : "<td><font color=\"ff0000\">Not activated</font></td><td><button value=\"Activate\" action=\"bypass -h menu_premium\" width=75 height=20 back=\"L2UI_CH3.Btn1_normalDisable\" fore=\"L2UI_CH3.Btn1_normalDisable\"></td>";
		if (activeChar.isPremium())
		{
			StringBuilder sb = new StringBuilder();
			StringUtil.append(sb, "<td><font color=\"LEVEL\"> ", PremiumManager.getPremiumEndDate(activeChar), "</font></td>");
			premiumInfo = sb.toString();
		}
		html.basicReplace("%premium%", premiumInfo);
		html.basicReplace("%exp%", stateToString(!activeChar.getStopExp(), ru));
		StringBuilder sbAcp = new StringBuilder();
		String acp = ru ? "<font color=\"ff0000\">Выкл</font></td>" : "<font color=\"ff0000\">Off</font></td>";
		boolean status = false;
		sbAcp.append("<font color=\"LEVEL\">");
		if (activeChar.isAcp(AcpManager.CP_ID))
		{
			sbAcp.append("CP");
			status = true;
		}
		if (activeChar.isAcp(AcpManager.HP_ID))
		{
			if (status)
				sbAcp.append("/");
			sbAcp.append("HP");
			status = true;
		}
		if (activeChar.isAcp(AcpManager.MP_ID))
		{
			if (status)
				sbAcp.append("/");
			sbAcp.append("MP ");
			status = true;
		}
		if (status)
		{
			sbAcp.append("</font>");
			acp = sbAcp.toString();
		}
		html.basicReplace("%acp%", acp);
		html.basicReplace("%ip%", activeChar.getClient().getConnection().getInetAddress().getHostAddress());
		html.basicReplace("%ipon%", stateToString(activeChar.isIpBlock(), ru));
		html.basicReplace("%hwidon%", stateToString(activeChar.isHwidBlock(), ru));
		String eventInfo = ru ? "<td><font color=\"ff0000\">Нет доступных ивентов</font></td>" : "<td><font color=\"ff0000\">No events</font></td>";
		if (EventManager.getInstance().getCurrentEvent() != null)
		{
			StringBuilder sb = new StringBuilder();
			StringUtil.append(sb, "<td><font color=\"LEVEL\">", EventManager.getInstance().getCurrentEvent().getName(), "</font></td><td><button value=\"", (ru ? "Информация" : "Information"), "\" action=\"bypass -h event_info\" width=75 height=20 back=\"L2UI_CH3.Btn1_normalDisable\" fore=\"L2UI_CH3.Btn1_normalDisable\"></td>");
			eventInfo = sb.toString();
		}
		html.basicReplace("%event%", eventInfo);
		activeChar.sendPacket(html);
	}
	
	private static String stateToString(boolean state, boolean ru)
	{
		return ru ? (state ? "<font color=\"LEVEL\">Вкл</font>" : "<font color=\"ff0000\">Выкл</font>") : (state ? "<font color=\"LEVEL\">On</font>" : "<font color=\"ff0000\">Off</font>");
	}
	
	public void onBypassFeedback(L2PcInstance activeChar, String command)
	{
		if (command.equals("en"))
		{
			activeChar.setLang(Language.EN);
		}
		else if (command.equals("ru"))
		{
			activeChar.setLang(Language.RU);
		}
		else if (command.equals("expon") && activeChar.getStopExp())
		{
			activeChar.setStopExp(false);
			activeChar.sendMessage("Опыт включен.");
		}
		else if (command.equals("expoff") && !activeChar.getStopExp())
		{
			activeChar.setStopExp(true);
			activeChar.sendMessage("Опыт выключен.");
		}
		else if (command.startsWith("acp"))
		{
			command = command.substring(3);
			AcpManager.onBypassFeedback(activeChar, command);
		}
		else if (command.equals("ipon"))
		{
			try
			{
				activeChar.setIpBlock(activeChar.getClient().getConnection().getInetAddress().getHostAddress());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (command.equals("ipoff"))
		{
			activeChar.setIpBlock("0");
		}
		else if (command.equals("hwidon"))
		{
			try
			{
				if (ProtectionConfig.HWID  && !activeChar.getClient().getHWID().getMAC().equals("N/A"))
				{
					activeChar.setHwidBlock(activeChar.getClient().getHWID().getMAC());
				}
				else
				{
					activeChar.sendMessage("Не доступно. Обратитесь к администратору.");
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if (command.equals("hwidoff"))
		{
			activeChar.setHwidBlock("0");
		}
		else if (command.equals("premium"))
		{
			PremiumManager.showChatWindow(activeChar);
			return;
		}
		showChatWindow(activeChar);
	}
}
