package io.flixion.game;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.flixion.accuracy.handlers.AccuracyHandler;
import io.flixion.accuracy.handlers.CommandHandler;
import io.flixion.accuracy.handlers.EntityHandler;
import io.flixion.accuracy.handlers.PlayerHandler;
import io.flixion.parkour.handler.ParkourHandler;
import io.flixion.persist.LoadPlayerManager;

public class Game extends JavaPlugin implements Listener {
	private static Game instance;
	private static Map<UUID, PlayerProfile> playerData = new HashMap<>();
	private static AccuracyHandler accuracyHandler;
	private static ParkourHandler parkourHandler;
	private static YamlConfiguration dataFile;
	private static File f;

	public static Game getPL() {
		return instance;
	}

	public static Map<UUID, PlayerProfile> getPlayerData() {
		return playerData;
	}

	public static AccuracyHandler getGameHandler() {
		return accuracyHandler;
	}
	
	public static ParkourHandler getParkourHandler() {
		return parkourHandler;
	}
	
	public static YamlConfiguration getDataFileManager() {
		return dataFile;
	}
	
	public static void saveFile() {
		try {
			dataFile.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onEnable() {
		instance = this;
		dataFile = initDataFile();

		accuracyHandler = new AccuracyHandler();
		parkourHandler = new ParkourHandler();
		Object command = new CommandHandler();
		EntityHandler entityHandler = new EntityHandler();
		PlayerHandler playerHandler = new PlayerHandler();
		accuracyHandler.setChildHandlers(entityHandler);

		getCommand("daily").setExecutor((CommandExecutor) command);
		getCommand("accept").setExecutor((CommandExecutor) command);
		Bukkit.getPluginManager().registerEvents((Listener) command, this);
		Bukkit.getPluginManager().registerEvents(playerHandler, this);
		Bukkit.getPluginManager().registerEvents(entityHandler, this);
		Bukkit.getPluginManager().registerEvents(this, this);

		this.getLogger().log(Level.INFO, "Successfully Enabled!");
	}
	
	private YamlConfiguration initDataFile() {
		getDataFolder().mkdirs();
		f = new File(getDataFolder(), "data.yml");
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				getLogger().log(Level.SEVERE, "Unable to create data.yml", e.getMessage());
				e.printStackTrace();
			}
		}
		return YamlConfiguration.loadConfiguration(f);
	}
	
	public static void addPlayerData(PlayerProfile p) {
		playerData.put(p.getUUID(), p);
	}
	
	@EventHandler
	public void initPlayerProfile(PlayerJoinEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, new LoadPlayerManager(e.getPlayer().getUniqueId(), false, null, false));
		e.getPlayer().sendMessage("Loaded your player profile!");
	}
	
	@EventHandler
	public void unloadProfile(PlayerQuitEvent e) {
		Bukkit.getScheduler().runTaskAsynchronously(this, new LoadPlayerManager(e.getPlayer().getUniqueId(), true, playerData.get(e.getPlayer().getUniqueId()), true));
	}

//	public void onDisable() {
//		for (Map.Entry<UUID, Region> entry : gameHandler.getPlayerMappedRegions().entrySet()) {
//			gameHandler.endMatch(entry.getValue(), "plugin_disable");
//		}
//	}
}
