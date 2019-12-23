package com.playmonumenta.plugins.abilities.rogue;

import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class DaggerThrow extends Ability {

	private static final String DAGGER_THROW_MOB_HIT_TICK = "HitByDaggerThrowTick";
	private static final int DAGGER_THROW_COOLDOWN = 12 * 20;
	private static final int DAGGER_THROW_RANGE = 8;
	private static final int DAGGER_THROW_1_DAMAGE = 6;
	private static final int DAGGER_THROW_2_DAMAGE = 12;
	private static final int DAGGER_THROW_DURATION = 10 * 20;
	private static final int DAGGER_THROW_1_VULN = 3;
	private static final int DAGGER_THROW_2_VULN = 7;
	private static final double DAGGER_THROW_SPREAD = Math.toRadians(25);
	private static final Particle.DustOptions DAGGER_THROW_COLOR = new Particle.DustOptions(Color.fromRGB(64, 64, 64), 1);

	private int mDamage;
	private int mVulnAmplifier;

	public DaggerThrow(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.DAGGER_THROW;
		mInfo.scoreboardId = "DaggerThrow";
		mInfo.cooldown = DAGGER_THROW_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mDamage = getAbilityScore() == 1 ? DAGGER_THROW_1_DAMAGE : DAGGER_THROW_2_DAMAGE;
		mVulnAmplifier = getAbilityScore() == 1 ? DAGGER_THROW_1_VULN : DAGGER_THROW_2_VULN;
	}

	@Override
	public void cast(Action action) {
		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, DAGGER_THROW_RANGE + 1, mPlayer);
		mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
		mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.25f);
		mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.0f);

		for (int a = -1; a <= 1; a++) {
			double angle = a * DAGGER_THROW_SPREAD;
			Vector newDir = new Vector(Math.cos(angle) * dir.getX() + Math.sin(angle) * dir.getZ(), dir.getY(), Math.cos(angle) * dir.getZ() - Math.sin(angle) * dir.getX());
			newDir.normalize();

			// Since we want some hitbox allowance, we use bounding boxes instead of a raycast
			BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);

			for (int i = 0; i <= DAGGER_THROW_RANGE; i++) {
				box.shift(newDir);
				Location bLoc = box.getCenter().toLocation(mWorld);
				Location pLoc = bLoc.clone();
				for (int t = 0; t < 10; t++) {
					pLoc.add((newDir.clone()).multiply(0.1));
					mWorld.spawnParticle(Particle.REDSTONE, pLoc, 1, 0.1, 0.1, 0.1, DAGGER_THROW_COLOR);
				}

				for (LivingEntity mob : mobs) {
					if (mob.getBoundingBox().overlaps(box)
						&& MetadataUtils.checkOnceThisTick(mPlugin, mob, DAGGER_THROW_MOB_HIT_TICK)) {
						bLoc.subtract((newDir.clone()).multiply(0.5));
						mWorld.spawnParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1);
						mWorld.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.4f, 2.5f);

						EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer);
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, DAGGER_THROW_DURATION, mVulnAmplifier, true, false));
						break;
					} else if (bLoc.getBlock().getType().isSolid()) {
						bLoc.subtract((newDir.clone()).multiply(0.5));
						mWorld.spawnParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1);
						break;
					}
				}
			}
		}

		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > -50) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			return InventoryUtils.isSwordItem(mainHand) && InventoryUtils.isSwordItem(offHand);
		}
		return false;
	}

}