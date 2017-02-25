package com.l2je.protection.hwid;

import net.sf.l2j.commons.lang.StringUtil;

public class Hwid
{
	private String _login;
	private String _hdd;
	private String _ip;
	private String _mac;
	private int _hash = 0;
	
	public Hwid(String login, String ip, String mac, String hdd)
	{
		_login = login;
		_ip = ip;
		_mac = mac;
		_hdd = hdd;
	}
	
	public String getHdd()
	{
		return _hdd;
	}
	
	public String getMac()
	{
		return _mac;
	}
	
	public String getLogin()
	{
		return _login;
	}
	
	public String getIP()
	{
		return _ip;
	}
	
	@Override
	public int hashCode()
	{
		if (_hash == 0)
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((_ip == null) ? 0 : _ip.hashCode());
			result = prime * result + ((_login == null) ? 0 : _login.hashCode());
			result = prime * result + ((_mac == null) ? 0 : _mac.hashCode());
			result = prime * result + ((_hdd == null) ? 0 : _hdd.hashCode());
			_hash = result;
		}
		return _hash;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Hwid other = (Hwid) obj;
		if (_ip == null)
		{
			if (other._ip != null)
				return false;
		}
		else if (!_ip.equals(other._ip))
			return false;
		if (_login == null)
		{
			if (other._login != null)
				return false;
		}
		else if (!_login.equals(other._login))
			return false;
		if (_mac == null)
		{
			if (other._mac != null)
				return false;
		}
		else if (!_mac.equals(other._mac))
			return false;
		if (_hdd == null)
		{
			if (other._hdd != null)
				return false;
		}
		else if (!_hdd.equals(other._hdd))
			return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		StringUtil.append(sb, "Account: ", _login, " Ip: ", _ip, " Mac: ", _mac, " Hdd: ", _hdd);
		return sb.toString();
	}
}
