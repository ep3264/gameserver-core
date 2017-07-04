package net.sf.l2j.gameserver.model.actor.instance;

import com.l2je.extensions.casino.Roulette;
import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.Language;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 
 * @author dbg
 * Official Website: http://l2je.com 
 * @date 17 февр. 2017 г. 14:17:10
 */
public class CustomL2ShopInstance extends L2NpcInstance
{
	public CustomL2ShopInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}	
	@Override
	public String getHtmlFolder()
	{
		return "/mods/shop/";		
	}
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
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
				val = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
			}
			showChatWindow(player, val);
		}
		else if (currentCommand.startsWith("Roulette"))
		{
			int val = 1;
			try
			{
				val = Integer.parseInt(st.nextToken());
			}
			catch (IndexOutOfBoundsException ioobe)
			{
			}
			Roulette.getInstance().twist(player, val);
			showChatWindow(player, 1);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
}
