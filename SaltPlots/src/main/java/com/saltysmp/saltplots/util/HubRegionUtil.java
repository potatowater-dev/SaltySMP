package com.saltysmp.saltplots.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class HubRegionUtil {

    private HubRegionUtil() {
    }

    public static Location centerOf(String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = (minX + maxX) / 2.0 + 0.5;
        double z = (minZ + maxZ) / 2.0 + 0.5;
        double y = Math.max(minY + 1, Math.min(maxY, world.getHighestBlockYAt((int) x, (int) z) + 1));
        return new Location(world, x, y, z);
    }
}
