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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import custom.PremiumAccount;

/**
 * @author user
 *
 */
public class AdminPremium implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_set_premium",
		"admin_unset_premium"
	};

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IAdminCommandHandler#useAdminCommand(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.equals("admin_set_premium"))
		{
			if(activeChar.getTarget() instanceof L2PcInstance)
			{
				L2PcInstance target  = (L2PcInstance) activeChar.getTarget();
				PremiumAccount.addPremiumServices(target, target.getAccountName(), 1);
			}
		}
		else if (command.equals("admin_unset_premium")){
			if(activeChar.getTarget() instanceof L2PcInstance)
			{
				L2PcInstance target  = (L2PcInstance) activeChar.getTarget();
				target.setPremiumService(0);
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.handler.IAdminCommandHandler#getAdminCommandList()
	 */
	@Override
	public String[] getAdminCommandList()
	{		
		return ADMIN_COMMANDS;
	}
	
}
