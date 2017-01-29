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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.BufferTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import custom.Buffer;


/**
 * @author user
 */
public class CustomL2BufferInstance extends L2NpcInstance
{
	public CustomL2BufferInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void showChatWindow(L2PcInstance player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(getHtmlPath(getNpcId(), val));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;
		if (val == 0)
			filename = "data/html/mods/custom_buffer/" + npcId + ".htm";
		else
			filename = "data/html/mods/custom_buffer/" + npcId + "-" + val + ".htm";
		
		return filename;
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
		else if (currentCommand.startsWith("Buff"))
		{
			String choice = st.nextToken();
			Buffer.getInstance().getBuff(player, choice, false);
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
		else if (currentCommand.startsWith("SaveList"))
		{
			showManageSchemeWindow(player);
		}
		else if (currentCommand.startsWith("LoadList"))
		{
			String target = st.nextToken();
			showGiveBuffsWindow(player, target);
		}
		else if (currentCommand.startsWith("givebuffs"))
		{
			final String targetType = st.nextToken();
			final String schemeName = st.nextToken();
			final int cost = Integer.parseInt(st.nextToken());
			if (player.isInCombat() || player.isInOlympiadMode() || player.isInDuel() || player.isInSiege())
			{
				player.sendMessage("You can't use buffer in PvP, Duel, Olympiad or Siege mode.");
				return;
			}
			final L2Character target = (targetType.equalsIgnoreCase("pet")) ? player.getPet() : player;
			if (target == null)
			{
				player.sendMessage("У вас нет питомца.");
			}
			else if (cost == 0 || player.reduceAdena("NPC CustomBuffer", cost, this, true))
			{
				for (int skillId : BufferTable.getInstance().getScheme(player.getObjectId(), schemeName))
				{
					if (skillId == 4553 || skillId == 4554)
					{
						SkillTable.getInstance().getInfo(skillId, 4).getEffects(this, target);
					}
					else
					{
						SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId)).getEffects(this, target);
					}
				}
			}
			showGiveBuffsWindow(player, targetType);
		}
		else if (currentCommand.startsWith("createscheme"))
		{
			try
			{
				final String schemeName = st.nextToken();
				if (schemeName.length() > 14)
				{
					player.sendMessage("Название схемы должно быть быть не более 14 символов.");
					showManageSchemeWindow(player);
					return;
				}
				final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
				if (schemes != null)
				{
					if (schemes.size() == Config.BUFFER_MAX_SCHEMES)
					{
						player.sendMessage("Вы создали максмальное количество схем.");
						showManageSchemeWindow(player);
						return;
					}
					if (schemes.containsKey(schemeName))
					{
						player.sendMessage("Схема с таким именем уже создана.");
						showManageSchemeWindow(player);
						return;
					}
				}
				
				BufferTable.getInstance().setScheme(player.getObjectId(), schemeName.trim(), new ArrayList<Integer>());
				addSkillsToScheme(player, schemeName.trim());
				showManageSchemeWindow(player);
			}
			catch (Exception e)
			{
				player.sendMessage("Название схемы должно быть быть не более 14 символов.");
				showManageSchemeWindow(player);
			}
		}
		else if (currentCommand.startsWith("deletescheme"))
		{
			try
			{
				final String schemeName = st.nextToken();
				final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
				
				if (schemes != null && schemes.containsKey(schemeName))
					schemes.remove(schemeName);
			}
			catch (Exception e)
			{
				player.sendMessage("Имя схемы невалидно.");
			}
			showManageSchemeWindow(player);
		}
		else if (currentCommand.startsWith("Flu"))
		{
			SkillTable.getInstance().getInfo(4553, 4).getEffects(this, player);
			showChatWindow(player, 9);
		}
		else if (currentCommand.startsWith("Malaria"))
		{
			SkillTable.getInstance().getInfo(4554, 4).getEffects(this, player);
			showChatWindow(player, 9);
		}
	}
	
	/**
	 * Add skills to Scheme
	 * @param player
	 * @param schemeName
	 */
	private static void addSkillsToScheme(L2PcInstance player, String schemeName)
	{
		final List<Integer> skills = BufferTable.getInstance().getScheme(player.getObjectId(), schemeName);
		L2Effect l2Effects[] = player.getAllEffects();
		for (L2Effect l2Effect : l2Effects)
		{	
			if (Buffer.ALLOWED_PLAYER_BUFFS.contains(Integer.valueOf(l2Effect.getSkill().getId())))
			{
				if (skills.size() < Config.BUFFER_MAX_SKILLS)
					skills.add(l2Effect.getSkill().getId());
				else
					player.sendMessage("В схеме максимальное количество бафов.");
			}
		}
	}
	
	/**
	 * Sends an html packet to player with Manage scheme menu info. This allows player to create/delete/clear schemes
	 * @param player : The player to make checks on.
	 */
	private void showManageSchemeWindow(L2PcInstance player)
	{
		final StringBuilder sb = new StringBuilder(200);
		
		final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
		if (schemes == null || schemes.isEmpty())
			sb.append("<font color=\"LEVEL\">Вы не создавали cхем бафов.</font>");
		else
		{
			sb.append("<table>");
			for (Map.Entry<String, ArrayList<Integer>> scheme : schemes.entrySet())
				StringUtil.append(sb, "<tr><td width=140>", scheme.getKey(), " (", scheme.getValue().size(), " skill(s))</td><td width=60><button value=\"Удалить\" action=\"bypass -h npc_%objectId%_deletescheme ", scheme.getKey(), "\" width=55 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");
			
			sb.append("</table>");
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 7));
		html.replace("%schemes%", sb.toString());
		html.replace("%max_schemes%", Config.BUFFER_MAX_SCHEMES);
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * Sends an html packet to player with Give Buffs menu info for player and pet, depending on targetType parameter {player, pet}
	 * @param player : The player to make checks on.
	 * @param targetType : a String used to define if the player or his pet must be used as target.
	 */
	private void showGiveBuffsWindow(L2PcInstance player, String targetType)
	{
		final StringBuilder sb = new StringBuilder(200);
		
		final Map<String, ArrayList<Integer>> schemes = BufferTable.getInstance().getPlayerSchemes(player.getObjectId());
		if (schemes == null || schemes.isEmpty())
			sb.append("<font color=\"LEVEL\">Вы не создавали схем бафов. Чтобы создать необходимо выбрать бафы и нажать \"Сохранить\".</font>");
		else
		{
			for (Map.Entry<String, ArrayList<Integer>> scheme : schemes.entrySet())
			{
				final int cost = 100000;// getFee(scheme.getValue());
				StringUtil.append(sb, "<font color=\"LEVEL\"><a action=\"bypass -h npc_%objectId%_givebuffs ", targetType, " ", scheme.getKey(), " ", cost, "\">", scheme.getKey(), " (", scheme.getValue().size(), " skill(s))</a>", ((cost > 0) ? " - Стоимость Adena: " + cost : ""), "</font><br1>");
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath(getNpcId(), 8));
		html.replace("%schemes%", sb.toString());
		html.replace("%targettype%", (targetType.equalsIgnoreCase("pet") ? "&nbsp;<a action=\"bypass -h npc_%objectId%_LoadList player\">Вы</a>&nbsp;|&nbsp;Ваш питомец" : "Вы&nbsp;|&nbsp;<a action=\"bypass -h npc_%objectId%_LoadList pet\">Ваш питомец</a>"));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	/**
	 * @param list : A list of skill ids.
	 * @return a global fee for all skills contained in list.
	 */
	private static int getFee(ArrayList<Integer> list)
	{
		if (Config.BUFFER_STATIC_BUFF_COST >= 0)
			return (list.size() * Config.BUFFER_STATIC_BUFF_COST);
		
		int fee = 0;
		for (int sk : list)
			fee += Config.BUFFER_BUFFLIST.get(sk).getValue();
		
		return fee;
	}
}
