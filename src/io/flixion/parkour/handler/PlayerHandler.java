package io.flixion.parkour.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import io.flixion.game.Game;
import io.flixion.game.GameState;
import io.flixion.game.GameUtil;
import io.flixion.parkour.data.ParkourRegion;

public class PlayerHandler implements Listener {
	private Map<UUID, BukkitTask> playerGameRunnables = new HashMap<>();
	
	public void addPlayerMonitor(Player p) {
		BukkitTask t = Bukkit.getScheduler().runTaskTimer(Game.getPL(), new Runnable() {
			ParkourRegion r = Game.getParkourHandler().getPlayerMappedRegions().get(p.getUniqueId());
			int jumps = 0;
			int totalJumps = 0;
			boolean firstFail = false;
			@Override
			public void run() {
				if (totalJumps == 1) {
					r.getCenterPoint().getBlock().setType(Material.AIR);
				}
				if (p.getFallDistance() > 20) {
					if (r.getWave() == 1 && !firstFail) {
						//allow one more turn
						r.getCenterPoint().getBlock().setType(Material.QUARTZ_BLOCK);
						p.teleport(new Location(r.getCenterPoint().getWorld(), r.getCenterPoint().getX(), r.getCenterPoint().getY() + 1, r.getCenterPoint().getZ()));
						p.sendMessage(GameUtil.addColor("Since you lost on round 1, you may have one more turn!"));
						r.clearPointData();
						firstFail = true;
						jumps = 0;
						totalJumps = 0;
						Game.getParkourHandler().doGeneration(r, 1, 0);
					} else {
						Game.getParkourHandler().endMatch(r, GameState.LOSE);
					}
					p.playSound(p.getLocation(), Sound.ANVIL_BREAK, 1, 0);
					p.setFallDistance(0F);
				} else {
					if (r.getTarget() != null) {
						if (Math.abs(r.getTarget().getX() - p.getLocation().getBlockX()) <= 0.5 && Math.abs(r.getTarget().getZ() - p.getLocation().getBlockZ()) <= 0.5 && Math.abs(r.getTarget().getY() - p.getLocation().getBlockY()) <= 2) {
							p.playSound(p.getLocation(), Sound.CHICKEN_EGG_POP, 1, 0);
							jumps++;
							totalJumps++;
							if (jumps == 4) {
								r.incrementWave();
								jumps = 0;
							} else {
								Game.getParkourHandler().doGeneration(r, r.getWave(), totalJumps);
							}
						}
					}
				}
			}
		}, 0, 10);
		playerGameRunnables.put(p.getUniqueId(), t);
	}

	public void removePlayerMonitor(Player p) {
		if (playerGameRunnables.containsKey(p.getUniqueId())) {
			playerGameRunnables.get(p.getUniqueId()).cancel();
			playerGameRunnables.remove(p.getUniqueId());
		}
	}
}
