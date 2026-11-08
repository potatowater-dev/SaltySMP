package com.saltysmp.saltplots.service;

import com.saltysmp.saltplots.config.PluginConfig;
import com.saltysmp.saltplots.model.Plot;
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlotBorderService {

    private final PluginConfig pluginConfig;
    private final Map<UUID, UUID> boundPlotOwner = new HashMap<>();

    public PlotBorderService(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void bindToPlot(Player player, Plot plot) {
        if (plot == null) {
            clear(player);
            return;
        }
        boundPlotOwner.put(player.getUniqueId(), plot.getOwnerId());
        applyBorder(player, plot);
    }

    public Plot getBoundPlot(Player player, PlotService plotService) {
        UUID ownerId = boundPlotOwner.get(player.getUniqueId());
        if (ownerId == null) {
            return null;
        }
        return plotService.getPlot(ownerId);
    }

    public void clear(Player player) {
        boundPlotOwner.remove(player.getUniqueId());
        player.setWorldBorder(null);
    }

    public void applyBorder(Player player, Plot plot) {
        int size = pluginConfig.getTierSize(plot.getTierIndex());
        double centerX = plot.getOriginX() + size / 2.0;
        double centerZ = plot.getOriginZ() + size / 2.0;

        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(centerX, centerZ);
        border.setSize(size);
        border.setWarningDistance(3);
        border.setDamageAmount(0);
        border.setDamageBuffer(0);
        player.setWorldBorder(border);
    }
}
