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
package custom.buffer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * Developers: Redist Team<br>
 * Official Website: <br>
 * <br>
 * Author: Redist<br>
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
	
	/*
	 * Format buff: 1068,3 Format set buffs: fbuff
	 */
	public Buffer()
	{
	}
	
	private final static int[] vipfbuff =
	{		
		4699,
		1392,
		1352,
		1353,
		1182,
		1189,
		1191
	};
	private final static int[] vipmbuff =
	{
		4703,		
		1392,
		1352,
		1353,
		1182,
		1189,
		1191
	};
	private final static int[] mbuff =
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
	
	private final static int[] fbuff =
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
			String regex = "\\d*,\\d{1}";
			if (action.matches(regex))
			{
				if (AllowedBuffsSet.ALLOWED_PLAYER_BUFFS.contains(action))
				{
					int lenStr = action.length();
					int buff = Integer.parseInt(action.substring(0, lenStr - 2));
					int levelBuff = Integer.parseInt(action.substring(lenStr - 1, lenStr));
					SkillTable.getInstance().getInfo(buff, levelBuff).getEffects(l2Character, l2Character);
				}
				else
				{
					// Punisher.getInstance().illegalAction(activeChar.getClient(), (byte) 0x11);
					// Illegal action
				}
			}
			else if (action.equalsIgnoreCase("mbuff"))
			{
				getBuffSet(l2Character, mbuff);
			}
			else if (action.equalsIgnoreCase("fbuff"))
			{
				getBuffSet(l2Character, fbuff);
			}
			else if (action.equalsIgnoreCase("dbuff"))
			{
				getBuffSet(l2Character, DAGGER_BUFFS);
			}
			else if (action.equalsIgnoreCase("tbuff"))
			{
				getBuffSet(l2Character, TANK_BUFFS);
			}
			else if (action.equalsIgnoreCase("vipfbuff"))
			{
				if (activeChar.getPremiumService() > 0)
				{
					getBuffSet(l2Character, vipfbuff);
				}
				else if (pay(activeChar))
				{
					getBuffSet(l2Character, vipfbuff);
				}
			}
			else if (action.equalsIgnoreCase("vipmbuff"))
			{
				if (activeChar.getPremiumService() > 0)
				{
					getBuffSet(l2Character, vipmbuff);
				}
				else if (pay(activeChar))
				{
					getBuffSet(l2Character, vipmbuff);
				}
			}
			else if (action.equalsIgnoreCase("heal"))
			{
				l2Character.setCurrentHpMp(l2Character.getMaxHp(), l2Character.getMaxMp());
				l2Character.setCurrentCp(l2Character.getMaxCp());
			}
			else if (action.equalsIgnoreCase("cancel"))
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
