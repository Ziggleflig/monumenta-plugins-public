package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.DeathsTouch;
import com.playmonumenta.plugins.abilities.warlock.reaper.GhoulishTaunt;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.FractalEnervation;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class CursedWound extends Ability {

	private static final int CURSED_WOUND_EFFECT_LEVEL = 1;
	private static final int CURSED_WOUND_DURATION = 6 * 20;
	private static final int CURSED_WOUND_RADIUS = 3;
	private static final double CURSED_WOUND_DAMAGE = 0.5;
	private static final int CURSED_WOUND_1_CAP = 2;
	private static final int CURSED_WOUND_2_CAP = 4;
	private static final int CURSED_WOUND_EXTENDED_DURATION = 2 * 20;

	public CursedWound(Plugin plugin, Player player) {
		super(plugin, player, "Cursed Wound");
		mInfo.mScoreboardId = "CursedWound";
		mInfo.mShorthandName = "CW";
		mInfo.mDescriptions.add("Critical strikes with a scythe apply 6s of Wither II to all mobs within a 3 block radius of the hit mob. Additionally, for each skill on cooldown, melee attacks are increased by 0.5 damage, capped at +2.");
		mInfo.mDescriptions.add("The damage cap is increased to +4, and critical strikes increase the duration of all debuffs applied to mobs in the radius by 2s.");

	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			float cursedWoundCap = getAbilityScore() == 1 ? CURSED_WOUND_1_CAP : CURSED_WOUND_2_CAP;
			LivingEntity damagee = (LivingEntity) event.getEntity();
			BlockData fallingDustData = Material.ANVIL.createBlockData();
			World world = mPlayer.getWorld();
			if (EntityUtils.isHostileMob(damagee)) {
				world.spawnParticle(Particle.FALLING_DUST, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 3,
				                     (damagee.getWidth() / 2) + 0.1, damagee.getHeight() / 3, (damagee.getWidth() / 2) + 0.1, fallingDustData);
				world.spawnParticle(Particle.SPELL_MOB, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 6,
				                     (damagee.getWidth() / 2) + 0.1, damagee.getHeight() / 3, (damagee.getWidth() / 2) + 0.1, 0);
				Ability[] abilities = new Ability[10];
				abilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, AmplifyingHex.class);
				abilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, ConsumingFlames.class);
				abilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, GraspingClaws.class);
				abilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, SoulRend.class);
				abilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, Exorcism.class);
				abilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, DarkPact.class);
				abilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, GhoulishTaunt.class);
				abilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, DeathsTouch.class);
				abilities[8] = AbilityManager.getManager().getPlayerAbility(mPlayer, FractalEnervation.class);
				abilities[9] = AbilityManager.getManager().getPlayerAbility(mPlayer, WitheringGaze.class);
				int cooldowns = 0;
				for (Ability ability : abilities) {
					if (ability != null && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ability.getInfo().mLinkedSpell)) {
						cooldowns++;
					}
				}
				event.setDamage(event.getDamage() + Math.min(cooldowns * CURSED_WOUND_DAMAGE, cursedWoundCap));
				CustomDamageEvent customDamageEvent = new CustomDamageEvent(mPlayer, damagee, 0, null);
				Bukkit.getPluginManager().callEvent(customDamageEvent);
			}

			if (PlayerUtils.isCritical(mPlayer)) {
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, 4.0f, 0.75f);
				for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), CURSED_WOUND_RADIUS, mPlayer)) {
					world.spawnParticle(Particle.FALLING_DUST, mob.getLocation().add(0, mob.getHeight() / 2, 0), 3,
					                     (mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, fallingDustData);
					world.spawnParticle(Particle.SPELL_MOB, mob.getLocation().add(0, mob.getHeight() / 2, 0), 6,
					                     (mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, 0);
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WITHER, CURSED_WOUND_DURATION, CURSED_WOUND_EFFECT_LEVEL, true, false));
					if (getAbilityScore() > 1) {
						//Bleed interaction
						if (EntityUtils.isBleeding(mPlugin, mob)) {
							EntityUtils.setBleedTicks(mPlugin, mob, EntityUtils.getBleedTicks(mPlugin, mob) + CURSED_WOUND_EXTENDED_DURATION);
						}
						//Custom slow effect interaction
						if (EntityUtils.isSlowed(mPlugin, mob)) {
							EntityUtils.setSlowTicks(mPlugin, mob, EntityUtils.getSlowTicks(mPlugin, mob) + CURSED_WOUND_EXTENDED_DURATION);
						}
						for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, mob)) {
							PotionEffect effect = mob.getPotionEffect(effectType);
							if (effect != null) {
								mob.removePotionEffect(effectType);
								// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
								mob.addPotionEffect(new PotionEffect(effectType, effect.getDuration() + CURSED_WOUND_EXTENDED_DURATION, effect.getAmplifier()));
							}
						}
					}
				}
			}
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand());
	}

}
