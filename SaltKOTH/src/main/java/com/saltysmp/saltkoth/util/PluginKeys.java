package com.saltysmp.saltkoth.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PluginKeys {

    public final NamespacedKey titleHologram;

    public PluginKeys(Plugin plugin) {
        this.titleHologram = new NamespacedKey(plugin, "title_hologram");
    }
}
