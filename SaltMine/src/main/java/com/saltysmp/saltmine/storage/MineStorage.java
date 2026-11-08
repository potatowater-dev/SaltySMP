package com.saltysmp.saltmine.storage;

import com.saltysmp.saltmine.SaltMinePlugin;
import com.saltysmp.saltmine.model.MineDefinition;
import com.saltysmp.saltmine.util.RegionUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class MineStorage {

    private final SaltMinePlugin plugin;
    private final File minesFolder;
    private final Map<String, MineDefinition> mines = new HashMap<>();

    public MineStorage(SaltMinePlugin plugin) {
        this.plugin = plugin;
        this.minesFolder = new File(plugin.getDataFolder(), "mines");
    }

    public void load() {
        if (!minesFolder.exists() && !minesFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create mines folder.");
        }
        mines.clear();
        File[] files = minesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4);
            mines.put(id.toLowerCase(), loadFile(id.toLowerCase(), file));
        }
    }

    public MineDefinition get(String id) {
        return mines.get(id.toLowerCase());
    }

    public Collection<MineDefinition> getAll() {
        return Collections.unmodifiableCollection(mines.values());
    }

    public List<String> getIds() {
        return mines.keySet().stream().sorted().toList();
    }

    public MineDefinition getOrCreate(String id) {
        String key = id.toLowerCase();
        MineDefinition existing = mines.get(key);
        if (existing != null) {
            return existing;
        }
        MineDefinition created = new MineDefinition(key);
        mines.put(key, created);
        save(created);
        return created;
    }

    public boolean exists(String id) {
        return mines.containsKey(id.toLowerCase());
    }

    public boolean delete(String id) {
        String key = id.toLowerCase();
        if (!mines.containsKey(key)) {
            return false;
        }
        mines.remove(key);
        File file = new File(minesFolder, key + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete mine file: " + file.getName());
        }
        return true;
    }

    public void save(MineDefinition definition) {
        File file = new File(minesFolder, definition.getId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        RegionUtil.Cuboid region = definition.getRegion();
        if (region != null) {
            yaml.set("world", region.worldName());
            yaml.set("region.min.x", region.minX());
            yaml.set("region.min.y", region.minY());
            yaml.set("region.min.z", region.minZ());
            yaml.set("region.max.x", region.maxX());
            yaml.set("region.max.y", region.maxY());
            yaml.set("region.max.z", region.maxZ());
        }
        Map<String, Integer> blocks = new LinkedHashMap<>();
        for (Map.Entry<Material, Integer> entry : definition.getBlockWeights().entrySet()) {
            blocks.put(entry.getKey().name(), entry.getValue());
        }
        yaml.set("blocks", blocks);
        if (definition.getResetIntervalMinutes() > 0) {
            yaml.set("reset-interval-minutes", definition.getResetIntervalMinutes());
        }
        yaml.set("announce-reset", definition.isAnnounceReset());
        yaml.set("reset-broadcast", definition.getResetBroadcast());
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save mine " + definition.getId(), exception);
        }
    }

    private MineDefinition loadFile(String id, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        MineDefinition definition = new MineDefinition(id);

        String worldName = yaml.getString("world");
        ConfigurationSection minSection = yaml.getConfigurationSection("region.min");
        ConfigurationSection maxSection = yaml.getConfigurationSection("region.max");
        if (worldName != null && minSection != null && maxSection != null) {
            definition.setRegion(new RegionUtil.Cuboid(
                    worldName,
                    minSection.getInt("x"),
                    minSection.getInt("y"),
                    minSection.getInt("z"),
                    maxSection.getInt("x"),
                    maxSection.getInt("y"),
                    maxSection.getInt("z")
            ));
        }

        ConfigurationSection blocksSection = yaml.getConfigurationSection("blocks");
        if (blocksSection != null) {
            for (String key : blocksSection.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null || !material.isBlock()) {
                    continue;
                }
                int weight = blocksSection.getInt(key);
                if (weight > 0) {
                    definition.setBlockWeight(material, weight);
                }
            }
        }
        definition.setResetIntervalMinutes(yaml.getInt("reset-interval-minutes", 0));
        definition.setAnnounceReset(yaml.getBoolean("announce-reset", true));
        definition.setResetBroadcast(yaml.getString("reset-broadcast"));
        return definition;
    }
}
