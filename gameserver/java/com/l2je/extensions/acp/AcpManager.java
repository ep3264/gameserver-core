package com.l2je.extensions.acp;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author dbg
 * Official Website: http://l2je.com 
 * @date 15 февр. 2017 г. 4:59:02 
 */
public class AcpManager
{
	public static final int HP_ID = 1539,
		MP_ID = 728,
		CP_ID = 5592, 
		MP_CD = 5000,
		HP_CD = 5000,
		CP_CD = 1000;
	public static void onBypassFeedback(L2PcInstance activeChar, String command)
	{
		if (command.equals("all"))
		{
			activeChar.cancelAcp();
			activeChar.setAcp(ThreadPool.scheduleAtFixedRate(new AcpCpTask(activeChar), 100, CP_CD), CP_ID);
			activeChar.setAcp(ThreadPool.scheduleAtFixedRate(new AcpHpTask(activeChar), 200, HP_CD), HP_ID);
			activeChar.setAcp(ThreadPool.scheduleAtFixedRate(new AcpMpTask(activeChar), 300, MP_CD), MP_ID);
			activeChar.sendMessage("ACP активировано.");
		}
		else if (command.equals("cp"))
		{
			if (!activeChar.isAcp(CP_ID))
			{
				activeChar.setAcp(ThreadPool.scheduleAtFixedRate(new AcpCpTask(activeChar), 100, CP_CD), CP_ID);
				activeChar.sendMessage("Автоиспользование CP активировано.");
			}
			else 
			{
				activeChar.cancelAcp(CP_ID);
				activeChar.sendMessage("Автоиспользование CP деактивировано.");
			}
		}
		else if(command.equals("hp"))
		{
			if (!activeChar.isAcp(HP_ID))
			{
				activeChar.setAcp(ThreadPool.scheduleAtFixedRate(new AcpHpTask(activeChar), 100, HP_CD), HP_ID);
				activeChar.sendMessage("Автоиспользование HP активировано.");
			}
			else
			{
				activeChar.cancelAcp(HP_ID);
				activeChar.sendMessage("Автоиспользование HP деактивировано.");
			}
		}
		else if(command.equals("mp"))
		{
			if (!activeChar.isAcp(MP_ID))
			{
				activeChar.setAcp(ThreadPool.scheduleAtFixedRate(new AcpMpTask(activeChar), 100, MP_CD), MP_ID);
				activeChar.sendMessage("Автоиспользование MP активировано.");				
			}
			else
			{
				activeChar.cancelAcp(MP_ID);
				activeChar.sendMessage("Автоиспользование MP деактивировано.");
			}
			
		}
		else if(command.equals("off"))
		{	
			activeChar.cancelAcp();
			activeChar.sendMessage("ACP деактивировано.");
		}
	}
}
