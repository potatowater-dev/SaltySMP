package com.saltysmp.saltplots.service;

import com.saltysmp.saltcoins.api.CoinService;
import com.saltysmp.saltplots.config.MessagesConfig;
import com.saltysmp.saltplots.config.PlotStorage;
import com.saltysmp.saltplots.config.PluginConfig;
import com.saltysmp.saltplots.model.HubRegion;
import com.saltysmp.saltplots.model.Plot;
import com.saltysmp.saltplots.util.HubRegionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlotService {

    private final PluginConfig pluginConfig;
    private final PlotStorage plotStorage;
    private final MessagesConfig messages;
    private final PlotGenerationService generationService;
    private final PlotBorderService borderService;
    private final Map<UUID, Location> hubSelectionsPos1 = new HashMap<>();
    private final Map<UUID, Location> hubSelectionsPos2 = new HashMap<>();

    public PlotService(
            PluginConfig pluginConfig,
            PlotStorage plotStorage,
            MessagesConfig messages,
            PlotGenerationService generationService,
            PlotBorderService borderService
    ) {
        this.pluginConfig = pluginConfig;
        this.plotStorage = plotStorage;
        this.messages = messages;
        this.generationService = generationService;
        this.borderService = borderService;
    }

    public PlotBorderService getBorderService() {
        return borderService;
    }

    public boolean isInHub(Player player) {
        HubRegion hub = plotStorage.getHubRegion();
        if (!hub.isConfigured()) {
            return false;
        }
        return hub.contains(player.getLocation());
    }

    public Plot findPlotAt(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        if (!location.getWorld().getName().equals(pluginConfig.getPlotWorld())) {
            return null;
        }
        int x = location.getBlockX();
        int z = location.getBlockZ();
        for (Plot plot : plotStorage.getAllPlots()) {
            int size = pluginConfig.getTierSize(plot.getTierIndex());
            if (plot.contains(x, z, size)) {
                return plot;
            }
        }
        return null;
    }

    public Plot getPlot(UUID ownerId) {
        return plotStorage.getPlot(ownerId);
    }

    public boolean createPlot(Player player) {
        if (plotStorage.getPlot(player.getUniqueId()) != null) {
            messages.send(player, "already-has-plot");
            return false;
        }

        World world = Bukkit.getWorld(pluginConfig.getPlotWorld());
        if (world == null) {
            messages.send(player, "plot-world-missing", "world", pluginConfig.getPlotWorld());
            return false;
        }

        int slot = plotStorage.allocateNextSlot();
        int spacing = pluginConfig.getGridSpacing();
        int rowLength = pluginConfig.getGridRowLength();
        int col = slot % rowLength;
        int row = slot / rowLength;
        int originX = col * spacing;
        int originZ = row * spacing;

        Plot plot = new Plot(player.getUniqueId(), originX, originZ, 0);
        generationService.generatePlatform(world, plot);
        plotStorage.putPlot(plot);
        enterPlot(player, plot);
        messages.send(player, "plot-created", "size", String.valueOf(pluginConfig.getTierSize(0)));
        messages.send(player, "privacy-hint");
        return true;
    }

    public boolean teleportToHub(Player player) {
        HubRegion hub = plotStorage.getHubRegion();
        if (!hub.isConfigured()) {
            messages.send(player, "hub-not-set");
            return false;
        }
        Location center = HubRegionUtil.centerOf(
                hub.getWorldName(),
                hub.getMinX(), hub.getMinY(), hub.getMinZ(),
                hub.getMaxX(), hub.getMaxY(), hub.getMaxZ()
        );
        if (center == null) {
            messages.send(player, "plot-world-missing", "world", hub.getWorldName());
            return false;
        }
        borderService.clear(player);
        player.teleport(center);
        messages.send(player, "hub-teleport");
        return true;
    }

    public void sendPlotInfo(Player player) {
        Plot plot = plotStorage.getPlot(player.getUniqueId());
        if (plot == null) {
            messages.send(player, "no-plot");
            return;
        }
        int size = pluginConfig.getTierSize(plot.getTierIndex());
        messages.send(player, "plot-info-header");
        messages.send(player, "plot-info-size", "size", String.valueOf(size));
        messages.send(player, "plot-info-privacy", "privacy", plot.isPrivate() ? "Private" : "Public");
        messages.send(player, "plot-info-trusted", "count", String.valueOf(plot.getTrusted().size()));

        int nextTier = plot.getTierIndex() + 1;
        if (nextTier >= pluginConfig.getTierCount()) {
            messages.send(player, "plot-info-max");
            return;
        }
        int nextSize = pluginConfig.getTierSize(nextTier);
        int cost = pluginConfig.getUpgradeCost(nextTier);
        CoinService coins = resolveCoinService();
        int balance = coins == null ? 0 : coins.count(player);
        messages.send(player, "plot-info-upgrade",
                "size", String.valueOf(nextSize),
                "cost", String.valueOf(cost),
                "balance", String.valueOf(balance));
    }

    public boolean teleportToPlot(Player player) {
        Plot plot = plotStorage.getPlot(player.getUniqueId());
        if (plot == null) {
            messages.send(player, "no-plot");
            return false;
        }
        World world = Bukkit.getWorld(pluginConfig.getPlotWorld());
        if (world == null) {
            messages.send(player, "plot-world-missing", "world", pluginConfig.getPlotWorld());
            return false;
        }
        enterPlot(player, plot);
        messages.send(player, "teleported");
        return true;
    }

    public boolean visitPlot(Player visitor, String ownerName) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerName);
        if (!owner.hasPlayedBefore() && !owner.isOnline()) {
            messages.send(visitor, "visit-not-found", "player", ownerName);
            return false;
        }
        Plot plot = plotStorage.getPlot(owner.getUniqueId());
        if (plot == null) {
            messages.send(visitor, "visit-no-plot", "player", ownerName);
            return false;
        }
        if (!canVisit(visitor, plot)) {
            messages.send(visitor, "visit-private", "player", ownerName);
            return false;
        }
        World world = Bukkit.getWorld(pluginConfig.getPlotWorld());
        if (world == null) {
            messages.send(visitor, "plot-world-missing", "world", pluginConfig.getPlotWorld());
            return false;
        }
        enterPlot(visitor, plot);
        messages.send(visitor, "visit-success", "player", ownerName);
        return true;
    }

    public void enterPlot(Player player, Plot plot) {
        Location location = generationService.getTeleportLocation(
                Bukkit.getWorld(pluginConfig.getPlotWorld()),
                plot
        );
        if (location == null || location.getWorld() == null) {
            return;
        }
        player.teleport(location);
        borderService.bindToPlot(player, plot);
    }

    public void setPrivacy(Player player, boolean isPrivate) {
        Plot plot = plotStorage.getPlot(player.getUniqueId());
        if (plot == null) {
            messages.send(player, "no-plot");
            return;
        }
        plot.setPrivate(isPrivate);
        plotStorage.updatePlot(plot);
        messages.send(player, isPrivate ? "privacy-private" : "privacy-public");
        messages.send(player, "privacy-hint");
    }

    public void addTrusted(Player owner, String targetName) {
        Plot plot = requireOwnedPlot(owner);
        if (plot == null) {
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            messages.send(owner, "trusted-player-offline", "player", targetName);
            return;
        }
        UUID targetId = target.getUniqueId();
        if (targetId.equals(owner.getUniqueId())) {
            messages.send(owner, "cannot-trust-self");
            return;
        }
        if (plot.isTrusted(targetId)) {
            messages.send(owner, "trusted-already", "player", targetName);
            return;
        }
        plot.addTrusted(targetId);
        plotStorage.updatePlot(plot);
        messages.send(owner, "trusted-added", "player", targetName);
    }

    public void removeTrusted(Player owner, String targetName) {
        Plot plot = requireOwnedPlot(owner);
        if (plot == null) {
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        UUID targetId = target != null
                ? target.getUniqueId()
                : Bukkit.getOfflinePlayer(targetName).getUniqueId();
        if (!plot.isTrusted(targetId)) {
            messages.send(owner, "trusted-not-found", "player", targetName);
            return;
        }
        plot.removeTrusted(targetId);
        plotStorage.updatePlot(plot);
        messages.send(owner, "trusted-removed", "player", targetName);
    }

    public boolean upgradePlot(Player player) {
        Plot plot = requireOwnedPlot(player);
        if (plot == null) {
            return false;
        }
        int nextTier = plot.getTierIndex() + 1;
        if (nextTier >= pluginConfig.getTierCount()) {
            messages.send(player, "upgrade-max");
            return false;
        }

        int cost = pluginConfig.getUpgradeCost(nextTier);
        CoinService coinService = resolveCoinService();
        if (coinService == null) {
            messages.send(player, "coins-unavailable");
            return false;
        }
        if (!coinService.has(player, cost)) {
            messages.send(player, "upgrade-no-coins",
                    "cost", String.valueOf(cost),
                    "balance", String.valueOf(coinService.count(player)));
            return false;
        }
        if (!coinService.withdraw(player, cost)) {
            messages.send(player, "upgrade-no-coins",
                    "cost", String.valueOf(cost),
                    "balance", String.valueOf(coinService.count(player)));
            return false;
        }

        World world = Bukkit.getWorld(pluginConfig.getPlotWorld());
        if (world == null) {
            messages.send(player, "plot-world-missing", "world", pluginConfig.getPlotWorld());
            return false;
        }

        plot.setTierIndex(nextTier);
        generationService.generatePlatform(world, plot);
        plotStorage.updatePlot(plot);
        borderService.bindToPlot(player, plot);

        int size = pluginConfig.getTierSize(nextTier);
        messages.send(player, "upgrade-success",
                "size", String.valueOf(size),
                "cost", String.valueOf(cost));
        return true;
    }

    public void setHubPos1(Player player, Location location) {
        hubSelectionsPos1.put(player.getUniqueId(), location.clone());
        messages.send(player, "sethub-pos1");
    }

    public void setHubPos2(Player player, Location location) {
        hubSelectionsPos2.put(player.getUniqueId(), location.clone());
        messages.send(player, "sethub-pos2");
    }

    public boolean confirmHub(Player player) {
        Location pos1 = hubSelectionsPos1.get(player.getUniqueId());
        Location pos2 = hubSelectionsPos2.get(player.getUniqueId());
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
            messages.send(player, "sethub-missing-selection");
            return false;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            messages.send(player, "sethub-missing-selection");
            return false;
        }
        HubRegion region = new HubRegion(pos1.getWorld().getName(), pos1, pos2);
        plotStorage.setHubRegion(region);
        hubSelectionsPos1.remove(player.getUniqueId());
        hubSelectionsPos2.remove(player.getUniqueId());
        messages.send(player, "sethub-saved");
        return true;
    }

    public boolean setUpgradePrice(int tierIndex, int cost) {
        if (tierIndex < 1 || tierIndex >= pluginConfig.getTierCount()) {
            return false;
        }
        pluginConfig.setUpgradeCost(tierIndex, cost);
        return true;
    }

    public boolean isOwner(Player player, Plot plot) {
        return plot.getOwnerId().equals(player.getUniqueId());
    }

    public boolean isTrusted(Player player, Plot plot) {
        return plot.isTrusted(player.getUniqueId());
    }

    public boolean canBuild(Player player, Plot plot) {
        if (plot == null) {
            return false;
        }
        return isOwner(player, plot) || isTrusted(player, plot);
    }

    public boolean canInteractContainer(Player player, Plot plot) {
        return canBuild(player, plot);
    }

    public boolean canVisit(Player player, Plot plot) {
        if (plot == null) {
            return true;
        }
        if (isOwner(player, plot) || isTrusted(player, plot)) {
            return true;
        }
        return !plot.isPrivate();
    }

    public Location getPlotTeleportLocation(Plot plot) {
        World world = Bukkit.getWorld(pluginConfig.getPlotWorld());
        if (world == null) {
            return null;
        }
        return generationService.getTeleportLocation(world, plot);
    }

    private Plot requireOwnedPlot(Player player) {
        Plot plot = plotStorage.getPlot(player.getUniqueId());
        if (plot == null) {
            messages.send(player, "no-plot");
            return null;
        }
        return plot;
    }

    private CoinService resolveCoinService() {
        RegisteredServiceProvider<CoinService> registration =
                Bukkit.getServicesManager().getRegistration(CoinService.class);
        if (registration == null) {
            return null;
        }
        return registration.getProvider();
    }
}
