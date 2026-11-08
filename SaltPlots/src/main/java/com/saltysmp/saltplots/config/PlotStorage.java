package com.saltysmp.saltplots.config;

import com.saltysmp.saltplots.SaltPlotsPlugin;
import com.saltysmp.saltplots.model.HubRegion;
import com.saltysmp.saltplots.model.Plot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class PlotStorage {

    private final SaltPlotsPlugin plugin;
    private final File file;
    private final Map<UUID, Plot> plotsByOwner = new HashMap<>();
    private HubRegion hubRegion = new HubRegion();
    private int nextSlot;

    public PlotStorage(SaltPlotsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "plots.yml");
    }

    public void load() {
        plotsByOwner.clear();
        hubRegion = new HubRegion();
        nextSlot = 0;

        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextSlot = yaml.getInt("next-slot", 0);

        ConfigurationSection hubSection = yaml.getConfigurationSection("hub");
        if (hubSection != null) {
            hubRegion.setWorldName(hubSection.getString("world"));
            hubRegion.setMinX(hubSection.getInt("min-x"));
            hubRegion.setMinY(hubSection.getInt("min-y"));
            hubRegion.setMinZ(hubSection.getInt("min-z"));
            hubRegion.setMaxX(hubSection.getInt("max-x"));
            hubRegion.setMaxY(hubSection.getInt("max-y"));
            hubRegion.setMaxZ(hubSection.getInt("max-z"));
        }

        ConfigurationSection plotsSection = yaml.getConfigurationSection("plots");
        if (plotsSection != null) {
            for (String ownerKey : plotsSection.getKeys(false)) {
                try {
                    UUID ownerId = UUID.fromString(ownerKey);
                    ConfigurationSection plotSection = plotsSection.getConfigurationSection(ownerKey);
                    if (plotSection == null) {
                        continue;
                    }
                    int originX = plotSection.getInt("origin-x");
                    int originZ = plotSection.getInt("origin-z");
                    int tierIndex = plotSection.getInt("tier", 0);
                    boolean isPrivate = plotSection.getBoolean("private", false);
                    List<String> trustedKeys = plotSection.getStringList("trusted");
                    List<UUID> trusted = trustedKeys.stream().map(UUID::fromString).toList();
                    plotsByOwner.put(ownerId, new Plot(ownerId, originX, originZ, tierIndex, isPrivate, trusted));
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Skipping invalid plot owner id: " + ownerKey);
                }
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-slot", nextSlot);

        if (hubRegion.isConfigured()) {
            yaml.set("hub.world", hubRegion.getWorldName());
            yaml.set("hub.min-x", hubRegion.getMinX());
            yaml.set("hub.min-y", hubRegion.getMinY());
            yaml.set("hub.min-z", hubRegion.getMinZ());
            yaml.set("hub.max-x", hubRegion.getMaxX());
            yaml.set("hub.max-y", hubRegion.getMaxY());
            yaml.set("hub.max-z", hubRegion.getMaxZ());
        }

        for (Plot plot : plotsByOwner.values()) {
            String path = "plots." + plot.getOwnerId();
            yaml.set(path + ".origin-x", plot.getOriginX());
            yaml.set(path + ".origin-z", plot.getOriginZ());
            yaml.set(path + ".tier", plot.getTierIndex());
            yaml.set(path + ".private", plot.isPrivate());
            yaml.set(path + ".trusted", plot.getTrusted().stream().map(UUID::toString).toList());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save plots.yml", exception);
        }
    }

    public HubRegion getHubRegion() {
        return hubRegion;
    }

    public void setHubRegion(HubRegion hubRegion) {
        this.hubRegion = hubRegion;
        save();
    }

    public int getNextSlot() {
        return nextSlot;
    }

    public int allocateNextSlot() {
        int slot = nextSlot;
        nextSlot++;
        save();
        return slot;
    }

    public Plot getPlot(UUID ownerId) {
        return plotsByOwner.get(ownerId);
    }

    public Collection<Plot> getAllPlots() {
        return Collections.unmodifiableCollection(plotsByOwner.values());
    }

    public void putPlot(Plot plot) {
        plotsByOwner.put(plot.getOwnerId(), plot);
        save();
    }

    public void updatePlot(Plot plot) {
        plotsByOwner.put(plot.getOwnerId(), plot);
        save();
    }

    public void clearAllPlots() {
        plotsByOwner.clear();
        nextSlot = 0;
        save();
    }
}
