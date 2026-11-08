package com.saltysmp.saltplots.service;

import com.saltysmp.saltplots.config.PluginConfig;
import com.saltysmp.saltplots.model.Plot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class PlotGenerationService {

    private static final int STONE_LAYERS = 4;

    private final PluginConfig pluginConfig;

    public PlotGenerationService(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void generatePlatform(World world, Plot plot) {
        int size = pluginConfig.getTierSize(plot.getTierIndex());
        int topY = pluginConfig.getPlatformY();
        int originX = plot.getOriginX();
        int originZ = plot.getOriginZ();

        for (int x = originX; x < originX + size; x++) {
            for (int z = originZ; z < originZ + size; z++) {
                setBlock(world, x, topY, z, Material.GRASS_BLOCK);
                for (int depth = 1; depth <= STONE_LAYERS; depth++) {
                    setBlock(world, x, topY - depth, z, Material.STONE);
                }
            }
        }
    }

    public Location getTeleportLocation(World world, Plot plot) {
        int size = pluginConfig.getTierSize(plot.getTierIndex());
        int y = pluginConfig.getPlatformY();
        return new Location(
                world,
                plot.getOriginX() + size / 2.0 + 0.5,
                y + 1.0,
                plot.getOriginZ() + size / 2.0 + 0.5
        );
    }

    public void clearPlatform(World world, Plot plot) {
        int size = pluginConfig.getMaxTierSize();
        int topY = pluginConfig.getPlatformY();
        int originX = plot.getOriginX();
        int originZ = plot.getOriginZ();

        for (int x = originX; x < originX + size; x++) {
            for (int z = originZ; z < originZ + size; z++) {
                for (int y = topY - STONE_LAYERS; y <= topY; y++) {
                    setBlock(world, x, y, z, Material.AIR);
                }
            }
        }
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(material, false);
    }
}
