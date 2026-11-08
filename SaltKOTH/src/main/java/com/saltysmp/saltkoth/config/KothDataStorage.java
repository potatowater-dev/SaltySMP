package com.saltysmp.saltkoth.config;

import com.saltysmp.saltkoth.SaltKothPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class KothDataStorage {

    private final SaltKothPlugin plugin;
    private final File file;
    private long nextEventStartMs;
    private long eventEndMs;

    public KothDataStorage(SaltKothPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!file.exists()) {
            nextEventStartMs = System.currentTimeMillis() + 10_800_000L;
            eventEndMs = 0L;
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextEventStartMs = yaml.getLong("next-event-start-ms", System.currentTimeMillis());
        eventEndMs = yaml.getLong("event-end-ms", 0L);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-event-start-ms", nextEventStartMs);
        yaml.set("event-end-ms", eventEndMs);
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data.yml", exception);
        }
    }

    public long getNextEventStartMs() {
        return nextEventStartMs;
    }

    public void setNextEventStartMs(long nextEventStartMs) {
        this.nextEventStartMs = nextEventStartMs;
        save();
    }

    public long getEventEndMs() {
        return eventEndMs;
    }

    public void setEventEndMs(long eventEndMs) {
        this.eventEndMs = eventEndMs;
        save();
    }

    public void extendEventEnd(long millis) {
        if (eventEndMs <= 0L) {
            return;
        }
        eventEndMs += millis;
        save();
    }
}
