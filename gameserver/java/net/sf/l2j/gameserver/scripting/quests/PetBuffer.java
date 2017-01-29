package net.sf.l2j.gameserver.scripting.quests;


import com.l2je.custom.Buffer;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;


/**
 * Developers: Redist Team<br>
 * <br>
 * <br>
 * Author: Redist<br>
 * Date: 5 нояб. 2015 г.<br>
 * Time: 1:14:47<br>
 * <br>
 */
public class PetBuffer extends Quest
{
	
	private static final String qn = "PetBuffer";
	private static final String DESCR = "PetBuffer";
	private static final String InitialHtml = "1.htm";
	private static final int ID = 0;
	private static final int NPC = 40001;
	private String curHtml;
	
	public PetBuffer()
	{
		super(ID,  DESCR);
		curHtml=InitialHtml;
		addStartNpc(NPC);
		addTalkId(NPC);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		curHtml = InitialHtml;
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
		if (event.matches("\\d{1}.htm"))
		{
			curHtml = event;
			return curHtml;
		}
		Buffer.getInstance().getBuff(player, event,true);
		st.setState(Quest.STATE_COMPLETED);
		return curHtml;
	}	
}
