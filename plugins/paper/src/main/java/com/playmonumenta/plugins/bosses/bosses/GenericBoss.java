package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.BossBarManager;

public class GenericBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_generic";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new GenericBoss(plugin, boss);
	}

	public GenericBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setRemoveWhenFarAway(false);

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.WHITE, BarStyle.SOLID, null);

		super.constructBoss(null, null, detectionRange, bossBar);
	}
}
