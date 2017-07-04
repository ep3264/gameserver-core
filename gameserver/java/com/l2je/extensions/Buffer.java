package com.l2je.extensions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;


/**
 * Developers: L2JE Team<br>
 * Official Website: http://l2je.com <br>
 * <br>
 * Author: dbg<br>
 * Date: 3 нояб. 2015 г.<br>
 * Time: 14:06:57<br>
 * <br>
 */
public class Buffer
{
	private static class SingletonHolder
	{
		protected static final Buffer _instance = new Buffer();
	}
	
	public static final Buffer getInstance()
	{
		return SingletonHolder._instance;
	}
	protected static final Logger _log = Logger.getLogger(Buffer.class.getName());
	public Buffer()
	{
	}
	public static final  HashSet <Integer> ALLOWED_PLAYER_BUFFS = new HashSet<>(
		Arrays.asList(
			365,307,276,309,274,275,272,277,311,
			271,273,310,
			364,264,268,306,269,270,265,363,349,
			308,305,304,267,266,
			4355,4352,1243,4347,4348,4351,1397,4360,
			4356,4359,4358,4357,4342,1032,4349,4346,
			1068,1303,4344,1087,4350,1304,1388,1389,
			1007,1009,1006,1002,1391,1251,1252,1253,
			1284,1310,1309,1308,1362,1363,1413,1390,
			1355,1356,1357,1268	,4553,4554
			));
	
	private final static int[] VIP_FBUFF =
	{
		4699,
		1392,
		1393,
		1352,
		1353,
		1182,
		1189,
		1191,
		1323 // nooble
	};
	private final static int[] VIP_MBUFF =
	{
		4703,
		1392,
		1393,
		1352,
		1353,
		1182,
		1189,
		1191,
		1323 // nooble
	};
	private final static int[] MBUFF =
	{
		1397,
		4342,
		4344,
		4346,
		4349,
		1243,
		1389,
		4347,
		4347,
		4355,
		4356,
		4352,
		1303,
		4351,
		264,
		268,
		267,
		304,
		273,
		276,
		365,
		349,
		1413
	};
	
	private final static int[] FBUFF =
	{
		4344,
		4346,
		4349,
		1068,
		1388,
		4347,
		4352,
		4360,
		4358,
		4357,
		4359,
		4342,
		264,
		267,
		268,
		271,
		274,
		275,
		1363
	};
	private final static int[] DAGGER_BUFFS =
	{
		4347,
		4357,
		4344,
		4346,
		4349,
		1389,
		267,
		1363,
		4359,
		4342,
		4360,
		264,
		274,
		275
	};
	private final static int[] TANK_BUFFS =
	{
		4344,
		4346,
		4349,
		1389,
		4347,
		1310,
		4360,
		4358,
		4357,
		4359,
		4342,
		264,
		267,
		304,
		275,
		310,
		1363
	};
	
	private static boolean isSkill(String string)
	{
		if (string == null || string.length() == 0)
		{
			return false;
		}		
		char c = string.charAt(0);
		if (!(c >= '0' && c <= '9'))
		{
			return false;
		}		
		return true;
	}
	
	public void getBuff(L2PcInstance activeChar, String action, boolean pet)
	{
		L2Character l2Character = null;
		if (pet)
		{
			l2Character = activeChar.getPet();
		}
		else
		{
			l2Character = activeChar;
		}
		
		if (pet && (l2Character == null))
		{
			activeChar.sendMessage("No pet or summon.");
			return;
		}
		
		if (activeChar.isInCombat() || activeChar.isInOlympiadMode() || activeChar.isInDuel() || activeChar.isInSiege())
		{
			activeChar.sendMessage("You can't use buffer in PvP, Duel, Olympiad or Siege mode.");
		}
		else
		{		
			if (isSkill(action))
			{
				Integer skillId=4342; // бафф по умолчанию Wind Walk
				try
				{
				   skillId = Integer.parseInt(action);
				}
				catch (NumberFormatException numberFormatException)
				{					
					_log.log(Level.WARNING, "Buffer error: skillID must be a number.", numberFormatException);					
				}
				if (ALLOWED_PLAYER_BUFFS.contains(skillId))
				{					
					SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId)).getEffects(l2Character, l2Character);
				}
				else
				{
					_log.info("Buff is not allowed!");
					// Punisher.getInstance().illegalAction(activeChar.getClient(), (byte) 0x11);
					// Illegal action
				}
			}
			else if (action.equals("mbuff"))
			{
				getBuffSet(l2Character, MBUFF);
			}
			else if (action.equals("fbuff"))
			{
				getBuffSet(l2Character, FBUFF);
			}
			else if (action.equals("dbuff"))
			{
				getBuffSet(l2Character, DAGGER_BUFFS);
			}
			else if (action.equals("tbuff"))
			{
				getBuffSet(l2Character, TANK_BUFFS);
			}
			else if (action.equals("vipfbuff"))
			{
				if (activeChar.getPremiumService() > 0)
				{
					getBuffSet(l2Character, VIP_FBUFF);
				}
				else if (pay(activeChar))
				{
					getBuffSet(l2Character, VIP_FBUFF);
				}
			}
			else if (action.equals("vipmbuff"))
			{
				if (activeChar.getPremiumService() > 0)
				{
					getBuffSet(l2Character, VIP_MBUFF);
				}
				else if (pay(activeChar))
				{
					getBuffSet(l2Character, VIP_MBUFF);
				}
			}
			else if (action.equals("heal"))
			{
				l2Character.setCurrentHpMp(l2Character.getMaxHp(), l2Character.getMaxMp());
				l2Character.setCurrentCp(l2Character.getMaxCp());
			}
			else if (action.equals("cancel"))
			{
				l2Character.stopAllEffectsExceptThoseThatLastThroughDeath();
			}
		}
	}
	
	private static void getBuffSet(L2Character l2Character, int[] buffset)
	{
		for (int i = 0; i < buffset.length; i++)
		{
			int skillId = buffset[i];
			SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId)).getEffects(l2Character, l2Character);
		}
	}
	
	private static boolean pay(L2PcInstance player)
	{
		ItemInstance item = player.getInventory().getItemByItemId(Config.VIP_BUFF_ITEM);
		if (item == null || item.getCount() < Config.VIP_BUFF_PRICE)
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
			return false;
		}
		if (!player.destroyItem("Buffer", item, Config.VIP_BUFF_PRICE, player, true))
		{
			return false;
		}
		return true;
	}
}
