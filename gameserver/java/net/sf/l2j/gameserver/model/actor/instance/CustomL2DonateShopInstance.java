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

import com.l2je.extensions.PremiumManager;

import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author user
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
	}
	
	@Override
	public String getHtmlFolder()
	{
		return "/mods/donate_shop/";
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(getHtmlPath(getNpcId(), val, player));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String currentCommand = st.nextToken();
		if (currentCommand.startsWith("Chat"))
		{
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			showChatWindow(player, val);
		}
		else if (currentCommand.startsWith("Premium"))
		{
			if (player.isPremium())
			{
				StringBuilder sb = new StringBuilder();
				StringUtil.append(sb, "Премиум уже активирован до: ", PremiumManager.getPremiumEndDate(player));
				player.sendMessage(sb.toString());
				return;
			}
			PremiumManager.showChatWindow(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
}
