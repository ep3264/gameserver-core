package com.l2je.extensions.events;

import com.l2je.commons.nproperty.Cfg;
import com.l2je.commons.nproperty.ConfigParser;
import com.l2je.extensions.events.commons.Fence;
import com.l2je.extensions.events.commons.Reward;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;


/**
 * @className:com.l2je.custom.events.EventConfig.java 
 * @author dbg
 * Official Website: http://l2je.com 
 * @date 3 февр. 2017 г. 6:51:42 
 */

@Cfg
public class EventConfig 
{
	protected static final Logger _log = Logger.getLogger(EventConfig.class.getName());
	public static final String FILE = "./config/extensions/events.ini";
	public static boolean AUTO_EVENT_SCHEDULER = false;
	public static boolean EVENT_MANAGER_ENABLE = true;
	public static boolean EVENT_MANAGER_DEBUG = false;
	public static int EVENT_MANAGER_INIT_DELAY =180;
	public static int EVENT_MANAGER_EVENTS_DELAY = 180;
	public static int [] EVENT_MANAGER_AUTO_EVENTS = {};
	// TVT
	public static int TVT_ID =1;	
	public static int TVT_REG_TIME = 60;
	public static int  TVT_MIN_PLAYERS = 2;
	public static int TVT_MAX_PLAYERS = 30;
	public static int TVT_TELEPORT_TIME = 10;
	public static int TVT_RUNNING_TIME = 300;
	public static int TVT_RES_TIME = 3;
	public static boolean TVT_SCORE_TITLE = true;
	public static String TVT_SCORE_TITLE_PATTERN = "-=[ %score% ]=-";
	public static boolean TVT_PVP_ON_KILL = true;

	// Team 1
	public static String TVT_TEAM1_NAME = "blue";
	public static String TVT_TEAM1_TITLE_COLOR= "FF6868";
	public static String TVT_TEAM1_NAME_COLOR= "FF6868";
	public static String [] TVT_TEAM1_SPAWN_LOCS= {"16148,109032,-9074","19246,109032,-9074"};
	public static int TVT_TEAM1_COLOR = 1;
	//Team 2
	public static String TVT_TEAM2_NAME = "red";
	public static String TVT_TEAM2_TITLE_COLOR= "6868FF";
	public static String TVT_TEAM2_NAME_COLOR= "0000CC";
	public static String [] TVT_TEAM2_SPAWN_LOCS= {"16408,113332,-9074","19017,113332,-9074"};
	public static int TVT_TEAM2_COLOR = 2;
	//
	public static boolean TVT_REWARD_TIE = true;
	public static String [] TVT_REWARD = {"5592,10","6577,1","6578,1"};
	public static int [] TVT_BLOCKED_SKILLS = {1050,1255,3205,1403,1429};
	public static int [] TVT_BLOCKED_ITEMS = {736,1538,1829,1830,3958,5858,5859,6663,6664,7117,7118,7119,7120,7121,
		7122,7123,7124,7125,7126,7127,7128,7129,7130,7131,7132,7133,7134,7135,7554,7555,7556,7557,7558,7559,7618,
		7619,9156,2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650};
	public static String [] TVT_FENCES = {"19459,109025,-9074,2,90,300,3",
		"15986,109025,-9074,2,90,300,3","17723,109432,-9212,2,300,90,3"};
	public static ArrayList<Fence> tvtFences = new ArrayList<>();
	public static ArrayList<Reward> tvtReward =new ArrayList<>();
	// Enchant War
	public static int EW_ID = 2;	
	public static int  EW_MIN_PLAYERS = 1;
	public static int EW_MAX_PLAYERS = 30;
	public static int EW_TELEPORT_TIME = 10;
	public static int EW_RUNNING_TIME = 300;
	public static int EW_RES_TIME = 10;
	public static boolean EW_SCORE_TITLE = true;
	public static String EW_SCORE_TITLE_PATTERN = "-=[ %score% ]=-";
	public static boolean EW_PVP_ON_KILL = true;
	public static String [] EW_REWARD = {"57,2000000"};	
	public static String EW_TITLE_COLOR = "0000FF";
	public static String EW_NAME_COLOR = "0000CC";
	public static int [] EW_BLOCKED_SKILLS = {1050,1255,3205,1403,1429};
	public static int [] EW_BLOCKED_ITEMS = {736,1538,1829,1830,3958,5858,5859,6663,6664,7117,7118,7119,7120,7121,
		7122,7123,7124,7125,7126,7127,7128,7129,7130,7131,7132,7133,7134,7135,7554,7555,7556,7557,7558,7559,7618,
		7619,9156,2375,3500,3501,3502,4422,4423,4424,4425,6648,6649,6650};
	public static int [] TOWN_IDS = {13};
	public static ArrayList<Reward> ewReward =new ArrayList<>();
   public static void init()
	{
		try
		{
			ConfigParser.parse(EventConfig.class, FILE);
			if(!getTvTFences())
			{
				_log.info("Error load tvt fences config");
			}
			if(!getRewards(EW_REWARD,ewReward))
			{
				_log.info("Error load ew reward config");
			}
			if(!getRewards(TVT_REWARD,tvtReward))
			{
				_log.info("Error load tvt reward config");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	private static boolean getRewards(String[] rewards, ArrayList <Reward> arrayList )
	{		
		for (String reward : rewards)
		{
			int[] rewardItem = stringToIntArr(reward);
			if (rewardItem.length == 2)
			{
				arrayList.add(new Reward(rewardItem[0],rewardItem[1]));
			}		
		}
		return !arrayList.isEmpty();
		
	}
	private static boolean getTvTFences()
	{
		String[] fences = EventConfig.TVT_FENCES;
		if (fences == null)
		{
			return false;
		}
		String[] raw;
		int x, y, z, type, width, length, height;
		for (String rawFence : fences)
		{
			raw = rawFence.split(",");
			if (raw.length != 7)
			{
				_log.info("Error load tvt fences config");
				continue;
			}
			x = Integer.valueOf(raw[0]);
			y = Integer.valueOf(raw[1]);
			z = Integer.valueOf(raw[2]);
			type = Integer.valueOf(raw[3]);
			width = Integer.valueOf(raw[4]);
			length = Integer.valueOf(raw[5]);
			height = Integer.valueOf(raw[6]);
			tvtFences.add(new Fence(x, y, z, type, width, length, height));	
		}
		return true;
		
	}
	public final static int[] stringToIntArr(String str)
	{
		StringTokenizer st = new StringTokenizer(str, ","); 
		int [] arr =  new int [st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens())
		{
			try
			{
				arr[i] = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			++i;
		}
		return arr;
	}
	
}
