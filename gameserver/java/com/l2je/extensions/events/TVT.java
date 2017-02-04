package com.l2je.extensions.events;

import com.l2je.extensions.events.CustomUtil;
import com.l2je.extensions.events.EventManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;

/**
 * Developers: l2je Team<br>
 * <br>
 * <br>
 * Author: evgeny<br>
 * Date: 24 февр. 2016 г.<br>
 * Time: 0:15:55<br>
 * <br>
 */
public class TVT extends CombatEvent
{
	private enum EventState
	{
		IDLE,
		REGISTERING,
		PRE_ACTIVE,
		ACTIVE,
		SUF_ACTIVE
	}
	
	private class EventTeam
	{
		private int _id;
		private int _score;
		private final HashSet<L2PcInstance> _members = new HashSet<>();
		private final List<int[]> _spawnLocations = new ArrayList<>();
		
		public EventTeam()
		{
		}
		
		public void setId(int id)
		{
			_id = id;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public void setScore(int score)
		{
			_score = score;
			String pattern = "%score%";
			if (EventConfig.TVT_SCORE_TITLE_PATTERN != null)
			{
				pattern = EventConfig.TVT_SCORE_TITLE_PATTERN;
			}
			for (L2PcInstance member : _members)
			{
				member.setEventTitle(pattern.replaceAll("%score%", String.valueOf(score)));
			}
		}
		
		public String getName()
		{
			return (getId() == 1) ? "Blue" : "Red";
		}
		
		public int getScore()
		{
			return _score;
		}
		
		public void addMember(L2PcInstance player)
		{
			if (EventConfig.EVENT_MANAGER_DEBUG)
				debug("Added " + player.getName() + " to team " + getName() + ".");
			_members.add(player);
			player.setEventTitleColor((getId() == 1) ? EventConfig.TVT_TEAM1_TITLE_COLOR : EventConfig.TVT_TEAM2_TITLE_COLOR);
			player.setEventNameColor((getId() == 1) ? EventConfig.TVT_TEAM1_NAME_COLOR : EventConfig.TVT_TEAM2_NAME_COLOR);
			player.setTeam(getId());
		}
		
		public boolean containsMember(L2PcInstance player)
		{
			return _members.contains(player);
		}
		
		public int countMemebers()
		{
			return _members.size();
		}
		
		public void removeMember(L2PcInstance player)
		{
			_members.remove(player);
		}
		
		public HashSet<L2PcInstance> getMembers()
		{
			return _members;
		}
		
		public void addSpawnLocation(int[] location)
		{
			if (EventConfig.EVENT_MANAGER_DEBUG)
				debug("Adding spawn location to team " + getId() + ": " + location[0] + "," + location[1] + "," + location[2]);
			_spawnLocations.add(location);
		}
		
		public void clearSpawnLocations()
		{
			_spawnLocations.clear();
		}
		
		public int countSpawnLocations()
		{
			return _spawnLocations.size();
		}
		
		public int[] getRandomSpawnLocation()
		{
			Random rnd = new Random();
			return _spawnLocations.get(rnd.nextInt(_spawnLocations.size()));
		}
	}
	
	// end class
	private EventState _eventState = EventState.IDLE;
	private final List<EventTeam> _teams = new ArrayList<>();
	
	public TVT()
	{
		super();
		setName("TVT");
		setId(EventConfig.TVT_ID);
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.l2j.gameserver.events.events.EventBase#start()
	 */
	@Override
	public void start()
	{
		trigger();
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.l2je.extensions.events.Event#init()
	 */
	@Override
	protected void init()
	{
		initBlockItems();
		initBlockSkills();
		initTeams();
	}
	
	private void initTeams()
	{
		initTeam(1);
		initTeam(2);
	}
	
	private void initTeam(int id)
	{
		String[] spawnLocation;
		if (_teams.size() < 2)
		{
			EventTeam team = new EventTeam();
			team.setId(id);
			if (EventConfig.EVENT_MANAGER_DEBUG)
				debug("Creating team "+team.getName());			
			String[] configs = (id == 1) ? EventConfig.TVT_TEAM1_SPAWN_LOCS : EventConfig.TVT_TEAM2_SPAWN_LOCS;
			for (String rawSpawnLocation : configs)
			{
				spawnLocation = rawSpawnLocation.split(",");
				if (spawnLocation.length != 3)
				{
					if (EventConfig.EVENT_MANAGER_DEBUG)
						debug("Failed to load sub-value from \"team  1 SpawnLocations\".");
					continue;
				}
				team.addSpawnLocation(new int[]
				{
					Integer.valueOf(spawnLocation[0]),
					Integer.valueOf(spawnLocation[1]),
					Integer.valueOf(spawnLocation[2])
				});
			}
			if (team.countSpawnLocations() == 0)
			{
				if (EventConfig.EVENT_MANAGER_DEBUG)
					debug("Failed to load value from \"team "+team.getName()+" SpawnLocations\".");
			}
			_teams.add(team);
		}
	}
	
	@Override
	public boolean isRegistering()
	{
		return _eventState == EventState.REGISTERING;
	}
	
	@Override
	public boolean isRunning()
	{
		return _eventState == EventState.ACTIVE;
	}
	
	@Override
	public boolean isTeleporting()
	{
		return (_eventState == EventState.PRE_ACTIVE) || (_eventState == EventState.SUF_ACTIVE);
	}
	
	@Override
	public void end(boolean inform)
	{
		if (!isRegistering())
		{
			teleportPlayersFromEvent();
		}
		_eventState = EventState.IDLE;
		_teams.clear(); // ?
		super.end(inform);
	}
	
	@Override
	public void trigger()
	{
		switch (_eventState)
		{
			case IDLE:
				init();
				announce("TVT: Регистрация игроков открыта.");
				announce("Команды: " + EVENT_COMMANDS);
				_eventState = EventState.REGISTERING;
				schedule(EventConfig.TVT_REG_TIME);
				break;
			case REGISTERING:
				announce("TVT: Регистрация игроков закончена.");
				if (_players.size() >= EventConfig.TVT_MIN_PLAYERS)
				{
					_eventState = EventState.PRE_ACTIVE;
					schedule(EventConfig.TVT_TELEPORT_TIME);
				}
				else
				{
					announce("Event has been aborted because there was not enough participants.");
					if (EventConfig.EVENT_MANAGER_DEBUG)
						debug("Event has been aborted because there was not enough participants.");
					EventManager.getInstance().end(false);
				}
				break;
			case PRE_ACTIVE:
				_eventState = EventState.ACTIVE;
				schedule(EventConfig.TVT_RUNNING_TIME);
				teleportPlayersToEvent();
				break;
			case ACTIVE:
				_eventState = EventState.SUF_ACTIVE;
				schedule(EventConfig.TVT_TELEPORT_TIME);
				break;
			case SUF_ACTIVE:
				EventManager.getInstance().end(true);
				break;
		}
	}
	
	private void addPlayerToTeam(L2PcInstance player)
	{
		EventTeam eventTeam = null;
		if (_teams.get(0).countMemebers() > _teams.get(1).countMemebers())
		{
			eventTeam = _teams.get(1);
		}
		else
		{
			eventTeam = _teams.get(0);
		}
		eventTeam.addMember(player);
		if (EventConfig.EVENT_MANAGER_DEBUG)
		{
			StringBuffer sb = new StringBuffer();
			StringUtil.append(sb, "Attmepting to add ", player.getName(), " to a team ", eventTeam.getName());
			debug(sb.toString());
		}
	}
	
	private EventTeam getTeam(L2PcInstance player)
	{
		for (EventTeam team : _teams)
		{
			if (team.containsMember(player))
			{
				return team;
			}
		}
		return null;
	}
	
	@Override
	protected void teleportPlayersToEvent()
	{
		spawnFences();
		if (EventConfig.EVENT_MANAGER_DEBUG)
			debug("Teleporting " + _players.size() + " to the event.");
		_locations.clear();
		for (L2PcInstance player : _players)
		{
			_locations.put(player, new int[]
			{
				player.getX(),
				player.getY(),
				player.getZ()
			});
			_effects.put(player, player.getAllEffects());
			setScore(player, 0);
			addPlayerToTeam(player);
			teleportPlayerToEvent(player);
		}
		for (EventTeam team : _teams)
		{
			team.setScore(0);
		}
	}
	
	@Override
	protected void teleportPlayerToEvent(L2PcInstance player)
	{
		onFixedRes(player);
		try
		{
			int[] location = getTeam(player).getRandomSpawnLocation();
			player.teleToLocation(location[0], location[1], location[2], 0);
		}
		catch (NullPointerException e)
		{
		}
	}
	
	@Override
	protected void teleportPlayersFromEvent()
	{
		deleteFences();
		rewardPlayers();
		if (EventConfig.EVENT_MANAGER_DEBUG)
			debug("Teleporting " + _players.size() + " participants from the event.");
		super.teleportPlayersFromEvent();
	}
	
	@Override
	protected void teleportPlayerFromEvent(L2PcInstance player)
	{
		if (player.isDead())
		{
			player.doRevive();
		}
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
		
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		int[] location = FAIL_SAFE_LOCATION;
		if (_locations.containsKey(player))
		{
			location = _locations.get(player);
			// _locations.remove(player);
		}
		try
		{
			getTeam(player).removeMember(player);
		}
		catch (NullPointerException e)
		{
		}
		player.setEventTitle(null);
		player.setEventTitleColor(null);
		player.setEventNameColor(null);
		player.setTeam(0);
		player.teleToLocation(location[0], location[1], location[2], 0);
	}
	
	private void rewardPlayers()
	{
		EventTeam _1stPlace = null;
		int globalScore = 0;
		boolean isTie = false;
		for (EventTeam team : _teams)
		{
			if (team.getScore() < 1)
			{
				continue;
			}
			if (_1stPlace == null)
			{
				_1stPlace = team;
				globalScore = _1stPlace.getScore();
			}
			else if (team.getScore() > _1stPlace.getScore())
			{
				_1stPlace = team;
				if (_1stPlace.getScore() == globalScore)
				{
					isTie = true;
				}
				else
				{
					isTie = false;
				}
			}
			else if (team.getScore() == globalScore)
			{
				isTie = true;
			}
			else
			{
				isTie = false;
			}
		}
		if (EventConfig.TVT_REWARD != null)
		{
			if (isTie && EventConfig.TVT_REWARD_TIE)
			{
				announce("Ничья!");
				for (L2PcInstance player : _players)
				{
					rewardPlayer(player);
				}
			}
			else if (_1stPlace != null)
			{
				StringBuilder sb = new StringBuilder();
				StringUtil.append(sb, "1-е место заняла команда ", _1stPlace.getName(), ". Сделав ", _1stPlace.getScore(), " убийств.");
				announce(sb.toString());
				for (L2PcInstance member : _1stPlace.getMembers())
				{
					rewardPlayer(member);
					
				}
			}
		}
	}
	
	public void rewardPlayer(L2PcInstance player)
	{
		String[] rewards = EventConfig.TVT_REWARD;
		for (String reward : rewards)
		{
			int[] rewardItem = EventConfig.stringToIntArr(reward);
			if (rewardItem.length == 2)
			{
				player.addItem("EventReward", rewardItem[0], rewardItem[1], player, true);
			}
			else if (EventConfig.EVENT_MANAGER_DEBUG)
			{
				debug("Error reward per kill player in EW!");
			}
		}
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
		if (EventConfig.EVENT_MANAGER_DEBUG)
			debug(killerPc.getName() + (killer instanceof L2SummonInstance ? "'s Summon" : "") + " has killed " + killedPc.getName() + (killed instanceof L2SummonInstance ? "'s Summon" : "") + ".");
		setScore(killerPc, _scores.get(killerPc) + 1);
		try
		{
			if (killed instanceof L2PcInstance)
			{
				addResurrector(killedPc);
			}
			EventTeam killerTeam = getTeam(killerPc);
			EventTeam killedTeam = getTeam(killedPc);
			if (killerTeam == killedTeam)
			{
				return;
			}
			killerTeam.setScore(killerTeam.getScore() + 1);
		}
		catch (NullPointerException e)
		{
		}
		if (EventConfig.TVT_PVP_ON_KILL)
		{
			killerPc.setPvpKills(killerPc.getPvpKills() + 1);
		}
	}
	
	@Override
	protected void setScore(L2PcInstance player, int score)
	{
		_scores.put(player, score);
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
			case REGISTERING:
				state = "Registration";
				break;
			case PRE_ACTIVE:
				state = "Teleporting To Event";
				break;
			case ACTIVE:
				state = "Running";
				break;
			case SUF_ACTIVE:
				state = "Teleporting From Event";
				break;
		}
		return state;
	}
	
	@Override
	protected void handleCounter(int delay)
	{
		switch (_eventState)
		{
			case REGISTERING:
				switch (delay)
				{
					case 10800:
					case 7200:
					case 3600:
						announce("Регистрация закончится через " + (delay / 3600) + " час(ов).");
						break;
					case 600:
					case 300:
					case 60:
						announce("Регистрация закончится через " + (delay / 60) + " минут(у).");
						break;
					case 30:
						// case 15:
					case 10:
					case 5:
						announce("Регистрация закончится через " + delay + " секунд(у).");
						break;
				}
				break;
			case PRE_ACTIVE:
				switch (delay)
				{
					case 60:
						sendMessageToAllPlayers("Вы будете телепортированы на event через " + (delay / 60) + " минут(у).");
						break;
					case 30:
					case 15:
					case 10:
					case 5:
						sendMessageToAllPlayers("Вы будете телепортированы на event через " + delay + " секунд(у).");
						break;
				}
				break;
			case ACTIVE:
				updateScoreBoard(delay);
				switch (delay)
				{
					case 600:
					case 300:
					case 60:
						sendMessageToAllPlayers("Event закончится через " + (delay / 60) + " минут(у).");
						break;
					case 30:
					case 15:
					case 10:
					case 5:
						sendMessageToAllPlayers("Event закончится через " + delay + " секунд(у).");
						break;
				}
				break;
			case SUF_ACTIVE:
				switch (delay)
				{
					case 60:
						sendMessageToAllPlayers("Вы будете телепортированы обратно через " + (delay / 60) + " минут(у).");
						break;
					case 30:
					case 15:
					case 10:
					case 5:
						sendMessageToAllPlayers("Вы будете телепортированы обратно через " + delay + " секунд(у).");
						break;
				}
				break;
		}
	}
	
	@Override
	public void showInfo(L2PcInstance player)
	{
		Map<String, String> variables = new HashMap<>();
		variables.put("%name%", getName());
		variables.put("%engine%", getName());
		variables.put("%state%", eventStateToString(_eventState));
		int seconds = _eventScheduler.getDelay();
		int mins = seconds / 60;
		int secs = seconds - (mins * 60);
		variables.put("%mins%", String.valueOf(mins));
		variables.put("%secs%", (secs < 10 ? "0" + secs : String.valueOf(secs)));
		variables.put("%players%", String.valueOf(_players.size()));
		variables.put("%minPlayers%", String.valueOf(EventConfig.TVT_MIN_PLAYERS));
		variables.put("%maxPlayers%", String.valueOf(EventConfig.TVT_MAX_PLAYERS));
		variables.put("%description%", (EventConfig.TVT_DESCR != null) ? EventConfig.TVT_DESCR : "n/a");
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
		return true;
	}
	
	@Override
	protected void initBlockItems()
	{
		for (Integer itemId : EventConfig.TVT_BLOCKED_ITEMS)
			_blockedItems.add(itemId);
	}	
	/*
	 * (non-Javadoc)
	 * @see com.l2je.extensions.events.Event#initBlockSkills()
	 */
	@Override
	protected void initBlockSkills()
	{
		for (Integer skillId : EventConfig.TVT_BLOCKED_SKILLS)
			_blockedSkills.add(skillId);
		
	}	
	/*
	 * (non-Javadoc)
	 * @see com.l2je.extensions.events.Event#getRunningTime()
	 */
	@Override
	public int getRunningTime()
	{
		return EventConfig.TVT_RUNNING_TIME;
	}
	@Override
	public boolean removePlayer(L2PcInstance player)
	{
		if (!super.removePlayer(player))
		{
			return false;
		}
		if (_players.size() < EventConfig.EW_MIN_PLAYERS)
		{
			announce("Слишком много игроков покинуло event.");
			announce("Event отменен.");
			EventManager.getInstance().end(true);
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see com.l2je.extensions.events.Event#getResTime()
	 */
	@Override
	protected int getResTime()
	{		
		return EventConfig.TVT_RES_TIME;
	}
}
