package com.l2je.protection;

import net.sf.l2j.Config;
import net.sf.l2j.commons.config.ExProperties;

public final class ProtectionConfig
{
	private static final String FILE = "./config/protection.properties";
	
	public static boolean RC5;
	public static boolean BLOWFISH;
	public static boolean HWID;
	public static boolean PUNISHER;
	public static String ON_HACK_ATTEMP;
	public static boolean HWID_BAN_IP;
	public static boolean HWID_BAN_MAC;
	public static boolean HWID_BAN_HDD;
	
	public static void init()
	{
		try
		{
			final ExProperties protection = Config.initProperties(FILE);			
			RC5 = Boolean.parseBoolean(protection.getProperty("RC5", "true"));
			BLOWFISH = Boolean.parseBoolean(protection.getProperty("BLOWFISH", "true"));
			HWID = Boolean.parseBoolean(protection.getProperty("HWID", "true"));
			PUNISHER = Boolean.parseBoolean(protection.getProperty("PUNISHER", "true"));
			ON_HACK_ATTEMP = protection.getProperty("ON_HACK_ATTEMP", "kick");
			HWID_BAN_IP =protection.getProperty("HWID_BAN_IP", false);
			HWID_BAN_MAC =protection.getProperty("HWID_BAN_MAC", false);
			HWID_BAN_HDD =protection.getProperty("HWID_BAN_HDD", false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}