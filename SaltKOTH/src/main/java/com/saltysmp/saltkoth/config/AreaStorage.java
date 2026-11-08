package com.saltysmp.saltkoth.config;

import com.saltysmp.saltkoth.SaltKothPlugin;
import com.saltysmp.saltkoth.model.KothArea;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class AreaStorage {

    private final SaltKothPlugin plugin;
    private final File file;
    private KothArea area;

    public AreaStorage(SaltKothPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "area.yml");
    }

    public void load() {
        if (!file.exists()) {
            area = null;
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String world = yaml.getString("world");
        if (world == null) {
            area = null;
            return;
        }
        area = new KothArea(
                world,
                yaml.getInt("x1"),
                yaml.getInt("y1"),
                yaml.getInt("z1"),
                yaml.getInt("x2"),
                yaml.getInt("y2"),
                yaml.getInt("z2")
        );
    }

    public void save(KothArea newArea) {
        this.area = newArea;
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("world", newArea.getWorldName());
        yaml.set("x1", newArea.getMinX());
        yaml.set("y1", newArea.getMinY());
        yaml.set("z1", newArea.getMinZ());
        yaml.set("x2", newArea.getMaxX());
        yaml.set("y2", newArea.getMaxY());
        yaml.set("z2", newArea.getMaxZ());
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save area.yml", exception);
        }
    }

    public KothArea getArea() {
        return area;
    }

    public boolean hasArea() {
        return area != null && area.isConfigured();
    }
}
