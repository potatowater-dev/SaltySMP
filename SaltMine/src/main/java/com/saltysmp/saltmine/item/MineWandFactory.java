package com.saltysmp.saltmine.item;

import com.saltysmp.saltmine.config.PluginConfig;
import com.saltysmp.saltmine.util.MessageUtil;
import com.saltysmp.saltmine.util.PluginKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class MineWandFactory {

    private final PluginConfig pluginConfig;
    private final PluginKeys pluginKeys;

    public MineWandFactory(PluginConfig pluginConfig, PluginKeys pluginKeys) {
        this.pluginConfig = pluginConfig;
        this.pluginKeys = pluginKeys;
    }

    public ItemStack createWand() {
        Material material = pluginConfig.getWandMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(MessageUtil.parse(pluginConfig.getWandDisplayName()));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : pluginConfig.getWandLore()) {
            lore.add(MessageUtil.parse(line));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(pluginKeys.wandKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != pluginConfig.getWandMaterial()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(pluginKeys.wandKey(), PersistentDataType.BYTE);
    }
}
