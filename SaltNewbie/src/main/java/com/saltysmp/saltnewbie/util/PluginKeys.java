package com.saltysmp.saltnewbie.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PluginKeys {

    public final NamespacedKey pickupOwner;
    public final NamespacedKey deathLoot;
    public final NamespacedKey playerDropped;

    public PluginKeys(Plugin plugin) {
        this.pickupOwner = new NamespacedKey(plugin, "pickup_owner");
        this.deathLoot = new NamespacedKey(plugin, "death_loot");
        this.playerDropped = new NamespacedKey(plugin, "player_dropped");
    }
}
