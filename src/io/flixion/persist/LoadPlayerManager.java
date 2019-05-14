package io.flixion.persist;

import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import io.flixion.game.Cooldown;
import io.flixion.game.Game;
import io.flixion.game.GameType;
import io.flixion.game.PlayerProfile;

public class LoadPlayerManager implements Runnable {
	private UUID u;
	private boolean update, unload;
	private PlayerProfile profile;
	public LoadPlayerManager(UUID u, boolean update, PlayerProfile profile, boolean unload) {
		this.u = u;
		this.update = update;
		this.profile = profile;
		this.unload = unload;
	}

	@Override
	public void run() {
		if (update) {
			update(Game.getDataFileManager(), u, profile);
			if (unload) {
				Game.getPlayerData().remove(u);
			}
		} else {
			if (Game.getDataFileManager().getConfigurationSection("users." + u) != null) {
				GameType type;
				if (!Game.getDataFileManager().getString("users." + u.toString() + ".lastGameType").equals("")) {
					type = GameType.valueOf(Game.getDataFileManager().getString("users." + u.toString() + ".lastGameType"));
				} else {
					type = null;
				}
				long accuracy = Game.getDataFileManager().getLong("users." + u.toString() + ".accuracy");
				long parkour = Game.getDataFileManager().getLong("users." + u.toString() + ".parkour");
				PlayerProfile profile = new PlayerProfile(u, type, new Cooldown(u, accuracy, parkour));
				Game.addPlayerData(profile);
			} else {
				create(Game.getDataFileManager(), u);
				PlayerProfile profile = new PlayerProfile(u, null, new Cooldown(u, 0, 0));
				Game.addPlayerData(profile);
			}
		}
	}
	
	private void update(YamlConfiguration c, UUID u, PlayerProfile profile) {
		c.set(("users." + u.toString() + ".parkour"), profile.getPlayerCooldown().getCooldownForType(GameType.PARKOUR));
		c.set(("users." + u.toString() + ".accuracy"), profile.getPlayerCooldown().getCooldownForType(GameType.ACCURACY));
		if (profile.getLastType() != null) {
			c.set(("users." + u.toString() + ".lastGameType"), profile.getLastType().toString());
		} else {
			c.set(("users." + u.toString() + ".lastGameType"), "");
		}
		Game.saveFile();
	}
	
	private void create(YamlConfiguration c, UUID u) {
		c.set(("users." + u.toString() + "." + GameType.PARKOUR.toString().toLowerCase()), 0);
		c.set(("users." + u.toString() + "." + GameType.ACCURACY.toString().toLowerCase()), 0);
		c.set(("users." + u.toString() + ".lastGameType"), "");
		Game.saveFile();
	}

}
