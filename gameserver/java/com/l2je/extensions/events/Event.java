package com.l2je.extensions.events;

import com.l2je.extensions.events.commons.Fence;
import com.l2je.extensions.events.commons.Reward;
import com.l2je.extensions.events.commons.Util;
import com.l2je.extensions.events.tvt.Team;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.FenceTable;
import net.sf.l2j.gameserver.model.L2Effect;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;

import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.util.Broadcast;

public abstract class Event
{
	protected static final int[] FAIL_SAFE_LOCATION = new int[]
	{
		82698,
		148638,
		-3473
	};
	
	protected static final String EVENT_COMMANDS = ".join -присоединиться   .info-информация   .leave- покинуть";
	protected static final Logger _log = Logger.getLogger(Event.class.getName());
	protected class EventScheduler implements Runnable
	{
		private ScheduledFuture<?> _task = null;
		private int _delay;
		
		public void schedule(int delay)
		{
			_delay = delay;
			if (_task != null)
			{
				cancel();
			}
			run();
		}
		
		public int getDelay()
		{
			return _delay / 1000;
		}
		
		@Override
		public void run()
		{
			if (_delay < 1000)
			{
				trigger();
				_task = null;
				return;
			}
			handleCounter(_delay / 1000);
			_task = ThreadPool.schedule(this, 1000);
			_delay = _delay - 1000;
		}
		
		public void cancel()
		{
			if (_task != null)
			{
				_task.cancel(false);
			}
			_task = null;
		}
	}
	
	protected EventScheduler _eventScheduler = new EventScheduler();
	protected HashSet<L2PcInstance> _players = new HashSet<>();
	protected Map<L2PcInstance, Integer> _scores = new HashMap<>();
	protected Map<L2PcInstance, int[]> _locations = new HashMap<>();
	protected Map<L2PcInstance, L2Effect[]> _effects = new HashMap<>();
	protected HashSet<Integer> _blockedSkills = new HashSet<>();
	protected HashSet<Integer> _blockedItems = new HashSet<>();
	protected List<L2FenceInstance> _fences = new LinkedList<>();
	protected ArrayList<int[]> _spawnLocations = new ArrayList<>();
	protected int _id;
	protected String _name = "n/a";
	protected final static String DESCRIPTION = "n/a";
	protected final static String DEFAULT_SCORE_PATTERN = "[%score%]";
	protected enum EventState
	{
		IDLE,
		REGISTERING,
		PRE_ACTIVE,
		ACTIVE,
		SUF_ACTIVE
	}
	
	protected EventState _eventState = EventState.IDLE;
	public Event()
	{
		EventManager.getInstance().addEvent(this);
	}
	
	public abstract void start();
	
	public abstract void end(boolean inform);
	
	public abstract void trigger();
	
	protected abstract void init();
	
	protected abstract void initBlockItems();
	
	protected abstract void initBlockSkills();
	
	public abstract int getRunningTime();
	
	protected String getScoreTitlePattern()
	{
		return DEFAULT_SCORE_PATTERN;
	}
	
	public String getName()
	{
		return _name;
	}
	protected ArrayList<Reward> getReward()
	{
		return null;
		
	}
	protected void setName(String name)
	{
		_name = name;
	}
	
	public int getId()
	{
		return _id;
	}
	
	protected void setId(int id)
	{
		_id = id;
	}
	
	protected int getMinPlayers()
	{
		return 0;
	}
	
	protected int getMaxPlayers()
	{
		return 20;
	}
	
	protected L2PcInstance getPcInstance(L2Character character)
	{
		L2PcInstance pcInstance = null;
		if (character instanceof L2PcInstance)
		{
			pcInstance = (L2PcInstance) character;
		}
		else if (character instanceof L2SummonInstance)
		{
			pcInstance = ((L2Summon) character).getOwner();
		}
		return pcInstance;
	}
	
	
	public boolean isRegistering()
	{
		return _eventState == EventState.REGISTERING;
	}
	
	
	public boolean isRunning()
	{
		return _eventState == EventState.ACTIVE;
	}
	
	public boolean isTeleporting()
	{
		return (_eventState == EventState.PRE_ACTIVE) || (_eventState == EventState.SUF_ACTIVE);
	}
	
	public boolean isEnd()
	{
		return _eventState == EventState.IDLE;
	}
	public String eventStateToString(String state)
	{
		return null;
	}
	
	public void schedule(int seconds)
	{
		_eventScheduler.schedule(seconds * 1000);
	}
	
	public void skip(L2PcInstance player)
	{
		if (EventConfig.EVENT_MANAGER_DEBUG)
			_log.info(player.getName() + " skipped the event.");
		schedule(1);
	}
	
	public boolean addPlayer(L2PcInstance player)
	{
		if (getMaxPlayers() == _players.size())
		{
			return false;
		}
		for (L2PcInstance p : _players)
		{
			if (!p.isOnline())
			{
				_players.remove(p);
				p.setEvent(null);
			}
		}
		player.setEvent(this);
		_players.add(player);
		_scores.put(player, 0);		
		_locations.put(player, new int[]
		{
			player.getX(),
			player.getY(),
			player.getZ()
		});
		_effects.put(player, player.getAllEffects());
		if (EventConfig.EVENT_MANAGER_DEBUG)
			_log.info(player.getName() + " has joined the event.");
		return true;
	}
	/**
	 * Воскресить вылечить и вернуть бафы игроку
	 * @param player Игрок
	 * @param restoreBuffs вернуть бафы
	 */
	protected final void resurrect(L2PcInstance player, boolean restoreBuffs)
	{
		if (player.isDead())
		{
			player.doRevive();
		}
		if (player.getPet() instanceof L2PetInstance)
		{
			player.getPet().unSummon(player);
		}
		if (player.getPet() != null)
		{
			heal(player.getPet());
		}
		player.setIsInvul(false);
		player.getAppearance().setVisible();		
		player.stopAllEffects();
		player.stopAllToggles();
		player.stopCubics();
		if (restoreBuffs)
		{
			returnBuffs(player);
		}
		heal(player);
		player.broadcastUserInfo();
	}
	/**
	 * Восстанавливает состояние игрока до ивента
	 * @param player
	 * @return
	 */
	protected boolean restorePlayer(L2PcInstance player)
	{		
		if (player.isDead())
		{
			player.doRevive();
		}
		resurrect(player, true);
		player.setEventTitle(null);
		player.setEventTitleColor(null);
		player.setEventNameColor(null);
		player.setTeam(0);
		return true;
	}
	
	/**
	 * Удаляет игрока с ивента
	 * @param player
	 * @return
	 */
	public boolean removePlayer(L2PcInstance player)
	{
		 player.setEvent(null);
		_players.remove(player);
		if (isRunning())
		{
			restorePlayer(player);
			Team team = getTeam(player);
			if (team != null)
				team.removeMember(player);
			teleportPlayerFromEvent(player);			
			if (_players.size() < getMinPlayers())
			{
				announce("Слишком много игроков покинуло event.");
				announce("Event отменен.");
				EventManager.getInstance().endCurrentEvent(true);
			}
		}
		_scores.remove(player);
		_effects.remove(player);
		_locations.remove(player);
		if (EventConfig.EVENT_MANAGER_DEBUG)
			_log.info(player.getName() + " has left the event.");
		return true;
	}
	
	/**
	 * Телепортирует игрока с ивента
	 * @param player
	 */
	protected void teleportPlayerFromEvent(L2PcInstance player)
	{
		int[] location = FAIL_SAFE_LOCATION;
		if (_locations.containsKey(player))
		{
			location = _locations.get(player);
		}
		player.teleToLocation(location[0], location[1], location[2], 0);
	}
	
	/**
	 * Удаляет всех игроков с ивента
	 */
	protected void removeAllPlayersFromEvent()
	{
		for (L2PcInstance player : _players)
		{
			player.setEvent(null);
			restorePlayer(player);
			if (!isRegistering() || (isRegistering() && isRunning()))
			{
				teleportPlayerFromEvent(player);
			}
		}
	}
	
	protected Team getTeam(L2PcInstance player)
	{
		return null;
	}
	
	public boolean containsPlayer(L2PcInstance player)
	{
		return player.getEvent() == this;
	}
	
	protected void setScore(L2PcInstance player, int score)
	{
		String pattern = "%score%";
		if (getScoreTitlePattern() != null)
		{
			pattern = getScoreTitlePattern();
		}
		player.setEventTitle(pattern.replaceAll("%score%", String.valueOf(score)));
		_scores.put(player, score);
	}
	
	protected void teleportPlayerToEvent(L2PcInstance player)
	{
		resurrect(player,true);
		Random rnd = new Random();
		int[] location = _spawnLocations.get(rnd.nextInt(_spawnLocations.size()));
		player.teleToLocation(location[0], location[1], location[2], 0);
	}
	
	public void onLogIn(L2PcInstance player)
	{
		if (isRegistering())
		{
			EventManager.getInstance().getInfo(player);
		}
	}
	
	public void onLogOut(L2PcInstance player)
	{
	}
	
	/**
	 * Восстанавливает игроку CP/MP/HP.
	 * @param player
	 */
	protected void heal(L2Character player)
	{
		player.getStatus().setCurrentCp(player.getMaxCp());
		player.getStatus().setCurrentMp(player.getMaxMp());
		player.getStatus().setCurrentHp(player.getMaxHp());
	}
	
	protected void returnBuffs(L2PcInstance player)
	{
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
	
	protected void updateScoreBoard(int seconds)
	{
		int mins = seconds / 60;
		int secs = seconds - (mins * 60);
		StringBuffer sb = new StringBuffer();
		StringUtil.append(sb, "До окончания ивента: ",String.valueOf(mins),"-");
		if(secs>9)
		{
			sb.append(String.valueOf(secs));
		}
		else 
		{
			sb.append("0");
			sb.append(String.valueOf(secs));
		}
		int position = 3;
		ExShowScreenMessage screenMessage = new ExShowScreenMessage(1, 0, position, false, 1, 0, 0, false, 700, true, sb.toString());
		for (L2PcInstance player : _players)
		{
			player.sendPacket(screenMessage);
		}
	}
	
	/*
	 * Make sure that this method outputs a message to the player explaining why they can't continue.
	 */
	public boolean canLogOut(L2PcInstance player)
	{
		if (player.getEvent() != this)
		{
			return true;
		}
		if (isRunning())
		{
			player.sendMessage("You can not logout at this time.");
			return false;
		}
		return true;
	}
	
	public boolean canUseItem(L2PcInstance player, ItemInstance item)
	{
		if (player.getEvent() != this)
		{
			return true;
		}
		if (_blockedItems.contains(item.getItemId()) && isRunning())
		{
			player.sendMessage("You can not use this item during the event.");
			return false;
		}
		return true;
	}
	
	public boolean canUseSkill(L2PcInstance player, L2Skill skill)
	{
		if (!containsPlayer(player))
		{
			return true;
		}
		if (_blockedSkills.contains(skill.getId()) && isRunning())
		{
			player.sendMessage("You can not use this skill during the event.");
			return false;
		}
		if (skill.isActive() && skill.isMagic() && isTeleporting())
		{
			player.sendMessage("You can not attack at this time.");
			return false;
		}
		return true;
	}
	
	public boolean playersAutoAttackable(L2Character target, L2Character attacker)
	{
		return false;
	}
	
	public boolean canAttackInPeace(L2Character player, L2Character target)
	{
		return false;
	}
	
	public boolean canRequestToNpc(L2PcInstance player, L2Character npc)
	{
		return true;
	}
	
	public boolean canUseMagic(L2Character player, L2Character target)
	{
		return true;
	}
	
	public void validatePostion(L2PcInstance player)
	{
	}	
	
	protected String eventStateToString()
	{		
		String state = "n/a";
		switch (_eventState)
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
	
	protected String getDesription()
	{
		return DESCRIPTION;
	}
	
	public void showInfo(L2PcInstance player)
	{
		Map<String, String> variables = new HashMap<>();
		variables.put("%name%", getName());
		variables.put("%state%", eventStateToString());
		int seconds = _eventScheduler.getDelay();
		int mins = seconds / 60;
		int secs = seconds - (mins * 60);
		variables.put("%mins%", String.valueOf(mins));
		variables.put("%secs%", (secs < 10 ? "0" + secs : String.valueOf(secs)));
		variables.put("%players%", String.valueOf(_players.size()));
		variables.put("%minPlayers%", String.valueOf(getMinPlayers()));
		variables.put("%maxPlayers%", String.valueOf(getMaxPlayers()));
		variables.put("%description%", getDesription());
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
		Util.sendHtml(player, EventManager.HTML_FILE_PATH + "eventInfo.htm", variables);
	}
	
	protected void handleCounter(int delay)
	{
	}
	
	protected void announce(String message)
	{
		Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", message));
	}
	
	protected void sendMessageToAllPlayers(String message)
	{
		for (L2PcInstance player : _players)
		{
			player.sendMessage(message);
		}
	}
	
	protected void teleportPlayersToEvent()
	{
	}
	
	protected ArrayList<Fence> getFences()
	{
		return null;
	}
	
	protected void spawnFences()
	{
		ArrayList<Fence> fences = getFences();
		if (fences == null || fences.size() == 0)
		{
			return;
		}
		L2FenceInstance fenceInstance;
		for (Fence fence : fences)
		{
			fenceInstance = FenceTable.getInstance().addFence(fence._x, fence._y, fence._z, fence._type, fence._width, fence._length, fence._height);
			_fences.add(fenceInstance);
		}
	}
	
	protected void deleteFences()
	{
		if (_fences.size() == 0)
		{
			return;
		}
		for (L2FenceInstance fence : _fences)
		{
			L2WorldRegion region = fence.getRegion();
			if (region != null)
			{
				region.removeVisibleObject(fence);
			}
			if (fence.getKnownList() != null)
			{
				fence.getKnownList().removeAllKnownObjects();
			}
			L2World.getInstance().removeObject(fence);
			FenceTable.getInstance().removeFence(fence);
		}
	}
	
	public boolean onKill(L2Character killer, L2Character killed)
	{
		return true;
	}
	/**
	 * Выдать награду игроку
	 * @param player
	 */
	public void rewardPlayer(L2PcInstance player)
	{
		if (getReward() != null)
		{
			for (Reward reward : getReward())
			{
				player.addItem("EventReward", reward.getItemid(), reward.getCount(), player, true);
			}
		}
	}
}