package com.saltysmp.saltmine.config;

import com.saltysmp.saltmine.SaltMinePlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PluginConfig {

    private final SaltMinePlugin plugin;
    private int resetIntervalMinutes;
    private int blocksPerTick;
    private boolean announceResets;
    private boolean cancelBreakOutsideMines;
    private Material wandMaterial;
    private String wandDisplayName;
    private List<String> wandLore;

    public PluginConfig(SaltMinePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        resetIntervalMinutes = Math.max(1, config.getInt("reset-interval-minutes", 15));
        if (config.contains("regen-interval-seconds") && !config.contains("reset-interval-minutes")) {
            int legacySeconds = config.getInt("regen-interval-seconds", 5);
            resetIntervalMinutes = Math.max(1, legacySeconds / 60);
        }
        blocksPerTick = Math.max(1, config.getInt("blocks-per-tick", 500));
        announceResets = config.getBoolean("announce-resets", true);
        cancelBreakOutsideMines = config.getBoolean("cancel-break-outside-mines", false);

        Material material = Material.matchMaterial(config.getString("wand.material", "GOLDEN_AXE"));
        wandMaterial = material != null ? material : Material.GOLDEN_AXE;
        wandDisplayName = config.getString("wand.display-name", "<gold><bold>Mine Wand");
        wandLore = new ArrayList<>(config.getStringList("wand.lore"));
    }

    public int getResetIntervalMinutes() {
        return resetIntervalMinutes;
    }

    public void setResetIntervalMinutes(int minutes) {
        resetIntervalMinutes = Math.max(1, minutes);
        plugin.getConfig().set("reset-interval-minutes", resetIntervalMinutes);
        plugin.saveConfig();
    }

    public int getBlocksPerTick() {
        return blocksPerTick;
    }

    public boolean isAnnounceResets() {
        return announceResets;
    }

    public boolean isCancelBreakOutsideMines() {
        return cancelBreakOutsideMines;
    }

    public Material getWandMaterial() {
        return wandMaterial;
    }

    public String getWandDisplayName() {
        return wandDisplayName;
    }

    public List<String> getWandLore() {
        return Collections.unmodifiableList(wandLore);
    }
}
