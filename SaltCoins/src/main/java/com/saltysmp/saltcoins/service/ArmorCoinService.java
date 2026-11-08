package com.saltysmp.saltcoins.service;

import com.saltysmp.saltcoins.config.PluginConfig;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class ArmorCoinService {

    private final PluginConfig config;

    public ArmorCoinService(PluginConfig config) {
        this.config = config;
    }

    public int evaluateAndStrip(Player victim) {
        if (!config.isArmorCoinsEnabled()) {
            return 0;
        }
        PlayerInventory inventory = victim.getInventory();
        int total = 0;
        total += evaluatePiece(inventory.getHelmet(), () -> inventory.setHelmet(null));
        total += evaluatePiece(inventory.getChestplate(), () -> inventory.setChestplate(null));
        total += evaluatePiece(inventory.getLeggings(), () -> inventory.setLeggings(null));
        total += evaluatePiece(inventory.getBoots(), () -> inventory.setBoots(null));
        return total;
    }

    private int evaluatePiece(ItemStack item, Runnable strip) {
        int coins = coinsForPiece(item);
        if (coins > 0) {
            strip.run();
        }
        return coins;
    }

    private int coinsForPiece(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        int protection = item.getEnchantmentLevel(Enchantment.PROTECTION);
        if (protection <= 0) {
            return 0;
        }
        Material type = item.getType();
        if (isNetheriteArmor(type)
                && protection >= config.getNetheriteProtectionMin()) {
            return config.getNetheriteCoinsPerPiece();
        }
        if (isDiamondArmor(type)
                && protection >= config.getDiamondProtectionMin()) {
            return config.getDiamondCoinsPerPiece();
        }
        return 0;
    }

    private boolean isDiamondArmor(Material material) {
        return material == Material.DIAMOND_HELMET
                || material == Material.DIAMOND_CHESTPLATE
                || material == Material.DIAMOND_LEGGINGS
                || material == Material.DIAMOND_BOOTS;
    }

    private boolean isNetheriteArmor(Material material) {
        return material == Material.NETHERITE_HELMET
                || material == Material.NETHERITE_CHESTPLATE
                || material == Material.NETHERITE_LEGGINGS
                || material == Material.NETHERITE_BOOTS;
    }
}
