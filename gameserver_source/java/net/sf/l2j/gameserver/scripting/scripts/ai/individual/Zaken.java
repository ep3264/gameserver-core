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
package net.sf.l2j.gameserver.scripting.scripts.ai.individual;

/**
 * Zaken AI
 * @author dEvilKinG
 */

import net.sf.l2j.Config;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;
import net.sf.l2j.gameserver.taskmanager.GameTimeTaskManager;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.util.Util;

public class Zaken extends AbstractNpcAI
{
	private static final L2BossZone _zakenLair = ZoneManager.getInstance().getZoneById(110000, L2BossZone.class);
	private L2Object _target;// used for CallSkills
	int _telecheck; // used for zakens self teleportings
	private int _minionStatus = 0; // used for spawning minions cycles
	private int hate = 0; // used for most hated players progress
	private static final int[] Xcoords =
	{
		53950,
		55980,
		54950,
		55970,
		53930,
		55970,
		55980,
		54960,
		53950,
		53930,
		55970,
		55980,
		54960,
		53950,
		53930
	};
	private static final int[] Ycoords =
	{
		219860,
		219820,
		218790,
		217770,
		217760,
		217770,
		219920,
		218790,
		219860,
		217760,
		217770,
		219920,
		218790,
		219860,
		217760
	};
	private static final int[] Zcoords =
	{
		-3488,
		-3488,
		-3488,
		-3488,
		-3488,
		-3216,
		-3216,
		-3216,
		-3216,
		-3216,
		-2944,
		-2944,
		-2944,
		-2944,
		-2944
	};
	
	// Skills
	private static final int TELEPORT = 4216;
	private static final int MASS_TELEPORT = 4217;
	private static final int DRAIN = 4218;
	private static final int HOLD = 4219;
	private static final int DUAL_ATTACK = 4220;
	private static final int MASS_DUAL_ATTACK = 4221;
	private static final int SELF_TELEPORT = 4222;
	private static final int NIGHT_TO_DAY = 4223;
	private static final int DAY_TO_NIGHT = 4224;
	private static final int REGEN_NIGHT = 4227;
	private static final int REGEN_DAY = 4242;
	
	// Boss
	private static final int ZAKEN = 29022;
	
	// Minions
	private static final int DOLLBLADER = 29023;
	private static final int VALEMASTER = 29024;
	private static final int PIRATECAPTAIN = 29026;
	private static final int PIRATEZOMBIE = 29027;
	
	// ZAKEN Status Tracking :
	private static final byte ALIVE = 0; // Zaken is spawned.
	private static final byte DEAD = 1; // Zaken has been killed.
	
	public Zaken()
	{
		super("ai/individual");
		
		ThreadPool.scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				if (GetTimeHour() == 0)
				{
					DoorTable.getInstance().getDoor(21240006).openMe();
					ThreadPool.schedule(new Runnable()
					{
						@Override
						public void run()
						{
							DoorTable.getInstance().getDoor(21240006).closeMe();
						}
					}, 300000L);
				}
			}
		}, 2000L, 600000L);
		
		int[] mobs =
		{
			ZAKEN,
			DOLLBLADER,
			VALEMASTER,
			PIRATECAPTAIN,
			PIRATEZOMBIE
		};
		registerMobs(mobs);
		
		final StatsSet info = GrandBossManager.getInstance().getStatsSet(ZAKEN);
		final int status = GrandBossManager.getInstance().getBossStatus(ZAKEN);
		if (status == DEAD)
		{
			// load the unlock date and time for zaken from DB
			long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// if zaken is locked until a certain time, mark it so and start the unlock timer
			// the unlock time has not yet expired.
			if (temp > 0)
			{
				startQuestTimer("zaken_unlock", temp, null, null, false);
			}
			else
			{
				// the time has already expired while the server was offline. Immediately spawn zaken.
				int i1 = Rnd.get(15);
				L2GrandBossInstance zaken = (L2GrandBossInstance) addSpawn(ZAKEN, Xcoords[i1], Ycoords[i1], Zcoords[i1], i1, false, 0, false);
				GrandBossManager.getInstance().setBossStatus(ZAKEN, ALIVE);
				spawnBoss(zaken);
			}
		}
		else
		{
			int loc_x = info.getInteger("loc_x");
			int loc_y = info.getInteger("loc_y");
			int loc_z = info.getInteger("loc_z");
			int heading = info.getInteger("heading");
			int hp = info.getInteger("currentHP");
			int mp = info.getInteger("currentMP");
			L2GrandBossInstance zaken = (L2GrandBossInstance) addSpawn(ZAKEN, loc_x, loc_y, loc_z, heading, false, 0, false);
			zaken.setCurrentHpMp(hp, mp);
			spawnBoss(zaken);
		}
	}
	
	public void spawnBoss(L2GrandBossInstance npc)
	{
		if (npc == null)
		{
			_log.warning("Zaken AI failed to load, missing Zaken in grandboss_data.sql");
			return;
		}
		GrandBossManager.getInstance().addBoss(npc);
		
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		
		hate = 0;
		if (_zakenLair == null)
		{
			_log.warning("Zaken AI failed to load, missing zone for Zaken");
			return;
		}
		if (_zakenLair.isInsideZone(npc))
		{
			_minionStatus = 1;
			startQuestTimer("minion_cycle", 1700, null, null, true);
		}
		_telecheck = 3;
		startQuestTimer("timer", 1000, npc, null, false);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		int status = GrandBossManager.getInstance().getBossStatus(ZAKEN);
		if ((status == DEAD) && !event.equalsIgnoreCase("zaken_unlock"))
		{
			return super.onAdvEvent(event, npc, player);
		}
		
		if (event.equalsIgnoreCase("timer"))
		{
			if (GetTimeHour() < 5)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(DAY_TO_NIGHT, 1);
				if (npc.getFirstEffect(skill) == null)
				{
					npc.doCast(SkillTable.getInstance().getInfo(DAY_TO_NIGHT, 1));
					npc.doCast(SkillTable.getInstance().getInfo(REGEN_NIGHT, 1));
					
				}
			}
			else if (GetTimeHour() > 5)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(NIGHT_TO_DAY, 1);
				if (npc.getFirstEffect(skill) == null)
				{
					_telecheck = 3;
					npc.doCast(SkillTable.getInstance().getInfo(NIGHT_TO_DAY, 1));
					npc.doCast(SkillTable.getInstance().getInfo(REGEN_DAY, 1));
				}
			}
			
			L2Character _mostHated = null;
			if ((npc.getAI().getIntention() == CtrlIntention.ATTACK) && (hate == 0))
			{
				if (((L2Attackable) npc).getMostHated() != null)
				{
					_mostHated = ((L2Attackable) npc).getMostHated();
					hate = 1;
				}
			}
			else if ((npc.getAI().getIntention() == CtrlIntention.ATTACK) && (hate != 0))
			{
				if (((L2Attackable) npc).getMostHated() != null)
				{
					if (_mostHated == ((L2Attackable) npc).getMostHated())
					{
						hate = hate + 1;
					}
					else
					{
						hate = 1;
						_mostHated = ((L2Attackable) npc).getMostHated();
					}
				}
			}
			if (npc.getAI().getIntention() == CtrlIntention.IDLE)
			{
				hate = 0;
			}
			if (hate > 5)
			{
				((L2Attackable) npc).stopHating(_mostHated);
				L2Character nextTarget = ((L2Attackable) npc).getMostHated();
				if (nextTarget != null)
				{
					npc.getAI().setIntention(CtrlIntention.ATTACK, nextTarget);
				}
				hate = 0;
			}
			
			if (getPlayersCountInRadius(1500, npc, true) == 0)
			{
				npc.doCast(SkillTable.getInstance().getInfo(SELF_TELEPORT, 1));
			}
			
			startQuestTimer("timer", 30000, npc, null, true);
		}
		
		if (event.equalsIgnoreCase("minion_cycle"))
		{
			if (_minionStatus == 1)
			{
				int rr = Rnd.get(15);
				addSpawn(PIRATECAPTAIN, Xcoords[rr], Ycoords[rr], Zcoords[rr], Rnd.get(65536), false, 0, true);
				_minionStatus = 2;
			}
			else if (_minionStatus == 2)
			{
				int rr = Rnd.get(15);
				addSpawn(DOLLBLADER, Xcoords[rr], Ycoords[rr], Zcoords[rr], Rnd.get(65536), false, 0, true);
				_minionStatus = 3;
			}
			else if (_minionStatus == 3)
			{
				addSpawn(VALEMASTER, Xcoords[Rnd.get(15)], Ycoords[Rnd.get(15)], Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, Xcoords[Rnd.get(15)], Ycoords[Rnd.get(15)], Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true);
				_minionStatus = 4;
			}
			else if (_minionStatus == 4)
			{
				addSpawn(PIRATEZOMBIE, Xcoords[Rnd.get(15)], Ycoords[Rnd.get(15)], Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, Xcoords[Rnd.get(15)], Ycoords[Rnd.get(15)], Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, Xcoords[Rnd.get(15)], Ycoords[Rnd.get(15)], Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, Xcoords[Rnd.get(15)], Ycoords[Rnd.get(15)], Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, Xcoords[Rnd.get(15)], Ycoords[Rnd.get(15)], Zcoords[Rnd.get(15)], Rnd.get(65536), false, 0, true);
				_minionStatus = 5;
			}
			else if (_minionStatus == 5)
			{
				addSpawn(DOLLBLADER, 52675, 219371, -3290, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 52687, 219596, -3368, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 52672, 219740, -3418, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 52857, 219992, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 52959, 219997, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 53381, 220151, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 54236, 220948, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54885, 220144, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55264, 219860, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 55399, 220263, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55679, 220129, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 56276, 220783, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 57173, 220234, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 56267, 218826, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 56294, 219482, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 56094, 219113, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 56364, 218967, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 57113, 218079, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 56186, 217153, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55440, 218081, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 55202, 217940, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55225, 218236, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54973, 218075, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 53412, 218077, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 54226, 218797, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 54394, 219067, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54139, 219253, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 54262, 219480, -3488, Rnd.get(65536), false, 0, true);
				_minionStatus = 6;
			}
			else if (_minionStatus == 6)
			{
				addSpawn(PIRATEZOMBIE, 53412, 218077, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 54413, 217132, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 54841, 217132, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 55372, 217128, -3343, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 55893, 217122, -3488, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 56282, 217237, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 56963, 218080, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 56267, 218826, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 56294, 219482, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 56094, 219113, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 56364, 218967, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 56276, 220783, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 57173, 220234, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54885, 220144, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55264, 219860, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 55399, 220263, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55679, 220129, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 54236, 220948, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 54464, 219095, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 54226, 218797, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 54394, 219067, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54139, 219253, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 54262, 219480, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 53412, 218077, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55440, 218081, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 55202, 217940, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55225, 218236, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54973, 218075, -3216, Rnd.get(65536), false, 0, true);
				_minionStatus = 7;
			}
			else if (_minionStatus == 7)
			{
				addSpawn(PIRATEZOMBIE, 54228, 217504, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 54181, 217168, -3216, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 54714, 217123, -3168, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 55298, 217127, -3073, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 55787, 217130, -2993, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 56284, 217216, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 56963, 218080, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 56267, 218826, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 56294, 219482, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 56094, 219113, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 56364, 218967, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 56276, 220783, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 57173, 220234, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54885, 220144, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55264, 219860, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 55399, 220263, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55679, 220129, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 54236, 220948, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 54464, 219095, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 54226, 218797, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(VALEMASTER, 54394, 219067, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54139, 219253, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(DOLLBLADER, 54262, 219480, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 53412, 218077, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 54280, 217200, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55440, 218081, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATECAPTAIN, 55202, 217940, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 55225, 218236, -2944, Rnd.get(65536), false, 0, true);
				addSpawn(PIRATEZOMBIE, 54973, 218075, -2944, Rnd.get(65536), false, 0, true);
				cancelQuestTimer("minion_cycle", null, null);
			}
		}
		
		else if (event.equalsIgnoreCase("zaken_unlock"))
		{
			int i1 = Rnd.get(15);
			L2GrandBossInstance zaken = (L2GrandBossInstance) addSpawn(ZAKEN, Xcoords[i1], Ycoords[i1], Zcoords[i1], i1, false, 0, false);
			GrandBossManager.getInstance().setBossStatus(ZAKEN, ALIVE);
			spawnBoss(zaken);
		}
		else if (event.equalsIgnoreCase("CreateOnePrivateEx"))
		{
			addSpawn(npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, true);
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	public String onFactionCall(L2Npc npc, L2NpcInstance caller, L2PcInstance attacker, boolean isPet)
	{
		if ((caller == null) || (npc == null))
		{
			return super.onFactionCall(npc, caller, attacker, isPet);
		}

		if ((GetTimeHour() < 5) && (caller.getNpcId() != ZAKEN) && (npc.getNpcId() != ZAKEN))
		{
			if ((npc.getAI().getIntention() == CtrlIntention.IDLE) && (caller.getCurrentHp() < (0.9 * caller.getMaxHp())) && (Rnd.get(450) < 1))
			{
				int x = caller.getX();
				int y = caller.getY();
				int z = caller.getZ();
				npc.teleToLocation(x, y, z, 0);
			}
		}
		return super.onFactionCall(npc, caller, attacker, isPet);
	}
	
	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		if (npc.getNpcId() == ZAKEN)
		{
			int skillId = skill.getId();
			if (skillId == SELF_TELEPORT)
			{
				int i1 = Rnd.get(15);
				npc.teleToLocation(Xcoords[i1], Ycoords[i1], Zcoords[i1], 0);
				npc.getAI().setIntention(CtrlIntention.IDLE);
			}
			else if (skillId == TELEPORT)
			{
				int i1 = Rnd.get(15);
				player.teleToLocation(Xcoords[i1], Ycoords[i1], Zcoords[i1], i1);
				((L2Attackable) npc).stopHating(player);
				L2Character nextTarget = ((L2Attackable) npc).getMostHated();
				if (nextTarget != null)
				{
					npc.getAI().setIntention(CtrlIntention.ATTACK, nextTarget);
				}
				
			}
			else if (skillId == MASS_TELEPORT)
			{
				int i1 = Rnd.get(15);
				player.teleToLocation(Xcoords[i1], Ycoords[i1], Zcoords[i1], i1);
				((L2Attackable) npc).stopHating(player);
				
				for (L2Character character : npc.getKnownList().getKnownType(L2MonsterInstance.class))
				{
					if ((character != player) && !Util.checkIfInRange(250, player, character, true))
					{
						int r1 = Rnd.get(15);
						character.teleToLocation(Xcoords[r1], Ycoords[r1], Zcoords[r1], r1);
						((L2Attackable) npc).stopHating(character);
					}
				}
				
				L2Character nextTarget = ((L2Attackable) npc).getMostHated();
				if (nextTarget != null)
				{
					npc.getAI().setIntention(CtrlIntention.ATTACK, nextTarget);
				}
			}
		}
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (skill.getAggroPoints() > 0)
		{
			((L2Attackable) npc).addDamageHate(caster, 0, ((skill.getAggroPoints() / npc.getMaxHp()) * 10 * 150));
		}
		
		if (Rnd.get(12) < 1)
		{
			_target = caster;
			CallSkills(npc);
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}
	
	@Override
	public String onAggro(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		if (npc == null)
			return null;
			
		final boolean isMage;
		final L2Playable character;
		if (isPet)
		{
			isMage = false;
			character = player.getPet();
		}
		else
		{
			isMage = player.isMageClass();
			character = player;
		}
		
		if (character == null)
			return null;
			
		if (!Config.RAID_DISABLE_CURSE && character.getLevel() - npc.getLevel() > 8)
		{
			L2Skill curse = null;
			if (isMage)
			{
				if (!character.isMuted() && Rnd.get(4) == 0)
					curse = FrequentSkill.RAID_CURSE.getSkill();
			}
			else
			{
				if (!character.isParalyzed() && Rnd.get(4) == 0)
					curse = FrequentSkill.RAID_CURSE2.getSkill();
			}
			
			if (curse != null)
			{
				npc.broadcastPacket(new MagicSkillUse(npc, character, curse.getId(), curse.getLevel(), 300, 0));
				curse.getEffects(npc, character);
			}
			
			((L2Attackable) npc).stopHating(character); // for calling again
			return null;
		}
		
		if (_zakenLair.isInsideZone(npc))
		{
			L2Character target = isPet ? player.getPet() : player;
			((L2Attackable) npc).addDamageHate(target, 1, 200);
		}
		
		int npcId = npc.getNpcId();
		if (npcId == ZAKEN)
		{
			if (Rnd.get(15) < 1)
			{
				_target = player;
				CallSkills(npc);
			}
		}
		return super.onAggro(npc, player, isPet);
	}
	
	public void CallSkills(L2Npc npc)
	{
		if (npc.isCastingNow())
			return;
			
		int chance = Rnd.get(225);
		npc.setTarget(_target);
		if (chance < 1)
			npc.doCast(SkillTable.getInstance().getInfo(TELEPORT, 1));
			
		else if (chance < 2)
			npc.doCast(SkillTable.getInstance().getInfo(MASS_TELEPORT, 1));
			
		else if (chance < 4)
			npc.doCast(SkillTable.getInstance().getInfo(HOLD, 1));
			
		else if (chance < 8)
			npc.doCast(SkillTable.getInstance().getInfo(DRAIN, 1));
			
		else if (chance < 15)
			npc.doCast(SkillTable.getInstance().getInfo(MASS_DUAL_ATTACK, 1));
			
		if (Rnd.get(2) < 1)
		{
			if (_target == ((L2Attackable) npc).getMostHated())
			{
				npc.doCast(SkillTable.getInstance().getInfo(DUAL_ATTACK, 1));
			}
		}
		
		if ((GetTimeHour() > 5) && (npc.getCurrentHp() < (npc.getMaxHp() * _telecheck) / 4))
		{
			_telecheck -= 1;
			npc.doCast(SkillTable.getInstance().getInfo(SELF_TELEPORT, 1));
		}
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill l2skill)
	{
		int npcId = npc.getNpcId();
		if (npcId == ZAKEN)
		{
			if (attacker.getMountType() == 1)
			{
				L2Skill skill = SkillTable.getInstance().getInfo(4258, 1);
				if (attacker.getFirstEffect(skill) == null)
				{
					npc.setTarget(attacker);
					npc.doCast(skill);
				}
			}
			final L2Character originalAttacker = isPet ? attacker.getPet() : attacker;
			final int hating = (int) ((damage / npc.getMaxHp() / 0.05) * 20000);
			((L2Attackable) npc).addDamageHate(originalAttacker, 0, hating);
			
			if (Rnd.get(10) < 1)
			{
				_target = attacker;
				CallSkills(npc);
			}
			
		}
		return super.onAttack(npc, attacker, damage, isPet, l2skill);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		int npcId = npc.getNpcId();
		if (npcId == ZAKEN)
		{
			cancelQuestTimer("timer", npc, null);
			cancelQuestTimer("minion_cycle", npc, null);
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			GrandBossManager.getInstance().setBossStatus(ZAKEN, DEAD);
			long respawnTime = (long) Config.SPAWN_INTERVAL_ZAKEN + Rnd.get(-Config.RANDOM_SPAWN_TIME_ZAKEN, Config.RANDOM_SPAWN_TIME_ZAKEN);
			respawnTime *= 3600000;
			startQuestTimer("zaken_unlock", respawnTime, null, null, false);
			
			StatsSet info = GrandBossManager.getInstance().getStatsSet(ZAKEN);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatsSet(ZAKEN, info);
		}
		else if (GrandBossManager.getInstance().getBossStatus(ZAKEN) == ALIVE)
		{
			startQuestTimer("CreateOnePrivateEx", ((30 + Rnd.get(60)) * 1000), npc, null, false);
		}
		return super.onKill(npc, killer, isPet);
	}
	
	public int GetTimeHour()
	{
		return GameTimeTaskManager.getInstance().getGameHour();
	}
}