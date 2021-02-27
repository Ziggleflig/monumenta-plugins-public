package com.playmonumenta.plugins.guis.singlepageguis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.guis.SinglePageGUI;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

import java.util.ArrayList;

public class OrinSinglePageGUI extends SinglePageGUI {
	
	private static final int ROWS = 6;
	private static final int COLUMNS = 9;

	//unused for now, these line up with the indexes below if a use is found
	//private static final int REGION_PLOTS[] = {0, 18, 27, 36};
	//private static final int REGION_ONE[] = {3, 20, 21, 22, 29, 30, 31, 38, 39, 40, 48};
	//private static final int REGION_TWO[] = {7, 24, 25, 26, 33, 34, 35, 42, 43, 44};
	
	//Layout, with P_x being plots, R1S as sierhaven,
	//R2M as mistport, and the rest matching access code
	
	//-----------------------------------------
	//P_D |x| xx |R1S | xx |x| xx |R2M | xx |x|
	//-----------------------------------------
	// xx |x| xx | xx | xx |x| xx | xx | xx |x|
	//-----------------------------------------
	//P_M |x| D0 | D1 | D3 |x| D6 | D7 | D8 |x|
	//-----------------------------------------
	//P_P |x| D3 | D4 | D5 |x| D9 |D10 |D11 |x|
	//-----------------------------------------
	//P_G |x|DB1 | DC |DS1 |x|DTL |DRL2|DTFF|x|
	//-----------------------------------------
	// xx |x| xx | xx | xx |x| xx | xx | xx |x|
	//-----------------------------------------
	
	public static class TeleportEntry {
		int slot;
		String name;
		String scoreboard;
		int score_required;
		Material type;
		String command;
		
		public TeleportEntry(int s, String n, String sc, Material t, String c) {
			slot = s;
			name = n;
			scoreboard = sc;
			score_required = 1;
			type = t;
			command = c;
		}
		//for situations like Mistport, which checks for >= 12 instead of > 1
		public TeleportEntry(int s, String n, String sc, Material t, String c, int sr) {
			slot = s;
			name = n;
			scoreboard = sc;
			score_required = sr;
			type = t;
			command = c;
		}
		
	}
	
	private static ArrayList<TeleportEntry> LOCATIONS = new ArrayList<>();
	
	static {
		LOCATIONS.add(new TeleportEntry( 0, "Docks", null, Material.LIGHT_BLUE_CONCRETE, "tp @s -1760.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 18, "Market", null, Material.BARREL, "tp @s -1757.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 27, "Personal Plot", null, Material.GRASS_BLOCK, "tp @s -1754.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 36, "Guild Plot", null, Material.YELLOW_BANNER, "tp @s -1751.5 3 -890.5"));
		
		LOCATIONS.add(new TeleportEntry( 3, "Sierhaven", null, Material.GREEN_CONCRETE, "tp @s -1748.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 20, "Labs", "D0Access", Material.GLASS_BOTTLE, "tp @s -1745.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 21, "White", "D1Access", Material.WHITE_WOOL, "tp @s -1742.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 22, "Orange", "D2Access", Material.ORANGE_WOOL, "tp @s -1739.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 29, "Magenta", "D3Access", Material.MAGENTA_WOOL, "tp @s -1736.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 30, "Light Blue", "D4Access", Material.LIGHT_BLUE_WOOL, "tp @s -1733.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 31, "Yellow", "D5Access", Material.YELLOW_WOOL, "tp @s -1730.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 38, "Willows", "DB1Access", Material.JUNGLE_LEAVES, "tp @s -1727.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 39, "Reverie", "DCAccess", Material.FIRE_CORAL, "tp @s -1724.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 40, "Sanctum", "DS1Access", Material.GRASS_BLOCK, "tp @s -1721.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 48, "Roguelike", "DRAccess", Material.MAGMA_BLOCK, "tp @s -1718.5 3 -890.5"));

		LOCATIONS.add(new TeleportEntry( 7, "Mistport", "Quest101", Material.SAND, "tp @s -1715.5 3 -890.5", 12));
		LOCATIONS.add(new TeleportEntry( 24, "Lime", "D6Access", Material.LIME_WOOL, "tp @s -1712.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 25, "Pink", "D7Access", Material.PINK_WOOL, "tp @s -1709.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 26, "Gray", "D8Access", Material.GRAY_WOOL, "tp @s -1706.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 33, "Light Gray", "D9Access", Material.LIGHT_GRAY_WOOL, "tp @s -1703.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 34, "Cyan", "D10Access", Material.CYAN_WOOL, "tp @s -1700.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 35, "Purple", "D11Access", Material.PURPLE_WOOL, "tp @s -1697.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 42, "Teal", "DTLAccess", Material.CYAN_CONCRETE_POWDER, "tp @s -1694.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 43, "Shifting City", "DRL2Access", Material.PRISMARINE_BRICKS, "tp @s -1691.5 3 -890.5"));
		LOCATIONS.add(new TeleportEntry( 44, "The Fallen Forum", "D0Access", Material.BOOKSHELF, "tp @s -1688.5 3 -890.5")); //TODO: access token for TFF
	}
	
	private final Player mPlayer;
	
	public OrinSinglePageGUI(Player player, String[] args) {
		super(player, args);
		mPlayer = player;
	}

	/*
	 * You MUST call this method from the SinglePageGUIManager. The example
	 * call for this example GUI is there for reference.
	 */
	@Override
	public void registerCommand() {
		registerCommand("openteleportergui");
	}

	@Override
	public SinglePageGUI constructGUI(Player player, String[] args) {
		return new OrinSinglePageGUI(player, args);
	}

	@Override
	public Inventory getInventory(Player player, String[] args) {

		Inventory inventory = Bukkit.createInventory(null, ROWS*COLUMNS, "Teleportation Choices");
		
		//ItemStack newItem = new ItemStack(Material.WHITE_WOOL, 1);
		
		for(TeleportEntry location : LOCATIONS) {
			if(location.scoreboard == null || ScoreboardUtils.getScoreboardValue(player, location.scoreboard) >= location.score_required) {
				ItemStack newItem = new ItemStack(location.type, 1);
				ItemMeta meta = newItem.getItemMeta();
				meta.setDisplayName(location.name);
				ArrayList<String> lore = new ArrayList<String>();
				lore.add("Pick the item up to teleport!");
				meta.setLore(lore);
				newItem.setItemMeta(meta);
				inventory.setItem(location.slot, newItem);
			}
		}
		
		for(int i = 0; i < (ROWS*COLUMNS); i++) {
			if(inventory.getItem(i) == null) {
				inventory.setItem(i,new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
			}
		}
		return inventory;
	}

	@Override
	public void processClick(InventoryClickEvent event) {

		
		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem != null && clickedItem.getType() != Material.BLACK_STAINED_GLASS_PANE) {
			int chosenSlot = event.getSlot();
			
			for(TeleportEntry location : LOCATIONS) {
				if(location.slot == chosenSlot) {
					mPlayer.performCommand(location.command);
					break;
				}
			}
			mPlayer.closeInventory();
		}
	}

}
