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
package com.l2je.extensions.events;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;

/**
 * @author user
 *
 */
public abstract class CombatEvent extends Event
{
	@Override
	public boolean playersAutoAttackable(L2Character target, L2Character attacker )
	{		
		if (!isRunning())
		{
			return false;
		}
		L2PcInstance attackerInstance;
		if (!(attacker instanceof L2PcInstance))
		{
			if (target instanceof L2SummonInstance)
			{
				attackerInstance = ((L2SummonInstance) target).getOwner();
			}
			else
			{
				return false;
			}
		}
		else
		{
			attackerInstance = (L2PcInstance) attacker;
		}
		L2PcInstance targetInstance;
		if (!(target instanceof L2PcInstance))
		{
			if (target instanceof L2SummonInstance)
			{
				targetInstance = ((L2SummonInstance) target).getOwner();
			}
			else
			{
				return false;
			}
		}
		else
		{
			targetInstance = (L2PcInstance) target;
		}
		if (!containsPlayer(attackerInstance) || !containsPlayer(targetInstance))
		{
			return false;
		}		
		return true;
	}
}
