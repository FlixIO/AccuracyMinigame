package io.flixion.accuracy.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class PlayerData {
	private ItemStack[] inventory;
	private double locX;
	private double locY;
	private double locZ;
	private String worldName;
	public PlayerData(ItemStack[] inventory, Location loc) {
		super();
		this.inventory = inventory;
		this.locX = loc.getX();
		this.locY = loc.getY();
		this.locZ = loc.getZ();
		this.worldName = loc.getWorld().getName();
	}
	public ItemStack[] getInventory() {
		return inventory;
	}
	public Location getLoc() {
		return new Location(Bukkit.getWorld(worldName), locX, locY, locZ);
	}
}
