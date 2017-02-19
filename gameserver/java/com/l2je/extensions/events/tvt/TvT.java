package com.l2je.extensions.events.tvt;

import com.l2je.extensions.events.CombatEvent;
import com.l2je.extensions.events.EventConfig;
import com.l2je.extensions.events.EventManager;
import com.l2je.extensions.events.commons.Fence;
import com.l2je.extensions.events.commons.Reward;

import java.util.ArrayList;
import java.util.List;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;


/**
 * Developers: L2JE Team<br>
 * <br>
 * <br>
 * Author: evgeny<br>
 * Date: 24 февр. 2016 г.<br>
 * Time: 0:15:55<br>
 */
public class TvT extends CombatEvent
{
	private final List<Team> _teams = new ArrayList<>();
	protected final static String DESCRIPTION = "Противостояние двух команд.</br> Команда набравшая больше очков выиграет!";
	
	public TvT()
	{
		super();
		setName("TvT");
		setId(EventConfig.TVT_ID);
	}
	
	@Override
	public void start()
	{
		trigger();
	}
	
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
		Team team = new Team();
		team.setId(id);
		if (EventConfig.EVENT_MANAGER_DEBUG)
			_log.info("Creating team " + team.getName());
		String[] configs = (id == 1) ? EventConfig.TVT_TEAM1_SPAWN_LOCS : EventConfig.TVT_TEAM2_SPAWN_LOCS;
		for (String rawSpawnLocation : configs)
		{
			spawnLocation = rawSpawnLocation.split(",");
			if (spawnLocation.length != 3)
			{
				if (EventConfig.EVENT_MANAGER_DEBUG)
					_log.info("Failed to load sub-value from \"team  1 SpawnLocations\".");
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
				_log.info("Failed to load value from \"team " + team.getName() + " SpawnLocations\".");
		}
		_teams.add(team);
		
	}
	
	@Override
	public void end(boolean inform)
	{
		deleteFences();
		rewardPlayers();		
		_teams.clear(); 
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
				if (_members.size() >= getMinPlayers())
				{
					_eventState = EventState.PRE_ACTIVE;
					schedule(EventConfig.TVT_TELEPORT_TIME);
				}
				else
				{
					announce("Event has been aborted because there was not enough participants.");
					if (EventConfig.EVENT_MANAGER_DEBUG)
						_log.info("Event has been aborted because there was not enough participants.");
					EventManager.getInstance().endCurrentEvent(false);
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
				EventManager.getInstance().endCurrentEvent(true);
				break;
		}
	}
	
	private void addPlayerToTeam(L2PcInstance player)
	{
		Team eventTeam = null;
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
			_log.info(sb.toString());
		}
	}
	
	@Override
	protected Team getTeam(L2PcInstance player)
	{
		for (Team team : _teams)
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
			_log.info("Teleporting " + _members.size() + " to the event.");
		for (L2PcInstance player : _members.values())
		{
			addPlayerToTeam(player);
			teleportPlayerToEvent(player);
		}
		for (Team team : _teams)
		{
			team.setScore(0);
		}
	}
	
	@Override
	protected void teleportPlayerToEvent(L2PcInstance player)
	{
		resurrect(player, true);
		try
		{
			int[] location = getTeam(player).getRandomSpawnLocation();
			player.teleToLocation(location[0], location[1], location[2], 0);
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
	}	

	private void rewardPlayers()
	{
		Team _1stPlace = null;
		int globalScore = 0;
		boolean isTie = false;
		for (Team team : _teams)
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
				for (L2PcInstance player : _members.values())
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

	@Override
	protected boolean pvpOnKill()
	{
	   return EventConfig.TVT_PVP_ON_KILL;
	}
	
	@Override
	protected void setScore(L2PcInstance player, int score)
	{
		_scores.put(player, score);
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
	protected void initBlockItems()
	{
		for (Integer itemId : EventConfig.TVT_BLOCKED_ITEMS)
			_blockedItems.add(itemId);
	}
	
	@Override
	protected void initBlockSkills()
	{
		for (Integer skillId : EventConfig.TVT_BLOCKED_SKILLS)
			_blockedSkills.add(skillId);
		
	}
	
	@Override
	public int getRunningTime()
	{
		return EventConfig.TVT_RUNNING_TIME;
	}
	
	@Override
	protected int getMinPlayers()
	{
		return EventConfig.TVT_MIN_PLAYERS;
	}
	@Override
	protected int getMaxPlayers()
	{
		return EventConfig.TVT_MAX_PLAYERS;
	}
	@Override
	protected ArrayList<Fence> getFences()
	{
		return EventConfig.tvtFences;
	}	
	@Override
	protected int getResTime()
	{
		return EventConfig.TVT_RES_TIME;
	}
	@Override
	protected String getScoreTitlePattern()
	{
		return EventConfig.TVT_SCORE_TITLE_PATTERN;
	}
	@Override
	protected String getDesription()
	{
		return DESCRIPTION;
	}
	@Override
	protected ArrayList<Reward> getReward()
	{
		return EventConfig.tvtReward;
		
	}
}
