package com.saltysmp.saltshop.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class SpawnedShopNpc {

    private final String shopId;
    private final UUID entityId;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public SpawnedShopNpc(String shopId, UUID entityId, Location location) {
        this.shopId = shopId.toLowerCase();
        this.entityId = entityId;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public SpawnedShopNpc(String shopId, UUID entityId, String worldName, double x, double y, double z, float yaw, float pitch) {
        this.shopId = shopId.toLowerCase();
        this.entityId = entityId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getShopId() {
        return shopId;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}
