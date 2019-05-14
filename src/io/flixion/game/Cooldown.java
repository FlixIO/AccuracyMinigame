package io.flixion.game;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Cooldown {
	private Map<GameType, Long> cooldownData = new HashMap<>();
	private static final long COOLDOWN = 86400000 * 12;
	private UUID u;
	public Cooldown(UUID u, long accuracy, long parkour) {
		this.u = u;
		cooldownData.put(GameType.ACCURACY, accuracy);
		cooldownData.put(GameType.PARKOUR, parkour);
	}
	public Date isCooldown(GameType type) {
		if (System.currentTimeMillis() < cooldownData.get(type) + COOLDOWN) {
			return new Date(cooldownData.get(type) + COOLDOWN - System.currentTimeMillis());
		}
		return null;
	}
	
	public void activate(GameType type) {
		cooldownData.replace(type, System.currentTimeMillis());
		Game.getPlayerData().get(u).setLastType(type);
	}
	
	public long getCooldownForType(GameType type) {
		return cooldownData.get(type);
	}
}
