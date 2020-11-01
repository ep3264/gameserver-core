package com.l2je.extensions.ghosts.generators;

import java.util.ArrayList;
import java.util.Random;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.PcTemplate;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.Sex;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;

/**
 * @author sector
 * @date Nov 26, 2017 2:17:31 AM 
 */
public class GhostGenarator
{
	private static final ArrayList<Integer> items = new ArrayList<>();
	static {
		items.add(7575);
		items.add(6382);
		items.add(6380);
		items.add(6381);
		items.add(6379);
		
	}
	private static final ArrayList<String> names = new ArrayList<>();
	static {
		names.add("destr");
		names.add("Lagau");
		names.add("nagibator");
		names.add("DONNN");
		
	}
	
	private static int counter = 0;
	
	public int x =147700;
	public int y = -55510;
	public int z = -2736;
	
	public L2PcInstance genarate() {
		final Random random = new Random();
		final PcTemplate template = CharTemplateTable.getInstance().getTemplate(ClassId.GHOST_SENTINEL.getId());
		final int id = IdFactory.getInstance().getNextId();
		final byte sex = (byte) random.nextInt(2);
		final L2PcInstance newChar = L2PcInstance.create(IdFactory.getInstance().getNextId(), template, "bot"+id, names.get(counter++), sex, sex, sex, Sex.values()[sex]);
		newChar.setIsGhost(true);
		newChar.setCurrentCp(0);
		newChar.setCurrentHp(newChar.getMaxHp());
		newChar.setCurrentMp(newChar.getMaxMp());
		long EXp = Experience.getExperiance(79);
		newChar.addExpAndSp(EXp , 0);
		newChar.addAdena("Init", Config.STARTING_ADENA, null, false);
		newChar.spawnMe( x, y, z);
		
		for (Integer ia : items)
		{
			ItemInstance item = newChar.getInventory().addItem("Init", ia, 1, newChar, null);

			if (item.isEquipable())
			{
				if (newChar.getActiveWeaponItem() == null && item.getItem().getType2() == Item.TYPE2_WEAPON){
					newChar.getInventory().equipItemAndRecord(item);
				}
				else if(item.getItem().getType2() == Item.TYPE2_SHIELD_ARMOR)
				{
					newChar.getInventory().equipItemAndRecord(item);
				}
			}
		}
		newChar.getInventory().addItem("Init", 1345, 1111, newChar, null);
		for (L2SkillLearn skill : SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId()))
		{
			newChar.addSkill(SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel()), true);
		}
		return newChar;
		
	}
}
