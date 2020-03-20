package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

/*
 * Shift-RClick with an Alchemist Potion to deploy a 5 block radius zone
 * that lasts 15 seconds where the potion lands. Mobs within this zone
 * take 3 extra damage whenever taking damage and are afflicted with
 * Weakness 1. Cooldown: 30 / 25 seconds, Charges: 1 / 2.
 */

public class ScorchedEarth extends Ability {

	private static final String SCORCHED_EARTH_POTION_METAKEY = "ScorchedEarthPotion";

	private static final int SCORCHED_EARTH_1_COOLDOWN = 20 * 30;
	private static final int SCORCHED_EARTH_2_COOLDOWN = 20 * 25;
	private static final int SCORCHED_EARTH_1_CHARGES = 1;
	private static final int SCORCHED_EARTH_2_CHARGES = 2;
	private static final int SCORCHED_EARTH_DURATION = 20 * 15;
	private static final int SCORCHED_EARTH_WEAKNESS_AMP = 0;
	private static final int SCORCHED_EARTH_BONUS_DAMAGE = 3;
	private static final double SCORCHED_EARTH_RADIUS = 5;
	private static final Particle.DustOptions SCORCHED_EARTH_COLOR_1 = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);
	private static final Particle.DustOptions SCORCHED_EARTH_COLOR_2 = new Particle.DustOptions(Color.fromRGB(64, 0, 0), 1.0f);

	/*
	 * I now hate myself for coming up with this skill, since jank is
	 * required unless we want to pollute the EntityListener with a
	 * bunch of zone checks and increase the event damage directly, but
	 * the basic idea here is to track mob health, and if mob health
	 * decreases and the mob in question is near a zone, then the mob
	 * must have taken damage and should be hit with an extra instance
	 * of damage.
	 *
	 * Problem comes when you have overlapping zones, zones from
	 * different players, etc. which is why we need a global tracker
	 * to resolve these issues.
	 */
	private static Map<Location, Map.Entry<Player, Integer>> mZoneCenters = new HashMap<Location, Map.Entry<Player, Integer>>();
	private static Map<LivingEntity, Double> mMobHealths = new HashMap<LivingEntity, Double>();
	private static BukkitRunnable mMobHealthsTracker;

	private final int mCooldown;
	private final int mMaxCharges;

	private int mTimeToNextCharge;
	private int mCharges;

	public ScorchedEarth(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Scorched Earth");
		mInfo.linkedSpell = Spells.SCORCHED_EARTH;
		mInfo.scoreboardId = "ScorchedEarth";
		mInfo.mShorthandName = "SE";
		mInfo.mDescriptions.add("Shift right click with an Alchemist Potion to deploy a 5 block radius zone that lasts 15 seconds where the potion lands. Mobs in this zone are afflicted with Weakness I and are dealt 3 extra damage whenever taking damage. Cooldown: 30s.");
		mInfo.mDescriptions.add("Cooldown reduced to 25s, and two charges of this ability can be stored at once.");
		mInfo.cooldown = 0;		// Manage cooldowns manually due to multiple charges
		mInfo.ignoreCooldown = true;
		mCooldown = getAbilityScore() == 1 ? SCORCHED_EARTH_1_COOLDOWN : SCORCHED_EARTH_2_COOLDOWN;
		mMaxCharges = getAbilityScore() == 1 ? SCORCHED_EARTH_1_CHARGES : SCORCHED_EARTH_2_CHARGES;
		mTimeToNextCharge = mCooldown;
		mCharges = mMaxCharges;

		// Only one runnable ever exists for Scorched Earth - it is a global list, not tied to any individual players
		if (mMobHealthsTracker == null) {
			mMobHealthsTracker = new BukkitRunnable() {
				@Override
				public void run() {
					// Tick and remove expired zones
					Iterator<Map.Entry<Location, Map.Entry<Player, Integer>>> iter = mZoneCenters.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry<Location, Map.Entry<Player, Integer>> entry = iter.next();
						Map.Entry<Player, Integer> timer = entry.getValue();
						if (timer.getValue() <= 0) {
							iter.remove();
						} else {
							timer.setValue(timer.getValue() - 1);

							Location loc = entry.getKey();
							mWorld.spawnParticle(Particle.REDSTONE, loc, 2, 4, 1, 4, 0, SCORCHED_EARTH_COLOR_1);
							mWorld.spawnParticle(Particle.REDSTONE, loc, 2, 4, 1, 4, 0, SCORCHED_EARTH_COLOR_2);
							mWorld.spawnParticle(Particle.LAVA, loc, 1, 4, 1, 4, 0.2);
						}
					}

					// Get the new mob healths and record which ones need to be damaged (health decreased)
					Map<LivingEntity, Double> newMobHealths = new HashMap<LivingEntity, Double>();
					Map<LivingEntity, Player> mobsToBeDamaged = new HashMap<LivingEntity, Player>();
					for (Location loc : mZoneCenters.keySet()) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, SCORCHED_EARTH_RADIUS)) {
							Double oldHealth = mMobHealths.get(mob);
							if (oldHealth != null && mob.getHealth() < oldHealth) {
								// We need a way to determine "zone ownership" when dealing damage
								mobsToBeDamaged.put(mob, mZoneCenters.get(loc).getKey());
							} else {
								// We'll put the health of the mobs to be damaged in the new map after we damage them
								newMobHealths.put(mob, mob.getHealth());
							}

							PotionUtils.applyPotion(mPlayer, mob,
									new PotionEffect(PotionEffectType.WEAKNESS, 30, SCORCHED_EARTH_WEAKNESS_AMP, false, true));
						}
					}

					// Damage the mobs
					for (Map.Entry<LivingEntity, Player> entry : mobsToBeDamaged.entrySet()) {
						LivingEntity mob = entry.getKey();
						mWorld.spawnParticle(Particle.SPELL_MOB, mob.getLocation(), 30, 0.2, 0.2, 0.2, 0);
						mob.setNoDamageTicks(0);
						Vector velocity = mob.getVelocity();
						EntityUtils.damageEntity(mPlugin, mob, SCORCHED_EARTH_BONUS_DAMAGE, entry.getValue());
						mob.setVelocity(velocity);
						newMobHealths.put(mob, mob.getHealth());
					}

					// Replace the old health map
					mMobHealths = newMobHealths;
				}
			};

			mMobHealthsTracker.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond && mCharges < mMaxCharges) {
			mTimeToNextCharge -= 20;

			if (mTimeToNextCharge <= 0) {
				mTimeToNextCharge = mCooldown;
				mCharges++;
				MessagingUtils.sendActionBarMessage(mPlayer, "Scorched Earth Charges: " + mCharges);
			}
		}
	}

	@Override
	public boolean playerThrewSplashPotionEvent(SplashPotion potion) {
		if (mCharges > 0 && mPlayer.isSneaking()
				&& InventoryUtils.testForItemWithName(mPlayer.getInventory().getItemInMainHand(), "Alchemist's Potion")) {
			potion.setMetadata(SCORCHED_EARTH_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));
			mCharges--;
			MessagingUtils.sendActionBarMessage(mPlayer, "Scorched Earth Charges: " + mCharges);
		}

		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata(SCORCHED_EARTH_POTION_METAKEY)) {
			Location loc = potion.getLocation();
			mWorld.playSound(loc, Sound.ENTITY_TNT_PRIMED, 3, 0.2f);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 300, 4, 1, 4, 0);
			mZoneCenters.put(loc, new AbstractMap.SimpleEntry<Player, Integer>(mPlayer, SCORCHED_EARTH_DURATION));
		}

		return true;
	}

}
