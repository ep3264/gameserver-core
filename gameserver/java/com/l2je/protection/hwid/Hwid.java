package com.l2je.protection.hwid;

public class Hwid {
	private String _login;
	private String _hwidHdd;
	private String _ip;

	public Hwid(String login,String HWID,String ip)
	{
		_login=login;
		_hwidHdd=HWID;
		_ip=ip;
		
	}

	public String getHwid() {
		return _hwidHdd;
	}

	public String getLogin() {
		return _login;
	}
	public String getIP() {
		return _ip;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_hwidHdd == null) ? 0 : _hwidHdd.hashCode());
		result = prime * result + ((_ip == null) ? 0 : _ip.hashCode());
		result = prime * result + ((_login == null) ? 0 : _login.hashCode());
		return result;
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
		if (_hwidHdd == null)
		{
			if (other._hwidHdd != null)
				return false;
		}
		else if (!_hwidHdd.equals(other._hwidHdd))
			return false;
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
		return true;
	}

}
