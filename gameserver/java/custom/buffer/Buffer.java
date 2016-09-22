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

import java.util.LinkedList;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


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
	 * Format buff: 1068,3
	 * Format set buffs: fbuff 
	 */
	public Buffer()
	{
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
					//Punisher.getInstance().illegalAction(activeChar.getClient(), (byte) 0x11);
					//Illegal action
				}
			}
			else if (action.equalsIgnoreCase("sbuffs"))
			{
				L2Effect l2Effects[] = activeChar.getAllEffects();
				activeChar.getSavedBuffs().clear();
				for (L2Effect l2Effect : l2Effects)
				{
					String str = Integer.toString(l2Effect.getSkill().getId()) +
						"," + Integer.toString(l2Effect.getSkill().getLevel());
					if (AllowedBuffsSet.ALLOWED_PLAYER_BUFFS.contains(str))
					{
						activeChar.getSavedBuffs().add(str);
					}
				}
			}
			else if (action.equalsIgnoreCase("gsbuffs"))
			{
				LinkedList<String> savedBuffs = activeChar.getSavedBuffs();
				for (String str : savedBuffs)
				{
					int lenStr = str.length();
					int buff = Integer.parseInt(str.substring(0, lenStr - 2));
					int levelBuff = Integer.parseInt(str.substring(lenStr - 1, lenStr));
					SkillTable.getInstance().getInfo(buff, levelBuff).getEffects(l2Character, l2Character);
				}
			}
			else if (action.equalsIgnoreCase("mbuff"))
			{
				getMageBuff(l2Character);
			}
			else if (action.equalsIgnoreCase("fbuff"))
			{
				getFigtherBuff(l2Character);
			}
			else if (action.equalsIgnoreCase("dbuff"))
			{
				getDaggerBuff(l2Character);
			}
			else if (action.equalsIgnoreCase("tbuff"))
			{
				getTankBuff(l2Character);
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
	
	/*
	private void getPetBuff()
	{
		final L2Summon summon = activeChar.getPet();
		if (summon != null)
		{
			activeChar.sendMessage("You get a Pet-buff complect.");
			SkillTable.getInstance().getInfo(4344, 3).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4346, 4).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4349, 2).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4345, 3).getEffects(activeChar.getPet(), activeChar.getPet());
			//Greater Might
			SkillTable.getInstance().getInfo(1388, 3).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4347, 6).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4352, 2).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4360, 3).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4358, 3).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4357, 2).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4359, 3).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4342, 2).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(264, 1).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(267, 1).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(268, 1).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(269, 1).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(271, 1).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(274, 1).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(275, 1).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(1363, 1).getEffects(activeChar.getPet(), activeChar.getPet());		
			SkillTable.getInstance().getInfo(4355, 3).getEffects(activeChar.getPet(), activeChar.getPet());
			SkillTable.getInstance().getInfo(4356, 3).getEffects(activeChar.getPet(), activeChar.getPet());
			
		}
		else
		{
			activeChar.sendMessage("No pet or summon.");
		}
	}
	*/
	private static void getFigtherBuff(L2Character l2Character)
	{
		SkillTable.getInstance().getInfo(4344, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4346, 4).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4349, 2).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(1068, 3).getEffects(l2Character, l2Character);
		//Greater Might
		SkillTable.getInstance().getInfo(1388, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4347, 6).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4352, 2).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4360, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4358, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4357, 2).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4359, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4342, 2).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(264, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(267, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(268, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(269, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(271, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(274, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(275, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(1363, 1).getEffects(l2Character, l2Character);
		
		l2Character.setCurrentHpMp(l2Character.getMaxHp(), l2Character.getMaxMp());
	}
	
	private static void getTankBuff(L2Character l2Character)
	{
		//Clann Hall: Shield
		SkillTable.getInstance().getInfo(4344, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Mental Shield
		SkillTable.getInstance().getInfo(4346, 4).getEffects(l2Character, l2Character);
		//Clan Hall: Magic Barrier
		SkillTable.getInstance().getInfo(4349, 2).getEffects(l2Character, l2Character);
		//Greater Shield
		SkillTable.getInstance().getInfo(1389, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Might
		//SkillTable.getInstance().getInfo(4345, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Blessed Body
		SkillTable.getInstance().getInfo(4347, 6).getEffects(l2Character, l2Character);
		//Clan Hall: Blessed Soul
		//SkillTable.getInstance().getInfo(4348, 6).getEffects(l2Character, l2Character);
		//Clan Hall: Berserker Spirit
		//SkillTable.getInstance().getInfo(4352, 2).getEffects(l2Character, l2Character);
		//Chant Vampire
		SkillTable.getInstance().getInfo(1310,4).getEffects(l2Character, l2Character);
		//Agility
		//SkillTable.getInstance().getInfo(1087, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Death Whisper
		SkillTable.getInstance().getInfo(4360, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Guidance
		SkillTable.getInstance().getInfo(4358, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Haste
		SkillTable.getInstance().getInfo(4357, 2).getEffects(l2Character, l2Character);
		//Clan Hall: Focus
		SkillTable.getInstance().getInfo(4359, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Wind Walk
		SkillTable.getInstance().getInfo(4342, 2).getEffects(l2Character, l2Character);
		//Clarity
		//SkillTable.getInstance().getInfo(1397, 3).getEffects(l2Character, l2Character);
		//Song of Earth
		SkillTable.getInstance().getInfo(264, 1).getEffects(l2Character, l2Character);
		//Song of Water
		//SkillTable.getInstance().getInfo(266, 1).getEffects(l2Character, l2Character);
		//Song of Warding
		SkillTable.getInstance().getInfo(267, 1).getEffects(l2Character, l2Character);
		//Song of Wind
		//SkillTable.getInstance().getInfo(268, 1).getEffects(l2Character, l2Character);
		//Song of Hunter
		//SkillTable.getInstance().getInfo(269, 1).getEffects(l2Character, l2Character);
		//Song of Vitality
		SkillTable.getInstance().getInfo(304, 1).getEffects(l2Character, l2Character);
		//Dance of the Warrior
		//SkillTable.getInstance().getInfo(271, 1).getEffects(l2Character, l2Character);
		//Dance of Fire
		//SkillTable.getInstance().getInfo(274, 1).getEffects(l2Character, l2Character);
		//Dance of Fury
		SkillTable.getInstance().getInfo(275, 1).getEffects(l2Character, l2Character);
		//Dance of the Vampire
		SkillTable.getInstance().getInfo(310, 1).getEffects(l2Character, l2Character);
		//Chant of Victory
		SkillTable.getInstance().getInfo(1363, 1).getEffects(l2Character, l2Character);
		//Gift Queen
		//SkillTable.getInstance().getInfo(4700, 3).getEffects(l2Character, l2Character);
		//Blessing of Seraphim
		//SkillTable.getInstance().getInfo(4703, 3).getEffects(l2Character, l2Character);
		//Noblesse Blessing
		//SkillTable.getInstance().getInfo(1323, 1).getEffects(l2Character, l2Character);
		l2Character.setCurrentHpMp(l2Character.getMaxHp(), l2Character.getMaxMp());
	}
	
	private static void getDaggerBuff(L2Character l2Character)
	{
		
		SkillTable.getInstance().getInfo(4347, 6).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4357, 2).getEffects(l2Character, l2Character);
		//Clann Hall: Shield
		SkillTable.getInstance().getInfo(4344, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Mental Shield
		SkillTable.getInstance().getInfo(4346, 4).getEffects(l2Character, l2Character);
		//Clan Hall: Magic Barrier
		SkillTable.getInstance().getInfo(4349, 2).getEffects(l2Character, l2Character);
		//Greater Shield
		SkillTable.getInstance().getInfo(1389, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(267, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(1363, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4359, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4342, 2).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4360, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(264, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(274, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(275, 1).getEffects(l2Character, l2Character);
		l2Character.setCurrentHpMp(l2Character.getMaxHp(), l2Character.getMaxMp());
	}
	
	private static void getMageBuff(L2Character l2Character)
	{
		SkillTable.getInstance().getInfo(1397, 3).getEffects(l2Character, l2Character);
		//Clan Hall: Wind Walk
		SkillTable.getInstance().getInfo(4342, 2).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4344, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4346, 4).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4349, 2).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(1243, 6).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(1389, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4347, 6).getEffects(l2Character, l2Character);
		//Clan Hall: Acumen
		SkillTable.getInstance().getInfo(4355, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4356, 3).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(4352, 2).getEffects(l2Character, l2Character);
		//Wild Magic
		SkillTable.getInstance().getInfo(1303, 1).getEffects(l2Character, l2Character);
		
		//Clan Hall: Concentration
		SkillTable.getInstance().getInfo(4351, 6).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(264, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(268, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(267, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(304, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(273, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(276, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(365, 1).getEffects(l2Character, l2Character);
		//renewal
		SkillTable.getInstance().getInfo(349, 1).getEffects(l2Character, l2Character);
		SkillTable.getInstance().getInfo(1413, 1).getEffects(l2Character, l2Character);
		
		l2Character.setCurrentHpMp(l2Character.getMaxHp(), l2Character.getMaxMp());
	}
	
}
