package com.l2je.extensions.events.ew;

import com.l2je.extensions.events.CombatEvent;
import com.l2je.extensions.events.EventConfig;
import com.l2je.extensions.events.EventManager;
import com.l2je.extensions.events.commons.Reward;

import java.util.ArrayList;

/**
 * Developers: L2JE Team<br>
 * <br>
 * <br>
 * Author: dbg<br>
 * Date: 21 февр. 2016 г.<br>
 * Time: 2:01:05<br>
 * <br>
 */


import java.util.Random;
import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;

import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.zone.ZoneId;

public class EnchantWar extends CombatEvent
{

	private final int NPC_ID = 40005;
	private int _currentTownId;
	private L2Spawn _spawn = null;
	protected final static String DESCRIPTION = "Ищите продавца заточек в городе:</br> ";
	
	public EnchantWar()
	{
		super();
		setName("Enchant War");
		setId(EventConfig.EW_ID);
	}
	
	@Override
	public void init()
	{
		initBlockItems();
		initBlockSkills();
		getTownId();
		spawnNpc();
	}
	
	private void getTownId()
	{
		int[] townIds = EventConfig.TOWN_IDS;
		if (townIds != null)
		{
			Random rnd = new Random();
			_currentTownId = townIds[rnd.nextInt(townIds.length)];
		}
		else
		{
			_currentTownId = 1;// Dark Elven Village
		}
	}
	
	@Override
	public void start()
	{
		trigger();
	}
	
	@Override
	public boolean isRunning()
	{
		return _eventState == EventState.ACTIVE;
	}
	
	@Override
	public void end(boolean inform)
	{		
		if (_spawn != null)
		{
			deleteSpawnNpc(false);
		}		
		super.end(inform);
	}
	/**
	 * Регистрация всегда открыта
	 */
	@Override
	public boolean isRegistering()
	{
		return true;
	}
	@Override
	public void trigger()
	{
		switch (_eventState)
		{
			case IDLE:
				init();
				announce("Хочешь заточиться участвуй в эвенте Enchant War!");
				announce("Event Enchant War начался.");
				announce("Команды: " + EVENT_COMMANDS);
				announce("Город: " + MapRegionTable.getInstance().getClosestTownName(_currentTownId));
				_eventState = EventState.ACTIVE;
				schedule(EventConfig.EW_RUNNING_TIME);
				break;
			case ACTIVE:
				EventManager.getInstance().endCurrentEvent(true);
				break;
		}
	}
	
	@Override
	public void validatePostion(L2PcInstance player)
	{
		if (!isRunning())
		{
			return;
		}
		if (!containsPlayer(player))
		{
			return;
		}
		if (MapRegionTable.getClosestTown(player.getX(), player.getY()) == MapRegionTable.getTown(_currentTownId))
		{
			if (!player.isInsideZone(ZoneId.PEACE))
			{
				playerLeftTown(player);
			}
		}
		else
		{
			playerLeftTown(player);
		}
	}
	
	private void playerLeftTown(L2PcInstance player)
	{
		if (EventConfig.EVENT_MANAGER_DEBUG)
			_log.info(player.getName() + " has left the event town.");
		teleportPlayerToEvent(player);
	}
	
	@Override
	protected boolean pvpOnKill()
	{
		return EventConfig.EW_PVP_ON_KILL;
	}
	
	@Override
	public boolean addPlayer(L2PcInstance player)
	{		
		if (!super.addPlayer(player))
		{
			return false;
		}		
		player.setEventTitleColor(EventConfig.EW_TITLE_COLOR);
		player.setEventNameColor(EventConfig.EW_NAME_COLOR);
		player.setTeam(1);		
		teleportPlayerToEvent(player);
		spawnNpc();
		return true;
	}
	
	private void spawnNpc()
	{
		if (_members.size() >= getMinPlayers() && _spawn == null)
		{
			Location loc = null;
			if (_currentTownId == 13)
			{
				loc = new Location(147576, -55992, -2776); // static spawn in Goddard
			}
			else
			{
				loc = MapRegionTable.getTown(_currentTownId).getSpawnLoc();
			}
			if (!spawnNpc(NPC_ID, 10, false, new int[]
			{
				loc.getX(),
				loc.getY(),
				loc.getZ()
			}))
			{
				if (EventConfig.EVENT_MANAGER_DEBUG)
					_log.info("Failed spawn enchant npc");
			}
		}
	}
	
	@Override
	public boolean canRequestToNpc(L2PcInstance player, L2Character npc)
	{
		NpcTemplate npcTemplate = (NpcTemplate) npc.getTemplate();
		if (!containsPlayer(player) && npcTemplate.getNpcId() == NPC_ID)
		{
			player.sendMessage("Вы не участник Enchant War.");
			return false;
		}
		return true;
	}
	
	@Override
	protected void teleportPlayerToEvent(L2PcInstance player)
	{
		resurrect(player,true);
		player.teleToLocation(MapRegionTable.getTown(_currentTownId).getSpawnLoc(), 5);
	}
	
	@Override
	protected void handleCounter(int delay)
	{
		StringBuilder sb = new StringBuilder();
		switch (_eventState)
		{
			case ACTIVE:
				switch (delay)
				{
					case 600:
					case 300:
					case 60:
						StringUtil.append(sb, "Event закончится через ", (delay / 60), " минут(у).");
						sendMessageToAllPlayers(sb.toString());
						break;
					case 30:
					case 15:
					case 10:
					case 5:
						StringUtil.append(sb, "Event закончится через ", delay, " секунд(у).");
						sendMessageToAllPlayers(sb.toString());
						break;
				}
				break;
		}
	}
	
	private boolean spawnNpc(int npcId, int respawnTime, boolean permanent, int[] coordinat)
	{
		NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		if (template == null)
		{
			return false;
		}
		try
		{
			_spawn = new L2Spawn(template);
			_spawn.setLoc(coordinat[0], coordinat[1], coordinat[2], -1);
			_spawn.setRespawnDelay(respawnTime);
			SpawnTable.getInstance().addNewSpawn(_spawn, permanent);
			_spawn.doSpawn(false);
			_spawn.setRespawnState(permanent);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}
	
	private void deleteSpawnNpc(boolean updateDb)
	{
		L2Npc l2Npc = _spawn.getNpc();
		l2Npc.deleteMe();
		SpawnTable.getInstance().deleteSpawn(_spawn, updateDb);
		_spawn = null;
	}
	
	@Override
	protected void initBlockItems()
	{
		for (Integer itemId : EventConfig.EW_BLOCKED_ITEMS)
			_blockedItems.add(itemId);
	}
	
	@Override
	protected void initBlockSkills()
	{
		for (Integer skillId : EventConfig.EW_BLOCKED_SKILLS)
			_blockedSkills.add(skillId);
	}
	@Override
	protected boolean rewardOnKill()
	{
		return EventConfig.EW_REWARD!=null;
	}	
	@Override
	public int getRunningTime()
	{
		return EventConfig.EW_RUNNING_TIME;
	}
	
	@Override
	protected int getResTime()
	{
		return EventConfig.EW_RES_TIME;
	}
	
	@Override
	protected int getMinPlayers()
	{
		return EventConfig.EW_MIN_PLAYERS;
	}
	
	@Override
	protected int getMaxPlayers()
	{
		return EventConfig.EW_MAX_PLAYERS;
	}
	
	@Override
	protected String getDesription()
	{
		StringBuilder sb = new StringBuilder();
		StringUtil.append(sb, DESCRIPTION,  MapRegionTable.getInstance().getClosestTownName(_currentTownId));
		return sb.toString();
	}
	@Override
	protected String getScoreTitlePattern()
	{
		return EventConfig.EW_SCORE_TITLE_PATTERN;
	}
	@Override
	protected ArrayList<Reward> getReward()
	{
		return EventConfig.ewReward;
		
	}
}