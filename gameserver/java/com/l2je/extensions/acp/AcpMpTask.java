package com.l2je.extensions.acp;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * @author user
 */
public class AcpMpTask extends AcpTask
{
	private final static double RATE =0.3;
	public AcpMpTask(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void run()
	{
		if (!_activeChar.isDead() && !_activeChar.isInOlympiadMode())
		{
			if (_activeChar.getCurrentMp() < _activeChar.getMaxMp() * RATE)
			{
				final ItemInstance manaPotion = _activeChar.getInventory().getItemByItemId(AcpManager.MP_ID);
				if (manaPotion != null)
				{
					final IItemHandler handler = ItemHandler.getInstance().getItemHandler(manaPotion.getEtcItem());
					if (handler != null)
						handler.useItem(_activeChar, manaPotion, false);
				}
			}
		}
	}
}