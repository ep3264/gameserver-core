package com.l2je.extensions.events;

import com.l2je.extensions.events.tvt.Team;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * Абстрактный класс, который представляет общий механизм военных ивентов
 * @author evgeny64 Official Website: http://l2je.com
 * @date 8 февр. 2017 г. 5:34:20
 */
public abstract class CombatEvent extends Event
{
	protected List<EventResurrector> _resurrectors = new LinkedList<>();
	private final static int DEFAULT_RES_TIME = 10;		
	
	protected int getResTime()
	{
		return DEFAULT_RES_TIME;
	}
	/**
	 * 
	 * @return давать +1 к Пвп счетчику
	 */
	protected boolean pvpOnKill()
	{
		return false;
	}
	/**
	 * 
	 * @return Награждать за каждое убийство
 	 */
	protected boolean rewardOnKill()
	{
		return false;
	}	

	@Override
	public boolean onKill(L2Character killer, L2Character killed)
	{
		if (!isRunning())
		{
			return false;
		}
		if (killer == killed)
		{
			return false;
		}
		L2PcInstance killerPc = getPcInstance(killer);
		L2PcInstance killedPc = getPcInstance(killed);
		if (killerPc == null || killedPc == null)
		{
			return false;
		}
		if (!containsPlayer(killerPc) || !containsPlayer(killedPc))
		{
			return false;
		}
		if (killerPc == killedPc)
		{
			return false;
		}
		setScore(killerPc, _scores.get(killerPc) + 1);
		if (killed instanceof L2PcInstance)
		{
			addResurrector(killedPc);
		}
		if (pvpOnKill())
		{
			killerPc.setPvpKills(killerPc.getPvpKills() + 1);
		}
		if (rewardOnKill())
		{
			rewardPlayer(killerPc);
		}
		Team killerTeam = getTeam(killerPc);
		Team killedTeam = getTeam(killedPc);
		if(killerTeam ==null || killedTeam==null )
		{
			return false;
		}
		if (killerTeam == killedTeam)
		{
			return false;
		}
		killerTeam.setScore(killerTeam.getScore() + 1);
		return true;
	}	
    /**
     * Можно ли использовать магию хил, баф и тд
     */
	@Override
	public boolean canUseMagic(L2Character attacker, L2Character target)
	{
		if (!isRunning())
		{
			return true;
		}
		L2PcInstance targetPcInstance = getPcInstance(target);
		L2PcInstance attackerPcInstance = getPcInstance(attacker);
		if (targetPcInstance == null || attackerPcInstance == null)
		{
			return true;
		}
		if (!containsPlayer(attackerPcInstance) && containsPlayer(targetPcInstance))
		{
			attackerPcInstance.sendMessage("В данный момент нельзя взаимодействовать с персонажем!");
			return false;
		}
		return true;
	}
	/**
	 * 
	 * @param target
	 * @param attacker
	 * @return Предназначина ли цель для автоатаки
	 */
	@Override
	public boolean playersAutoAttackable(L2Character target, L2Character attacker)
	{
		if (!isRunning())
		{
			return false;
		}		
		L2PcInstance targetPcInstance = getPcInstance(target);
		L2PcInstance attackerPcInstance = getPcInstance(attacker);
		if (targetPcInstance == null || attackerPcInstance == null)
		{
			return false;
		}
		if (!containsPlayer(targetPcInstance) || !containsPlayer(attackerPcInstance))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Завершает ивент
	 * @param inform анонс о окончании ивента
	 */
	@Override
	public void end(boolean inform)
	{		
		removeAllPlayersFromEvent();
		_eventScheduler.cancel();
		for (EventResurrector resurrector : _resurrectors)
		{
			resurrector.cancel();
		}
		_locations.clear();
		_resurrectors.clear();
		_scores.clear();
		_players.clear();
		_eventState = EventState.IDLE;
		if (inform)
		{
			announce("Event закончился.");
		}		
	}
	/**
	 * Можно ли атаковать в мирной зоне
	 */
	@Override
	public boolean canAttackInPeace(L2Character attacker, L2Character target)
	{
		if (!isRunning())
		{
			return false;
		}
		L2PcInstance targetPcInstance = getPcInstance(target);
		L2PcInstance attackerPcInstance = getPcInstance(attacker);
		if (targetPcInstance == null || attackerPcInstance == null)
		{
			return false;
		}
		if (!containsPlayer(targetPcInstance) || !containsPlayer(attackerPcInstance))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Добавить задачу воскрешения игрока
	 * @param player игрок, которого надо воскресить
	 */
	protected void addResurrector(L2PcInstance player)
	{
		_resurrectors.add(new EventResurrector(player, getResTime()));
	}
	
	/**
	 * Класс поток, который воскрешает игроков
	 * @author evgeny64 Official Website: http://l2je.com
	 * @date 8 февр. 2017 г. 5:32:37
	 */
	protected class EventResurrector implements Runnable
	{
		private ScheduledFuture<?> _task = null;
		private final L2PcInstance _player;
		
		public EventResurrector(L2PcInstance player, int delay)
		{
			_player = player;
			StringBuffer stringBuffer = new StringBuffer();
			StringUtil.append(stringBuffer, "Вы будете воскрешенны через ", delay, " секунд(ы).");
			_player.sendMessage(stringBuffer.toString());
			_task = ThreadPool.schedule(this, delay * 1000);
		}
		
		@Override
		public void run()
		{
			_task = null;
			teleportPlayerToEvent(_player);
			_resurrectors.remove(this);
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
}
