package com.saltysmp.saltkoth.service;

import com.saltysmp.saltkoth.SaltKothPlugin;
import com.saltysmp.saltkoth.config.PluginConfig;
import com.saltysmp.saltkoth.util.MessageUtil;
import com.saltysmp.saltkoth.util.PluginKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;

public final class KothHologramService {

    private final SaltKothPlugin plugin;
    private final PluginConfig config;
    private final PluginKeys keys;
    private UUID titleHologramId;

    public KothHologramService(SaltKothPlugin plugin, PluginConfig config, PluginKeys keys) {
        this.plugin = plugin;
        this.config = config;
        this.keys = keys;
    }

    public void load() {
        titleHologramId = null;
        String stored = plugin.getConfig().getString("title-hologram.uuid");
        if (stored == null) {
            return;
        }
        try {
            titleHologramId = UUID.fromString(stored);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void save() {
        if (titleHologramId == null) {
            plugin.getConfig().set("title-hologram.uuid", null);
        } else {
            plugin.getConfig().set("title-hologram.uuid", titleHologramId.toString());
        }
        plugin.saveConfig();
    }

    public boolean spawnTitle(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        removeTitle();
        World world = location.getWorld();
        Location spawnAt = location.clone().add(0, config.getTitleHologramHeight(), 0);
        TextDisplay display = world.spawn(spawnAt, TextDisplay.class, entity -> {
            entity.text(MessageUtil.parse(config.getTitleHologramText()));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(true);
            entity.setShadowed(true);
            entity.setDefaultBackground(false);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setPersistent(true);
            entity.setInvulnerable(true);
            entity.setGravity(false);
            float scale = config.getTitleHologramScale();
            entity.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            ));
            entity.getPersistentDataContainer().set(keys.titleHologram, PersistentDataType.BYTE, (byte) 1);
        });
        titleHologramId = display.getUniqueId();
        save();
        return true;
    }

    public boolean removeTitle() {
        if (titleHologramId == null) {
            return false;
        }
        boolean removed = removeEntity(titleHologramId);
        titleHologramId = null;
        save();
        return removed;
    }

    public boolean hasTitle() {
        if (titleHologramId == null) {
            return false;
        }
        for (World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(titleHologramId);
            if (entity != null && entity.isValid()) {
                return true;
            }
        }
        return false;
    }

    private boolean removeEntity(UUID entityId) {
        for (World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.remove();
                return true;
            }
        }
        return false;
    }
}
