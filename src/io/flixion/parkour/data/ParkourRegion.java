package io.flixion.parkour.data;

import java.util.LinkedList;
import java.util.Queue;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import io.flixion.game.Game;
import io.flixion.game.GameState;
import io.flixion.game.GameUtil;

public class ParkourRegion {
	private int xCoord;
	private int zCoord;
	private Location centerPoint;
	private Player regionOwner;
	private BukkitTask asyncAwaitTask;
	private int wave = 0;
	private Queue<Block> blockQueue = new LinkedList<>();
	private Block currentTarget;

	public ParkourRegion(int xCoord, int zCoord, World w, Player p) {
		super();
		this.xCoord = xCoord;
		this.zCoord = zCoord;
		regionOwner = p;
		centerPoint = new Location(w, xCoord - 150.5, 180, zCoord - 150.5);
	}

	public int getxCoord() {
		return xCoord;
	}
	
	public BukkitTask getAsyncAwaitTask() {
		return asyncAwaitTask;
	}
	
	public void setAsyncAwaitTask(BukkitTask t) {
		asyncAwaitTask = t;
	}

	public int getzCoord() {
		return zCoord;
	}

	public Location getCenterPoint() {
		return centerPoint;
	}

	public Player getP() {
		return regionOwner;
	}

	public int getWave() {
		return wave;
	}
	
	public void setCurrentTargetB(Block b) {
		currentTarget = b;
	}
	
	public Block getTarget() {
		return currentTarget;
	}
	
	public void setWave(int wave) {
		this.wave = wave;
	}
	
	public void incrementWave() {
		wave++;
		regionOwner.sendMessage(GameUtil.addColor("&7Congrats you’ve reached wave &f" + wave + "&7!"));
		if (wave == 11) {
			Game.getParkourHandler().endMatch(this, GameState.WIN);
		}
		Game.getParkourHandler().doGeneration(this, wave, 2);
		regionOwner.playSound(regionOwner.getLocation(), Sound.LEVEL_UP, 1, 0);
	}

	public String getLocString() {
		return xCoord + "," + zCoord;
	}

	public Queue<Block> getActiveRegionBlocks() {
		return blockQueue;
	}
	
	public void clearPointData() {
		for (Block b : blockQueue) {
			b.setType(Material.AIR);
		}
		blockQueue.clear();
	}

	public void addBlock(Block b) {
		blockQueue.add(b);
	}
	
	public Block removeLastBlock() {
		return blockQueue.poll();
	}
	
}
