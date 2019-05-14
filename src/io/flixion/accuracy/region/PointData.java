package io.flixion.accuracy.region;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class PointData {
	private Entity e;
	private Block b;
	public PointData(Entity e, Block b, Region r) {
		super();
		this.e = e;
		this.b = b;
	}
	public Entity getE() {
		return e;
	}
	public Block getB() {
		return b;
	}
}
