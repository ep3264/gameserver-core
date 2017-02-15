package net.sf.l2j.gameserver.handler.voicecommandhandlers;


import com.l2je.extensions.acp.*;

import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author user
 *
 */
public class Acp implements IVoicedCommandHandler
{  	
	private final String[] _voicedCommands =
	{
		"acp"
	};


	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String params)
	{		
		if (command.equals("acp"))
		{
			if(params==null)
			{
				activeChar.sendMessage("Команды чата ACP:");
				activeChar.sendMessage(".acp all — CP/HP/MP");
				activeChar.sendMessage(".acp cp — CP");
				activeChar.sendMessage(".acp hp — HP");
				activeChar.sendMessage(".acp mp — MP");
				activeChar.sendMessage(".acp off — выключить");
			}
			else
			{				
				AcpManager.onBypassFeedback(activeChar, params);
			}		
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}	
}
