package com.saltysmp.saltshop.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class GuiItems {

    private static org.bukkit.NamespacedKey fillerKey;

    private GuiItems() {
    }

    public static void init(Plugin owningPlugin) {
        fillerKey = new org.bukkit.NamespacedKey(owningPlugin, "gui_filler");
    }

    public static ItemStack filler(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            if (fillerKey != null) {
                meta.getPersistentDataContainer().set(fillerKey, PersistentDataType.BYTE, (byte) 1);
            }
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public static boolean isFiller(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta() || fillerKey == null) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(fillerKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public static ItemStack slotLabel(Material material, String miniName, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(miniName));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            if (fillerKey != null) {
                meta.getPersistentDataContainer().set(fillerKey, PersistentDataType.BYTE, (byte) 2);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSlotLabel(ItemStack item) {
        if (item == null || !item.hasItemMeta() || fillerKey == null) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(fillerKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 2;
    }

    public static boolean isGlassPane(ItemStack item) {
        if (item == null) {
            return false;
        }
        String name = item.getType().name();
        return name.endsWith("_STAINED_GLASS_PANE") || item.getType() == Material.GLASS_PANE;
    }
}
