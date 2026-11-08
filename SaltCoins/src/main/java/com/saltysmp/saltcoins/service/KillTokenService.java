package com.saltysmp.saltcoins.service;

import com.saltysmp.saltcoins.config.PluginConfig;
import com.saltysmp.saltcoins.item.KillTokenFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class KillTokenService {

    private final PluginConfig config;
    private final KillTokenFactory factory;

    public KillTokenService(PluginConfig config, KillTokenFactory factory) {
        this.config = config;
        this.factory = factory;
    }

    public KillTokenFactory getFactory() {
        return factory;
    }

    public int give(Player player, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int given = 0;
        int maxStack = config.getKillTokenMaxStack();
        int remaining = amount;

        while (remaining > 0) {
            int batch = Math.min(remaining, maxStack);
            ItemStack token = factory.create(batch);
            given += batch;
            remaining -= batch;
            deliver(player, token);
        }
        return given;
    }

    public int countInInventory(Player player) {
        int total = 0;
        total += countStack(player.getInventory().getContents());
        total += countStack(player.getInventory().getArmorContents());
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (factory.isKillToken(offhand)) {
            total += offhand.getAmount();
        }
        return total;
    }

    public int removeAmount(Player player, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && removed < amount; slot++) {
            ItemStack item = contents[slot];
            if (!factory.isKillToken(item)) {
                continue;
            }
            int take = Math.min(amount - removed, item.getAmount());
            item.setAmount(item.getAmount() - take);
            removed += take;
            if (item.getAmount() <= 0) {
                contents[slot] = null;
            }
        }
        player.getInventory().setContents(contents);

        if (removed < amount) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (factory.isKillToken(offhand)) {
                int take = Math.min(amount - removed, offhand.getAmount());
                offhand.setAmount(offhand.getAmount() - take);
                removed += take;
                if (offhand.getAmount() <= 0) {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }
        return removed;
    }

    public int removeAllFromInventory(Player player) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (!factory.isKillToken(item)) {
                continue;
            }
            removed += item.getAmount();
            contents[slot] = null;
        }
        player.getInventory().setContents(contents);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (factory.isKillToken(offhand)) {
            removed += offhand.getAmount();
            player.getInventory().setItemInOffHand(null);
        }
        return removed;
    }

    private int countStack(ItemStack[] stacks) {
        int total = 0;
        for (ItemStack stack : stacks) {
            if (factory.isKillToken(stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private void deliver(Player player, ItemStack token) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(token);
        if (leftover.isEmpty()) {
            return;
        }
        Location dropAt = player.getLocation().clone().add(0, 0.5, 0);
        for (ItemStack drop : leftover.values()) {
            var entity = player.getWorld().dropItem(dropAt, drop);
            entity.setVelocity(entity.getVelocity().zero());
        }
    }
}
