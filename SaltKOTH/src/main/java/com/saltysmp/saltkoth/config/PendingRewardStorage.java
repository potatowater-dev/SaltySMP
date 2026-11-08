package com.saltysmp.saltkoth.config;

import com.saltysmp.saltkoth.SaltKothPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class PendingRewardStorage {

    private final SaltKothPlugin plugin;
    private final File file;
    private final Map<UUID, Integer> pendingKeys = new HashMap<>();

    public PendingRewardStorage(SaltKothPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pending-rewards.yml");
    }

    public void load() {
        pendingKeys.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("pending");
        if (section == null) {
            return;
        }
        for (String uuidString : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                int amount = section.getInt(uuidString, 0);
                if (amount > 0) {
                    pendingKeys.put(uuid, amount);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : pendingKeys.entrySet()) {
            yaml.set("pending." + entry.getKey(), entry.getValue());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save pending-rewards.yml", exception);
        }
    }

    public void add(UUID playerId, int amount) {
        if (amount <= 0) {
            return;
        }
        pendingKeys.merge(playerId, amount, Integer::sum);
        save();
    }

    public int takeAll(UUID playerId) {
        Integer amount = pendingKeys.remove(playerId);
        if (amount == null || amount <= 0) {
            return 0;
        }
        save();
        return amount;
    }

    public Map<UUID, Integer> snapshot() {
        return Map.copyOf(pendingKeys);
    }
}
