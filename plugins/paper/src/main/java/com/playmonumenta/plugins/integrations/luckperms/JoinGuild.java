package com.playmonumenta.plugins.integrations.luckperms;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.User;

public class JoinGuild {
	public static void register(Plugin plugin) {
		// joinguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.joinguild");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("joinguild")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(plugin, (Player) args[0]);
			})
			.register();
	}

	private static void run(Plugin plugin, Player player) throws WrapperCommandSyntaxException {
		Group currentGuild = LuckPermsIntegration.getGuild(player);
		String currentGuildName = LuckPermsIntegration.getGuildName(currentGuild);
		if (currentGuildName != null) {
			String err = ChatColor.RED + "You are already in the guild '" + currentGuildName + "' !";
			player.sendMessage(err);
			CommandAPI.fail(err);
		}

		// Check for nearby founder
		for (Player p : PlayerUtils.playersInRange(player, 1, false)) {
			if (ScoreboardUtils.getScoreboardValue(p, "Founder") == 1) {
				/* Nearby player is a founder - join to that guild */
				Group group = LuckPermsIntegration.getGuild(p);
				if (group == null) {
					continue;
				} else {
					String guildName = LuckPermsIntegration.getGuildName(group);
					// Add user to guild
					new BukkitRunnable() {
						@Override
						public void run() {
							User user = LuckPermsIntegration.LP.getUser(player.getUniqueId());
							user.setPermission(LuckPermsIntegration.LP.getNodeFactory().makeGroupNode(group).build());
							LuckPermsIntegration.LP.getUserManager().saveUser(user);
							LuckPermsIntegration.LP.runUpdateTask();
							LuckPermsIntegration.LP.getMessagingService().ifPresent(MessagingService::pushUpdate);
						}
					}.runTaskAsynchronously(plugin);

					// Success indicators
					player.sendMessage(ChatColor.GOLD + "Congratulations! You have joined " + guildName + "!");
					p.sendMessage(ChatColor.WHITE + player.getName() + ChatColor.GOLD + " has joined your guild");
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					                       "execute at " + player.getName()
					                       + " run summon minecraft:firework_rocket ~ ~1 ~ "
					                       + "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");

					// All done
					return;
				}
			}
		}

		String err = ChatColor.RED + "A founder of the guild you wish to join needs to stand within 1 block of you";
		player.sendMessage(err);
		CommandAPI.fail(err);
	}
}
