package com.saltysmp.saltkits.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record Kit(
        String name,
        ItemStack[] items,
        boolean starter,
        long cooldownSeconds,
        String permission,
        String displayName,
        Material iconMaterial,
        List<String> description,
        String category,
        boolean autoClaimOnJoin,
        int menuOrder
) {
    public static final int INVENTORY_SIZE = 41;
    public static final int DEFAULT_MENU_ORDER = 100;

    public Kit {
        if (items == null) {
            items = new ItemStack[INVENTORY_SIZE];
        } else if (items.length != INVENTORY_SIZE) {
            items = migrateItems(items);
        }
        if (description == null) {
            description = List.of();
        } else {
            description = List.copyOf(description);
        }
        if (iconMaterial == null) {
            iconMaterial = Material.CHEST;
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = name;
        }
        if (category == null) {
            category = "";
        }
    }

    public static ItemStack[] migrateItems(ItemStack[] source) {
        ItemStack[] migrated = new ItemStack[INVENTORY_SIZE];
        if (source.length == 27) {
            System.arraycopy(source, 0, migrated, 9, 27);
            return migrated;
        }
        System.arraycopy(source, 0, migrated, 0, Math.min(source.length, INVENTORY_SIZE));
        return migrated;
    }

    public String resolvedDisplayName() {
        return displayName;
    }

    public Kit withStarter(boolean starter) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withCooldownSeconds(long cooldownSeconds) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withPermission(String permission) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withItems(ItemStack[] items) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withDisplayName(String displayName) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withIconMaterial(Material iconMaterial) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withDescription(List<String> description) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withCategory(String category) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withAutoClaimOnJoin(boolean autoClaimOnJoin) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public Kit withMenuOrder(int menuOrder) {
        return new Kit(name, items, starter, cooldownSeconds, permission, displayName, iconMaterial, description, category, autoClaimOnJoin, menuOrder);
    }

    public int itemCount() {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }
}
