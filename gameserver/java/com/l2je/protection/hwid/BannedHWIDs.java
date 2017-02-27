package com.l2je.protection.hwid;

import java.util.HashSet;

/**
 * @author evgeny64 Official Website: http://l2je.com
 * @date 24 февр. 2017 г. 20:00:56
 */
public class BannedHWIDs
{
	private HashSet<String> _ips;
	private HashSet<String> _hdds;
	private HashSet<String> _macs;
	
	public BannedHWIDs()
	{
		_ips = new HashSet<>();
		_hdds = new HashSet<>();
		_macs = new HashSet<>();
	}
		
	public void addIP(String ip)
	{
		_ips.add(ip);
	}
	
	public void addMAC(String mac)
	{
		_macs.add(mac);
	}
	
	public void addHDD(String hdd)
	{
		_hdds.add(hdd);
	}
	
	public int countIPs()
	{
		return _ips.size();
	}
	
	public int countMACs()
	{
		return _macs.size();
	}
	
	public int countHDDs()
	{
		return _hdds.size();
	}
	
	public void clear()
	{
		_ips.clear();
		_macs.clear();
		_hdds.clear();
	}
	
	public boolean containsIp(String ip)
	{
		return _ips.contains(ip);
	}
	
	public boolean containsHdd(String hdd)
	{
		return _hdds.contains(hdd);
	}
	
	public boolean containsMac(String mac)
	{
		return _macs.contains(mac);
	}
}
