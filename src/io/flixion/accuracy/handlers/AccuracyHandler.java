package io.flixion.accuracy.handlers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import io.flixion.accuracy.region.PointData;
import io.flixion.accuracy.region.PlayerData;
import io.flixion.accuracy.region.Region;
import io.flixion.game.Game;
import io.flixion.game.GameType;
import io.flixion.game.GameUtil;
import io.flixion.persist.LoadPlayerManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

public class AccuracyHandler {
	private Map<UUID, Region> inUseRegions = new LinkedHashMap<>();
	private Map<UUID, Region> playerMappedRegions = new HashMap<>();
	private Map<UUID, Boolean> pendingConfirmations = new HashMap<>();
	private Map<UUID, PlayerData> cachedData = new HashMap<>();
	private static int REGION_OFFSET = 200;
	private World w;
	private EntityHandler entityHandler;

	public AccuracyHandler() {
		Game.getPL().getLogger().log(Level.SEVERE, Bukkit.getWorlds().toString());
		w = Bukkit.getWorld("daily");
		if (w == null) {
			Bukkit.getPluginManager().disablePlugin(Game.getPL());
			throw new IllegalArgumentException("This world does not exist! - daily");
		}
	}

	public Region generateRegion(Player p) { // O(n) although I deemed this sufficient since n won't be larger than								// 1000, so basically O(1)
		if (inUseRegions.isEmpty()) { // Special case #1 - No regions
			Region rC = new Region(0, 0, w, p);
			inUseRegions.put(UUID.nameUUIDFromBytes("0,0".getBytes()), rC);
			playerMappedRegions.put(p.getUniqueId(), rC);
			return rC;
		} else {
			Region rC = findAvailableRegion(p);
			inUseRegions.put(UUID.nameUUIDFromBytes(rC.getLocString().getBytes()), rC);
			playerMappedRegions.put(p.getUniqueId(), rC);
			return rC;
		}
	}

	private Region findAvailableRegion(Player p) {
		Region rC = null;
		for (int x = 0; x < 10000; x += REGION_OFFSET) { // Assuming 10k to be max WorldBorder
			if (rC != null) {
				break;
			}
			for (int z = 0; z < 10000; z += REGION_OFFSET) {
				if (!inUseRegions.containsKey(UUID.nameUUIDFromBytes((x + "," + z).getBytes()))) {
					rC = new Region(x, z, w, p);
					break;
				}
			}
		}
		return rC;
	}

	public void initPreGame(Region r) {
		Player p = r.getP();
		cachedData.put(p.getUniqueId(), new PlayerData(p.getInventory().getContents().clone(), p.getLocation()));
		p.getInventory().clear();
		p.updateInventory();
		TextComponent confirmBegin = new TextComponent(GameUtil.addColor("&f&lCLICK HERE IF YOU READ IT"));
		p.sendMessage(GameUtil.addColor("&8&m---------------------------------------------"));
		p.sendMessage(GameUtil.addColor("&c&lAccuracy’s game instructions:"));
		p.sendMessage(GameUtil.addColor(
				"&7You will get teleported on a platform, and your job is to hit every mob with one try with an arrow. Once you missed, the game is over."));
		p.sendMessage(GameUtil.addColor("&7(The better you perform the more keys you will get)."));
		p.sendMessage(GameUtil.addColor("&8&m---------------------------------------------"));
		confirmBegin
				.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new ComponentBuilder("Click Here to begin!").create()));
		confirmBegin.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept"));
		p.spigot().sendMessage(confirmBegin);
		p.sendMessage(GameUtil.addColor("&7(or use: /accept)"));
		r.getCenterPoint().getBlock().setType(Material.QUARTZ_BLOCK);
		p.teleport(r.getCenterPoint().add(0, 1, 0));
		p.setFoodLevel(20);
		pendingConfirmations.put(p.getUniqueId(), false);
		BukkitTask t = r.getAsyncAwaitTask();
		t = Bukkit.getScheduler().runTaskTimer(Game.getPL(), new Runnable() {
			int timeout = 60;

			@Override
			public void run() {
				if (timeout != 0) {
					if (pendingConfirmations.containsKey(p.getUniqueId())) {
						if (pendingConfirmations.get(p.getUniqueId()) == true) {
							for (int i = 0; i < 4; i++) {
								if (i == 3) {
									Bukkit.getScheduler().runTaskLater(Game.getPL(), new Runnable() {

										@Override
										public void run() {
											p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1, 10);
											initMainGame(r);
										}
									}, 20 * i);
								} else {
									Bukkit.getScheduler().runTaskLater(Game.getPL(), new Runnable() {

										@Override
										public void run() {
											p.playSound(p.getLocation(), Sound.NOTE_PLING, 1, 0);
										}
									}, 20 * i);
								}
							}
							r.getAsyncAwaitTask().cancel();
						}
						timeout-=0.5;
					} else {
						r.getAsyncAwaitTask().cancel(); // This shouldn't be running if the player doesn't exist in the
														// confirmation map
					}
				} else {
					pendingConfirmations.remove(p.getUniqueId()); // Failed to confirm or quits during confirmation
																	// stage
					r.getAsyncAwaitTask().cancel();
					endMatch(r, "fail_confirmation");
				}
			}
		}, 0, 10);
		r.setAsyncAwaitTask(t);
	}

	private void initMainGame(Region r) {
		Bukkit.getScheduler().runTask(Game.getPL(), new Runnable() {

			@Override
			public void run() {
				Game.getPlayerData().get(r.getP().getUniqueId()).getPlayerCooldown().activate(GameType.ACCURACY);
				Bukkit.getScheduler().runTaskAsynchronously(Game.getPL(), new LoadPlayerManager(r.getP().getUniqueId(), true, Game.getPlayerData().get(r.getP().getUniqueId()), false));
				handleWave(r.getWave(), r);
				GameUtil.setGameInventory(r.getP());
			}
		});
	}

	public Location generateSpawnLocation(int upperBound, int lowerBound, Region r) {
		int x = 1, y = 1, z = 1;
		if (GameUtil.generateRandomBoolean()) {
			x = -1;
		}
		if (GameUtil.generateRandomBoolean()) {
			y = -1;
		}
		if (GameUtil.generateRandomBoolean()) {
			z = -1;
		}
		Location loc = new Location(r.getP().getWorld(), x * GameUtil.generateRandomInt(upperBound, lowerBound),
				y * GameUtil.generateRandomInt(10, 0), z * GameUtil.generateRandomInt(upperBound, lowerBound))
				.add(r.getCenterPoint());
		return loc;
				
	}

	private void doGeneration(EntityType e, int upperBound, int lowerBound, Region r) {
		boolean baby = false;
		for (int i = 0; i < 5; i++) {
			if (r.getWave() >= 4) {
				baby = true;
			}
			if (entityHandler.constructOwnedEntityWithPlatform(e, generateSpawnLocation(upperBound, lowerBound, r),
					r.getP(), baby, r) == false) {
				i--;
			}
		}
	}

	public void handleWave(int wave, Region r) {
		switch (wave) {
		case (1):
			doGeneration(EntityType.ZOMBIE, 10, 5, r);
			break;
		case (2):
			doGeneration(EntityType.ZOMBIE, 18, 14, r);
			break;
		case (3):
			doGeneration(EntityType.ZOMBIE, 20, 15, r);
			break;
		case (4):
			doGeneration(EntityType.ZOMBIE, 20, 15, r);
			break;
		case (5):
			doGeneration(EntityType.ZOMBIE, 25, 20, r);
			break;
		case (6):
			doGeneration(EntityType.ZOMBIE, 30, 25, r);
			break;
		case (7):
			doGeneration(EntityType.ZOMBIE, 35, 30, r);
			break;
		case (8):
			doGeneration(EntityType.ZOMBIE, 40, 35, r);
			break;
		case (9):
			doGeneration(EntityType.ZOMBIE, 45, 40, r);
			break;
		case (10):
			doGeneration(EntityType.ZOMBIE, 50, 45, r);
			break;
		case (11):
			endMatch(r, "win");
			break;
		}
	}

	public void endMatch(Region r, String s) {
		if (s.equals("lose")) {
			r.getP().sendMessage(
					GameUtil.addColor("&cYou missed! You have completed &f" + (r.getWave() - 1) + " &cwaves!"));
			GameUtil.sendTitleAndSubtitle(r.getP(), "", GameUtil.addColor("&cMissed!"), 50, 5, 15);
			defaultEndTasks(r, true, false);
			if (r.getWave() - 1 > 0) { // Lost on first round
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"crates give " + r.getP().getName() + " Daily " + (r.getWave() - 1));
			}
			entityHandler.getEntityPlayerRelations().remove(r.getP().getUniqueId());
		} else if (s.equals("win")) {
			r.getP().sendMessage(GameUtil.addColor("&7Holy crap, you’ve completed all &f10 &7waves!"));
			defaultEndTasks(r, true, false);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crates give " + r.getP().getName() + " Daily 10");
			entityHandler.getEntityPlayerRelations().remove(r.getP().getUniqueId());
		} else if (s.equals("fail_confirmation")) {
			defaultEndTasks(r, false, false);
		} else if (s.equals("quit")) {
			if (r.getAsyncAwaitTask() != null) {
				r.getAsyncAwaitTask().cancel();
			}
			defaultEndTasks(r, true, false);
			entityHandler.getEntityPlayerRelations().remove(r.getP().getUniqueId());
		} 
//			else if (s.equals("plugin_disable")) {
//			r.getP().getInventory().clear();
//			r.getP().teleport(cachedData.get(r.getP().getUniqueId()).getLoc());
//			r.getP().getInventory().setContents(cachedData.get(r.getP().getUniqueId()).getInventory());
//			if (entityHandler.getEntityPlayerRelations().containsKey(r.getP().getUniqueId())) {
//				for (Map.Entry<UUID, PointData> entry : entityHandler.getEntityPlayerRelations()
//						.get(r.getP().getUniqueId()).getBlockEntityMapping().entrySet()) {
//					entry.getValue().getE().remove();
//					entry.getValue().getB().setType(Material.AIR);
//				}
//			}
//		}
	}

	private void defaultEndTasks(Region r, boolean handleEntities, boolean concurrency) {
		r.getP().getInventory().clear();
		r.getCenterPoint().getBlock().setType(Material.AIR);
		r.getP().teleport(cachedData.get(r.getP().getUniqueId()).getLoc());
		r.getP().getInventory().setContents(cachedData.get(r.getP().getUniqueId()).getInventory());
		cachedData.remove(r.getP().getUniqueId());
		if (!concurrency) {
			inUseRegions.remove(UUID.nameUUIDFromBytes(r.getLocString().getBytes()));
		}
		playerMappedRegions.remove(r.getP().getUniqueId());
		if (entityHandler.getEntityPlayerRelations().containsKey(r.getP().getUniqueId())) {
			for (Map.Entry<UUID, PointData> entry : entityHandler.getEntityPlayerRelations()
					.get(r.getP().getUniqueId()).getBlockEntityMapping().entrySet()) {
				entry.getValue().getE().remove();
				entry.getValue().getB().setType(Material.AIR);
			}
		}	
	}

	public Map<UUID, Boolean> getPendingConfirmationSet() {
		return pendingConfirmations;
	}

	public Map<UUID, Region> getInUseRegions() {
		return inUseRegions;
	}

	public void setChildHandlers(EntityHandler e) {
		this.entityHandler = e;
	}

	public Map<UUID, Region> getPlayerMappedRegions() {
		return playerMappedRegions;
	}


}
