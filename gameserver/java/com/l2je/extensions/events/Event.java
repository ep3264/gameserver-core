package com.l2je.extensions.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.FenceTable;

import net.sf.l2j.gameserver.model.L2Effect;

import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.L2WorldRegion;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;

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
	
	protected class EventScheduler implements Runnable
	{
		private ScheduledFuture<?> _task = null;
		private int _delay;
		
		public  void schedule(int delay)
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
		
		public  void cancel()
		{
			if (_task != null)
			{
				_task.cancel(false);
			}
			_task = null;
		}
	}
	
	protected class EventResurrector implements Runnable
	{
		private ScheduledFuture<?> _task = null;
		private final L2PcInstance _player;
		
		public EventResurrector(L2PcInstance player, int delay)
		{
			_player = player;
			StringBuffer stringBuffer = new StringBuffer();
			StringUtil.append(stringBuffer, "Вы будете воскрешенны через ", delay , " секунд(ы).");
			_player.sendMessage(stringBuffer.toString());
			_task = ThreadPool.schedule(this, delay * 1000);
		}
		
		@Override
		public  void run()
		{
			_task = null;
			teleportPlayerToEvent(_player);
			_resurrectors.remove(this);
		}
		
		public  void cancel()
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
	protected List<EventResurrector> _resurrectors = new LinkedList<>();
	protected Map<L2PcInstance, Integer> _scores = new HashMap<>();
	protected Map<L2PcInstance, int[]> _locations = new HashMap<>();
	protected Map<L2PcInstance, L2Effect[]> _effects = new HashMap<>();
	protected Map<String, String> _config = new HashMap<>();
	protected HashSet<Integer> _blockedSkills = new HashSet<>();
	protected HashSet<Integer> _blockedItems = new HashSet<>();
	protected List<L2FenceInstance> _fences = new LinkedList<>();
	protected ArrayList<int[]> _spawnLocations = new ArrayList<>();
	protected int _id;
	protected String _name;
	public Event()
	{		
		EventManager.getInstance().addEvent(this);
		
	}
	
	public abstract void start();	
	public abstract void trigger();
	public abstract boolean isRunning();
	public abstract void onKill(L2Character killer, L2Character killed);	
	protected abstract void teleportPlayerFromEvent(L2PcInstance player);
	protected abstract void init();
	protected abstract void initBlockItems();
	protected abstract void initBlockSkills();
	public abstract int getRunningTime();
	protected abstract int getResTime();
	public String getName()
	{
		return _name;
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
	
	public boolean isRegistering()
	{
		return false;
	}
	
	public boolean isTeleporting()
	{
		return false;
	}	
	public String eventStateToString(String state)
	{
		return null;
	}	
	
	public void end(boolean inform)
	{
		for(L2PcInstance player:_players)
		{
			player.setEvent(null);
		}
		_eventScheduler.cancel();
		for (EventResurrector resurrector : _resurrectors)
		{
			resurrector.cancel();
		}
		_resurrectors.clear();		
		_scores.clear();
		_players.clear();
		if (inform)
		{			
			announce("Event закончился.");
		}		
	}	

	public void debug(String message)
	{
		EventManager.getInstance().debug(message);
	}
	
	public void schedule(int seconds)
	{
		_eventScheduler.schedule(seconds * 1000);
	}
	
	public void skip(L2PcInstance player)
	{
		if (EventConfig.EVENT_MANAGER_DEBUG)
			debug(player.getName() + " skipped the event.");
		schedule(1);
	}
	
	public boolean addPlayer(L2PcInstance player)
	{
		if (EventConfig.EVENT_MANAGER_DEBUG)
			debug(player.getName() + " has joined the event.");
		for(L2PcInstance p : _players )
		{
			if(!p.isOnline())
			{
				_players.remove(p);
				p.setEvent(null);
			}
		}
		_players.add(player);		
		_scores.put(player, 0);	
		player.setEvent(this);
		return true;
	}
	
	public boolean removePlayer(L2PcInstance player)
	{
		if (EventConfig.EVENT_MANAGER_DEBUG)
			debug(player.getName() + " has left the event.");
		_players.remove(player);
		player.setEvent(null);
		if (isRunning())
		{
			teleportPlayerFromEvent(player);
		}
		return true;
	}
	
	public boolean containsPlayer(L2PcInstance player)
	{		
		return player.getEvent()==this;		
	}
	
	protected void setScore(L2PcInstance player, int score)
	{
		String pattern = "%score%";
		if (EventConfig.EW_SCORE_TITLE_PATTERN != null)
		{
			pattern = EventConfig.EW_SCORE_TITLE_PATTERN;
		}
		player.setEventTitle(pattern.replaceAll("%score%", String.valueOf(score)));
		_scores.put(player, score);
	}
	
	protected void teleportPlayerToEvent(L2PcInstance player)
	{
		onFixedRes(player);
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
	/*
	* Handles all player && summon buffs, and onTeleport actions; including fixed res ofcourse.
	*/
	public void onFixedRes(L2PcInstance player)
	{
		if (player.isDead())
		{
			player.doRevive();
		}
		//==
		if (player.getPet() instanceof L2PetInstance)
		{
			player.getPet().unSummon(player);
		}
		if (player.getPet() != null)
		{			
			player.getPet().setCurrentCp(player.getMaxCp());
			player.getPet().setCurrentHp(player.getMaxHp());
			player.getPet().setCurrentMp(player.getMaxMp());
		}
		//==
		player.setIsInvul(false);
		player.getAppearance().setVisible();
		player.broadcastUserInfo();
		returnBuffs(player);
		//==
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
	}	
	private void returnBuffs(L2PcInstance player)
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
		String message = EventConfig.EW_BOARD_PATTERN;
		message = message.replaceAll("%mins%", String.valueOf(mins));
		message = message.replaceAll("%secs%", (secs < 10 ? "0" + secs : String.valueOf(secs)));
		int position = 8;//3
		ExShowScreenMessage screenMessage = new ExShowScreenMessage(1, 0, position, false, 1, 0, 0, false, 980, false, message);
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
		if (player.getEvent()!=this)
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
		if (player.getEvent()!=this)
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
		if (player.getEvent()!=this)
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
	
	public void showInfo(L2PcInstance player)
	{
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
	
	protected void teleportPlayersFromEvent()
	{
		for (L2PcInstance player : _players)
		{		
			teleportPlayerFromEvent(player);			
		}
		_locations.clear();
	}	
	
	protected void addResurrector(L2PcInstance player)
	{
		_resurrectors.add(new EventResurrector(player, getResTime()));
	}
	
	protected void spawnFences()
	{
		String [] fences = EventConfig.TVT_FENCES;
		if (fences == null)
		{
			return;
		}
		int x, y, z, type, width, length, height;
		L2FenceInstance fence;
		String[] raw;
		for (String rawFence :fences)
		{
			raw = rawFence.split(",");
			if (raw.length != 7)
			{
				if (EventConfig.EVENT_MANAGER_DEBUG)
					debug("Failed to load sub-value from \"fences\".");
				continue;
			}
			x = Integer.valueOf(raw[0]);
			y = Integer.valueOf(raw[1]);
			z = Integer.valueOf(raw[2]);
			type = Integer.valueOf(raw[3]);
			width = Integer.valueOf(raw[4]);
			length = Integer.valueOf(raw[5]);
			height = Integer.valueOf(raw[6]);
			fence=FenceTable.getInstance().addFence(x, y, z, type, width, length, height);
			_fences.add(fence);			
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

	public boolean playersAutoAttackable(L2Character target, L2Character attacker)
	{
		return false;
	}	
}