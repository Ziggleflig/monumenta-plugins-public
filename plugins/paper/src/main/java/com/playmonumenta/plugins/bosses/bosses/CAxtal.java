package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellAxtalMeleeMinions;
import com.playmonumenta.plugins.bosses.spells.SpellAxtalTntThrow;
import com.playmonumenta.plugins.bosses.spells.SpellAxtalWitherAoe;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindRandomPlayer;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class CAxtal extends BossAbilityGroup {
	public static final String identityTag = "boss_caxtal";
	public static final int detectionRange = 110;

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new CAxtal(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public CAxtal(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);

		mBoss.addScoreboardTag("Boss");

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellAxtalWitherAoe(plugin, mBoss, 13, 4),
			new SpellAxtalMeleeMinions(plugin, mBoss, 10, 3, 3, 20, 12),
			new SpellTpBehindRandomPlayer(plugin, mBoss, 160),
			new SpellAxtalTntThrow(plugin, mBoss, 5, 15),
			new SpellBaseLaser(plugin, boss, 60, 140, false, true, 160,
			                   // Tick action per player
			                   (Player player, int ticks, boolean blocked) -> {
			                       player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (ticks / 100f) * 1.5f);
			                       boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (ticks / 100f) * 1.5f);
			                       player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2, 0.5f + (ticks / 100f) * 1.5f);
			                       boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (ticks / 100f) * 1.5f);
			                   },
			                   // Particles generated by the laser
			                   (Location loc) -> {
			                       loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0.02, 0.02, 0.02, 0);
			                       loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 1, 0.02, 0.02, 0.02, 0);
			                       loc.getWorld().spawnParticle(Particle.SPELL_MOB, loc, 1, 0.02, 0.02, 0.02, 1);
			                   },
			                   // TNT generated at the end of the attack
			                   (Player player, Location loc, boolean blocked) -> {
			                       loc.getWorld().createExplosion(loc, 7);
			                   })
		));
		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			// Teleport the boss to spawnLoc if he gets too far away from where he spawned
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > 80),
			// Teleport the boss to spawnLoc if he is stuck in bedrock
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
			                                                   b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
			                                                   b.getLocation().getBlock().getType() == Material.LAVA)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"At last, the keys are collected. I can be free finally...\",\"color\":\"dark_red\"}]");
		});
		events.put(50, (mBoss) -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"PLEASE. KILL ME. KAUL HOLDS ONTO MY MIND, BUT I YEARN FOR FREEDOM.\",\"color\":\"dark_red\"}]");
		});
		events.put(25, (mBoss) -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"YOU ARE CLOSE. END THIS. END THE REVERIE!\",\"color\":\"dark_red\"}]");
		});
		events.put(10, (mBoss) -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"My servant is nearly dead. You dare to impose your will on the jungle?\",\"color\":\"dark_green\"}]");
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int player_count = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hp_del = 1024;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0) {
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);

		//launch event related spawn commands
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"C'Axtal\",\"color\":\"dark_red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"The Soulspeaker\",\"color\":\"red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

	@Override
	public void death() {
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"It ends at last... Is this what freedom feels like?..\",\"color\":\"dark_red\"}]");
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
