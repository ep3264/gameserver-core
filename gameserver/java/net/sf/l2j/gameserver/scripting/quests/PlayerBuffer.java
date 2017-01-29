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
package net.sf.l2j.gameserver.scripting.quests;




import com.l2je.custom.Buffer;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

/**
 * Developers: Redist Team<br>
 * Official Website: <br>
 * <br>
 * Author: Redist<br>
 * Date: 2 нояб. 2015 г.<br>
 * Time: 16:16:36<br>
 * <br>
 */
public class PlayerBuffer extends Quest
{
	private static final String qn = "PlayerBuffer";
	private static final String DESCR = "PlayerBuffer";
	private static final String InitialHtml = "1.htm";
	private static final int ID = 0;
	private static final int NPC= 40001;
	private String curHtml;
	
	public PlayerBuffer()
	{
		super(ID, DESCR);
		curHtml=InitialHtml;
		addStartNpc(NPC);
		addTalkId(NPC);
	}
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{		
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		curHtml=InitialHtml;
		if (st == null)
		{
			return htmltext;
		}
		st.setState(Quest.STATE_STARTED);
		return curHtml;
	}
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		if(event.matches("\\d{1}.htm")){
			curHtml=event;
			return curHtml;
		}		
		Buffer.getInstance().getBuff(player, event,false);
		st.setState(Quest.STATE_COMPLETED);		
		return curHtml;
	}
	
}
