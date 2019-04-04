package com.playmonumenta.plugins.enchantments.evasions;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.other.EvasionEnchant;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class MeleeEvasion implements BaseEnchantment {

	private static String PROPERTY_NAME = ChatColor.GRAY + "Melee Evasion";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.ARMOR);
	}

	@Override
	public void onHurtByEntity(Plugin plugin, Player player, int level, EntityDamageByEntityEvent event) {
		//Add the extra location distance check because mob ability count as ENTITY_ATTACK for some reason.
		if (event.getCause() == DamageCause.ENTITY_ATTACK && event.getDamager().getLocation().distance(player.getLocation()) < 2) {
			EvasionEnchant evasion = (EvasionEnchant) AbilityManager.getManager().getPlayerAbility(player, EvasionEnchant.class);
			evasion.chance += (16 * level);
		}
	}

}
