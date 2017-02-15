package com.l2je.extensions.acp;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author evgeny64
 * Official Website: http://l2je.com 
 * @date 15 февр. 2017 г. 4:59:02 
 */
public class AcpManager
{
	public static void onBypassFeedback(L2PcInstance activeChar, String command)
	{
		if (command.equals("all"))
		{
			activeChar.onAcp(new AcpAllTask(activeChar));
			activeChar.sendMessage("Автоиспользование CP/HP/MP активировано.");
		}
		else if (command.equals("cp"))
		{
			activeChar.onAcp(new AcpCpTask(activeChar));
			activeChar.sendMessage("Автоиспользование CP активировано.");
		}
		else if(command.equals("hp"))
		{
			activeChar.onAcp(new AcpHpTask(activeChar));
			activeChar.sendMessage("Автоиспользование HP активировано.");
		}
		else if(command.equals("mp"))
		{
			activeChar.onAcp(new AcpMpTask(activeChar));
			activeChar.sendMessage("Автоиспользование MP активировано.");
		}
		else if(command.equals("off"))
		{
			activeChar.offAcp();
			activeChar.sendMessage("ACP деактивировано.");
		}
	}
}
