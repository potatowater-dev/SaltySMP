package com.saltysmp.saltkits.kit;

import com.saltysmp.saltkits.model.Kit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class KitGiveService {

    public record GiveResult(int itemCount, List<String> equippedArmor, List<String> itemNames) {
        public static GiveResult empty() {
            return new GiveResult(0, List.of(), List.of());
        }
    }

    public boolean wouldOverflow(Player player, ItemStack[] kitItems) {
        int needed = 0;
        for (ItemStack item : kitItems) {
            if (item != null && !item.getType().isAir()) {
                needed++;
            }
        }

        int free = 0;
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < Kit.INVENTORY_SIZE; slot++) {
            if (isEmpty(getSlot(inventory, slot))) {
                free++;
            }
        }
        return needed > free;
    }

    public GiveResult give(Player player, ItemStack[] kitItems) {
        List<String> equipped = new ArrayList<>();
        List<String> names = new ArrayList<>();
        int count = 0;

        PlayerInventory inventory = player.getInventory();
        Location dropLocation = player.getLocation();
        int limit = Math.min(kitItems.length, Kit.INVENTORY_SIZE);

        for (int slot = 0; slot < limit; slot++) {
            ItemStack item = kitItems[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }

            ItemStack stack = item.clone();
            count += stack.getAmount();
            addName(names, stack);

            if (isEmpty(getSlot(inventory, slot))) {
                setSlot(inventory, slot, stack);
                if (slot >= 36) {
                    addName(equipped, stack);
                }
                continue;
            }

            Map<Integer, ItemStack> leftover = inventory.addItem(stack);
            for (ItemStack dropped : leftover.values()) {
                dropItem(player, dropLocation, dropped);
            }
        }

        return new GiveResult(count, equipped, names);
    }

    public static boolean placeInMatchingSlot(Player player, int kitSlot, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || kitSlot < 0 || kitSlot >= Kit.INVENTORY_SIZE) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        if (!isEmpty(getSlot(inventory, kitSlot))) {
            Map<Integer, ItemStack> leftover = inventory.addItem(stack);
            leftover.values().forEach(dropped -> {
                var drop = player.getWorld().dropItem(player.getLocation().clone().add(0, 0.5, 0), dropped);
                drop.setVelocity(drop.getVelocity().zero());
            });
            return false;
        }
        setSlot(inventory, kitSlot, stack);
        return true;
    }

    public static ItemStack getSlot(PlayerInventory inventory, int slot) {
        if (slot == 40) {
            return inventory.getItemInOffHand();
        }
        return inventory.getItem(slot);
    }

    public static void setSlot(PlayerInventory inventory, int slot, ItemStack item) {
        if (slot == 40) {
            inventory.setItemInOffHand(item);
            return;
        }
        inventory.setItem(slot, item);
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private void dropItem(Player player, Location location, ItemStack stack) {
        var dropped = player.getWorld().dropItem(location.clone().add(0, 0.5, 0), stack);
        dropped.setVelocity(dropped.getVelocity().zero());
    }

    private void addName(List<String> names, ItemStack stack) {
        if (names.size() >= 3) {
            return;
        }
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            names.add(PlainTextComponentSerializer.plainText().serialize(stack.getItemMeta().displayName()));
            return;
        }
        names.add(formatMaterial(stack.getType().name()));
    }

    private String formatMaterial(String materialName) {
        String lower = materialName.toLowerCase().replace('_', ' ');
        if (lower.isEmpty()) {
            return materialName;
        }
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
