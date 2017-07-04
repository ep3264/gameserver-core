package com.l2je.extensions.events.commons;

/**
 * @author dbg Official Website: http://l2je.com
 * @date 10 февр. 2017 г. 4:17:29
 */
public class Reward
{
	private int _itemId;
	private int _count;
	
	public Reward(int itemId, int count)
	{
		_itemId = itemId;
		_count = count;
	}
	
	public int getItemid()
	{
		return _itemId;
	}
	
	public int getCount()
	{
		return _count;
	}
}
