package com.l2je.extensions.acp;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * @author user
 */
public class AcpHpTask extends AcpTask
{	
	private final static double RATE = 0.95;
	
	public AcpHpTask(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void run()
	{
		if (!_activeChar.isDead() && !_activeChar.isInOlympiadMode())
		{
			if (_activeChar.getCurrentHp() < _activeChar.getMaxHp() * RATE)
			{
				final ItemInstance healingPotion = _activeChar.getInventory().getItemByItemId(AcpManager.HP_ID);
				if (healingPotion != null)
				{
					final IItemHandler handler = ItemHandler.getInstance().getItemHandler(healingPotion.getEtcItem());
					if (handler != null)
						handler.useItem(_activeChar, healingPotion, false);
				}
			}
		}
	}
}
