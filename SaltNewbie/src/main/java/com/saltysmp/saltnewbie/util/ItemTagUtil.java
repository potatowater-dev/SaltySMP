package com.saltysmp.saltnewbie.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class ItemTagUtil {

    private ItemTagUtil() {
    }

    public static void markMinedBy(ItemStack stack, PluginKeys keys, UUID miner) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        var meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(keys.pickupOwner, PersistentDataType.STRING, miner.toString());
        container.remove(keys.deathLoot);
        stack.setItemMeta(meta);
    }

    public static void markDeathLoot(ItemStack stack, PluginKeys keys) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        var meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(keys.deathLoot, PersistentDataType.BYTE, (byte) 1);
        container.remove(keys.pickupOwner);
        container.remove(keys.playerDropped);
        stack.setItemMeta(meta);
    }

    public static void markPlayerDropped(ItemStack stack, PluginKeys keys) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        var meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(keys.playerDropped, PersistentDataType.BYTE, (byte) 1);
        container.remove(keys.deathLoot);
        stack.setItemMeta(meta);
    }

    public static boolean isDeathLoot(ItemStack stack, PluginKeys keys) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        var meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(keys.deathLoot, PersistentDataType.BYTE);
    }

    public static boolean isPlayerDropped(ItemStack stack, PluginKeys keys) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        var meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(keys.playerDropped, PersistentDataType.BYTE);
    }

    public static UUID pickupOwner(ItemStack stack, PluginKeys keys) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        var meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(keys.pickupOwner, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean canProtectedNewbiePickup(ItemStack stack, PluginKeys keys, UUID playerId) {
        if (isDeathLoot(stack, keys)) {
            return false;
        }
        UUID owner = pickupOwner(stack, keys);
        if (owner != null && owner.equals(playerId)) {
            return true;
        }
        return isPlayerDropped(stack, keys);
    }
}
