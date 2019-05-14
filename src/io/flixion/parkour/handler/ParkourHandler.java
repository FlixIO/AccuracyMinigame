package io.flixion.parkour.handler;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import io.flixion.accuracy.region.PlayerData;
import io.flixion.game.Game;
import io.flixion.game.GameState;
import io.flixion.game.GameType;
import io.flixion.game.GameUtil;
import io.flixion.game.GameUtil.CardinalDirection;
import io.flixion.parkour.data.ParkourRegion;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

public class ParkourHandler {
	private Map<UUID, ParkourRegion> inUseRegions = new LinkedHashMap<>();
	private Map<UUID, ParkourRegion> playerMappedRegions = new HashMap<>();
	private Map<UUID, Boolean> pendingConfirmations = new HashMap<>();
	private Map<Integer, Byte> blockDataLevelMap = new HashMap<>();
	private Map<UUID, PlayerData> cachedData = new HashMap<>();
	private static int REGION_OFFSET = 200;
	private World w;
	private PlayerHandler playerHandler = new PlayerHandler();

	@SuppressWarnings("deprecation")
	public ItemStack gameIcon(GameType type, Player p) {
		Material m;
		if (type == GameType.ACCURACY) {
			m = Material.BOW;
		} else {
			m = Material.GOLDEN_APPLE;
		}
		ItemStack i = new ItemStack(m);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(GameUtil.addColor("&8[&6" + WordUtils.capitalize(type.toString().toLowerCase() + "&8]")));
		List<String> lore = new ArrayList<>();
		lore.add(GameUtil.addColor("&bJoin the " + WordUtils.capitalize(type.toString().toLowerCase() + " minigame")));
		if (Game.getPlayerData().get(p.getUniqueId()).getPlayerCooldown().isCooldown(type) != null) {
			Date d = Game.getPlayerData().get(p.getUniqueId()).getPlayerCooldown().isCooldown(type);
			lore.add(GameUtil.addColor("&4Cooldown: &a[" + d.getHours() + "h " + d.getMinutes() + "m" + d.getSeconds() + "s]"));
		} else {
			lore.add(GameUtil.addColor("&4Cooldown: &aFALSE"));
		}
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}


	public ParkourHandler() {
		w = Bukkit.getWorld("daily");
		if (w == null) {
			Bukkit.getPluginManager().disablePlugin(Game.getPL());
			throw new IllegalArgumentException("This world does not exist! - daily");
		}
		blockDataLevelMap.put(1, (byte) 4);
		blockDataLevelMap.put(2, (byte) 2);
		blockDataLevelMap.put(3, (byte) 5);
		blockDataLevelMap.put(4, (byte) 3);
		blockDataLevelMap.put(5, (byte) 8);
		blockDataLevelMap.put(6, (byte) 1);
		blockDataLevelMap.put(7, (byte) 13);
		blockDataLevelMap.put(8, (byte) 6);
		blockDataLevelMap.put(9, (byte) 11);
		blockDataLevelMap.put(10, (byte) 15);
	}

	public ParkourRegion generateRegion(Player p) { // O(n) although I deemed this sufficient since n won't be larger than								// 1000, so basically O(1)
		if (inUseRegions.isEmpty()) { // Special case #1 - No regions
			ParkourRegion rC = new ParkourRegion(0, 0, w, p);
			inUseRegions.put(UUID.nameUUIDFromBytes("0,0".getBytes()), rC);
			playerMappedRegions.put(p.getUniqueId(), rC);
			return rC;
		} else {
			ParkourRegion rC = findAvailableRegion(p);
			inUseRegions.put(UUID.nameUUIDFromBytes(rC.getLocString().getBytes()), rC);
			playerMappedRegions.put(p.getUniqueId(), rC);
			return rC;
		}
	}

	private ParkourRegion findAvailableRegion(Player p) {
		ParkourRegion rC = null;
		for (int x = 0; x > -10000; x += REGION_OFFSET) { // Assuming 10k to be max WorldBorder
			if (rC != null) {
				break;
			}
			for (int z = 0; z > -10000; z += REGION_OFFSET) {
				if (!inUseRegions.containsKey(UUID.nameUUIDFromBytes((x + "," + z).getBytes()))) {
					rC = new ParkourRegion(x, z, w, p);
					break;
				}
			}
		}
		return rC;
	}

	public void initPreGame(ParkourRegion r) {
		Player p = r.getP();
		cachedData.put(p.getUniqueId(), new PlayerData(p.getInventory().getContents().clone(), p.getLocation()));
		p.getInventory().clear();
		p.updateInventory();
		TextComponent confirmBegin = new TextComponent(GameUtil.addColor("&f&lCLICK HERE IF YOU READ IT"));
		p.sendMessage(GameUtil.addColor("&8&m---------------------------------------------"));
		p.sendMessage(GameUtil.addColor("&c&lParkour’s game instructions:"));
		p.sendMessage(GameUtil.addColor(
				"&7You will get teleported on a platform, and your job is to succeed every block jump. Each wave will be more difficult."));
		p.sendMessage(GameUtil.addColor("&7(The better you perform the more keys you will get)."));
		p.sendMessage(GameUtil.addColor("&8&m---------------------------------------------"));
		confirmBegin
				.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new ComponentBuilder("Click Here to begin!").create()));
		confirmBegin.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept"));
		p.spigot().sendMessage(confirmBegin);
		p.sendMessage(GameUtil.addColor("&7(or use: /accept)"));
		r.getCenterPoint().getBlock().setType(Material.QUARTZ_BLOCK);
		p.teleport(new Location(w, r.getCenterPoint().getX(), r.getCenterPoint().getY() + 1, r.getCenterPoint().getZ()));
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
					endMatch(r, GameState.FAIL_CONFIRMATION);
				}
			}
		}, 0, 10);
		r.setAsyncAwaitTask(t);
	}

	private void initMainGame(ParkourRegion r) {
		Bukkit.getScheduler().runTask(Game.getPL(), new Runnable() {

			@Override
			public void run() {
				//Game.getPlayerData().get(r.getP().getUniqueId()).getPlayerCooldown().activate(GameType.PARKOUR);
				//Bukkit.getScheduler().runTaskAsynchronously(Game.getPL(), new LoadPlayerManager(r.getP().getUniqueId(), true, Game.getPlayerData().get(r.getP().getUniqueId()), false));
				r.setWave(1);
				doGeneration(r, r.getWave(), 0);
				playerHandler.addPlayerMonitor(r.getP());
			}
		});
	}
	
	@SuppressWarnings("deprecation")
	public void doGeneration(ParkourRegion r, int wave, int jump) {
		Location currentLocation = r.getP().getLocation();
		Location loc = null;
		int yRandom = GameUtil.generateRandomInt(1, 0);
		if (GameUtil.generateRandomBoolean()) {
			yRandom = -yRandom;
		}
		int yOffset = currentLocation.getBlockY() - 1 + yRandom;
		int xOffset = currentLocation.getBlockX();
		int zOffset = currentLocation.getBlockZ();
//		int x = 1;
//		int z = 1;
//		if (GameUtil.generateRandomBoolean()) {
//			x = -1;
//		}
//		if (GameUtil.generateRandomBoolean()) {
//			z = -1;
//		}
//		int yOffset = currentLocation.getBlockY() - 2 + GameUtil.generateRandomInt(2, 0);
//		int xOffset = currentLocation.getBlockX();
//		int zOffset = currentLocation.getBlockZ();
//		if (wave == 1) {
//			xOffset += 2 * x;
//			zOffset += 2 * z;
//		} else if (wave == 2 || wave == 3) {
//			xOffset += 3 * x;
//			zOffset += 3 * z;
//		} else if (wave == 4 || wave == 5 || wave == 6) {
//			xOffset += 4 * x;
//			zOffset += 4 * z;
//		} else if (wave == 7 || wave == 8 || wave == 9 || wave == 10) {
//			xOffset += 5 * x;
//			zOffset += 5 * z;
//		} else {
//			endMatch(r, GameState.WIN);
//		}
		float randomYaw = GameUtil.generateRandomPlayerYaw();
		CardinalDirection direction = GameUtil.returnNearestCardinalDirection(randomYaw);
		int offset = checkRadius(wave);
		switch (direction) {
		case EAST:
			xOffset += offset;
			break;
		case NORTH:
			zOffset -= offset;
			break;
		case NORTH_EAST:
			xOffset += offset;
			zOffset -= offset;
			break;
		case NORTH_WEST:
			xOffset -= offset;
			zOffset -= offset;
			break;
		case SOUTH:
			zOffset += offset;
			break;
		case SOUTH_EAST:
			xOffset += offset;
			zOffset += offset;
			break;
		case SOUTH_WEST:
			xOffset -= offset;
			zOffset += offset;
			break;
		case WEST:
			xOffset -= offset;
			break;
		}
		loc = new Location(w, xOffset, yOffset, zOffset);
		r.addBlock(loc.getBlock());
		r.setCurrentTargetB(loc.getBlock());
		if (jump >= 2) {
			if (r.getActiveRegionBlocks().size() > 1) {
				r.removeLastBlock().setType(Material.AIR);
			}
		}
		loc.getBlock().setType(Material.STAINED_CLAY);
		loc.getBlock().setData(blockDataLevelMap.get(wave));
	}
	
	private int checkRadius(int wave) {
		if (wave == 1) {
			return 2;
		} else if (wave == 2 || wave == 3) {
			return 3;
		} else if (wave == 4 || wave == 5 || wave == 6) {
			return 4;
		} else if (wave == 7 || wave == 8 || wave == 9 || wave == 10) {
			return 5;
		}
		return -1;
	}

	public void endMatch(ParkourRegion r, GameState s) {
		if (s.equals(GameState.LOSE)) {
			r.getP().sendMessage(
					GameUtil.addColor("&cYou fell! You have completed &f" + (r.getWave() - 1) + " &cwaves!"));
			GameUtil.sendTitleAndSubtitle(r.getP(), "", GameUtil.addColor("&cFell!"), 50, 5, 15);
			defaultEndTasks(r);
			if (r.getWave() - 1 > 0) { // Lost on first round
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"crates give " + r.getP().getName() + " Daily " + (r.getWave() - 1));
			}
		} else if (s.equals(GameState.WIN)) {
			r.getP().sendMessage(GameUtil.addColor("&7Holy crap, you’ve completed all &f10 &7waves!"));
			defaultEndTasks(r);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crates give " + r.getP().getName() + " Daily 10");
		} else if (s.equals(GameState.FAIL_CONFIRMATION)) {
			defaultEndTasks(r);
		} else if (s.equals(GameState.QUIT)) {
			if (r.getAsyncAwaitTask() != null) {
				r.getAsyncAwaitTask().cancel();
			}
			defaultEndTasks(r);
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

	private void defaultEndTasks(ParkourRegion r) {
		r.getP().getInventory().clear();
		r.getCenterPoint().getBlock().setType(Material.AIR);
		r.getP().teleport(cachedData.get(r.getP().getUniqueId()).getLoc());
		r.getP().getInventory().setContents(cachedData.get(r.getP().getUniqueId()).getInventory());
		r.getP().updateInventory();
		inUseRegions.remove(UUID.nameUUIDFromBytes(r.getLocString().getBytes()));
		playerMappedRegions.remove(r.getP().getUniqueId());
		playerHandler.removePlayerMonitor(r.getP());
		for (Block b : r.getActiveRegionBlocks()) {
			b.setType(Material.AIR);
		}
	}

	public Map<UUID, Boolean> getPendingConfirmationSet() {
		return pendingConfirmations;
	}

	public Map<UUID, ParkourRegion> getInUseRegions() {
		return inUseRegions;
	}

	public Map<UUID, ParkourRegion> getPlayerMappedRegions() {
		return playerMappedRegions;
	}


}
