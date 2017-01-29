package com.l2je.protection.hwid;

public class HWID {
	private String _login;
	private String _HWID;
	private String _ip;

	public HWID(String login,String HWID,String ip)
	{
		_login=login;
		_HWID=HWID;
		_ip=ip;
		
	}

	public String getHwid() {
		return _HWID;
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
		result = prime * result + ((_HWID == null) ? 0 : _HWID.hashCode());
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
		HWID other = (HWID) obj;
		if (_HWID == null)
		{
			if (other._HWID != null)
				return false;
		}
		else if (!_HWID.equals(other._HWID))
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
