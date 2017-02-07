package com.l2je.extensions.auction;


import net.sf.l2j.Config;
import net.sf.l2j.commons.config.ExProperties;

/**
 * @className:AuctionConfig.AuctionConfig.java 
 * @author evgeny64
 * Official Website: http://l2je.com 
 * @date 26 янв. 2017 г. 20:30:42 
 */
public class AuctionConfig {
	   public static boolean AUCTION_ENABLE;
	   public static boolean ALLOW_AUGMENT_ITEMS;
	   public static boolean AUCTION_LOG;
	   public static int AUCTION_NPC_ID;
	   public static boolean AUCTION_PERCENTAGE;
	   public static int AUCTION_GET_PERCENT;
	   public static int[] AUCTION_PRICE;
	   public static int[] AUCTION_AUGMENT_PRICE;
	   public static int[] AUCTION_ALLOWED_ITEM_ID;
	   public static int AUCTION_COUNT_DAY_FOR_DELETE_ITEM;
	   public static int AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE;	   
	   private final static String FILE = "./config/extensions/auction.ini"; 

	public static void init()
	{	
		try{
		    final ExProperties auctionPropeties = Config.initProperties(FILE);		
			AUCTION_ENABLE = Boolean.parseBoolean(auctionPropeties.getProperty("AuctionEnable", "true"));
			if (!AUCTION_ENABLE)
			{
				return;
			}
			ALLOW_AUGMENT_ITEMS = Boolean.parseBoolean(auctionPropeties.getProperty("AllowAugmentItems", "true"));
			AUCTION_LOG = Boolean.parseBoolean(auctionPropeties.getProperty("AuctionLogEnable", "false"));
			AUCTION_NPC_ID = Integer.parseInt(auctionPropeties.getProperty("AuctionNpcId", "50018"));
			AUCTION_PERCENTAGE = Boolean.parseBoolean(auctionPropeties.getProperty("AuctionPercentage", "false"));
			AUCTION_GET_PERCENT = Integer.parseInt(auctionPropeties.getProperty("AuctionGetPercent", "10"));
			String[] temp = auctionPropeties.getProperty("AuctionPrice", "57 1000").split(";");
			AUCTION_PRICE = new int[2];
			AUCTION_PRICE[0] = Integer.parseInt(temp[0]);
			AUCTION_PRICE[1] = Integer.parseInt(temp[1]);
			temp = auctionPropeties.getProperty("AuctionAugmentPrice", "4358 5").split(";");
			AUCTION_AUGMENT_PRICE = new int[2];
			AUCTION_AUGMENT_PRICE[0] = Integer.parseInt(temp[0]);
			AUCTION_AUGMENT_PRICE[1] = Integer.parseInt(temp[1]);
			temp = auctionPropeties.getProperty("AuctionAllowedItemId", "57 4358").split(";");
			AUCTION_ALLOWED_ITEM_ID = new int[temp.length];
			
			for (int i = 0; i <= temp.length - 1; ++i)
			{
				AUCTION_ALLOWED_ITEM_ID[i] = Integer.parseInt(temp[i]);
			}
			AUCTION_COUNT_DAY_FOR_DELETE_ITEM = Integer.parseInt(auctionPropeties.getProperty("AuctionCountDayForDeleteItem", "7"));
			AUCTION_SEE_COUNT_PRODUCTS_ON_PAGE = Integer.parseInt(auctionPropeties.getProperty("AuctionSeeCountProductsOnPage", "5"));
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
	}
			
}
