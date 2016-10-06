package custom.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;

import custom.events.CustomUtil;
import custom.events.EventManager;

/**
 * Developers: Redist Team<br>
 * <br>
 * <br>
 * Author: Redist<br>
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
			if (getString("scoreInTitlePattern") != null)
			{
				pattern = getString("scoreInTitlePattern");
			}
			for (L2PcInstance member : _members)
			{
				member.setEventTitle(pattern.replaceAll("%score%", String.valueOf(score)));
			}
		}
		
		public int getScore()
		{
			return _score;
		}
		
		public void addMember(L2PcInstance player)
		{
			debug("Added " + player.getName() + " to team " + getId() + ".");
			_members.add(player);
			player.setEventTitleColor(getString("team" + getId() + "TitleColor"));
			player.setEventNameColor(getString("team" + getId() + "NameColor"));
			player.setTeam(getInt("team" + getId() + "Color"));
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
			debug("Adding spawn location to team " + getString("team" + getId() + "Name") + ": " + location[0] + "," + location[1] + "," + location[2]);
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
	private final int ID = 1;
	private final String NAME = "TVT";
	private EventState _eventState = EventState.IDLE;
	private final List<EventTeam> _teams = new ArrayList<>();
	
	public TVT()
	{
		super();
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
	
	@Override
	public void setConfig(Map<String, String> config)
	{
		super.setConfig(config);
		String[] spawnLocation;
		if (_teams.size() == 0)
		{
			for (int i = 1; i <= getInt("numberOfTeams"); i++)
			{
				EventTeam team = new EventTeam();
				debug("Creating team " + getString("team" + i + "Name") + ".");
				team.setId(i);
				for (String rawSpawnLocation : getString("team" + i + "SpawnLocations").split(";"))
				{
					spawnLocation = rawSpawnLocation.split(",");
					if (spawnLocation.length != 3)
					{
						debug("Failed to load sub-value from \"team" + i + "SpawnLocations\".");
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
					debug("Failed to load value from \"team" + i + "SpawnLocations\".");
					continue;
				}
				_teams.add(team);
			}
		}
		else
		{// reload config for teams.
			for (EventTeam team : _teams)
			{
				team.clearSpawnLocations();
				for (String rawSpawnLocation : getString("team" + team.getId() + "SpawnLocations").split(";"))
				{
					spawnLocation = rawSpawnLocation.split(",");
					team.addSpawnLocation(new int[]
					{
						Integer.valueOf(spawnLocation[0]),
						Integer.valueOf(spawnLocation[1]),
						Integer.valueOf(spawnLocation[2])
					});
				}
			}
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
				announce("TVT: Регистрация игроков открыта.");
				announce("Команды: " + EVENT_COMMANDS);
				_eventState = EventState.REGISTERING;
				schedule(getInt("registrationTime"));
				break;
			case REGISTERING:
				announce("TVT: Регистрация игроков закончена.");
				if (_players.size() >= getInt("minPlayersRequired"))
				{
					_eventState = EventState.PRE_ACTIVE;
					schedule(getInt("teleportTime"));
				}
				else
				{
					announce("Event has been aborted because there was not enough participants.");
					debug("Event has been aborted because there was not enough participants.");
					EventManager.getInstance().end(false);
				}
				break;
			case PRE_ACTIVE:
				_eventState = EventState.ACTIVE;
				schedule(getInt("runningTime"));
				teleportPlayersToEvent();
				break;
			case ACTIVE:
				_eventState = EventState.SUF_ACTIVE;
				schedule(getInt("teleportTime"));
				break;
			case SUF_ACTIVE:
				EventManager.getInstance().end(true);
				break;
		}
	}
	
	private void addPlayerToTeam(L2PcInstance player)
	{
		debug("Attmepting to add " + player.getName() + " to a team.");
		EventTeam teamToBe = null;
		for (EventTeam team : _teams)
		{
			if (teamToBe == null)
			{
				teamToBe = team;
			}
			else if (team.countMemebers() < teamToBe.countMemebers())
			{
				teamToBe = team;
			}
		}
		if (teamToBe != null)
		{
			teamToBe.addMember(player);
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
		doorsToCloseOnStart();
		clearZones();
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
		doorsToOpenOnEnd();
		rewardPlayers();
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
		if (getString("reward") != null)
		{
			if (isTie && getBoolean("rewardTie"))
			{
				announce("Ничья!");
				for (L2PcInstance player : _players)
				{
					if (_scores.get(player) >= getInt("minKillsToGetReward"))
					{
						rewardPlayer(player, "reward");
					}
				}
			}
			else if (_1stPlace != null)
			{
				announce("1-е место заняла команда " + getString("team" + _1stPlace.getId() + "Name") + ". Сделав " + _1stPlace.getScore() + " убийств.");
				for (L2PcInstance member : _1stPlace.getMembers())
				{
					if (_scores.get(member) >= getInt("minKillsToGetReward"))
					{
						rewardPlayer(member, "reward");
					}
				}
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
		debug(killerPc.getName() + (killer instanceof L2SummonInstance ? "'s Summon" : "") + " has killed " + killedPc.getName() + (killed instanceof L2SummonInstance ? "'s Summon" : "") + ".");
		setScore(killerPc, _scores.get(killerPc) + 1);
		try
		{
			if (killed instanceof L2PcInstance)
			{
				if (!getBoolean("allowFixedResurrection"))
				{
					addResurrector(killedPc);
				}
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
		if (getBoolean("rewardPvpOnKill"))
		{
			killerPc.setPvpKills(killerPc.getPvpKills() + 1);
		}
		if (getString("rewardPerKill") != null)
		{
			rewardPlayer(killerPc, "rewardPerKill");
		}
		
		// if(getBoolean("decreaseScoreOnDeath"))
		// setScore(killedPc,_scores.get(killedPc)-1);
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
		variables.put("%name%", getString("name"));
		variables.put("%engine%", getString("engine"));
		variables.put("%state%", eventStateToString(_eventState));
		int seconds = _eventScheduler.getDelay();
		int mins = seconds / 60;
		int secs = seconds - (mins * 60);
		variables.put("%mins%", String.valueOf(mins));
		variables.put("%secs%", (secs < 10 ? "0" + secs : String.valueOf(secs)));
		variables.put("%players%", String.valueOf(_players.size()));
		variables.put("%minPlayers%", getString("minPlayersRequired"));
		variables.put("%maxPlayers%", getString("maxPlayersAllowed"));
		variables.put("%description%", (getString("description") != null) ? getString("description").replace("[br1]", "<br1>").replace("[br]", "<br>") : "n/a");
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
	
}
