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
package custom.acp;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * @author user
 *
 */
public class AcpMpTask extends AcpTask
{
	public AcpMpTask(L2PcInstance activeChar)
	{
		super(activeChar);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		if (!activeChar.isDead() && !activeChar.isInOlympiadMode())
		{
			if (activeChar.getCurrentMp() < activeChar.getMaxMp() * 0.20)
			{
				int mpId = 728;
				final ItemInstance manaPotion = activeChar.getInventory().getItemByItemId(mpId);
				if (manaPotion != null)
				{
					final IItemHandler handler = ItemHandler.getInstance().getItemHandler(manaPotion.getEtcItem());
					if (handler != null)
						handler.useItem(activeChar, manaPotion, false);
				}
			}
		}
	}
}
