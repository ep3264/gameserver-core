package com.l2je.extensions.acp;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author user
 *
 */
public  abstract class AcpTask implements Runnable
{
	protected L2PcInstance _activeChar;
	public AcpTask(L2PcInstance activeChar)
	{
		_activeChar=activeChar;
	}
}
