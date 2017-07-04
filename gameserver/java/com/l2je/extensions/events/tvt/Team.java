package com.l2je.extensions.events.tvt;

import com.l2je.extensions.events.EventConfig;
import com.l2je.extensions.events.EventManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author dbg
 * Official Website: http://l2je.com 
 * @date 8 февр. 2017 г. 6:45:29 
 */
public class Team
{
	private int _id;
	private int _score;
	private final HashSet<L2PcInstance> _members = new HashSet<>();
	private final List<int[]> _spawnLocations = new ArrayList<>();
	
	public Team()
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
		if (EventConfig.TVT_SCORE_TITLE_PATTERN != null)
		{
			pattern = EventConfig.TVT_SCORE_TITLE_PATTERN;
		}
		for (L2PcInstance member : _members)
		{
			member.setEventTitle(pattern.replaceAll("%score%", String.valueOf(score)));
		}
	}
	
	public String getName()
	{
		return (getId() == 1) ? "Blue" : "Red";
	}
	
	public int getScore()
	{
		return _score;
	}
	
	public void addMember(L2PcInstance player)
	{
		if (EventConfig.EVENT_MANAGER_DEBUG)
			EventManager.getInstance().debug("Added " + player.getName() + " to team " + getName() + ".");
		_members.add(player);
		player.setEventTitleColor((getId() == 1) ? EventConfig.TVT_TEAM1_TITLE_COLOR : EventConfig.TVT_TEAM2_TITLE_COLOR);
		player.setEventNameColor((getId() == 1) ? EventConfig.TVT_TEAM1_NAME_COLOR : EventConfig.TVT_TEAM2_NAME_COLOR);
		player.setTeam(getId());
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
		if (EventConfig.EVENT_MANAGER_DEBUG)
			EventManager.getInstance().debug("Adding spawn location to team " + getId() + ": " + location[0] + "," + location[1] + "," + location[2]);
		_spawnLocations.add(location);
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
