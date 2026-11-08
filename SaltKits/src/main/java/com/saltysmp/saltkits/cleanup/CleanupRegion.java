package com.saltysmp.saltkits.cleanup;

import org.bukkit.World;

import java.util.function.Predicate;

public record CleanupRegion(
        String id,
        World world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        Predicate<int[]> contains
) {
    public boolean contains(int x, int y, int z) {
        if (x < minX || x > maxX || y < minY || y > maxY || z < minZ || z > maxZ) {
            return false;
        }
        return contains == null || contains.test(new int[]{x, y, z});
    }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public static Predicate<int[]> always() {
        return coords -> true;
    }
}
