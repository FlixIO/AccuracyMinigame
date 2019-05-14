package io.flixion.game;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction;
import net.minecraft.server.v1_8_R3.PlayerConnection;

public class GameUtil {
	private static Random random = new Random();
	
	public static String addColor(String m) {
		return ChatColor.translateAlternateColorCodes('&', m);
	}
	
	public static String stripColor(String m) {
		return ChatColor.stripColor(m);
	}
	
	public static void setGameInventory (Player p) {
		p.getInventory().clear();
		ItemStack bow = new ItemStack(Material.BOW);
		bow.getItemMeta().spigot().setUnbreakable(true);
		p.getInventory().setItem(0, bow);
		p.getInventory().setItem(8, new ItemStack(Material.ARROW, 1));
		p.updateInventory();
	}
	
	public static int generateRandomInt(int upperBound, int lowerBound) {
		return random.nextInt(upperBound - lowerBound + 1) + lowerBound;
	}
	
	public static boolean generateRandomBoolean() {
		return random.nextBoolean();
	}
	
	public static void sendTitleAndSubtitle(Player p, String title, String subtitle, int stay, int fadeIn, int fadeOut) {
		PacketPlayOutTitle timePacket = new PacketPlayOutTitle(fadeIn, stay, fadeOut);
        PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, ChatSerializer.a("{'text': '" + subtitle + "'}"));
        PacketPlayOutTitle titlePacket = new PacketPlayOutTitle(EnumTitleAction.TITLE, ChatSerializer.a("{'text': '" + title + "'}"), fadeIn, stay, fadeOut);
        PlayerConnection con = ((CraftPlayer) p).getHandle().playerConnection;
        con.sendPacket(timePacket);
        con.sendPacket(titlePacket);
        con.sendPacket(subtitlePacket);
	}
	
	public static ItemStack nullPane() {
		ItemStack i = new ItemStack(Material.STAINED_GLASS_PANE);
		i.setDurability((short) 15);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(" ");
		i.setItemMeta(meta);
		return i;
	}
	
	public static CardinalDirection returnNearestCardinalDirection(float yaw) { //8 points
		if (yaw >= 0 && yaw < 22.5) {
			return CardinalDirection.SOUTH;
		} else if (yaw >= 22.5 && yaw < 67.5) {
			return CardinalDirection.SOUTH_WEST;
		} else if (yaw >= 67.5 && yaw < 112.5) {
			return CardinalDirection.WEST;
		} else if (yaw >= 112.5 && yaw < 157.5) {
			return CardinalDirection.NORTH_WEST;
		} else if (yaw >= 157.5 && yaw <= 180) {
			return CardinalDirection.NORTH;
		} else if (yaw > -180 && yaw < -157.5) {
			return CardinalDirection.NORTH;
		} else if (yaw >= -157.5 && yaw < -112.5) {
			return CardinalDirection.NORTH_EAST;
		} else if (yaw >= -112.5 && yaw < -67.5) {
			return CardinalDirection.EAST;
		} else if (yaw >= -67.5 && yaw < -22.5) {
			return CardinalDirection.SOUTH_EAST;
		} else if (yaw >= -22.5 && yaw < 0) {
			return CardinalDirection.SOUTH;
		} else {
			return CardinalDirection.NORTH;
		}
	}
	
	public enum CardinalDirection {
		NORTH,
		SOUTH,
		EAST,
		WEST,
		NORTH_EAST,
		NORTH_WEST,
		SOUTH_EAST,
		SOUTH_WEST
	}
	
	public static float generateRandomPlayerYaw() {
		if (generateRandomBoolean()) {
			return -random.nextInt(181);
		} else {
			return random.nextInt(181);
		}
	}
}
