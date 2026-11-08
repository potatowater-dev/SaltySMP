package com.saltysmp.saltkits.kit;

import com.saltysmp.saltkits.SaltKitsPlugin;
import com.saltysmp.saltkits.util.DurationParser;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {

    private final SaltKitsPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public CooldownManager(SaltKitsPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create data folder: " + dataFolder.getAbsolutePath());
        }
    }

    public void loadPlayer(UUID playerId) {
        cache.computeIfAbsent(playerId, this::readPlayerData);
    }

    public void unloadPlayer(UUID playerId) {
        PlayerData data = cache.remove(playerId);
        if (data != null && data.dirty) {
            writePlayerData(playerId, data);
        }
    }

    public boolean isOnCooldown(UUID playerId, String kitName, long cooldownSeconds) {
        if (cooldownSeconds <= 0L) {
            return false;
        }
        long lastClaim = getClaims(playerId).getOrDefault(normalize(kitName), 0L);
        if (lastClaim <= 0L) {
            return false;
        }
        long elapsedSeconds = (System.currentTimeMillis() - lastClaim) / 1000L;
        return elapsedSeconds < cooldownSeconds;
    }

    public long getRemainingSeconds(UUID playerId, String kitName, long cooldownSeconds) {
        long lastClaim = getClaims(playerId).getOrDefault(normalize(kitName), 0L);
        if (lastClaim <= 0L || cooldownSeconds <= 0L) {
            return 0L;
        }
        long elapsedSeconds = (System.currentTimeMillis() - lastClaim) / 1000L;
        return Math.max(cooldownSeconds - elapsedSeconds, 0L);
    }

    public void recordClaim(UUID playerId, String kitName) {
        PlayerData data = getData(playerId);
        data.claims.put(normalize(kitName), System.currentTimeMillis());
        data.dirty = true;
        writePlayerData(playerId, data);
    }

    public boolean hasAutoClaimed(UUID playerId, String kitName) {
        return getData(playerId).autoClaimed.contains(normalize(kitName));
    }

    public void markAutoClaimed(UUID playerId, String kitName) {
        PlayerData data = getData(playerId);
        data.autoClaimed.add(normalize(kitName));
        data.dirty = true;
        writePlayerData(playerId, data);
    }

    public void resetCooldown(UUID playerId, String kitName) {
        PlayerData data = getData(playerId);
        if (kitName.equalsIgnoreCase("all")) {
            data.claims.clear();
        } else {
            data.claims.remove(normalize(kitName));
        }
        data.dirty = true;
        writePlayerData(playerId, data);
    }

    public String formatRemaining(long remainingSeconds) {
        return DurationParser.formatSeconds(remainingSeconds);
    }

    private Map<String, Long> getClaims(UUID playerId) {
        return getData(playerId).claims;
    }

    private PlayerData getData(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::readPlayerData);
    }

    private PlayerData readPlayerData(UUID playerId) {
        File file = getPlayerFile(playerId);
        PlayerData data = new PlayerData();
        if (!file.exists()) {
            return data;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("claims") != null) {
            for (String key : config.getConfigurationSection("claims").getKeys(false)) {
                data.claims.put(key.toLowerCase(Locale.ROOT), config.getLong("claims." + key));
            }
        }
        for (String kit : config.getStringList("auto-claimed")) {
            data.autoClaimed.add(kit.toLowerCase(Locale.ROOT));
        }
        return data;
    }

    private void writePlayerData(UUID playerId, PlayerData data) {
        File file = getPlayerFile(playerId);
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Long> entry : data.claims.entrySet()) {
            config.set("claims." + entry.getKey(), entry.getValue());
        }
        config.set("auto-claimed", data.autoClaimed.stream().toList());
        try {
            config.save(file);
            data.dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player data for " + playerId + ": " + exception.getMessage());
        }
    }

    private File getPlayerFile(UUID playerId) {
        return new File(dataFolder, playerId + ".yml");
    }

    private String normalize(String kitName) {
        return kitName.toLowerCase(Locale.ROOT);
    }

    private static final class PlayerData {
        private final Map<String, Long> claims = new HashMap<>();
        private final java.util.Set<String> autoClaimed = new java.util.HashSet<>();
        private boolean dirty;
    }
}
