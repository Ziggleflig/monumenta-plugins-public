package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.CalculateReforge;

import io.github.jorelali.commandapi.api.CommandAPI;

public class ReforgeHeldItem extends GenericCommand {

	public static void register() {
		registerPlayerCommand("reforgehelditem", "monumenta.command.reforgehelditem", (sender, player) -> {
			run(sender, player);
		});
	}

	private static void run(CommandSender sender, Player player) throws CommandSyntaxException {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (player.hasMetadata("PlayerCanReforge")) {
			player.removeMetadata("PlayerCanReforge", Plugin.getInstance());
			if (item == null || item.getLore() == null) {
				CommandAPI.fail("Player must have a Shattered item in their main hand!");
			}
			List<String> oldLore = item.getLore();
			List<String> newLore = new ArrayList<>();
			boolean isShattered = false;
			for (String loreEntry : oldLore) {
				if (loreEntry.contains(ChatColor.DARK_RED + "" + ChatColor.BOLD + "* SHATTERED *") ||
				    loreEntry.contains(ChatColor.DARK_RED + "Maybe a Master Repairman") ||
				    loreEntry.contains(ChatColor.DARK_RED + "could reforge it...")) {
					isShattered = true;
				} else {
					newLore.add(loreEntry);
				}
			}

			if (isShattered) {
				ItemRegion region = ItemUtils.getItemRegion(item);
				int cost = ItemUtils.getReforgeCost(item);
				PlayerInventory inventory = player.getInventory();
				if (region == ItemRegion.MONUMENTA) {
					if (player.getWorld().getName().equals("Project_Epic-region_1")) {
						region = ItemRegion.KINGS_VALLEY;
					} else if (player.getWorld().getName().equals("Project_Epic-region_2")) {
						region = ItemRegion.CELSIAN_ISLES;
					} else {
						region = ItemRegion.KINGS_VALLEY;
					}
				}
				if (region == ItemRegion.KINGS_VALLEY) {
					ItemStack cxp = CalculateReforge.mCXP.clone();
					ItemStack hxp = CalculateReforge.mHXP.clone();
					if (inventory.containsAtLeast(cxp, cost)) {
						cxp.setAmount(cost);
						inventory.removeItem(cxp);
					} else if (inventory.containsAtLeast(hxp, cost / 64) && inventory.containsAtLeast(cxp, cost % 64)) {
						hxp.setAmount(cost / 64);
						cxp.setAmount(cost % 64);
						inventory.removeItem(hxp);
						inventory.removeItem(cxp);
					} else {
						player.sendMessage("You can't afford that");
						CommandAPI.fail("Player doesn't have enough currency");
						return;
					}
				} else if (region == ItemRegion.CELSIAN_ISLES) {
					ItemStack ccs = CalculateReforge.mCCS.clone();
					ItemStack hcs = CalculateReforge.mHCS.clone();
					if (inventory.containsAtLeast(ccs, cost)) {
						ccs.setAmount(cost);
						inventory.removeItem(ccs);
					} else if (inventory.containsAtLeast(hcs, cost / 64) && inventory.containsAtLeast(ccs, cost % 64)) {
						hcs.setAmount(cost / 64);
						ccs.setAmount(cost % 64);
						inventory.removeItem(hcs);
						inventory.removeItem(ccs);
					} else {
						player.sendMessage("You can't afford that");
						CommandAPI.fail("Player doesn't have enough currency");
						return;
					}
				} else {
					player.sendMessage("Something went wrong");
					CommandAPI.fail("Invalid ItemRegion");
					return;
				}
				item.setLore(newLore);
				player.sendMessage("Your item has been reforged!");
				sender.sendMessage("Succesfully reforged the player's held item");
			} else {
				CommandAPI.fail("Player must have a Shattered item in their main hand!");
			}
		} else {
			CommandAPI.fail("Player doesn't have the metadata tag to use this command");
		}
	}
}