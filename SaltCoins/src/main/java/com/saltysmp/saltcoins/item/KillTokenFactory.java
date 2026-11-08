package com.saltysmp.saltcoins.item;

import com.saltysmp.saltcoins.config.PluginConfig;
import com.saltysmp.saltcoins.util.MessageUtil;
import com.saltysmp.saltcoins.util.PluginKeys;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class KillTokenFactory {

    private final PluginConfig config;
    private final PluginKeys keys;

    public KillTokenFactory(PluginConfig config, PluginKeys keys) {
        this.config = config;
        this.keys = keys;
    }

    public ItemStack create() {
        return create(1);
    }

    public ItemStack create(int amount) {
        int stackSize = Math.min(Math.max(1, amount), config.getKillTokenMaxStack());
        ItemStack item = new ItemStack(config.getKillTokenMaterial(), stackSize);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(MessageUtil.parse(config.getKillTokenDisplayName()));
        List<Component> lore = new ArrayList<>();
        for (String line : config.getKillTokenLore()) {
            lore.add(MessageUtil.parse(line));
        }
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }

        if (config.isKillTokenGlint()) {
            meta.setEnchantmentGlintOverride(true);
        }

        if (config.getKillTokenMaxStack() > 1) {
            meta.setMaxStackSize(config.getKillTokenMaxStack());
        }

        meta.getPersistentDataContainer().set(keys.uniqueCurrency, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);

        if (config.getUseCooldownSeconds() > 0) {
            item.setData(
                    DataComponentTypes.USE_COOLDOWN,
                    UseCooldown.useCooldown(config.getUseCooldownSeconds())
                            .cooldownGroup(NamespacedKey.minecraft(config.getUseCooldownGroup()))
                            .build()
            );
        }
        return item;
    }

    public boolean isKillToken(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        var container = meta.getPersistentDataContainer();
        return container.has(keys.uniqueCurrency, PersistentDataType.BYTE)
                || container.has(keys.killToken, PersistentDataType.BYTE);
    }
}
