package com.l2je.extensions.events.commons;

/**
 * Класс ограда
 * @author dbg
 * Official Website: http://l2je.com 
 * @date 8 февр. 2017 г. 4:47:50 
 */
public class Fence
{
	public int _x;
	public int _y;
	public int _z;
	public int _type;
	public int _width;
	public int _length;
	public int _height;
	
	public Fence(int x, int y, int z, int type, int width, int length, int height)
	{
		_x = x;
		_y = y;
		_z = z;
		_type = type;
		_width = width;
		_length = length;
		_height = height;
	}
}
