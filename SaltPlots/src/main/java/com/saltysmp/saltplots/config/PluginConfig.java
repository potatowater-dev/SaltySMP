package com.saltysmp.saltplots.config;

import com.saltysmp.saltplots.SaltPlotsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PluginConfig {

    private final SaltPlotsPlugin plugin;
    private String plotWorld;
    private int platformY;
    private int gridGap;
    private boolean gridGapMaxPlot;
    private int gridRowLength;
    private int plotDataVersion;
    private final List<Integer> tierSizes = new ArrayList<>();
    private final List<Integer> upgradeCosts = new ArrayList<>();

    public PluginConfig(SaltPlotsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        plotWorld = config.getString("plot-world", "plotworld");
        platformY = config.getInt("platform-y", 64);
        gridGapMaxPlot = config.getBoolean("grid-gap-max-plot", true);
        gridGap = Math.max(0, config.getInt("grid-gap", 0));
        gridRowLength = config.getInt("grid-row-length", 100);
        plotDataVersion = config.getInt("plot-data-version", 1);

        tierSizes.clear();
        upgradeCosts.clear();
        List<?> tiers = config.getList("tiers");
        if (tiers != null) {
            for (Object entry : tiers) {
                if (entry instanceof java.util.Map<?, ?> map) {
                    Object sizeValue = map.get("size");
                    Object costValue = map.get("upgrade-cost");
                    if (sizeValue instanceof Number sizeNumber) {
                        tierSizes.add(sizeNumber.intValue());
                        int cost = costValue instanceof Number costNumber ? costNumber.intValue() : 0;
                        upgradeCosts.add(cost);
                    }
                }
            }
        }
        if (tierSizes.isEmpty()) {
            tierSizes.addAll(List.of(10, 15, 20, 25));
            upgradeCosts.addAll(List.of(0, 500, 1500, 3000));
        }
    }

    public String getPlotWorld() {
        return plotWorld;
    }

    public int getPlatformY() {
        return platformY;
    }

    public int getGridSpacing() {
        return getMaxTierSize() + getPlotGap();
    }

    public int getPlotGap() {
        if (gridGapMaxPlot) {
            return getMaxTierSize();
        }
        return gridGap;
    }

    public int getPlotDataVersion() {
        return plotDataVersion;
    }

    public void setPlotDataVersion(int version) {
        plotDataVersion = version;
        plugin.getConfig().set("plot-data-version", version);
        plugin.saveConfig();
    }

    public int getGridRowLength() {
        return gridRowLength;
    }

    public List<Integer> getTierSizes() {
        return Collections.unmodifiableList(tierSizes);
    }

    public int getMaxTierSize() {
        return tierSizes.get(tierSizes.size() - 1);
    }

    public int getTierSize(int tierIndex) {
        return tierSizes.get(tierIndex);
    }

    public int getTierCount() {
        return tierSizes.size();
    }

    public int getUpgradeCost(int tierIndex) {
        if (tierIndex < 0 || tierIndex >= upgradeCosts.size()) {
            return 0;
        }
        return upgradeCosts.get(tierIndex);
    }

    public void setUpgradeCost(int tierIndex, int cost) {
        while (upgradeCosts.size() <= tierIndex) {
            upgradeCosts.add(0);
        }
        upgradeCosts.set(tierIndex, cost);
        saveTierCosts();
    }

    private void saveTierCosts() {
        FileConfiguration config = plugin.getConfig();
        List<java.util.Map<String, Object>> tiers = new ArrayList<>();
        for (int index = 0; index < tierSizes.size(); index++) {
            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("size", tierSizes.get(index));
            entry.put("upgrade-cost", upgradeCosts.get(index));
            tiers.add(entry);
        }
        config.set("tiers", tiers);
        plugin.saveConfig();
    }
}
