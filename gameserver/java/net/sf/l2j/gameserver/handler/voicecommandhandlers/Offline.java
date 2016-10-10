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


	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#useVoicedCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String params)
	{
		if(!Config.ALLOW_OFFLINE_TRADE)
		{
			activeChar.sendMessage("Offline trade desabled");
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

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IVoicedCommandHandler#getVoicedCommandList()
	 */
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
}
