package com.l2je.extensions.events;

import com.l2je.extensions.events.commons.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public final class EventManager
{
	private class AutoEventScheduler implements Runnable
	{
		
		private ScheduledFuture<?> _task = null;
		
		public AutoEventScheduler()
		{
			_log.info("EventManager: Auto event scheduler loaded.");
			int initialDelay = 0;
			initialDelay = EventConfig.EVENT_MANAGER_INIT_DELAY;
			if (initialDelay > 0)
			{
				_task = ThreadPool.schedule(this, initialDelay * 1000);
			}
		}
		
		@Override
		public void run()
		{
			if (getCurrentEvent() == null)
			{
				startEvent(getRandomEvent());
			}
			if(_task!=null)
			{
				_task.cancel(false);
			}
			int delayBetweenEvents = 0;
			delayBetweenEvents = EventConfig.EVENT_MANAGER_EVENTS_DELAY + getCurrentEvent().getRunningTime();
			if (delayBetweenEvents > 0)
			{
				_task = ThreadPool.schedule(this, delayBetweenEvents * 1000);
			}
		}
	}
	
	protected static final Logger _log = Logger.getLogger(EventManager.class.getName());
	public AutoEventScheduler _autoEventScheduler;
	private Event _currentEvent;
	final List<Event> _events = new ArrayList<>();
	public static final String HTML_FILE_PATH = "data/html/event_manager/";
	
	public static EventManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	static int getRandomEvent()
	{
		Random random = new Random();
		return EventConfig.EVENT_MANAGER_AUTO_EVENTS[random.nextInt(EventConfig.EVENT_MANAGER_AUTO_EVENTS.length)];
	}
	
	public EventManager()
	{
		_log.info("EventManager was created.");
	}
	
	public void startAutoEventScheduler()
	{
		if (EventConfig.AUTO_EVENT_SCHEDULER)
		{
			_autoEventScheduler = new AutoEventScheduler();
		}
	}
	
	public Event getCurrentEvent()
	{	
		return _currentEvent;
	}
	
	public void onShutDown()
	{
		_log.info("EventManager shuts down.");
		endCurrentEvent(true);
	}
	
	public final void addEvent(Event event)
	{
		// Event does not exist, return.
		if (event == null)
		{
			return;
		}
		// Event already loaded, unload id.
		Event old = getEvent(event.getId());
		if ((old != null))
		{
			_events.remove(old);
			_log.info("EventManager: Replaced: (" + old.getName() + ") with a new version (" + event.getName() + ").");
		}
		// Add new event.
		_events.add(event);
	}
	
	/**
	 * @param eventId
	 * @return
	 */
	public final Event getEvent(int eventId)
	{
		for (Event event : _events)
		{
			if (event.getId() == eventId)
			{
				return event;
			}
		}
		return null;
	}
	
	public final Event getEvent(String eventName)
	{
		for (Event event : _events)
		{
			if (event.getName().equalsIgnoreCase(eventName))
			{
				return event;
			}
		}
		return null;
	}
	
	public void startEvent(int id)
	{
		if (getCurrentEvent() != null)
		{
			getCurrentEvent().end(false);
		}
		_currentEvent = getEvent(id);
		
		if (getCurrentEvent() != null)
		{
			if (EventConfig.EVENT_MANAGER_DEBUG)
				debug("Event has started.");
			getCurrentEvent().start();
		}
		else
		{
			_log.info("EventManager: Failed to load event \"" + getCurrentEvent().getName() + "\".");
		}
	}
	
	public void endCurrentEvent(boolean inform)
	{
		if (getCurrentEvent() != null)
		{
			if (EventConfig.EVENT_MANAGER_DEBUG)
				debug("Event has ended.");
			getCurrentEvent().end(inform);
		}
		_currentEvent = null;
	}
	
	public void getInfo(L2PcInstance player)
	{
		if (getCurrentEvent() != null)
		{
			getCurrentEvent().showInfo(player);
		}
		else
		{
			player.sendMessage("В данный момент нет доступных эвентов.");
		}
	}
	
	public void leaveFromEvent(L2PcInstance player)
	{
		if (getCurrentEvent() != null)
		{
			if (player.getEvent() == getCurrentEvent())
			{
				if (player.isInCombat())
				{
					player.sendMessage("Вы не можете сейчас покинуть event.");
				}
				else if (getCurrentEvent().removePlayer(player))
				{
					player.sendMessage("Вы покинули event.");
				}
				else
				{
					player.sendMessage("There was an error while removing you from the event.");
				}
			}
			else
			{
				player.sendMessage("Вы не участвуете в event.");
			}
		}
		else
		{
			player.sendMessage("В данный момент нет доступных эвентов");
		}
	}
	
	public void joinToEvent(L2PcInstance player)
	{
		if (getCurrentEvent() != null)
		{
			if (player.isInCombat() || player.isInOlympiadMode() || player.isInDuel() || player.isInSiege())
			{
				player.sendMessage("Нельзя присоединиться во время боя.");
			}
			else if (player.getEvent() == getCurrentEvent())
			{
				player.sendMessage("Вы уже зарегистрированы.");
				getInfo(player);
			}
			else if(!getCurrentEvent().isRegistering())
			{
				player.sendMessage("Регистрация закончена.");
			}
			else if (getCurrentEvent().addPlayer(player))
			{
				player.sendMessage("Вы успешно зарегистрированы.");
				getInfo(player);
			}
			else
			{
				player.sendMessage("There was an error while adding you to the event.");
			}
		}
		else
		{
			player.sendMessage("Нету доступных эвентов в данный момент.");
		}
	}
	
	public void adminByPass(L2PcInstance player, String command)
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				if (EventConfig.EVENT_MANAGER_DEBUG)
					debug("AdminByPass: " + player.getName() + " Command " + command);
				if (command.equalsIgnoreCase("reload"))
				{
					EventConfig.init();
				}
				else if (command.equalsIgnoreCase("startRandom"))
				{
					if (getCurrentEvent() != null)
					{
						getCurrentEvent().end(true);
					}
					
					startEvent(getRandomEvent());
					getInfo(player);
				}
				else if (command.startsWith("start"))
				{
					String rawName = "";
					int eventId = 0;
					try
					{
						rawName = command.substring(5);
						String[] splitRaw = rawName.split("_");
						eventId = Integer.valueOf(splitRaw[0].trim());
					}
					catch (IndexOutOfBoundsException e)
					{
						player.sendMessage("Usage: //event start <id>");
					}
					catch (NumberFormatException e)
					{
						player.sendMessage("Usage: //event start <id>");
					}
					if (eventId > 0)
					{
						if (getCurrentEvent() != null)
						{
							getCurrentEvent().end(true);
						}
						try
						{
							startEvent(eventId);
						}
						catch (NullPointerException e)
						{
							player.sendMessage("Server: Event not found.");
						}
					}
				}
				else if (command.equalsIgnoreCase("skip"))
				{
					if (getCurrentEvent() != null)
					{
						getCurrentEvent().skip(player);
					}
				}
				else if (command.equalsIgnoreCase("end"))
				{
					if (getCurrentEvent() != null)
					{
						endCurrentEvent(true);
					}
				}
				Map<String, String> variables = new HashMap<>();
				StringBuffer sb = new StringBuffer();
				for (Event event : _events)
				{
					StringUtil.append(sb, event.getId(), "_", event.getName(), ";");
				}
				variables.put("%eventsList%", sb.toString());
				Util.sendHtml(player, HTML_FILE_PATH + "index.htm", variables);
				
			}
		}, 0);
	}
	
	public void debug(String message)
	{
		String eventName = "";
		if (getCurrentEvent() != null)
		{
			eventName =  getCurrentEvent().getName();
		}
		_log.info("EventDebug[" + eventName + "]: " + message);
	}
	
	private static class SingletonHolder
	{
		protected static final EventManager _instance = new EventManager();
	}
}