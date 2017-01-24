package custom.events;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;

public class EventManager
{
	protected static final Logger _log = Logger.getLogger(EventManager.class.getName());
	
	public static final String HTML_FILE_PATH = "data/html/event_manager/";
	public static final String CONFIG_FILE_PATH = "./data/xml/events.xml";
	
	private class AutoEventScheduler implements Runnable
	{
		@SuppressWarnings("unused")
		private ScheduledFuture<?> _task = null;
		
		public AutoEventScheduler()
		{
			_log.info("EventManager: Auto event scheduler loaded.");
			int initialDelay = 0;
			try
			{
				initialDelay = Integer.valueOf(getVar("initialDelay"));
			}
			catch (NumberFormatException e)
			{
				_log.info("EventManager: Failed to load 'initialDelay' from events.xml");
			}
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
				startEvent(_config.getRandomEvent());
			}
			int delayBetweenEvents = 0;
			try
			{
				delayBetweenEvents = Integer.valueOf(getVar("delayBetweenEvents"));
			}
			catch (NumberFormatException e)
			{
				_log.info("EventManager: Failed to load 'delayBetweenEvents' from events.xml");
			}
			if (delayBetweenEvents > 0)
			{
				_task = ThreadPool.schedule(this, delayBetweenEvents * 1000);
			}
		}
	}
	
	public final class EventConfig
	{
		private final Map<Integer, Map<String, String>> _events = new HashMap<>();
		private final Map<String, String> _config = new HashMap<>();
		private int _lastRandomEventId = 0;
		
		public EventConfig()
		{
			loadEventsTable();
		}
		
		public void loadEventsTable()
		{
			int variableCount = 0;
			HashSet<String> _names = new HashSet<>();
			try
			{
				final File f = new File(CONFIG_FILE_PATH);
				final Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
				final Node n = doc.getFirstChild();
				
				String id, name, key, value;
				Map<String, String> event;
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					NamedNodeMap attrs = d.getAttributes();
					
					if ("eventmanager".equalsIgnoreCase(d.getNodeName()))
					{
						_config.put("enabled", attrs.getNamedItem("enabled").getNodeValue());
						_config.put("debug", attrs.getNamedItem("debug").getNodeValue());
						
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("variable".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								key = attrs.getNamedItem("key").getNodeValue();
								if (key != null && !key.isEmpty())
								{
									value = attrs.getNamedItem("value").getNodeValue();
									_config.put(key, value);
								}
							}
						}
					}
					else if ("event".equalsIgnoreCase(d.getNodeName()))
					{
						event = new HashMap<>();
						id = attrs.getNamedItem("id").getNodeValue();
						name = attrs.getNamedItem("name").getNodeValue();
						if (_names.contains(name))
						{
							_log.info("EventManager: Failed to load event \"" + name + "\", name is already used.");
							continue;
						}
						_names.add(name);
						event.put("id", id);
						event.put("name", name);
						event.put("engine", attrs.getNamedItem("engine").getNodeValue());
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("variable".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								key = attrs.getNamedItem("key").getNodeValue();
								if (key != null && !key.isEmpty())
								{
									value = attrs.getNamedItem("value").getNodeValue();
									event.put(key, value);
								}
							}
						}
						_events.put(Integer.valueOf(id), event);
						variableCount = variableCount + event.size();
					}
				}
			}
			catch (Exception e)
			{
				_log.warning("Error parsing events.xml: " + e.toString());
			}
			_log.info("EventManager: Loaded " + (variableCount + _config.size()) + " variables for " + _events.size() + " events.");
		}
		
		public String getVar(String key)
		{
			return _config.get(key);
		}
		
		public Map<String, String> getEvent(int event)
		{
			return _events.get(event);
		}
		
		public List<Map<String, String>> getEvents()
		{
			List<Map<String, String>> events = new ArrayList<>();
			for (Map<String, String> event : _events.values())
			{
				events.add(event);
			}
			return events;
		}
		
		public Map<String, String> getEventById(int id)
		{
			if (_events.containsKey(id))
			{
				return _events.get(id);
			}
			return null;
		}
		
		public Map<String, String> getEventByName(String name)
		{
			for (Map<String, String> event : _events.values())
			{
				if ((event.get("name")).equalsIgnoreCase(name))
				{
					return event;
				}
			}
			return null;
		}
		
		public Map<String, String> getRandomEvent()
		{
			Map<String, String> event = null;
			List<Integer> autoEvents = new ArrayList<>();
			for (String rawAuto : getVar("eventListForAutomation").split(","))
			{
				autoEvents.add(Integer.valueOf(rawAuto.trim()));
			}
			Random rnd = new Random();
			int eventId = autoEvents.get(rnd.nextInt(autoEvents.size()));
			int tempId;
			event = getEventById(eventId);
			if (event != null)
			{
				try
				{
					tempId = Integer.valueOf(event.get("id"));
					if ((autoEvents.size() > 1) && (_lastRandomEventId == tempId))
					{
						return getRandomEvent();
					}
					_lastRandomEventId = Integer.valueOf(event.get("id"));
				}
				catch (NumberFormatException e)
				{
					_log.info("EventManager#getRandomEvent#NumberFormatException: " + e.getMessage());
				}
			}
			else
			{
				_log.info("EventManager: Event not found.");
			}
			return event;
		}
		
		public int countEvents()
		{
			return _events.size();
		}
	}
	
	public EventConfig _config;
	public AutoEventScheduler _autoEventScheduler;
	private Event _currentEvent;
	private final List<Event> _events = new ArrayList<>();
	
	public static EventManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public EventManager()
	{
		_config = new EventConfig();
		
	}
	
	public void startAutoEventScheduler()
	{
		if (getVar("enabled").equalsIgnoreCase("true"))
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
	}
	
	public String getVar(String key)
	{
		return _config.getVar(key);
	}
	
	public final void addEvent(Event event)
	{
		// Quest does not exist, return.
		if (event == null)
		{
			return;
		}
		
		// Event already loaded, unload id.
		Event old = getEvent(event.getEventId());
		if ((old != null))
		{
			_events.remove(old);
			_log.info("EventManager: Replaced: (" + old.getName() + ") with a new version (" + event.getName() + ").");
			
		}
		
		// Add new quest.
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
			if (event.getEventId() == eventId)
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
	
	public void startEvent(Map<String, String> event)
	{
		if (getCurrentEvent() != null)
		{
			getCurrentEvent().end(false);
		}
		_currentEvent = getEvent(Integer.parseInt(event.get("id")));
		
		if (getCurrentEvent() != null)
		{
			debug("Event has started.");
			getCurrentEvent().setConfig(event);
			getCurrentEvent().start();
		}
		else
		{
			_log.info("EventManager: Failed to load event \"" + event.get("name") + "\".");
		}
	}
	
	public void end(boolean inform)
	{
		if (getCurrentEvent() != null)
		{
			debug("Event has ended.");
			getCurrentEvent().end(inform);
		}	
		_currentEvent = null;
	}
	
	public void userByPass(L2PcInstance player, String command)
	{
		if (command.equalsIgnoreCase("join"))
		{
			if (getCurrentEvent() != null)
			{
				if (player.isInCombat() || player.isInOlympiadMode() || player.isInDuel() || player.isInSiege())
				{
					player.sendMessage("Нельзя присоединиться во время боя.");
				}
				else if (player.getEvent()==getCurrentEvent())
				{
					player.sendMessage("Вы уже зарегистрированы.");
				}
				else if (getCurrentEvent().addPlayer(player))
				{
					player.sendMessage("Вы успешно зарегистрированы.");
					userByPass(player, "info");
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
		else if (command.equalsIgnoreCase("info"))
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
		else if (command.equalsIgnoreCase("leave"))
		{
			if (getCurrentEvent() != null)
			{
				if (player.getEvent()==getCurrentEvent())
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
	}
	
	public void adminByPass(L2PcInstance player, String command)
	{
		ThreadPool.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				debug("AdminByPass: " + player.getName() + " Command " + command);
				
				if (command.equalsIgnoreCase("reload"))
				{
					_config.loadEventsTable();
					if (getCurrentEvent() != null)
					{
						getCurrentEvent().setConfig(_config.getEventById(getCurrentEvent().getInt("id")));
					}
				}
				else if (command.equalsIgnoreCase("startRandom"))
				{
					if (getCurrentEvent() != null)
					{
						getCurrentEvent().end(true);
					}
					
					startEvent(_config.getRandomEvent());
					userByPass(player, "info");
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
							startEvent(_config.getEventById(eventId));
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
						end(true);
					}
				}
				
				Map<String, String> variables = new HashMap<>();
				String eventsList = "";
				for (Map<String, String> event : _config.getEvents())
				{
					eventsList += event.get("id") + "_" + event.get("name").replaceAll(" ", "_") + ";";
				}
				variables.put("%eventsList%", eventsList);
				CustomUtil.sendHtml(player, HTML_FILE_PATH + "index.htm", variables);
				
			}
		}, 0);
	}
	
	public void debug(String message)
	{
		if (getVar("debug").equalsIgnoreCase("true"))
		{
			String eventName = "";
			if (getCurrentEvent() != null)
			{
				eventName = "[" + getCurrentEvent().getString("name") + "]";
			}
			_log.info("EventDebug" + eventName + ": " + message);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final EventManager _instance = new EventManager();
	}
}