package net.sf.l2j.gameserver.handler.voicecommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author user
 *
 */
public class Offline implements IVoicedCommandHandler
{
	private final String[] _voicedCommands =
	{
		"offline"				
	};


	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String params)
	{
		if(!Config.ALLOW_OFFLINE_TRADE)
		{
			activeChar.sendMessage("Оффлайн трейд выключен.");
			return false;
		}
		if(!activeChar.isInStoreMode())
		{
			activeChar.sendMessage("Вы не торгуете!");
			return false;
		}
		if(activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("Нельзя использовать на олимпиаде!");
			return false;
		}
		activeChar.setOfflineTrader();
		return true;
	}


	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
}
