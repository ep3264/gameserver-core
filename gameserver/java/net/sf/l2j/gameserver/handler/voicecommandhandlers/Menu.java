package net.sf.l2j.gameserver.handler.voicecommandhandlers;

import com.l2je.extensions.MenuManager;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author dbg
 * Official Website: http://l2je.com 
 * @date 14 февр. 2017 г. 8:24:14 
 */
public class Menu implements IVoicedCommandHandler
{	
	private final String[] _voicedCommands =
	{
		"menu"				
	};

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String params)
	{
		MenuManager.getInstance().showChatWindow(activeChar);
		return true;
	}

	
	@Override
	public String[] getVoicedCommandList()
	{		
		return _voicedCommands;
	}
	
}
