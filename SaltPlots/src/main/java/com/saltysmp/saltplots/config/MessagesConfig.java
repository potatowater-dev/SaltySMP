package com.saltysmp.saltplots.config;

import com.saltysmp.saltplots.SaltPlotsPlugin;
import com.saltysmp.saltplots.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class MessagesConfig {

    private final SaltPlotsPlugin plugin;
    private FileConfiguration messages;
    private YamlConfiguration defaults;
    private String prefix;

    public MessagesConfig(SaltPlotsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        defaults = null;
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream != null) {
                defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)
                );
                messages.setDefaults(defaults);
                messages.options().copyDefaults(true);
                mergeMissingKeys(file);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not load messages defaults: " + exception.getMessage());
        }
        prefix = resolveRaw("prefix", "<gray>[<green>Plots<gray>] ");
    }

    private void mergeMissingKeys(File file) {
        if (defaults == null) {
            return;
        }
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key)) {
                continue;
            }
            String current = messages.getString(key);
            String fallback = defaults.getString(key);
            if (current == null || current.isBlank() || current.equals(key)) {
                messages.set(key, fallback);
                changed = true;
            }
        }
        if (changed) {
            try {
                messages.save(file);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not update messages.yml with defaults", exception);
            }
        }
    }

    private String resolveRaw(String key, String fallback) {
        String value = messages.getString(key);
        if (isBroken(value, key) && defaults != null) {
            value = defaults.getString(key);
        }
        if (isBroken(value, key)) {
            return fallback;
        }
        return value;
    }

    private static boolean isBroken(String value, String key) {
        return value == null || value.isBlank() || value.equals(key);
    }

    public String get(String key) {
        return resolveRaw(key, key).replace("{prefix}", prefix);
    }

    public String format(String key, String... replacements) {
        return MessageUtil.format(get(key), replacements);
    }

    public void send(CommandSender sender, String key, String... replacements) {
        MessageUtil.send(sender, format(key, replacements));
    }
}
