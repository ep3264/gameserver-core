package com.l2je.protection;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public final class ProtectionProperties
{
	private static final String FILE = "./config/protection.properties";
	
	public static boolean RC5;
	public static boolean BLOWFISH;
	public static boolean HWID;
	public static boolean PUNISHER;
	public static String ON_HACK_ATTEMP;
	public static void init()
	{
		try (InputStream is = new FileInputStream(new File(FILE)))
		{
			Properties p = new Properties();
			p.load(is);
			
			RC5 = Boolean.parseBoolean(p.getProperty("RC5", "true"));
			BLOWFISH= Boolean.parseBoolean(p.getProperty("BLOWFISH", "true"));
			HWID = Boolean.parseBoolean(p.getProperty("HWID", "true"));
			PUNISHER=Boolean.parseBoolean(p.getProperty("PUNISHER", "true"));
			ON_HACK_ATTEMP=p.getProperty("OnHackAttempt", "kick");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Server failed to load " + FILE + " file.");
		}
	}
}