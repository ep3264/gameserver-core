package custom.auction;

import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance.ItemLocation;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;

/**
 * @className:custom.auction.AuctionInventory.java 
 * @author evgeny64
 * Official Website: http://l2je.com 
 * @date 27 янв. 2017 г. 0:32:31 
 */
public class AuctionInventory extends ItemContainer
{
	private static AuctionInventory _insatnce;
	
	public static AuctionInventory getInstance()
	{
		if (_insatnce == null)
		{
			_insatnce = new AuctionInventory();
		}
		
		return _insatnce;
	}

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.itemcontainer.ItemContainer#getOwner()
	 */
	@Override
	protected L2Character getOwner()
	{
		return null;
	}
	@Override
	public int getOwnerId()
	{
		return 1;
	}
	

	/* (non-Javadoc)
	 * @see net.sf.l2j.gameserver.model.itemcontainer.ItemContainer#getBaseLocation()
	 */
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
}
