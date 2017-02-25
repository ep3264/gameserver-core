package com.l2je.protection.hwid;

import java.util.HashSet;

/**
 * @author evgeny64 Official Website: http://l2je.com
 * @date 24 февр. 2017 г. 20:00:56
 */
public class BannedHwids
{
	private HashSet<String> _ips;
	private HashSet<String> _hdds;
	private HashSet<String> _macs;
	
	public BannedHwids()
	{
		_ips = new HashSet<>();
		_hdds = new HashSet<>();
		_macs = new HashSet<>();
	}
	
	public void add(String ip, String mac, String hdd)
	{
		_ips.add(ip);
		_macs.add(mac);
		_hdds.add(hdd);
	}
	
	public void add(Hwid hwid)
	{
		_ips.add(hwid.getIP());
		_macs.add(hwid.getMac());
		_hdds.add(hwid.getHdd());
		
	}
	
	public int size()
	{
		return _ips.size();
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
