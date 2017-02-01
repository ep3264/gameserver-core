package com.l2je.custom.events;

import com.l2je.custom.events.CustomUtil;
import com.l2je.custom.events.EventManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Developers: Redist Team<br>
 * <br>
 * <br>
 * Author: Redist<br>
 * Date: 21 февр. 2016 г.<br>
 * Time: 2:01:05<br>
 * <br>
 */

import java.util.List;
import java.util.Map;
import java.util.Random;
//import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.lang.StringUtil;
//import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.zone.ZoneId;
//import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceld;
//import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
//import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;

public class EW extends CombatEvent
{
	private enum EventState
	{
		IDLE,
		ACTIVE
	}
	
	private EventState _eventState = EventState.IDLE;
	
	private final int ID = 2;
	private final String NAME = "EW";
	private final int NPC_ID = 40005;
	private int _townId;
	private int _minPlayersRequiredForNpc = 5;
	private int _maxPlayersAllowed = 75;
	private L2Spawn spawn = null;
	
	public EW()
	{
		super();
	}
	
	@Override
	public void setConfig(Map<String, String> config)
	{
		super.setConfig(config);
		String townIds = getString("townIds");
		if (townIds != null)
		{
			List<Integer> possibleTownIds = new ArrayList<>();
			for (String townId : townIds.split(","))
			{
				possibleTownIds.add(Integer.valueOf(townId.trim()));
			}
			Random rnd = new Random();
			_townId = possibleTownIds.get(rnd.nextInt(possibleTownIds.size()));
		}
		else
		{
			_townId = 1;// Dark Elven Village
		}
		_minPlayersRequiredForNpc = getInt("minPlayersRequiredForNpc");
		_maxPlayersAllowed = getInt("maxPlayersAllowed");
	}
	
	@Override
	public boolean isRegistering()
	{
		return true;
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
		_eventState = EventState.IDLE;
		if (spawn != null)
		{
			deleteSpawnNpc(false);
		}
		teleportPlayersFromEvent();
		super.end(inform);
	}
	
	@Override
	public void trigger()
	{
		switch (_eventState)
		{
			case IDLE:
				announce("Хочешь заточиться участвуй в эвенте Enchant War!");
				announce("Event Enchant War начался.");
				announce("Команды: " + EVENT_COMMANDS);
				announce("Город: " + MapRegionTable.getInstance().getClosestTownName(_townId));
				_eventState = EventState.ACTIVE;
				schedule(getInt("runningTime"));
				break;
			case ACTIVE:
				_eventState = EventState.IDLE;
				EventManager.getInstance().end(true);
				break;
		}
	}
	
	@Override
	protected void teleportPlayerFromEvent(L2PcInstance player)
	{
		if (player.isDead())
		{
			player.doRevive();
		}
		if (getBoolean("removeBuffs"))
		{
			player.stopAllEffects();
			player.stopAllToggles();
			player.stopCubics();
			if (_effects.containsKey(player))
			{
				L2Skill buff;
				for (L2Effect effect : _effects.get(player))
				{
					buff = effect.getSkill();
					if (buff != null)
					{
						buff.getEffects(player, player);
					}
				}
			}
		}
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		int[] location = FAIL_SAFE_LOCATION;
		if (_locations.containsKey(player))
		{
			location = _locations.get(player);
			// _locations.remove(player);
		}
		player.setEventTitle(null);
		player.setEventTitleColor(null);
		player.setEventNameColor(null);
		player.setTeam(0);
		player.teleToLocation(location[0], location[1], location[2], 0);
	}
	
	@Override
	public boolean canAttackInPeace(L2Character attacker, L2Character target)
	{
		if (!isRunning())
		{
			return false;
		}
		L2PcInstance attackerInstance;
		if (!(attacker instanceof L2PcInstance))
		{
			if (target instanceof L2SummonInstance)
			{
				attackerInstance = ((L2SummonInstance) target).getOwner();
			}
			else
			{
				return false;
			}
		}
		else
		{
			attackerInstance = (L2PcInstance) attacker;
		}
		L2PcInstance targetInstance;
		if (!(target instanceof L2PcInstance))
		{
			if (target instanceof L2SummonInstance)
			{
				targetInstance = ((L2SummonInstance) target).getOwner();
			}
			else
			{
				return false;
			}
		}
		else
		{
			targetInstance = (L2PcInstance) target;
		}
		if (!containsPlayer(attackerInstance) || !containsPlayer(targetInstance))
		{
			return false;
		}
		if (MapRegionTable.getClosestTown(attackerInstance.getX(), attackerInstance.getY()) == MapRegionTable.getTown(_townId))
		{
			return true;
		}
		return false;
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
		if (MapRegionTable.getClosestTown(player.getX(), player.getY()) == MapRegionTable.getTown(_townId))
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
		debug(player.getName() + " has left the event town.");
		teleportPlayerToEvent(player);
	}
	
	@Override
	public void onKill(L2Character killer, L2Character killed)
	{
		if (!isRunning())
		{
			return;
		}
		if (killer == killed)
		{
			return;
		}
		L2PcInstance killerPc = null;
		L2PcInstance killedPc = null;
		if (killer instanceof L2PcInstance)
		{
			killerPc = (L2PcInstance) killer;
		}
		else if (killer instanceof L2SummonInstance)
		{
			killerPc = ((L2Summon) killer).getOwner();
		}
		if (killed instanceof L2PcInstance)
		{
			killedPc = (L2PcInstance) killed;
		}
		else if (killed instanceof L2SummonInstance)
		{
			killedPc = ((L2Summon) killed).getOwner();
		}
		if (killerPc == null || killedPc == null)
		{
			return;
		}
		if (!containsPlayer(killerPc) || !containsPlayer(killedPc))
		{
			return;
		}
		if (killerPc == killedPc)
		{
			return;
		}
		if (MapRegionTable.getClosestTown(killerPc.getX(), killerPc.getY()) != MapRegionTable.getTown(_townId) || MapRegionTable.getClosestTown(killedPc.getX(), killedPc.getY()) != MapRegionTable.getTown(_townId))
		{
			return;
		}
		debug(killerPc.getName() + (killer instanceof L2SummonInstance ? "'s Summon" : "") + " has killed " + killedPc.getName() + (killed instanceof L2SummonInstance ? "'s Summon" : "") + ".");
		setScore(killerPc, _scores.get(killerPc) + 1);
		if (killed instanceof L2PcInstance)
		{
			if (!getBoolean("allowFixedResurrection"))
			{
				addResurrector(killedPc);
			}
		}
		if (getBoolean("rewardPvpOnKill"))
		{
			killerPc.setPvpKills(killerPc.getPvpKills() + 1);
		}
		if (getBoolean("decreaseScoreOnDeath"))
		{
			setScore(killedPc, _scores.get(killedPc) - 1);
		}
		if (getString("rewardPerKill") != null)
		{
			rewardPlayer(killerPc, "rewardPerKill");
		}
	}
	
	@Override
	public boolean addPlayer(L2PcInstance player)
	{
		if (_maxPlayersAllowed == _players.size())
		{
			return false;
		}
		if (!super.addPlayer(player))
		{
			return false;
		}
		
		_locations.put(player, new int[]
		{
			player.getX(),
			player.getY(),
			player.getZ()
		});
		player.setEventTitleColor(getString("titleColor"));
		player.setEventNameColor(getString("nameColor"));
		player.setTeam(getInt("team"));
		if (_scores.containsKey(player))
		{
			setScore(player, _scores.get(player));
		}
		else
		{
			setScore(player, 0);
		}
		if (getBoolean("removeBuffs"))
		{
			_effects.put(player, player.getAllEffects());
		}
		_effects.put(player, player.getAllEffects());
		// _eventTeleporters.put(player, new EventTeleporter(player, getInt("teleportTime"), true));
		teleportPlayerToEvent(player);
		if (_players.size() >= _minPlayersRequiredForNpc && spawn == null)
		{
			Location loc = null;
			if (_townId == 13)
			{
				loc = new Location(147576, -55992, -2776); // static spawn in Goddard
			}
			else
			{
				loc = MapRegionTable.getTown(_townId).getSpawnLoc();
			}
			if (!spawnNpc(NPC_ID, 10, false, new int[]
			{
				loc.getX(),
				loc.getY(),
				loc.getZ()
			}))
			{
				debug("Failed spawn enchant npc");
			}
		}
		return true;
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
		onFixedRes(player);
		player.teleToLocation(MapRegionTable.getTown(_townId).getSpawnLoc(), 5);
	}
	
	@Override
	public void showInfo(L2PcInstance player)
	{
		Map<String, String> variables = new HashMap<>();
		variables.put("%name%", getString("name"));
		variables.put("%state%", eventStateToString(_eventState));
		int seconds = _eventScheduler.getDelay();
		int mins = seconds / 60;
		int secs = seconds - (mins * 60);
		variables.put("%mins%", String.valueOf(mins));
		variables.put("%secs%", (secs < 10 ? "0" + secs : String.valueOf(secs)));
		variables.put("%players%", String.valueOf(_players.size()));
		variables.put("%minPlayers%", getString("minPlayersRequiredForNpc"));// "~.~"
		variables.put("%maxPlayers%", getString("maxPlayersAllowed"));
		variables.put("%description%", (getString("description") != null) ? MapRegionTable.getInstance().getClosestTownName(_townId) + " " + getString("description").replace("[br1]", "<br1>").replace("[br]", "<br>") : "n/a");
		String userCommands = "";
		if (containsPlayer(player))
		{
			userCommands = "<button value=\"Leave\" action=\"bypass -h event_leave\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">";
			variables.put("%action%", userCommands);
		}
		else
		{
			userCommands = "<button value=\"Join\" action=\"bypass -h event_join\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">";
			variables.put("%action%", userCommands);
		}
		userCommands = "<button value=\"Refresh\" action=\"bypass -h event_info\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">";
		variables.put("%userCommands%", userCommands);
		CustomUtil.sendHtml(player, EventManager.HTML_FILE_PATH + "eventInfo.htm", variables);
	}
	
	private String eventStateToString(EventState definedState)
	{
		EventState eState = (definedState == null) ? _eventState : definedState;
		String state = "n/a";
		switch (eState)
		{
			case IDLE:
				state = "Idle";
				break;
			case ACTIVE:
				state = "Running";
				break;
		}
		return state;
	}
	
	@Override
	public boolean removePlayer(L2PcInstance player)
	{
		if (!super.removePlayer(player))
		{
			return false;
		}
		/*
		 * if (isRunning()) { //_eventTeleporters.put(player.new EventTeleporter(player,getInt("teleportTime"),false)); if (_eventTeleporters.containsKey(player)) { _eventTeleporters.get(player).cancel(); } }
		 */
		if (_players.size() < _minPlayersRequiredForNpc)
		{
			if (spawn != null)
			{
				deleteSpawnNpc(false);
			}
		}
		return true;
	}
	
	@Override
	protected void handleCounter(int delay)
	{
		StringBuilder sb= new StringBuilder();
		switch (_eventState)
		{
			case ACTIVE:
				updateScoreBoard(delay);
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
			spawn = new L2Spawn(template);
			spawn.setLoc(coordinat[0], coordinat[1], coordinat[2], -1);
			spawn.setRespawnDelay(respawnTime);
			SpawnTable.getInstance().addNewSpawn(spawn, permanent);
			spawn.doSpawn(false);
			spawn.setRespawnState(permanent);
		}
		catch (SecurityException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NoSuchMethodException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	private void deleteSpawnNpc(boolean updateDb)
	{
		L2Npc l2Npc = spawn.getNpc();
		l2Npc.deleteMe();
		SpawnTable.getInstance().deleteSpawn(spawn, updateDb);
		spawn = null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.events.events.EventBase#getName()
	 */
	@Override
	public String getName()
	{
		return NAME;
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.events.events.EventBase#getEventId()
	 */
	@Override
	public int getEventId()
	{
		return ID;
	}
	
	@Override
	public boolean canUseMagic(L2Character attacker, L2Character target)
	{
		if (!isRunning())
		{
			return true;
		}
		L2PcInstance attackerInstance;
		if (!(attacker instanceof L2PcInstance))
		{
			if (target instanceof L2SummonInstance)
			{
				attackerInstance = ((L2SummonInstance) target).getOwner();
			}
			else
			{
				return false;
			}
		}
		else
		{
			attackerInstance = (L2PcInstance) attacker;
		}
		L2PcInstance targetInstance;
		if (!(target instanceof L2PcInstance))
		{
			if (target instanceof L2SummonInstance)
			{
				targetInstance = ((L2SummonInstance) target).getOwner();
			}
			else
			{
				return true;
			}
		}
		else
		{
			targetInstance = (L2PcInstance) target;
		}
		if (!containsPlayer(attackerInstance) && containsPlayer(targetInstance))
		{
			attackerInstance.sendMessage("В данный момент нельзя взаимодействовать с персонажем!");
			return false;
		}
		return true;
	}
}