package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.CrowdControlImmunity;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class CrowdControlImmunityBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_ccimmune";
	public static final int detectionRange = 45;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CrowdControlImmunityBoss(plugin, boss);
	}

	public CrowdControlImmunityBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		List<Spell> passiveSpells = Arrays.asList(
			new CrowdControlImmunity(mBoss)
		);

		mBoss.setRemoveWhenFarAway(false);
		super.constructBoss(plugin, identityTag, mBoss, null, passiveSpells, detectionRange, null);
	}
}
