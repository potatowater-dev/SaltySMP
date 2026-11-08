package com.saltysmp.saltcoins.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PluginKeys {

    public final NamespacedKey killToken;
    public final NamespacedKey uniqueCurrency;
    public final NamespacedKey tokenId;

    public PluginKeys(Plugin plugin) {
        this.killToken = new NamespacedKey(plugin, "kill_token");
        this.uniqueCurrency = new NamespacedKey(plugin, "unique_currency");
        this.tokenId = new NamespacedKey(plugin, "token_id");
    }
}
