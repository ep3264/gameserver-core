package custom.events;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.FenceTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
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
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.util.Broadcast;


public abstract class EventBase
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
		
		public synchronized void schedule(int delay)
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
		public synchronized void run()
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
		
		public synchronized void cancel()
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
			_player.sendMessage("Вы будете воскрешенны через " + delay + " секунд(ы).");
			_task = ThreadPool.schedule(this, delay * 1000);
		}
		
		@Override
		public synchronized void run()
		{
			_task = null;
			teleportPlayerToEvent(_player);
			_resurrectors.remove(this);
		}
		
		public synchronized void cancel()
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
	public EventBase()
	{		
		EventManager.getInstance().addEvent(this);
	}
	
	public abstract void start();	
	public abstract void trigger();
	public abstract String getName();
	public abstract int getEventId();
	public abstract boolean isRunning();
	public abstract void onKill(L2Character killer, L2Character killed);	
	protected abstract void teleportPlayerFromEvent(L2PcInstance player);
	
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
			player.setInEvent(false);
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
	
	public void setConfig(Map<String, String> config)
	{		
		_config = config;
		if (getString("blockedSkills") != null)
		{
			_blockedSkills.clear();
			for (String skill : getString("blockedSkills").split(","))
			{
				_blockedSkills.add(Integer.valueOf(skill.trim()));
			}
		}
		if (getString("blockedItems") != null)
		{
			_blockedItems.clear();
			for (String item : getString("blockedItems").split(","))
			{
				_blockedItems.add(Integer.valueOf(item.trim()));
			}
		}
	}
	
	public String getString(String key)
	{
		if (_config.containsKey(key))
		{
			return _config.get(key);
		}
		return null;
	}
	
	public int getInt(String key)
	{
		return Integer.valueOf(getString(key));
	}
	
	public boolean getBoolean(String key)
	{
		return Boolean.valueOf(getString(key));
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
		debug(player.getName() + " skipped the event.");
		schedule(1);
	}
	
	public boolean addPlayer(L2PcInstance player)
	{
		debug(player.getName() + " has joined the event.");
		for(L2PcInstance p : _players )
		{
			if(!p.isOnline())
			{
				_players.remove(p);
			}
		}
		_players.add(player);		
		_scores.put(player, 0);	
		player.setInEvent(true);
		return true;
	}
	
	public boolean removePlayer(L2PcInstance player)
	{
		debug(player.getName() + " has left the event.");
		_players.remove(player);
		player.setInEvent(false);
		//_scores.remove(player);
		if (isRunning())
		{
			teleportPlayerFromEvent(player);			
			if (getString("minPlayersRequired") != null)
			{
				if (_players.size() < getInt("minPlayersRequired"))
				{
					announce("Слишком много игроков покинуло event.");
					announce("Event отменен.");
					EventManager.getInstance().end(true);
				}
			}
		}
		return true;
	}
	
	public boolean containsPlayer(L2Character character)
	{
		if (character instanceof L2PcInstance)
		{
			return _players.contains(character);
		}
		return false;
	}
	
	protected void setScore(L2PcInstance player, int score)
	{
		String pattern = "%score%";
		if (getString("scoreInTitlePattern") != null)
		{
			pattern = getString("scoreInTitlePattern");
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
			EventManager.getInstance().userByPass(player, "info");
		}
		if (!player.isInEvent() && isRunning())
		{
			if (getString("zoneId") != null)
			{
				debug("Clearing defined zones.");
				try
				{
					L2ZoneType zone;
					for (String zoneId : getString("zoneId").split(","))
					{
						zone = ZoneManager.getInstance().getZoneById(Integer.valueOf(zoneId));
						if (zone == null)
						{
							debug("Failed to load zone \"" + zoneId + "\".");
							continue;
						}
						if (zone.isInsideZone(player))
						{
							player.sendMessage("Event: You can not be here right now.");
							player.teleToLocation(FAIL_SAFE_LOCATION[0], FAIL_SAFE_LOCATION[1], FAIL_SAFE_LOCATION[2], 0);
						}
					}
				}
				catch (NullPointerException e)
				{
				}
			}
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
			if (getBoolean("removeBuffs"))
			{
				player.getPet().stopAllEffects();
			}
			player.getPet().setCurrentCp(player.getMaxCp());
			player.getPet().setCurrentHp(player.getMaxHp());
			player.getPet().setCurrentMp(player.getMaxMp());
		}
		//==
		player.setIsInvul(false);
		player.getAppearance().setVisible();
		player.broadcastUserInfo();
		if (getBoolean("removeBuffs"))
		{
			player.stopAllEffects();
			player.stopAllToggles();
			player.stopCubics();
		}
		if (getBoolean("giveBuffs"))
		{
			String buffType = "fighterBuffs";
			if (player.isMageClass() || (player.getClassId().getId() == 49) || //Orc Mystic
			(player.getClassId().getId() == 50) || //Orc Shaman
			(player.getClassId().getId() == 51) || //Overlord
			(player.getClassId().getId() == 52) || //Warcryer
			(player.getClassId().getId() == 115) || //Dominator
			(player.getClassId().getId() == 116//Doom Cryer
			))
			{
				buffType = "mageBuffs";
			}
			String[] buff;
			L2Skill skill;
			for (String rawBuff : getString(buffType).split(";"))
			{
				buff = rawBuff.split(",");
				if (buff.length != 2)
				{
					debug("Failed to load sub-value from \"" + buffType + "\".");
					continue;
				}
				skill = SkillTable.getInstance().getInfo(Integer.valueOf(buff[0].trim()), Integer.valueOf(buff[1].trim()));
				if (skill != null)
				{
					skill.getEffects(player, player);
				}
			}
		}
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
		String message = getString("scoreBoardPattern");
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
		if (!player.isInEvent())
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
		if (!player.isInEvent())
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
		if (!player.isInEvent())
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
	public boolean canRequestToNpc(L2Character player, L2Character npc)
	{
		return true;
	}
	public boolean canUseMagic(L2Character player,L2Character target)
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
		_resurrectors.add(new EventResurrector(player, getInt("resurrectorTime")));
	}
	
	protected void spawnFences()
	{
		String fences = getString("fences");
		if (fences == null)
		{
			return;
		}
		int x, y, z, type, width, length, height;
		L2FenceInstance fence;
		String[] raw;
		for (String rawFence : fences.split(";"))
		{
			raw = rawFence.split(",");
			if (raw.length != 7)
			{
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
	
	protected void doorsToCloseOnStart()
	{
		if (getString("doorsToCloseOnStart") != null)
		{
			debug("Opening defined doors.");
			try
			{
				for (String door : getString("doorsToCloseOnStart").split(","))
				{
					DoorTable.getInstance().getDoor(Integer.valueOf(door.trim())).closeMe();
				}
			}
			catch (NullPointerException e)
			{
				debug("Failed to open door " + e);
			}
		}
	}
	
	protected void doorsToOpenOnEnd()
	{
		if (getString("doorsToOpenOnEnd") != null)
		{
			debug("Closing defined doors.");
			try
			{
				for (String door : getString("doorsToOpenOnEnd").split(","))
				{
					DoorTable.getInstance().getDoor(Integer.valueOf(door.trim())).openMe();
				}
			}
			catch (NullPointerException e)
			{
				debug("Failed to close door " + e);
			}
		}
	}
	
	protected void clearZones()
	{
		if (getString("zoneId") != null)
		{
			debug("Clearing defined zones.");
			try
			{
				L2ZoneType zone;
				for (String zoneId : getString("zoneId").split(","))
				{
					zone = ZoneManager.getInstance().getZoneById(Integer.valueOf(zoneId));
					if (zone == null)
					{
						debug("Failed to load zone \"" + zoneId + "\".");
						continue;
					}
					for (L2Character character : zone.getCharactersInside())
					{
						if (character instanceof L2PcInstance)
						{
							character.sendMessage("Event: You can not be here right now.");
							character.teleToLocation(FAIL_SAFE_LOCATION[0], FAIL_SAFE_LOCATION[1], FAIL_SAFE_LOCATION[2], 0);
						}
						else if ((character instanceof L2SummonInstance) || (character instanceof L2PetInstance))
						{
							character.sendMessage("Event: Your summon has been moved.");
							character.teleToLocation(((L2Summon) character).getX(), ((L2Summon) character).getY(), ((L2Summon) character).getZ(), 0);
						}
					}
				}
			}
			catch (NullPointerException e)
			{
			}
		}
	}
	
	protected void rewardPlayer(L2PcInstance player, String type)
	{
		if (getString(type) != null)
		{
			String[] reward;
			for (String rawReward : getString(type).split(";"))
			{
				reward = rawReward.split(",");
				if (reward.length == 2)
				{
					player.addItem("EventReward[" + getString("name") + "]", Integer.valueOf(reward[0].trim()), Integer.valueOf(reward[1].trim()), player, true);
				}
			}
		}
	}
	
}