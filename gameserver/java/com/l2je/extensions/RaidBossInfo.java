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
package com.l2je.extensions;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author Redist
 */
public class RaidBossInfo
{
	
	private static class SingletonHolder
	{
		protected static final RaidBossInfo _instance = new RaidBossInfo();
	}
	
	public static RaidBossInfo getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected final static Map<Integer, Long> _respawns = new HashMap<>();
	protected final static Map<Integer, Long> _epicRespawns = new HashMap<>();
	
	public long getRespawnTime(int bossId)
	{
		if (_respawns.containsKey(bossId))
		{
			return _respawns.get(bossId);
		}
		return 1;
	}
	public long getEpicRespawnDate(int bossId)
	{
		if (_epicRespawns.containsKey(bossId))
		{
			return _epicRespawns.get(bossId);
		}
		return 1;
	}
	public void setEpicRespawnDate(int bossId, long time)
	{
		_epicRespawns.put(bossId, time);
	}
	public void setRespawnTime(int bossId, long time)
	{
		_respawns.put(bossId, time);
	}
	
	public void removeBossInfo(int bossId)
	{
		if (_respawns.containsKey(bossId))
		{
			_respawns.remove(bossId);
		}
	}
	public void cleanUp()
	{
		_respawns.clear();
	}
	public void announce(String message)
	{
		Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", "Raid Boss Info: " + message));
	}
}
