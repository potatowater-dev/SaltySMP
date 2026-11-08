package com.saltysmp.saltkits.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class KitGuiKeys {

    private static NamespacedKey guidePane;

    private KitGuiKeys() {
    }

    public static void register(Plugin plugin) {
        guidePane = new NamespacedKey(plugin, "kit_guide_pane");
    }

    public static NamespacedKey guidePane() {
        return guidePane;
    }
}
