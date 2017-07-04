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
package com.l2je.extensions.casino;

import java.util.logging.Logger;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author user
 */
public class Roulette
{
	protected static final Logger _log = Logger.getLogger(Roulette.class.getName());
	
	public static Roulette getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final Roulette _instance = new Roulette();
	}
	
	private static Mode getModeById(int mode)
	{
		if (mode == 1)
		{
			return new LowMode();
		}
		else if (mode == 2)
		{
			return new MiddleMode();
		}
		else if (mode == 3)
		{
			return new HighMode();
		}
		return new LowMode();
		
	}
	
	public void twist(L2PcInstance player, int modeId)
	{
		if (player == null)
		{
			return;
		}
		
		if (!player.getInventory().validateCapacity(1))
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}
		
		Mode mode = getModeById(modeId);
		mode.run(player);
	}
}
