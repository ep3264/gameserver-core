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

import net.sf.l2j.commons.lang.Language;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author user
 *
 */
public class Help  implements IVoicedCommandHandler
{
	private static final String HTML_PATH = "data/html/commands/help/Help.htm";
	private static final String HTML_RU_PATH = "data/html-ru/commands/help/Help.htm";
	private final String[] _voicedCommands =
	{
		"help"
	};

	@Override
	public boolean useVoicedCommand(String command, L2PcInstance activeChar, String params)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		if (activeChar.getLang() == Language.RU)
			html.setFile(HTML_RU_PATH);
		else
			html.setFile(HTML_PATH);
		activeChar.sendPacket(html);
		return true;
	}


	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
	
}
