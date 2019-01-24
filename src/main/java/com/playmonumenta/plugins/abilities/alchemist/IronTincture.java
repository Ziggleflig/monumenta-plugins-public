package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class IronTincture extends Ability {
	private static final int IRON_TINCTURE_THROW_COOLDOWN = 10 * 20;
	private static final int IRON_TINCTURE_USE_COOLDOWN = 50 * 20;
	private static final int IRON_TINCTURE_TICK_PERIOD = 2;
	private static final double IRON_TINCTURE_VELOCITY = 0.7;

	public IronTincture(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.IRON_TINCTURE;
		mInfo.scoreboardId = "IronTincture";
		mInfo.cooldown = IRON_TINCTURE_USE_COOLDOWN; // Full duration cooldown
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (mainHand == null ||
			    (!InventoryUtils.isBowItem(mainHand)
			     && mainHand.getType() != Material.SPLASH_POTION
			     && mainHand.getType() != Material.LINGERING_POTION)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean cast() {
		int ironTincture = getAbilityScore();
		Location loc = mPlayer.getLocation().add(0, 1.8, 0);
		ItemStack itemTincture = new ItemStack(Material.SPLASH_POTION);
		mWorld.playSound(loc, Sound.ENTITY_SNOWBALL_THROW, 1, 0.15f);
		Item tincture = mWorld.dropItem(loc, itemTincture);
		tincture.setPickupDelay(Integer.MAX_VALUE);

		Vector vel = mPlayer.getEyeLocation().getDirection().normalize();
		vel.multiply(IRON_TINCTURE_VELOCITY);

		tincture.setVelocity(vel);
		tincture.setGlowing(true);

		// Full duration cooldown - is shortened if not picked up
		putOnCooldown();

		new BukkitRunnable() {
			int tinctureDecay = 0;

			@Override
			public void run() {
				if (tincture.isDead() || tincture == null) {
					this.cancel();
				}
				mWorld.spawnParticle(Particle.SPELL, tincture.getLocation(), 3, 0, 0, 0, 0.1);

				for (Player p : PlayerUtils.getNearbyPlayers(tincture.getLocation(), 1)) {
					// Prevent players from picking up their own tincture instantly
					if (p == mPlayer && tincture.getTicksLived() < 12) {
						continue;
					}

					mWorld.playSound(tincture.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.85f);
					mWorld.spawnParticle(Particle.BLOCK_DUST, tincture.getLocation(), 250, 0.1, 0.1, 0.1, 0.1, Material.GLASS.createBlockData());
					tincture.remove();

					p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, IRON_TINCTURE_USE_COOLDOWN - tinctureDecay, ironTincture));

					if (p != mPlayer) {
						mPlayer.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, IRON_TINCTURE_USE_COOLDOWN - tinctureDecay, ironTincture));
						mWorld.spawnParticle(Particle.LAVA, mPlayer.getLocation().add(0, 1, 0), 15, 1.0, 1.0, 1.0, 0.001);
						mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);
					}
					mPlugin.mTimers.removeCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell);
					putOnCooldown();

					mWorld.spawnParticle(Particle.LAVA, p.getLocation().add(0, 1, 0), 15, 1.0, 1.0, 1.0, 0.001);
					mWorld.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);

					this.cancel();
					break;
				}

				tinctureDecay += IRON_TINCTURE_TICK_PERIOD;
				if (tinctureDecay >= IRON_TINCTURE_THROW_COOLDOWN) {
					tincture.remove();
					this.cancel();

					// Take the skill off cooldown (by setting to 0)
					mPlugin.mTimers.AddCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell, 0);
				}
			}

		}.runTaskTimer(mPlugin, 0, IRON_TINCTURE_TICK_PERIOD);
		return true;
	}
}