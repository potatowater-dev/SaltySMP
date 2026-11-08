package com.saltysmp.saltnewbie.service;

import com.saltysmp.saltnewbie.SaltNewbiePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class NewbieService {

    public static final long PROTECTION_SECONDS = 2 * 3600L;
    public static final long REMOVE_CONFIRM_SECONDS = 30L;

    private final SaltNewbiePlugin plugin;
    private final Map<UUID, Long> expiryMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pendingRemoveMillis = new ConcurrentHashMap<>();

    public NewbieService(SaltNewbiePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean isProtected(UUID playerId) {
        Long expiry = expiryMillis.get(playerId);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiry) {
            expiryMillis.remove(playerId);
            saveAsync();
            return false;
        }
        return true;
    }

    public OptionalLong remainingSeconds(UUID playerId) {
        Long expiry = expiryMillis.get(playerId);
        if (expiry == null) {
            return OptionalLong.empty();
        }
        long remaining = (expiry - System.currentTimeMillis() + 999L) / 1000L;
        if (remaining <= 0) {
            expiryMillis.remove(playerId);
            saveAsync();
            return OptionalLong.empty();
        }
        return OptionalLong.of(remaining);
    }

    public boolean activate(UUID playerId) {
        if (isProtected(playerId)) {
            return false;
        }
        expiryMillis.put(playerId, System.currentTimeMillis() + (PROTECTION_SECONDS * 1000L));
        pendingRemoveMillis.remove(playerId);
        saveAsync();
        return true;
    }

    public enum RemoveResult {
        NOT_PROTECTED,
        AWAITING_CONFIRM,
        EXPIRED,
        CONFIRMED
    }

    public RemoveResult tryRemove(UUID playerId) {
        if (!isProtected(playerId)) {
            return RemoveResult.NOT_PROTECTED;
        }
        long now = System.currentTimeMillis();
        Long pending = pendingRemoveMillis.get(playerId);
        if (pending != null && now - pending <= REMOVE_CONFIRM_SECONDS * 1000L) {
            remove(playerId);
            return RemoveResult.CONFIRMED;
        }
        if (pending != null) {
            pendingRemoveMillis.put(playerId, now);
            return RemoveResult.EXPIRED;
        }
        pendingRemoveMillis.put(playerId, now);
        return RemoveResult.AWAITING_CONFIRM;
    }

    public void remove(UUID playerId) {
        expiryMillis.remove(playerId);
        pendingRemoveMillis.remove(playerId);
        saveAsync();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("players")) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                long expiry = config.getLong("players." + key);
                if (expiry > now) {
                    expiryMillis.put(id, expiry);
                }
            } catch (IllegalArgumentException ignored) {
                // skip bad uuid
            }
        }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    private void saveSync() {
        File file = new File(plugin.getDataFolder(), "data.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : expiryMillis.entrySet()) {
            if (entry.getValue() > System.currentTimeMillis()) {
                config.set("players." + entry.getKey().toString(), entry.getValue());
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not save data.yml", exception);
        }
    }

    public void shutdown() {
        saveSync();
    }
}
