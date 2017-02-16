package com.l2je.extensions.acp;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

/**
 * @author user
 */
public class AcpCpTask extends AcpTask
{	
	/**
	 * @param activeChar
	 */
	public AcpCpTask(L2PcInstance activeChar)
	{
		super(activeChar);
	}
	
	@Override
	public void run()
	{
		if (!_activeChar.isDead() && !_activeChar.isInOlympiadMode())
		{
			if (_activeChar.getCurrentCp() < _activeChar.getMaxCp() - 200)
			{
				final ItemInstance cpPotion = _activeChar.getInventory().getItemByItemId(AcpManager.CP_ID);
				if (cpPotion != null)
				{
					final IItemHandler handler = ItemHandler.getInstance().getItemHandler(cpPotion.getEtcItem());
					if (handler != null)
						handler.useItem(_activeChar, cpPotion, false);
				}
			}
		}
		
	}
	
}
