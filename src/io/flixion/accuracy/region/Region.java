package io.flixion.accuracy.region;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import io.flixion.game.Game;
import io.flixion.game.GameUtil;

public class Region {
	private int xCoord;
	private int zCoord;
	private Location centerPoint;
	private UUID regionOwner;
	private BukkitTask asyncAwaitTask;
	private int wave = 1;
	private Map<String, Block> activeRegionBlocks = new HashMap<>();
	private Map<UUID, LivingEntity> activeRegionEntities = new HashMap<>();

	public Region(int xCoord, int zCoord, World w, Player p) {
		super();
		this.xCoord = xCoord;
		this.zCoord = zCoord;
		regionOwner = p.getUniqueId();
		centerPoint = new Location(w, xCoord + 100.5, 180, zCoord + 100.5);
	}

	public int getxCoord() {
		return xCoord;
	}

	public int getzCoord() {
		return zCoord;
	}

	public Location getCenterPoint() {
		return centerPoint;
	}

	public UUID getRegionOwner() {
		return regionOwner;
	}

	public BukkitTask getAsyncAwaitTask() {
		return asyncAwaitTask;
	}

	public Player getP() {
		return Bukkit.getPlayer(regionOwner);
	}

	public int getWave() {
		return wave;
	}
	
	public void incrementWave() {
		wave++;
		Player p = Bukkit.getPlayer(regionOwner);
		p.sendMessage(GameUtil.addColor("&7Congrats you’ve reached wave &f" + wave + "&7!"));
		Game.getGameHandler().handleWave(wave, this);
		p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 0);
	}

	public String getLocString() {
		return xCoord + "," + zCoord;
	}

	public void setAsyncAwaitTask(BukkitTask asyncAwaitTask) {
		this.asyncAwaitTask = asyncAwaitTask;
	}

	public Map<UUID, LivingEntity> getActiveRegionEntities() {
		return activeRegionEntities;
	}

	public void addEntity(UUID uuid, LivingEntity b) {
		activeRegionEntities.put(uuid, b);
	}

	public Map<String, Block> getActiveRegionBlocks() {
		return activeRegionBlocks;
	}
	
	public void clearPointData() {
		activeRegionBlocks.clear();
		activeRegionEntities.clear();
	}

	public void addBlock(String loc, Block b) {
		activeRegionBlocks.put(loc, b);
	}
	
}
