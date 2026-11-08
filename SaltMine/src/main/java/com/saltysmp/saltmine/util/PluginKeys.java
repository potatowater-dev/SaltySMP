package com.saltysmp.saltmine.util;

import com.saltysmp.saltmine.SaltMinePlugin;
import org.bukkit.NamespacedKey;

public final class PluginKeys {

    private final NamespacedKey wandKey;

    public PluginKeys(SaltMinePlugin plugin) {
        wandKey = new NamespacedKey(plugin, "mine-wand");
    }

    public NamespacedKey wandKey() {
        return wandKey;
    }
}
