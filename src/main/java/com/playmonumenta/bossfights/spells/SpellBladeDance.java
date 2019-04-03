package com.playmonumenta.bossfights.spells;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellBladeDance extends Spell {
	private Plugin mPlugin;
	private Entity mCaster;

	public SpellBladeDance(Plugin plugin, Entity caster) {
		mPlugin = plugin;
		mCaster = caster;
	}

	@Override
	public void run() {
		World world = mCaster.getWorld();
		mCaster.setInvulnerable(true);
		world.playSound(mCaster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
		world.spawnParticle(Particle.SWEEP_ATTACK, mCaster.getLocation(), 150, 4, 4, 4, 0);
		new BukkitRunnable() {
			int i = 0;
			float pitch = 0;
			@Override
			public void run() {
				if (mCaster.isDead() || !mCaster.isValid()) {
					this.cancel();
					return;
				}

				i += 2;
				world.spawnParticle(Particle.SWEEP_ATTACK, mCaster.getLocation(), 10, 4, 4, 4, 0);
				world.playSound(mCaster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, pitch);
				pitch += 0.2;
				new BukkitRunnable() {
					Location loc1 = mCaster.getLocation().add(6, 6, 6);
					Location loc2 = mCaster.getLocation().add(-6, -1, -6);

					double x1 = ThreadLocalRandom.current().nextDouble(loc2.getX(), loc1.getX());
					double y1 = ThreadLocalRandom.current().nextDouble(loc2.getY(), loc1.getY());
					double z1 = ThreadLocalRandom.current().nextDouble(loc2.getZ(), loc1.getZ());
					Location l1 = new Location(world, x1, y1, z1);

					double x2 = ThreadLocalRandom.current().nextDouble(loc2.getX(), loc1.getX());
					double y2 = ThreadLocalRandom.current().nextDouble(loc2.getY(), loc1.getY());
					double z2 = ThreadLocalRandom.current().nextDouble(loc2.getZ(), loc1.getZ());
					Location l2 = new Location(world, x2, y2, z2);

					Vector dir = Utils.getDirectionTo(l2, l1);

					int t = 0;
					@Override
					public void run() {
						t++;
						l1.add(dir.clone().multiply(1.15));
						world.spawnParticle(Particle.CRIT_MAGIC, l1, 4, 0, 0, 0, 0.35);
						world.spawnParticle(Particle.CLOUD, l1, 1, 0, 0, 0, 0);
						world.spawnParticle(Particle.SWEEP_ATTACK, l1, 1, 0, 0, 0, 0);
						if (t >= 10) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);

				if (i >= 40) {
					mCaster.setInvulnerable(false);
					this.cancel();

					//Ultra flash
					new BukkitRunnable() {
						double rotation = 0;
						Location loc = mCaster.getLocation();
						double radius = 0;
						double y = 2.5;
						double yminus = 0.35;

						@Override
						public void run() {

							radius += 1;
							for (int i = 0; i < 15; i += 1) {
								rotation += 24;
								double radian1 = Math.toRadians(rotation);
								loc.add(Math.cos(radian1) * radius, y, Math.sin(radian1) * radius);
								world.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0.1, 0.1, 0.1, 0);
								world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 3, 0.1, 0.1, 0.1, 0.1);
								loc.subtract(Math.cos(radian1) * radius, y, Math.sin(radian1) * radius);

							}
							y -= y * yminus;
							yminus += 0.02;
							if (yminus >= 1) {
								yminus = 1;
							}
							if (radius >= 7) {
								this.cancel();
							}

						}

					}.runTaskTimer(mPlugin, 0, 1);
					world.playSound(mCaster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
					world.playSound(mCaster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 0.5f);
					world.spawnParticle(Particle.FLAME, mCaster.getLocation(), 150, 0, 0, 0, 0.25);
					world.spawnParticle(Particle.CLOUD, mCaster.getLocation(), 70, 0, 0, 0, 0.25);
					world.spawnParticle(Particle.SWEEP_ATTACK, mCaster.getLocation(), 150, 4, 4, 4, 0);
					for (Player player : Utils.playersInRange(mCaster.getLocation(), 4)) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 2));
						Utils.KnockAway(mCaster.getLocation(), player, 0.45f);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public int duration() {
		return 20 * 5;
	}

}
