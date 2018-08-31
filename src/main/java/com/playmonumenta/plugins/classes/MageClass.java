package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect;
import com.playmonumenta.plugins.utils.particlelib.ParticleEffect.OrdinaryColor;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.bukkit.World;

/*
    ManaLance
    FrostNova
    Prismatic
    Magma
    ArcaneStrike
    ArcaneStrikeHits
    Elemental
    SpellShock
    Intellect
*/

public class MageClass extends BaseClass {
	public class SpellShockedMob {
		public LivingEntity mob;
		public int ticksLeft;
		public Player initiator;

		public SpellShockedMob(LivingEntity inMob, int ticks, Player inInitiator) {
			mob = inMob;
			ticksLeft = ticks;
			initiator = inInitiator;
		}
	}

	private static final int MANA_LANCE_1_DAMAGE = 8;
	private static final int MANA_LANCE_2_DAMAGE = 10;
	private static final int MANA_LANCE_1_COOLDOWN = 5 * 20;
	private static final int MANA_LANCE_2_COOLDOWN = 3 * 20;
	private static final int MANA_LANCE_R = 91;
	private static final int MANA_LANCE_G = 187;
	private static final int MANA_LANCE_B = 255;
	private static final int MANA_LANCE_STAGGER_DURATION = (int)(0.95 * 20);

	private static final float FROST_NOVA_RADIUS = 6.0f;
	private static final int FROST_NOVA_1_DAMAGE = 3;
	private static final int FROST_NOVA_2_DAMAGE = 6;
	private static final int FROST_NOVA_EFFECT_LVL = 2;
	private static final int FROST_NOVA_COOLDOWN = 18 * 20;
	private static final int FROST_NOVA_DURATION = 8 * 20;

	private static final int PRISMATIC_SHIELD_TRIGGER_HEALTH = 6;
	private static final int PRISMATIC_SHIELD_EFFECT_LVL_1 = 1;
	private static final int PRISMATIC_SHIELD_EFFECT_LVL_2 = 2;
	private static final int PRISMATIC_SHIELD_1_DURATION = 10 * 20;
	private static final int PRISMATIC_SHIELD_2_DURATION = 10 * 20;
	private static final int PRISMATIC_SHIELD_BASE_COOLDOWN = 3 * 60 * 20;
	private static final int PRISMATIC_SHIELD_INTELLECT_COOLDOWN = 2 * 60 * 20;

	private static final int MAGMA_SHIELD_COOLDOWN = 12 * 20;
	private static final int MAGMA_SHIELD_RADIUS = 6;
	private static final int MAGMA_SHIELD_FIRE_DURATION = 4 * 20;
	private static final int MAGMA_SHIELD_1_DAMAGE = 6;
	private static final int MAGMA_SHIELD_2_DAMAGE = 12;
	private static final float MAGMA_SHIELD_KNOCKBACK_SPEED = 0.5f;
	private static final double MAGMA_SHIELD_DOT_ANGLE = 0.33;

	private static final float ARCANE_STRIKE_RADIUS = 4.0f;
	private static final int ARCANE_STRIKE_1_DAMAGE = 5;
	private static final int ARCANE_STRIKE_2_DAMAGE = 8;
	private static final int ARCANE_STRIKE_BURN_DAMAGE = 4;
	private static final int ARCANE_STRIKE_COOLDOWN = 6 * 20;

	private static final int ELEMENTAL_ARROWS_ICE_DURATION = 8 * 20;
	private static final int ELEMENTAL_ARROWS_ICE_EFFECT_LVL = 1;
	private static final int ELEMENTAL_ARROWS_FIRE_DURATION = 5 * 20;
	private static final double ELEMENTAL_ARROWS_RADIUS = 4.0;

	private static final int SPELL_SHOCK_DURATION = 6 * 20;
	private static final int SPELL_SHOCK_TEST_PERIOD = 2;
	private static final int SPELL_SHOCK_DEATH_RADIUS = 2;
	private static final int SPELL_SHOCK_DEATH_DAMAGE = 3;
	private static final int SPELL_SHOCK_SPELL_RADIUS = 4;
	private static final int SPELL_SHOCK_SPELL_DAMAGE = 3;

	private static double PASSIVE_DAMAGE = 1.5;

	private static Map<UUID, SpellShockedMob>mSpellShockedMobs = new HashMap<UUID, SpellShockedMob>();

	private World mWorld;

	public MageClass(Plugin plugin, Random random, World world) {
		super(plugin, random);

		mWorld = world;

		// SpellShock task to process tagged mobs
		// pri
		new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				Iterator<Map.Entry<UUID, SpellShockedMob>>it = mSpellShockedMobs.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<UUID, SpellShockedMob> entry = it.next();
					SpellShockedMob shocked = entry.getValue();

					Location loc = shocked.mob.getLocation().add(0, 1, 0);
					ParticleUtils.playParticlesInWorld(world, Particle.SPELL_WITCH, loc, 1, 0.01, 0.1, 0.01, 0.001);

					if (shocked.mob.isDead() || shocked.mob.getHealth() <= 0) {
						// Mob has died - trigger effects
						ParticleUtils.playParticlesInWorld(world, Particle.SPELL_WITCH, loc, 50, 1, 1, 1, 0.001);
						world.playSound(loc, "entity.player.hurt_on_fire", 10.0f, 2.0f);
						for (Entity nearbyMob : shocked.mob.getNearbyEntities(SPELL_SHOCK_DEATH_RADIUS,
						                                                      SPELL_SHOCK_DEATH_RADIUS,
																			  SPELL_SHOCK_DEATH_RADIUS)) {
							if (EntityUtils.isHostileMob(nearbyMob)) {
								EntityUtils.damageEntity(plugin, (LivingEntity)nearbyMob, SPELL_SHOCK_DEATH_DAMAGE, shocked.initiator);
							}
						}

						it.remove();
						continue;
					}

					shocked.ticksLeft -= SPELL_SHOCK_TEST_PERIOD;
					if (shocked.ticksLeft <= 0) {
						it.remove();
						continue;
					}
				}
			}

		}.runTaskTimer(plugin, 1, SPELL_SHOCK_TEST_PERIOD);
	}

	// Spell Shock
	private void SpellDamageMob(Plugin plugin, LivingEntity mob, float dmg, Player player) {
		SpellShockedMob shocked = mSpellShockedMobs.get(mob.getUniqueId());
		if (shocked != null) {
			// Hit a shocked mob with a real spell - extra damage

			// Consume the "charge"
			mSpellShockedMobs.remove(mob.getUniqueId());

			Location loc = shocked.mob.getLocation().add(0, 1, 0);
			ParticleUtils.playParticlesInWorld(mWorld, Particle.SPELL_WITCH, loc, 100, 2, 2, 2, 0.001);
			mWorld.playSound(loc, "entity.player.hurt_on_fire", 10.0f, 2.5f);
			mWorld.playSound(loc, "entity.player.hurt_on_fire", 10.0f, 2.0f);
			mWorld.playSound(loc, "entity.player.hurt_on_fire", 10.0f, 1.5f);
			for (Entity nearbyMob : shocked.mob.getNearbyEntities(SPELL_SHOCK_SPELL_RADIUS,
																  SPELL_SHOCK_SPELL_RADIUS,
																  SPELL_SHOCK_SPELL_RADIUS)) {
				// Only damage hostile mobs and specifically not the mob originally hit
				if (EntityUtils.isHostileMob(nearbyMob) && nearbyMob != mob) {
					EntityUtils.damageEntity(plugin, (LivingEntity)nearbyMob, SPELL_SHOCK_SPELL_DAMAGE, player);
				}
			}

			dmg += SPELL_SHOCK_SPELL_DAMAGE;
		}

		// Apply damage to the hit mob all in one shot
		EntityUtils.damageEntity(mPlugin, mob, dmg, player);
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();

		//  Arcane Strike
		{
			if (InventoryUtils.isWandItem(mainHand)) {
				int arcaneStrike = ScoreboardUtils.getScoreboardValue(player, "ArcaneStrike");
				if (arcaneStrike > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ARCANE_STRIKE)) {
						if (!MetadataUtils.checkOnceThisTick(mPlugin, player, Constants.ENTITY_DAMAGE_NONCE_METAKEY)) {
							int extraDamage = arcaneStrike == 1 ? ARCANE_STRIKE_1_DAMAGE : ARCANE_STRIKE_2_DAMAGE;

							List<Entity> entities = damagee.getNearbyEntities(ARCANE_STRIKE_RADIUS, ARCANE_STRIKE_RADIUS, ARCANE_STRIKE_RADIUS);
							entities.add(damagee);
							for (Entity e : entities) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity mob = (LivingEntity)e;
									int dmg = extraDamage;

									// Arcane strike fire damage for level 2.
									// First check if the mob is burning
									// If burning, must either not have the metadata value or it must not match this player doing damage
									if (arcaneStrike > 1 && mob.getFireTicks() > 0
									    && MetadataUtils.checkOnceThisTick(mPlugin, mob, Constants.ENTITY_COMBUST_NONCE_METAKEY)) {
										dmg += ARCANE_STRIKE_BURN_DAMAGE;
									}

									EntityUtils.damageEntity(mPlugin, mob, dmg, player);
								}
							}

							Location loc = damagee.getLocation();
							ParticleUtils.playParticlesInWorld(mWorld, Particle.EXPLOSION_NORMAL, loc.add(0, 1, 0), 50, 2.5, 1, 2.5, 0.001);
							ParticleUtils.playParticlesInWorld(mWorld, Particle.SPELL_WITCH, loc.add(0, 1, 0), 200, 2.5, 1, 2.5, 0.001);

							mWorld.playSound(loc, "entity.enderdragon_fireball.explode", 0.5f, 1.5f);

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.ARCANE_STRIKE, ARCANE_STRIKE_COOLDOWN);
						}
					}
				}
			}
		}

		// Spell Shock (add static to mobs here)
		if (InventoryUtils.isWandItem(mainHand) && cause.equals(DamageCause.ENTITY_ATTACK)) {
			int spellShock = ScoreboardUtils.getScoreboardValue(player, "SpellShock");
			if (spellShock > 0) {
				mSpellShockedMobs.put(damagee.getUniqueId(),
				                      new SpellShockedMob(damagee, SPELL_SHOCK_DURATION, player));
			}
		}

		if (InventoryUtils.isWandItem(mainHand)) {
			EntityUtils.damageEntity(mPlugin, damagee, PASSIVE_DAMAGE, player);
		}

		return true;
	}


	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		//  Magma Shield
		{
			//  If we're sneaking and we block with a shield we can attempt to trigger the ability.
			if (player.isSneaking()) {
				ItemStack offHand = player.getInventory().getItemInOffHand();
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if (((offHand.getType() == Material.SHIELD && InventoryUtils.isWandItem(mainHand)) || (mainHand.getType() == Material.SHIELD))
				    && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && !ItemUtils.isInteractable(blockClicked)) {

					int magmaShield = ScoreboardUtils.getScoreboardValue(player, "Magma");
					if (magmaShield > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.MAGMA_SHIELD)) {
							Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
							List<Entity> entities = player.getNearbyEntities(MAGMA_SHIELD_RADIUS, MAGMA_SHIELD_RADIUS, MAGMA_SHIELD_RADIUS);
							for (Entity e : entities) {
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity mob = (LivingEntity)e;

									Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
									if (playerDir.dot(toMobVector) > MAGMA_SHIELD_DOT_ANGLE) {
										MovementUtils.KnockAway(player, mob, MAGMA_SHIELD_KNOCKBACK_SPEED);
										mob.setFireTicks(MAGMA_SHIELD_FIRE_DURATION);

										int extraDamage = magmaShield == 1 ? MAGMA_SHIELD_1_DAMAGE : MAGMA_SHIELD_2_DAMAGE;
										SpellDamageMob(mPlugin, mob, extraDamage, player);
									}
								}
							}

							ParticleUtils.explodingConeEffect(mPlugin, player, MAGMA_SHIELD_RADIUS, Particle.FLAME, 0.75f, Particle.LAVA, 0.25f, MAGMA_SHIELD_DOT_ANGLE);

							mWorld.playSound(player.getLocation(), "entity.firework.large_blast", 0.5f, 1.5f);
							mWorld.playSound(player.getLocation(), "entity.generic.explode", 0.25f, 1.0f);

							boolean intellectBonus = ScoreboardUtils.getScoreboardValue(player, "Intellect") == 2;
							int cooldown = intellectBonus ? (MAGMA_SHIELD_COOLDOWN / 3) * 2 : MAGMA_SHIELD_COOLDOWN;

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.MAGMA_SHIELD, cooldown);
						}
					}
				}
			} else {
				//Mana Lance
				if (action == Action.RIGHT_CLICK_AIR || (action == Action.RIGHT_CLICK_BLOCK && !ItemUtils.isInteractable(blockClicked))) {
					int manaLance = ScoreboardUtils.getScoreboardValue(player, "ManaLance");
					ItemStack mainHand = player.getInventory().getItemInMainHand();
					if (InventoryUtils.isWandItem(mainHand)) {
						if (manaLance > 0 && player.getGameMode() != GameMode.SPECTATOR) {
							if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.MANA_LANCE)) {

								int extraDamage = manaLance == 1 ? MANA_LANCE_1_DAMAGE : MANA_LANCE_2_DAMAGE;
								int cooldown = manaLance == 1 ? MANA_LANCE_1_COOLDOWN : MANA_LANCE_2_COOLDOWN;

								Location loc = player.getEyeLocation();
								Vector dir = loc.getDirection();
								loc.add(dir);
								ParticleEffect.EXPLOSION_NORMAL.display(0, 0, 0, 0.125f, 10, loc, 40);
								double pOffset = 0.35;

								for (int i = 0; i < 8; i++) {
									loc.add(dir);

									ParticleEffect.EXPLOSION_NORMAL.display(0.05f, 0.05f, 0.05f, 0.025f, 2, loc, 40);
									for (int t = 0; t < 18; t++) {
										Location pLoc = loc.clone();
										double os1 = ThreadLocalRandom.current().nextDouble(-pOffset, pOffset);
										double os2 = ThreadLocalRandom.current().nextDouble(-pOffset, pOffset);
										double os3 = ThreadLocalRandom.current().nextDouble(-pOffset, pOffset);
										pLoc.add(os1, os2, os3);
										ParticleEffect.REDSTONE.display(new OrdinaryColor(MANA_LANCE_R, MANA_LANCE_G, MANA_LANCE_B), pLoc, 40);
									}
									if (loc.getBlock().getType().isSolid()) {
										loc.subtract(dir.multiply(0.5));
										ParticleEffect.CLOUD.display(0, 0, 0, 0.125f, 30, loc, 40);
										loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_BLAST, 1, 1.65f);
										break;
									}
									for (Entity e : loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
										if (EntityUtils.isHostileMob(e)) {
											LivingEntity mob = (LivingEntity) e;
											SpellDamageMob(mPlugin, mob, extraDamage, player);
											mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, MANA_LANCE_STAGGER_DURATION, 10, true, false));
										}
									}
								}

								mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.MANA_LANCE, cooldown);
								player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
							}
						}
					}
				}
			}
		}

		//  Frost Nova
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			if (player.isSneaking()) {
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				if (InventoryUtils.isWandItem(mainHand)) {
					int frostNova = ScoreboardUtils.getScoreboardValue(player, "FrostNova");
					if (frostNova > 0) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.FROST_NOVA)) {
							List<Entity> entities = player.getNearbyEntities(FROST_NOVA_RADIUS, FROST_NOVA_RADIUS, FROST_NOVA_RADIUS);
							entities.add(player);
							for (int i = 0; i < entities.size(); i++) {
								Entity e = entities.get(i);
								if (EntityUtils.isHostileMob(e)) {
									LivingEntity mob = (LivingEntity)(e);

									int extraDamage = frostNova == 1 ? FROST_NOVA_1_DAMAGE : FROST_NOVA_2_DAMAGE;
									SpellDamageMob(mPlugin, mob, extraDamage, player);

									mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, FROST_NOVA_DURATION, FROST_NOVA_EFFECT_LVL, true, false));
									if (frostNova > 1) {
										EntityUtils.applyFreeze(mPlugin, FROST_NOVA_DURATION, mob);
									}

									mob.setFireTicks(0);
								} else if (e instanceof Player) {
									e.setFireTicks(0);
								}
							}

							int intellect = ScoreboardUtils.getScoreboardValue(player, "Intellect");
							int cooldown = intellect < 2 ? FROST_NOVA_COOLDOWN : (FROST_NOVA_COOLDOWN / 3) * 2;

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.FROST_NOVA, cooldown);

							Location loc = player.getLocation();
							ParticleUtils.playParticlesInWorld(mWorld, Particle.SNOW_SHOVEL, loc.add(0, 1, 0), 400, 4, 1, 4, 0.001);
							ParticleUtils.playParticlesInWorld(mWorld, Particle.CRIT_MAGIC, loc.add(0, 1, 0), 200, 4, 1, 4, 0.001);

							mWorld.playSound(loc, "block.glass.break", 0.5f, 1.0f);
						}
					}
				}
			}
		}
	}

	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		//  Elemental Arrows
		{
			int elementalArrows = ScoreboardUtils.getScoreboardValue(player, "Elemental");
			if (elementalArrows > 0) {
				if (arrow.hasMetadata("FireArrow")) {
					if (damagee instanceof Stray) {
						event.setDamage(event.getDamage() + 8);
					}

					damagee.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);

					if (elementalArrows == 2) {
						List<Entity> entities = damagee.getNearbyEntities(ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_RADIUS);
						for (Entity entity : entities) {
							if (EntityUtils.isHostileMob(entity)) {
								LivingEntity mob = (LivingEntity)entity;
								mob.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
							}
						}
					}
				} else if (arrow.hasMetadata("IceArrow")) {
					if (damagee instanceof Blaze) {
						event.setDamage(event.getDamage() + 8);
					}

					damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ELEMENTAL_ARROWS_ICE_DURATION, ELEMENTAL_ARROWS_ICE_EFFECT_LVL, false, true));

					if (elementalArrows == 2) {
						List<Entity> entities = damagee.getNearbyEntities(ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_RADIUS, ELEMENTAL_ARROWS_RADIUS);
						for (Entity entity : entities) {
							if (EntityUtils.isHostileMob(entity)) {
								LivingEntity mob = (LivingEntity)entity;
								mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ELEMENTAL_ARROWS_ICE_DURATION, ELEMENTAL_ARROWS_ICE_EFFECT_LVL, false, true));
								if ((entity instanceof Blaze) && (entity != damagee)) {
									EntityUtils.damageEntity(mPlugin, mob, 8, player);
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void PlayerShotArrowEvent(Player player, Arrow arrow) {
		//  Elemental Arrows
		{
			int elementalArrows = ScoreboardUtils.getScoreboardValue(player, "Elemental");
			if (elementalArrows > 0) {
				//  If sneaking, Ice Arrow
				if (player.isSneaking()) {
					arrow.setFireTicks(0);
					arrow.setMetadata("IceArrow", new FixedMetadataValue(mPlugin, 0));
					mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SNOW_SHOVEL);
				}
				//  else Fire Arrow
				else {
					arrow.setFireTicks(ELEMENTAL_ARROWS_FIRE_DURATION);
					arrow.setMetadata("FireArrow", new FixedMetadataValue(mPlugin, 0));
					mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FLAME);
				}
			}
		}
	}

	@Override
	public boolean PlayerDamagedEvent(Player player, DamageCause cause, double damage) {
		if (!player.isDead()) {
			double correctHealth = player.getHealth() - damage;
			if (correctHealth > 0 && correctHealth <= PRISMATIC_SHIELD_TRIGGER_HEALTH) {
				int prismatic = ScoreboardUtils.getScoreboardValue(player, "Prismatic");
				if (prismatic > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.PRISMATIC_SHIELD)) {
						int effectLevel = prismatic == 1 ? PRISMATIC_SHIELD_EFFECT_LVL_1 : PRISMATIC_SHIELD_EFFECT_LVL_2;
						int duration = prismatic == 1 ? PRISMATIC_SHIELD_1_DURATION : PRISMATIC_SHIELD_2_DURATION;

						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.ABSORPTION, duration, effectLevel, true, false));
						ParticleEffect.FIREWORKS_SPARK.display(0.2f, 0.35f, 0.2f, 0.5f, 150, player.getLocation().add(0, 1.15, 0), 40);
						ParticleEffect.SPELL_INSTANT.display(0.2f, 0.35f, 0.2f, 1, 100, player.getLocation().add(0, 1.15, 0), 40);
						player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1.35f);
						MessagingUtils.sendActionBarMessage(mPlugin, player, "Prismatic Shield has been activated");

						boolean intellectBonus = ScoreboardUtils.getScoreboardValue(player, "Intellect") == 2;
						int cooldown = intellectBonus ? PRISMATIC_SHIELD_INTELLECT_COOLDOWN : PRISMATIC_SHIELD_BASE_COOLDOWN;
						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.PRISMATIC_SHIELD, cooldown);
					}
				}
			}
		}
		return true;
	}

}
