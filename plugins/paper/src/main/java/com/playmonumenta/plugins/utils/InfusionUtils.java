package com.playmonumenta.plugins.utils;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.enchantments.infusions.Acumen;
import com.playmonumenta.plugins.enchantments.infusions.Focus;
import com.playmonumenta.plugins.enchantments.infusions.Perspicacity;
import com.playmonumenta.plugins.enchantments.infusions.Tenacity;
import com.playmonumenta.plugins.enchantments.infusions.Vigor;
import com.playmonumenta.plugins.enchantments.infusions.Vitality;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;

import io.github.jorelali.commandapi.api.CommandAPI;

public class InfusionUtils {

	private static final String PULSATING_GOLD = ChatColor.GOLD + "" + ChatColor.BOLD + "Pulsating Gold";
	private static final String PULSATING_GOLD_BAR = ChatColor.GOLD + "" + ChatColor.BOLD + "Pulsating Gold Bar";
	private static final String PULSATING_EMERALD = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Pulsating Emerald";
	private static final String PULSATING_EMERALD_BLOCK = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Pulsating Emerald Block";

	public enum InfusionSelection {
		ACUMEN("acumen", Acumen.PROPERTY_NAME),
		FOCUS("focus", Focus.PROPERTY_NAME),
		PERSPICACITY("perspicacity", Perspicacity.PROPERTY_NAME),
		TENACITY("tenacity", Tenacity.PROPERTY_NAME),
		VIGOR("vigor", Vigor.PROPERTY_NAME),
		VITALITY("vitality", Vitality.PROPERTY_NAME);

		private final String mLabel;
		private final String mEnchantName;
		InfusionSelection(String label, String enchantName) {
			mLabel = label;
			mEnchantName = enchantName;
		}

		public String getLabel() {
			return mLabel;
		}

		public String getEnchantName() {
			return mEnchantName;
		}
	}

	public static void doInfusion(CommandSender sender, Player player, ItemStack item, List<ItemFrame> paymentFrames, InfusionSelection selection) throws CommandSyntaxException {
		ItemRegion region = ItemUtils.getItemRegion(item);
		int payment = calcPaymentValue(paymentFrames, region);
		int cost = calcInfuseCost(item);
		if (cost < 0) {
			CommandAPI.fail("You must have a valid item to infuse in your main hand!");
			return;
		}

		if (item.getAmount() > 1) {
			CommandAPI.fail("Only one item can be infused!");
			return;
		}

		if (payment == cost) {
			if (ExperienceUtils.getTotalExperience(player) >= getExpInfuseCost(getCostMultiplier(item))) {
				//Infusion accepted
				for (ItemFrame frame : paymentFrames) {
					ItemStack frameItem = frame.getItem();
					if (frameItem == null || frameItem.getItemMeta() == null ||
							frameItem.getItemMeta().getDisplayName() == null) {
						continue;
					}
					if (region.equals(ItemRegion.KINGS_VALLEY)) {
						if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_GOLD)) {
							//Clear item frame contents
							frame.setItem(null);
						} else if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_GOLD_BAR)) {
							//Clear item frame contents
							frame.setItem(null);
						}
					} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
						if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_EMERALD)) {
							//Clear item frame contents
							frame.setItem(null);
						} else if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_EMERALD_BLOCK)) {
							//Clear item frame contents
							frame.setItem(null);
						}
					}
				}

				int newXP = ExperienceUtils.getTotalExperience(player) - getExpInfuseCost(getCostMultiplier(item));
				ExperienceUtils.setTotalExperience(player, newXP);

				int prevLvl = InventoryUtils.getCustomEnchantLevel(item, selection.getEnchantName(), true);
				if (prevLvl > 0) {
					InventoryUtils.removeCustomEnchant(item, selection.getEnchantName());
				}
				String numeral = "";
				switch (prevLvl) {
					case 1:
						numeral = " II";
						break;
					case 2:
						numeral = " III";
						break;
					case 3:
						numeral = " IV";
						break;
					case 0:
						numeral = " I";
						break;
					default:
						CommandAPI.fail("ERROR while assigning infusion level. Please contact a moderator if you see this message!");
				}
				CommandUtils.enchantify(sender, player, ChatColor.stripColor(selection.getEnchantName()) + numeral);
			} else {
				CommandAPI.fail("You don't have enough exp to infuse that item!");
			}
		} else {
			if (region.equals(ItemRegion.KINGS_VALLEY)) {
				CommandAPI.fail("You must insert exactly " + cost + " Pulsating Gold into the 4 item frames!");
			} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
				CommandAPI.fail("You must insert exactly " + cost + " Pulsating Emeralds into the 4 item frames!");
			} else {
				CommandAPI.fail("You must have a valid item to infuse in your main hand!");
			}
		}
	}

	private static int calcInfuseCost(ItemStack item) throws CommandSyntaxException {
		int infuseLvl = getInfuseLevel(item);
		int cost = getCostMultiplier(item);
		if (infuseLvl <= 3) {
			cost *= Math.pow(2, infuseLvl);
		} else {
			CommandAPI.fail("Items may only be infused 4 times");
		}
		return cost;
	}

	private static int getInfuseLevel(ItemStack item) {
		return InventoryUtils.getCustomEnchantLevel(item, Acumen.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Focus.PROPERTY_NAME, true)
		 		+ InventoryUtils.getCustomEnchantLevel(item, Perspicacity.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Tenacity.PROPERTY_NAME, true)
				+ InventoryUtils.getCustomEnchantLevel(item, Vigor.PROPERTY_NAME, true) + InventoryUtils.getCustomEnchantLevel(item, Vitality.PROPERTY_NAME, true);
	}

	private static int getCostMultiplier(ItemStack item) throws CommandSyntaxException {
		switch (ItemUtils.getItemTier(item)) {
			case RARE:
			case PATRON_MADE:
				return 1;
			case RELIC:
			case ARTIFACT:
			case ENHANCED_RARE:
				return 2;
			case EPIC:
				return 4;
			default:
				CommandAPI.fail("Invalid item tier! Must be rare or higher to infuse");
				return 99999999;
		}
	}

	private static int calcPaymentValue(List<ItemFrame> paymentFrames, ItemRegion region) {
		int payment = 0;
		for (ItemFrame iframe : paymentFrames) {
			ItemStack frameItem = iframe.getItem();
			if (frameItem == null || frameItem.getItemMeta() == null ||
					frameItem.getItemMeta().getDisplayName() == null) {
				continue;
			}
			if (region.equals(ItemRegion.KINGS_VALLEY)) {
				if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_GOLD)) {
					payment += 1;
				} else if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_GOLD_BAR)) {
					payment += 8;
				}
			} else if (region.equals(ItemRegion.CELSIAN_ISLES) || region.equals(ItemRegion.MONUMENTA)) {
				if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_EMERALD)) {
					payment += 1;
				} else if (frameItem.getItemMeta().getDisplayName().equals(PULSATING_EMERALD_BLOCK)) {
					payment += 8;
				}
			}
		}
		return payment;
	}

	private static int getExpInfuseCost(int scoreMult) throws CommandSyntaxException {
		switch (scoreMult) {
			case 1:
				return 8670;
			case 2:
				return 18020;
			case 4:
				return 30970;
			default:
				CommandAPI.fail("Invalid score multiplier");
				return 99999999;
		}
	}
}