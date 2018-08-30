package com.playmonumenta.plugins.integrations;

import com.playmonumenta.plugins.Plugin;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class VotifierIntegration implements Listener {
	private Plugin mPlugin;

	public VotifierIntegration(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();

		//  Loop through all online players and find the matching one
		//  and send them a message
		for (Player player : mPlugin.getServer().getOnlinePlayers()) {
			if (player.getName() == vote.getUsername()) {
				// TODO
				player.sendMessage("Thanks for voting! Debug: " + vote.toString());
			}
		}
    }
}
