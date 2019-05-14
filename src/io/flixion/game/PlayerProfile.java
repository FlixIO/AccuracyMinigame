package io.flixion.game;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

public class PlayerProfile {
	private UUID u;
	private GameType lastType;
	private Cooldown c;
	
	public PlayerProfile(UUID u, GameType lastType, Cooldown c) {
		super();
		this.u = u;
		this.lastType = lastType;
		this.c = c;
	}

	public UUID getUUID() {
		return u;
	}
	
	public Cooldown getPlayerCooldown() {
		return c;
	}
	
	public Inventory getMenuGUI() {
		Inventory i = Bukkit.createInventory(null, 27, "Game Menu");
		for (int j = 0; j < i.getSize(); j++) {
			i.setItem(j, GameUtil.nullPane());
		}
		i.setItem(12, Game.getParkourHandler().gameIcon(GameType.PARKOUR, Bukkit.getPlayer(u)));
		i.setItem(14, Game.getParkourHandler().gameIcon(GameType.ACCURACY, Bukkit.getPlayer(u)));
		return i;
	}
	
	public GameType getLastType() {
		 return lastType;
	}
	
	public void setLastType(GameType type) {
		lastType = type;
	}
}
