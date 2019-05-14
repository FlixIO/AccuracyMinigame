package io.flixion.accuracy.handlers;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import io.flixion.game.Game;
import io.flixion.game.GameType;
import io.flixion.game.GameUtil;

public class CommandHandler implements CommandExecutor, Listener {
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String [] args) {
		if (cmd.getName().equalsIgnoreCase("daily")) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				p.openInventory(Game.getPlayerData().get(p.getUniqueId()).getMenuGUI());
			}
		}
		else if (cmd.getName().equals("accept")) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				if (Game.getGameHandler().getPendingConfirmationSet().containsKey(p.getUniqueId())) {
					if (Game.getGameHandler().getPendingConfirmationSet().get(p.getUniqueId()) == false) {
						Game.getGameHandler().getPendingConfirmationSet().replace(p.getUniqueId(), true);
					}
				} else if (Game.getParkourHandler().getPendingConfirmationSet().containsKey(p.getUniqueId())) {
					if (Game.getParkourHandler().getPendingConfirmationSet().get(p.getUniqueId()) == false) {
						Game.getParkourHandler().getPendingConfirmationSet().replace(p.getUniqueId(), true);
						
					}
				}
			}
		}
		return true;
	}
	
	@EventHandler
	public void handleGUI(InventoryClickEvent e) {
		if (e.getClickedInventory() != null) {
			if (e.getClickedInventory().getName().equals("Game Menu")) {
				if (e.getCurrentItem() != null) {
					Player p = (Player) e.getWhoClicked();
					if (e.getCurrentItem().getType() == Material.BOW) { //Accuracy
						//init accuracy
						if (Game.getPlayerData().get(p.getUniqueId()).getPlayerCooldown().isCooldown(GameType.ACCURACY) == null) {
							if (Game.getPlayerData().get(p.getUniqueId()).getLastType() == GameType.ACCURACY) {
								p.sendMessage(GameUtil.addColor("&eYou cannot play this game on two days in a row, try the parkour!"));
							} else {
								Game.getGameHandler().initPreGame(Game.getGameHandler().generateRegion(p));
							}
						} else {
							p.sendMessage(GameUtil.addColor("&eYou are currently still on cooldown for this game!"));
						}
					} else if (e.getCurrentItem().getType() == Material.GOLDEN_APPLE ) { //parkour
						if (Game.getPlayerData().get(p.getUniqueId()).getPlayerCooldown().isCooldown(GameType.PARKOUR) == null) {
							if (Game.getPlayerData().get(p.getUniqueId()).getLastType() == GameType.PARKOUR) {
								p.sendMessage(GameUtil.addColor("&eYou cannot play this game on two days in a row, try the accuracy!"));
							} else {
								Game.getParkourHandler().initPreGame(Game.getParkourHandler().generateRegion(p));
							}
						} else {
							p.sendMessage(GameUtil.addColor("&eYou are currently still on cooldown for this game!"));
						}
					}
				}
				e.setCancelled(true);
			}
		}
	}
}
