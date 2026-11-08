package com.saltysmp.saltkits.cleanup;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class WorldGuardHook {

    private WorldGuardHook() {
    }

    public static boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public static CleanupRegion resolve(String id, World world, String regionName) {
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return null;
        }
        ProtectedRegion region = manager.getRegion(regionName);
        if (region == null) {
            return null;
        }
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new CleanupRegion(
                id,
                world,
                min.x(),
                min.y(),
                min.z(),
                max.x(),
                max.y(),
                max.z(),
                coords -> region.contains(BlockVector3.at(coords[0], coords[1], coords[2]))
        );
    }
}
