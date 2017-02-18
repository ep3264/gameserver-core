/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Vehicle;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.model.zone.type.L2DerbyTrackZone;
import net.sf.l2j.gameserver.model.zone.type.L2PeaceZone;
import net.sf.l2j.gameserver.model.zone.type.L2TownZone;

public final class WorldRegion
{
	private final List<L2Object> _objects = new ArrayList<>();
	private final List<WorldRegion> _surroundingRegions = new ArrayList<>();
	private final List<L2ZoneType> _zones = new ArrayList<>();
	
	private final int _tileX;
	private final int _tileY;
	
	private final Lock _lock = new ReentrantLock();
	
	private boolean _active;
	private ScheduledFuture<?> _activateTask;
	private int _playersCount;
	
	public WorldRegion(int x, int y)
	{
		_tileX = x;
		_tileY = y;
	}
	
	@Override
	public String toString()
	{
		return "WorldRegion [_tileX=" + _tileX + ", _tileY=" + _tileY + ", _active=" + _active + ", _playersCount=" + _playersCount + "]";
	}
	
	public List<L2Object> getObjects()
	{
		return _objects;
	}
	
	public void addSurroundingRegion(WorldRegion region)
	{
		_surroundingRegions.add(region);
	}
	
	public List<WorldRegion> getSurroundingRegions()
	{
		return _surroundingRegions;
	}
	
	public List<L2ZoneType> getZones()
	{
		return _zones;
	}
	
	public void addZone(L2ZoneType zone)
	{
		_zones.add(zone);
	}
	
	public void removeZone(L2ZoneType zone)
	{
		_zones.remove(zone);
	}
	
	public void revalidateZones(L2Character character)
	{
		// Do NOT update the world region while the character is still in the process of teleporting
		if (character.isTeleporting())
			return;
		
		_zones.forEach(z -> z.revalidateInZone(character));
	}
	
	public void removeFromZones(L2Character character)
	{
		_zones.forEach(z -> z.removeCharacter(character));
	}
	
	public boolean containsZone(int zoneId)
	{
		for (L2ZoneType z : _zones)
		{
			if (z.getId() == zoneId)
				return true;
		}
		return false;
	}
	
	public boolean checkEffectRangeInsidePeaceZone(L2Skill skill, final int x, final int y, final int z)
	{
		final int range = skill.getEffectRange();
		final int up = y + range;
		final int down = y - range;
		final int left = x + range;
		final int right = x - range;
		
		for (L2ZoneType e : _zones)
		{
			if ((e instanceof L2TownZone && ((L2TownZone) e).isPeaceZone()) || e instanceof L2DerbyTrackZone || e instanceof L2PeaceZone)
			{
				if (e.isInsideZone(x, up, z))
					return false;
				
				if (e.isInsideZone(x, down, z))
					return false;
				
				if (e.isInsideZone(left, y, z))
					return false;
				
				if (e.isInsideZone(right, y, z))
					return false;
				
				if (e.isInsideZone(x, y, z))
					return false;
			}
		}
		return true;
	}
	
	public void onDeath(L2Character character)
	{
		_zones.stream().filter(z -> z.isCharacterInZone(character)).forEach(z -> z.onDieInside(character));
	}
	
	public void onRevive(L2Character character)
	{
		_zones.stream().filter(z -> z.isCharacterInZone(character)).forEach(z -> z.onReviveInside(character));
	}
	
	public boolean isActive()
	{
		return _active;
	}
	
	public int getPlayersCount()
	{
		return _playersCount;
	}
	
	/**
	 * Check if all 9 neighbors (including self) are inactive or active but with no players.
	 * @return true if the above condition is met.
	 */
	public boolean areNeighborsEmpty()
	{
		// if this region is occupied, return false.
		if (isActive() && _playersCount != 0)
			return false;
		
		// if any one of the neighbors is occupied, return false
		for (WorldRegion neighbor : _surroundingRegions)
		{
			if (neighbor.isActive() && neighbor.getPlayersCount() != 0)
				return false;
		}
		
		// in all other cases, return true.
		return true;
	}
	
	/**
	 * This function turns this region's AI and geodata on or off
	 * @param value
	 */
	public void setActive(boolean value)
	{
		if (_active == value)
			return;
		
		_active = value;
		
		// turn the AI on or off to match the region's activation.
		if (!value)
		{
			for (L2Object o : _objects)
			{
				if (o instanceof L2Attackable)
				{
					L2Attackable mob = (L2Attackable) o;
					
					// Set target to null and cancel Attack or Cast
					mob.setTarget(null);
					
					// Stop movement
					mob.stopMove(null);
					
					// Stop all active skills effects in progress on the L2Character
					mob.stopAllEffects();
					
					mob.getAggroList().clear();
					mob.getAttackByList().clear();
					mob.getKnownList().removeAllKnownObjects();
					
					// stop the ai tasks
					if (mob.hasAI())
					{
						mob.getAI().setIntention(CtrlIntention.IDLE);
						mob.getAI().stopAITask();
					}
				}
				else if (o instanceof L2Vehicle)
					((L2Vehicle) o).getKnownList().removeAllKnownObjects();
			}
		}
		else
		{
			for (L2Object o : _objects)
			{
				if (o instanceof L2Attackable)
					((L2Attackable) o).getStatus().startHpMpRegeneration();
				else if (o instanceof L2Npc)
					((L2Npc) o).startRandomAnimationTimer();
			}
		}
	}
	
	public void addVisibleObject(L2Object object)
	{
		if (object == null)
			return;
		
		_lock.lock();
		try
		{
			_objects.add(object);
			
			if (object instanceof L2PcInstance)
			{
				// if this is the first player to enter the region, activate self & neighbors
				if (_playersCount++ == 0)
				{
					// first set self to active and do self-tasks...
					setActive(true);
					
					// if the timer to deactivate neighbors is running, cancel it.
					if (_activateTask != null)
						_activateTask.cancel(true);
					
					// then, set a timer to activate the neighbors
					_activateTask = ThreadPool.schedule(new ActivateTask(true), 1000);
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public void removeVisibleObject(L2Object object)
	{
		if (object == null)
			return;
		
		_lock.lock();
		try
		{
			_objects.remove(object);
			
			if (object instanceof L2PcInstance)
			{
				if (--_playersCount == 0)
				{
					// if the timer to activate neighbors is running, cancel it.
					if (_activateTask != null)
						_activateTask.cancel(true);
					
					// start a timer to "suggest" a deactivate to self and neighbors.
					// suggest means: first check if a neighbor has L2PcInstances in it. If not, deactivate.
					_activateTask = ThreadPool.schedule(new ActivateTask(false), 60000);
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public class ActivateTask implements Runnable
	{
		private final boolean _isActivating;
		
		public ActivateTask(boolean isActivating)
		{
			_isActivating = isActivating;
		}
		
		@Override
		public void run()
		{
			if (_isActivating)
			{
				// for each neighbor, if it's not active, activate.
				for (WorldRegion neighbor : getSurroundingRegions())
					neighbor.setActive(true);
			}
			else
			{
				if (areNeighborsEmpty())
					setActive(false);
				
				// check and deactivate
				for (WorldRegion neighbor : getSurroundingRegions())
					if (neighbor.areNeighborsEmpty())
						neighbor.setActive(false);
			}
		}
	}
}