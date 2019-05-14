package io.flixion.accuracy.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.flixion.accuracy.region.PointData;
import io.flixion.accuracy.region.Region;
import io.flixion.accuracy.region.RegionMappedLocation;
import io.flixion.game.Game;
import io.flixion.game.GameUtil;

public class EntityHandler implements Listener {
	private Map<UUID, RegionMappedLocation> entityPlayerRelations = new HashMap<>();
	private Map<UUID, UUID> playerMappedArrows = new HashMap<>();
	
	public boolean constructOwnedEntityWithPlatform(EntityType entityType, Location loc, Player ownerPlayer, boolean baby, Region r) {
		Location testValid = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
		testValid.subtract(0, 5, 0);
		for (int i = 0; i < 10; i++) {
			if (r.getActiveRegionBlocks().containsKey(testValid.getBlock().getX() + "," + testValid.getBlock().getY() + "," + testValid.getBlock().getZ())) {
				return false;
			}
			testValid.add(0, 1, 0);
		}
		loc.subtract(0, 2, 0).getBlock().setType(Material.GOLD_BLOCK);
		LivingEntity newEntity = (LivingEntity) loc.getWorld().spawnEntity(loc.add(0, 2, 0), entityType);
		if (entityType == EntityType.ZOMBIE) {
			Zombie z = (Zombie) newEntity;
			if (baby) {
				z.setBaby(true);
			} else {
				z.setBaby(false);
			}
			if (z.isInsideVehicle()) {
				z.getVehicle().remove();
			}
			z.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
		}
		newEntity.setCanPickupItems(false);
		newEntity.setRemoveWhenFarAway(false);
		newEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 128, false, false));
		newEntity.setMetadata("isAccuracyMob", new FixedMetadataValue(Game.getPL(), true));
		if (entityPlayerRelations.containsKey(ownerPlayer.getUniqueId())) {
			entityPlayerRelations.get(ownerPlayer.getUniqueId()).getBlockEntityMapping().put(newEntity.getUniqueId(), new PointData(newEntity, loc.subtract(0, 2, 0).getBlock(), r));
		} else {
			RegionMappedLocation rP = new RegionMappedLocation();
			rP.getBlockEntityMapping().put(newEntity.getUniqueId(), new PointData(newEntity, loc.subtract(0, 2, 0).getBlock(), r));
			entityPlayerRelations.put(ownerPlayer.getUniqueId(), rP);
		}
		r.addBlock(loc.subtract(0, 2, 0).getBlock().getX() + "," + loc.subtract(0, 2, 0).getBlock().getY() + "," + loc.subtract(0, 2, 0).getBlock().getZ(), loc.subtract(0, 2, 0).getBlock());
		r.addEntity(newEntity.getUniqueId(), newEntity);
		return true;
	}
	
	@EventHandler
	public void trackArrow (ProjectileLaunchEvent e) {
		if (e.getEntity().getShooter() instanceof Player) {
			Player p = (Player) e.getEntity().getShooter();
			if (Game.getGameHandler().getPlayerMappedRegions().containsKey(p.getUniqueId())) {
				playerMappedArrows.put(p.getUniqueId(), e.getEntity().getUniqueId());
				Bukkit.getScheduler().runTaskLater(Game.getPL(), new Runnable() { // For servers with small render
																						// distances where the arrow goes
																						// into unloaded chunks
					@Override
					public void run() {
						if (playerMappedArrows.containsKey(p.getUniqueId())) {
							if (playerMappedArrows.get(p.getUniqueId()).equals(e.getEntity().getUniqueId())) {
								playerMappedArrows.remove(p.getUniqueId());
								if (p.getInventory().getItem(8) == null) {
									Game.getGameHandler().endMatch(
											Game.getGameHandler().getPlayerMappedRegions().get(p.getUniqueId()),
											"lose");
								}
							}
						}
					}
				}, 100);
			}
		}
	}
	
	@EventHandler
	public void failSafe(EntityDeathEvent e) {
		if (e.getEntity().hasMetadata("isAccuracyMob")) {
			for (Map.Entry<UUID, Region> entry : Game.getGameHandler().getPlayerMappedRegions().entrySet()) {
				if (entry.getValue().getActiveRegionEntities().containsKey(e.getEntity().getUniqueId())) {
					Region r = entry.getValue();
					entityPlayerRelations.get(r.getP().getUniqueId()).getBlockEntityMapping().get(e.getEntity().getUniqueId()).getB().setType(Material.AIR);
					entityPlayerRelations.get(r.getP().getUniqueId()).getBlockEntityMapping().remove(e.getEntity().getUniqueId());
					e.getEntity().remove();
					if (entityPlayerRelations.get(r.getP().getUniqueId()).getBlockEntityMapping().size() == 0) {
						Game.getGameHandler().getPlayerMappedRegions().get(r.getP().getUniqueId()).clearPointData();
						Game.getGameHandler().getPlayerMappedRegions().get(r.getP().getUniqueId()).incrementWave();
					}
					break;
				}
			}
		}
	}
	
	@EventHandler
	public void handleMobHit(EntityDamageByEntityEvent e) {
		if (e.getDamager().getLocation().getWorld().getName().equals("daily")) {
			if (e.getDamager() instanceof Arrow) {
				Arrow arrow = (Arrow) e.getDamager();
				if (arrow.getShooter() instanceof Player) {
					Player p = (Player) arrow.getShooter();
					if (!Game.getGameHandler().getPlayerMappedRegions().containsKey(p.getUniqueId())) {
						return;
					}
					if (!entityPlayerRelations.containsKey(p.getUniqueId())) {
						e.setCancelled(true); //Prevent players not in games from hitting entities
					} else {
						if (!entityPlayerRelations.get(p.getUniqueId()).getBlockEntityMapping().containsKey(e.getEntity().getUniqueId())) {
							e.setCancelled(true); //Prevent other players in games from hitting other game entities
						} else {
							playerMappedArrows.remove(p.getUniqueId());
							p.getInventory().setItem(8, new ItemStack(Material.ARROW, 1));
							p.playSound(p.getLocation(), Sound.ORB_PICKUP, 100, 0);
							GameUtil.sendTitleAndSubtitle(p, "", GameUtil.addColor("&aHit!"), 1, 15, 4);
							entityPlayerRelations.get(p.getUniqueId()).getBlockEntityMapping().get(e.getEntity().getUniqueId()).getB().setType(Material.AIR);
							entityPlayerRelations.get(p.getUniqueId()).getBlockEntityMapping().remove(e.getEntity().getUniqueId());
							e.getEntity().remove();
							if (entityPlayerRelations.get(p.getUniqueId()).getBlockEntityMapping().size() == 0) {
								Game.getGameHandler().getPlayerMappedRegions().get(p.getUniqueId()).clearPointData();
								Game.getGameHandler().getPlayerMappedRegions().get(p.getUniqueId()).incrementWave();
							}
						}
					}
				}
			}
		}
	}

	public Map<UUID, RegionMappedLocation> getEntityPlayerRelations() {
		return entityPlayerRelations;
	}
}
