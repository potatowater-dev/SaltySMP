package com.saltysmp.saltkoth.config;

import com.saltysmp.saltkoth.SaltKothPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class WinStatsStorage {

    private final SaltKothPlugin plugin;
    private final File file;
    private final Map<UUID, Integer> wins = new HashMap<>();
    private String lastWinnerName = "—";
    private int lastWinnerSeconds;

    public WinStatsStorage(SaltKothPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void load() {
        wins.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        lastWinnerName = yaml.getString("last-winner.name", "—");
        lastWinnerSeconds = yaml.getInt("last-winner.seconds", 0);
        var section = yaml.getConfigurationSection("wins");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                wins.put(UUID.fromString(key), section.getInt(key, 0));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("last-winner.name", lastWinnerName);
        yaml.set("last-winner.seconds", lastWinnerSeconds);
        for (Map.Entry<UUID, Integer> entry : wins.entrySet()) {
            yaml.set("wins." + entry.getKey(), entry.getValue());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", exception);
        }
    }

    public void recordWin(UUID playerId, String playerName, int seconds) {
        wins.merge(playerId, 1, Integer::sum);
        lastWinnerName = playerName;
        lastWinnerSeconds = seconds;
        save();
    }

    public String getLastWinnerName() {
        return lastWinnerName;
    }

    public int getLastWinnerSeconds() {
        return lastWinnerSeconds;
    }

    public List<Map.Entry<UUID, Integer>> topWins(int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(wins.entrySet());
        list.sort(Map.Entry.<UUID, Integer>comparingByValue().reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public int getWins(UUID playerId) {
        return wins.getOrDefault(playerId, 0);
    }
}
