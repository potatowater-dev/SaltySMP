package com.saltysmp.saltshop.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PluginKeys {

    public final NamespacedKey shopId;
    public final NamespacedKey shopNpc;

    public PluginKeys(Plugin plugin) {
        this.shopId = new NamespacedKey(plugin, "shop_id");
        this.shopNpc = new NamespacedKey(plugin, "shop_npc");
    }
}
