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
		"acp"/*,
		"acp all",
		"acp cp",
		"acp hp",
		"acp mp",
		"acp off" */
	};

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#useVoicedCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String params)
	{		
		if (command.equalsIgnoreCase("acp"))
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
				if(params.equalsIgnoreCase("all"))
				{
					activeChar.onAcp(new AcpAllTask(activeChar));
					activeChar.sendMessage("Автоиспользование CP/HP/MP активировано.");
				}
				else if(params.equalsIgnoreCase("cp"))
				{
					activeChar.onAcp(new AcpCpTask(activeChar));
					activeChar.sendMessage("Автоиспользование CP активировано.");
				}
				else if(params.equalsIgnoreCase("hp"))
				{
					activeChar.onAcp(new AcpHpTask(activeChar));
					activeChar.sendMessage("Автоиспользование HP активировано.");
				}
				else if(params.equalsIgnoreCase("mp"))
				{
					activeChar.onAcp(new AcpMpTask(activeChar));
					activeChar.sendMessage("Автоиспользование MP активировано.");
				}
				else if(params.equalsIgnoreCase("off"))
				{
					activeChar.offAcp();
					activeChar.sendMessage("ACP деактивировано.");
				}
			}		
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#getVoicedCommandList()
	 */
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}	
}
