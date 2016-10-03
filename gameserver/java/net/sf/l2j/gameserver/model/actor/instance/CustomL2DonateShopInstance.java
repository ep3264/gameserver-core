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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author user
 *
 */
public class CustomL2DonateShopInstance extends L2NpcInstance
{

	/**
	 * @param objectId
	 * @param template
	 */
	public CustomL2DonateShopInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		// TODO Auto-generated constructor stub
	}
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;		
		if (val == 0)
			filename = "data/html/mods/donateshop/" + npcId + ".htm";
		else
			filename = "data/html/mods/donateshop/" + npcId + "-" + val + ".htm";
			
		return filename;		
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(getHtmlPath(getNpcId(), val));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
}
