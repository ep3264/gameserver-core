/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2je.extensions.casino;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;



import java.util.logging.Logger;

import net.sf.l2j.commons.random.Rnd;
/**
 * @author user
 *
 */
public class Roulette
{
	protected static final Logger _log = Logger.getLogger(Roulette.class.getName());
	final static int PRICE_ADENA = 100000000;
	final static int BLODDY=4358;
	final static int COL=9213;
	public static Roulette getInstance()
	{
		return SingletonHolder._instance;
	}
	private static class SingletonHolder
	{
		protected static final Roulette _instance = new Roulette();
	}
    public void twist(L2PcInstance player, int mode)
    {
    	if (player == null)
			return;
    	if (!player.getInventory().validateCapacity(1))
		{
    		player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}
    	if(mode == 1)
    	{
    		
    		if (!player.reduceAdena("Roulette", PRICE_ADENA, player.getCurrentFolkNPC(), true))
    		{    			
    			return;
    		}
    		int val = Rnd.get(1000);
    		//EW 
    		if(val<=250){
    			player.addItem("RouletteReward[mode:" + mode + "]", 6577, 1, player, true);
    		}
    		else if(val<=500)
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 6578, 1, player, true);
    		}
    		else if(val<=600)
    		{ //cp potion
    			player.addItem("RouletteReward[mode:" + mode + "]", 5592, 10, player, true);
    		}
    		else if(val <=700) //adena
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 57, 100000000, player, true);
    		}
    		else if(val<=730)
    		{//bloody
    			player.addItem("RouletteReward[mode:" + mode + "]", BLODDY, 1, player, true);
    		}
    		else if(val==731){//col
    			player.addItem("RouletteReward[mode:" + mode + "]", COL, 1, player, true);
    		}
    		else if(val <=1000)//loose
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 57, 1, player, true);
    		}   		
    	}
    	else if(mode==2)
    	{
    		ItemInstance item = player.getInventory().getItemByItemId(BLODDY);
			if (item == null || item.getCount() < 1)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
				return;
			}
			if(!player.destroyItem("Roulette", item,1, player, true))
			{
				return;
			}
			int val = Rnd.get(1000);
    		//EW 
    		if(val<=150){
    			player.addItem("RouletteReward[mode:" + mode + "]", 6577, 1, player, true);
    		}
    		else if(val<=300)
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 6578, 1, player, true);
    		}
    		else if(val<=400)
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8732, 1, player, true);
    		}
    		else if(val<=450)
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8742, 1, player, true);
    		}
    		else if(val<=475)
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8752, 1, player, true);
    		}
    		else if(val<=485)
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8762, 1, player, true);
    		}
    		else if(val<=600)//cp potion
    		{ 
    			player.addItem("RouletteReward[mode:" + mode + "]", 5592, 15, player, true);
    		}    		
    		else if(val<=650)//bloody
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", BLODDY, 1, player, true);
    		}
    		else if(val<=654){//col
    			player.addItem("RouletteReward[mode:" + mode + "]", COL, 1, player, true);
    		}
    		else if(val<=657){//Jester Hat
    			player.addItem("RouletteReward[mode:" + mode + "]", 8562, 1, player, true);
    		}
    		else if(val <=1000)
    		{//loose
    			player.addItem("RouletteReward[mode:" + mode + "]", 57, 1, player, true);
    		}   		
    	}
    	else if(mode == 3)
    	{
    		ItemInstance item = player.getInventory().getItemByItemId(COL);
			if (item == null || item.getCount() < 1)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_ITEM_COUNT));
				return;
			}
			if(!player.destroyItem("Roulette", item,1, player, true))
			{
				return;
			}			
			int val = Rnd.get(1000);
			if(val==777)
			{
				_log.info(player.getClient().toString()+"earned AQ");
				Broadcast.toAllOnlinePlayers(new CreatureSay(0, Say2.CRITICAL_ANNOUNCE, "", player.getName()+ " Выиграл AQ"));
				player.addItem("RouletteReward[mode:" + mode + "]", 6660, 1, player, true);
			
			}
    		//EW 
			else if(val<=150){
    			player.addItem("RouletteReward[mode:" + mode + "]", 6577, 1, player, true);
    		}
    		else if(val<=300)
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 6578, 1, player, true);
    		}
    		else if(val<=400)//LS
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8732, 1, player, true);
    		}
    		else if(val<=500)//mid ls
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8742, 1, player, true);
    		}
    		else if(val<=575)//high ls
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8752, 1, player, true);
    		}
    		else if(val<=625)//top ls
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8762, 1, player, true);
    		}
    		else if(val<=775) //cp potion
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 5592, 50, player, true);
    		}    		
    		else if(val<=850)//bloody
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", BLODDY, 4, player, true);
    		}
    		else if(val<=855){//col
    			player.addItem("RouletteReward[mode:" + mode + "]", COL, 1, player, true);
    		}
    		else if(val<=862){//Jester Hat
    			player.addItem("RouletteReward[mode:" + mode + "]", 8562, 1, player, true);
    		}
    		else if(val <=868) //mask
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 8552, 1, player, true);
    		}
    		else if(val <=950) //adena
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 57, 100000000, player, true);
    		}
    		else if(val <=1000)//loose
    		{
    			player.addItem("RouletteReward[mode:" + mode + "]", 57, 1, player, true);
    		}  
    	}
    	
    	return;
    }   
}
