package net.sf.l2j.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * @author evgeny64 Official Website: http://l2je.com
 * @date 18 февр. 2017 г. 9:18:58
 */
public class Util
{
	private static final Logger _log = Logger.getLogger(Util.class.getName());
	
	/**
	 * prints all other methods.
	 */
	public static void printGeneralSystemInfo()
	{
		printSystemTime();
		printOSInfo();
		printCpuInfo();
		printRuntimeInfo();
		printJreInfo();
		printJvmInfo();
	}
	
	private static void printCpuInfo()
	{
		_log.info("Avaible CPU(s): " + Runtime.getRuntime().availableProcessors());
		_log.info("Processor(s) Identifier: " + System.getenv("PROCESSOR_IDENTIFIER"));
		_log.info("..................................................");
		_log.info("..................................................");
	}
	
	/**
	 * returns the operational system server is running on it.
	 */
	private static void printOSInfo()
	{
		_log.info("OS: " + System.getProperty("os.name") + " Build: " + System.getProperty("os.version"));
		_log.info("OS Arch: " + System.getProperty("os.arch"));
		_log.info("..................................................");
		_log.info("..................................................");
	}
	
	/**
	 * returns JAVA Runtime Enviroment properties
	 */
	private static void printJreInfo()
	{
		_log.info("Java Platform Information");
		_log.info("Java Runtime  Name: " + System.getProperty("java.runtime.name"));
		_log.info("Java Version: " + System.getProperty("java.version"));
		_log.info("Java Class Version: " + System.getProperty("java.class.version"));
		_log.info("..................................................");
		_log.info("..................................................");
	}
	
	/**
	 * returns general infos related to machine
	 */
	private static void printRuntimeInfo()
	{
		_log.info("Runtime Information");
		_log.info("Current Free Heap Size: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " mb");
		_log.info("Current Heap Size: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + " mb");
		_log.info("Maximum Heap Size: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " mb");
		_log.info("..................................................");
		_log.info("..................................................");
	}
	
	/**
	 * calls time service to get system time.
	 */
	private static void printSystemTime()
	{
		// instanciates Date Objec
		Date dateInfo = new Date();
		
		// generates a simple date format
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa");
		
		// generates String that will get the formater info with values
		String dayInfo = df.format(dateInfo);
		
		_log.info("..................................................");
		_log.info("System Time: " + dayInfo);
		_log.info("..................................................");
	}
	
	/**
	 * gets system JVM properties.
	 */
	private static void printJvmInfo()
	{
		_log.info("Virtual Machine Information (JVM)");
		_log.info("JVM Name: " + System.getProperty("java.vm.name"));
		_log.info("JVM installation directory: " + System.getProperty("java.home"));
		_log.info("JVM version: " + System.getProperty("java.vm.version"));
		_log.info("JVM Vendor: " + System.getProperty("java.vm.vendor"));
		_log.info("JVM Info: " + System.getProperty("java.vm.info"));
		_log.info("..................................................");
		_log.info("..................................................");
	}
	
}
