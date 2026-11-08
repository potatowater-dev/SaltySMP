package com.saltysmp.saltmine.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public final class RegionUtil {

    private RegionUtil() {
    }

    public record Cuboid(String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public static Cuboid fromLocations(Location pos1, Location pos2) {
            if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
                throw new IllegalArgumentException("Both positions must be set in a world.");
            }
            if (!pos1.getWorld().equals(pos2.getWorld())) {
                throw new IllegalArgumentException("Both positions must be in the same world.");
            }
            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            return new Cuboid(pos1.getWorld().getName(), minX, minY, minZ, maxX, maxY, maxZ);
        }

        public long volume() {
            long dx = (long) maxX - minX + 1;
            long dy = (long) maxY - minY + 1;
            long dz = (long) maxZ - minZ + 1;
            return dx * dy * dz;
        }

        public boolean contains(Location location) {
            if (location == null || location.getWorld() == null) {
                return false;
            }
            if (!location.getWorld().getName().equals(worldName)) {
                return false;
            }
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        public boolean contains(Block block) {
            return contains(block.getLocation());
        }

        public String minLabel() {
            return minX + ", " + minY + ", " + minZ;
        }

        public String maxLabel() {
            return maxX + ", " + maxY + ", " + maxZ;
        }
    }

    public record BlockSnapshot(int x, int y, int z) {
    }

    public static List<BlockSnapshot> collectAllBlocks(Cuboid cuboid) {
        List<BlockSnapshot> blocks = new ArrayList<>();
        for (int x = cuboid.minX(); x <= cuboid.maxX(); x++) {
            for (int y = cuboid.minY(); y <= cuboid.maxY(); y++) {
                for (int z = cuboid.minZ(); z <= cuboid.maxZ(); z++) {
                    blocks.add(new BlockSnapshot(x, y, z));
                }
            }
        }
        return blocks;
    }
}
