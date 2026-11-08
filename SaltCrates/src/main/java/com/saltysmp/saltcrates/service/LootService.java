package com.saltysmp.saltcrates.service;

import com.saltysmp.saltcrates.config.PluginConfig;
import com.saltysmp.saltcrates.model.CrateDefinition;
import com.saltysmp.saltcrates.model.LootEntry;
import com.saltysmp.saltcrates.util.MessageUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class LootService {

    private final PluginConfig config;

    public LootService(PluginConfig config) {
        this.config = config;
    }

    public LootEntry roll(CrateDefinition definition) {
        int total = definition.getTotalWeight();
        if (total <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (LootEntry entry : definition.getLoot()) {
            cumulative += entry.getWeight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return definition.getLoot().get(definition.getLoot().size() - 1);
    }

    public boolean wouldOverflow(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        int needed = 1;
        int free = countFreeSlots(player);
        return needed > free;
    }

    public void giveReward(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (config.isWarnOnFullInventory() && wouldOverflow(player, item)) {
            MessageUtil.send(player, "<yellow>⚠ Your inventory is full — overflow will drop on the ground.");
        }

        ItemStack stack = item.clone();
        if (isArmor(stack.getType())) {
            if (tryEquipArmor(player.getInventory(), stack)) {
                return;
            }
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        dropOverflow(player, leftover);
    }

    private boolean tryEquipArmor(PlayerInventory inventory, ItemStack stack) {
        String name = stack.getType().name();
        if (name.endsWith("_HELMET") && isEmpty(inventory.getHelmet())) {
            inventory.setHelmet(stack);
            return true;
        }
        if (name.endsWith("_CHESTPLATE") && isEmpty(inventory.getChestplate())) {
            inventory.setChestplate(stack);
            return true;
        }
        if (name.endsWith("_LEGGINGS") && isEmpty(inventory.getLeggings())) {
            inventory.setLeggings(stack);
            return true;
        }
        if (name.endsWith("_BOOTS") && isEmpty(inventory.getBoots())) {
            inventory.setBoots(stack);
            return true;
        }
        return false;
    }

    private boolean isArmor(org.bukkit.Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private int countFreeSlots(Player player) {
        int free = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (isEmpty(slot)) {
                free++;
            }
        }
        if (isEmpty(player.getInventory().getItemInOffHand())) {
            free++;
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (isEmpty(armor)) {
                free++;
            }
        }
        return free;
    }

    private void dropOverflow(Player player, Map<Integer, ItemStack> leftover) {
        if (leftover.isEmpty()) {
            return;
        }
        Location location = player.getLocation();
        for (ItemStack dropped : leftover.values()) {
            var entity = player.getWorld().dropItem(location.clone().add(0, 0.5, 0), dropped);
            entity.setVelocity(entity.getVelocity().zero());
        }
    }

    public void maybeBroadcast(Player player, CrateDefinition definition, LootEntry entry) {
        if (!definition.isBroadcastEnabled()) {
            return;
        }
        if (entry.getWeight() > definition.getBroadcastThresholdWeight()) {
            return;
        }
        String itemName = PlainTextComponentSerializer.plainText().serialize(entry.getItem().displayName());
        if (itemName.isBlank()) {
            itemName = entry.getItem().getType().name();
        }
        String message = config.getPrefix() + MessageUtil.format(
                config.getBroadcastFormat(),
                "player", player.getName(),
                "item", itemName,
                "crate", definition.getDisplayName()
        );
        Bukkit.getServer().sendMessage(MessageUtil.parse(message));
    }
}
