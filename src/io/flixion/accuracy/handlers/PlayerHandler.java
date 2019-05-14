package io.flixion.accuracy.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.flixion.game.Game;
import io.flixion.game.GameState;

public class PlayerHandler implements Listener {

	@EventHandler
	public void handlePlayerMovement(PlayerMoveEvent e) {
		if (Game.getGameHandler().getPlayerMappedRegions().containsKey(e.getPlayer().getUniqueId())) {
			if (e.getFrom().getZ() != e.getTo().getZ() || e.getFrom().getX() != e.getTo().getX()) {
				e.setTo(e.getFrom());
			}
		}
		if (Game.getParkourHandler().getPlayerMappedRegions().containsKey(e.getPlayer().getUniqueId())) {
			if (Game.getParkourHandler().getPlayerMappedRegions().get(e.getPlayer().getUniqueId()).getWave() == 0) {
				if (e.getFrom().getZ() != e.getTo().getZ() || e.getFrom().getX() != e.getTo().getX()) {
					e.setTo(e.getFrom());
				}
			}
		}
	}

	@EventHandler (ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void cancelCommands(PlayerCommandPreprocessEvent e) {
		if (Game.getGameHandler().getPlayerMappedRegions().containsKey(e.getPlayer().getUniqueId()) || Game.getParkourHandler().getPlayerMappedRegions().containsKey(e.getPlayer().getUniqueId())) {
			if (e.getMessage().startsWith("/")) {
				if (!e.getMessage().toLowerCase().startsWith("/accept")
						&& !e.getMessage().toLowerCase().startsWith("/msg")
						&& !e.getMessage().toLowerCase().startsWith("/pm")
						&& !e.getMessage().toLowerCase().startsWith("/m")
						&& !e.getMessage().toLowerCase().startsWith("/r"))
					e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void handleIngameQuit(PlayerQuitEvent e) {
		if (Game.getGameHandler().getPlayerMappedRegions().containsKey(e.getPlayer().getUniqueId())) {
			Game.getGameHandler().endMatch(
					Game.getGameHandler().getPlayerMappedRegions().get(e.getPlayer().getUniqueId()),
					"quit");
		} else if (Game.getParkourHandler().getPlayerMappedRegions().containsKey(e.getPlayer().getUniqueId())) {
			Game.getParkourHandler().endMatch(Game.getParkourHandler().getPlayerMappedRegions().get(e.getPlayer().getUniqueId()), GameState.QUIT);
		}
	}

//	@EventHandler
//	public void dailyMessage(PlayerJoinEvent e) {
//		Bukkit.getScheduler().runTaskLaterAsynchronously(Game.getPL(), new Runnable() {
//
//			@Override
//			public void run() {
//				if (!Game.getCooldowns().containsKey(e.getPlayer().getUniqueId())) { // No cooldown exists for
//																							// player
//					e.getPlayer().sendMessage(GameUtil
//							.addColor("&c&lDaily &7» &fYou can activate the DailyGame again with &a/daily&f!"));
//				} else {
//					if (System.currentTimeMillis()
//							- Game.getCooldowns().get(e.getPlayer().getUniqueId()) >= 72000000) { // Check time for
//																										// > 20h
//						e.getPlayer().sendMessage(GameUtil
//								.addColor("&c&lDaily &7» &fYou can activate the DailyGame again with &a/daily&f!"));
//					}
//				}
//			}
//		}, 3);
//	}
}
