package com.saltysmp.saltkits.gui;

import com.saltysmp.saltkits.kit.CooldownManager;
import com.saltysmp.saltkits.model.Kit;
import com.saltysmp.saltkits.util.DurationParser;
import com.saltysmp.saltkits.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class KitClaimMenu implements KitGuiHolder {

    public static final int KITS_PER_PAGE = 45;
    public static final int PREV_SLOT = 45;
    public static final int NEXT_SLOT = 53;

    private final Player player;
    private final List<Kit> kits;
    private final CooldownManager cooldownManager;
    private final int page;
    private final boolean customizeMode;
    private final Inventory inventory;

    public KitClaimMenu(Player player, List<Kit> kits, CooldownManager cooldownManager, int page, boolean customizeMode) {
        this.player = player;
        this.kits = kits;
        this.cooldownManager = cooldownManager;
        this.page = page;
        this.customizeMode = customizeMode;
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse("<aqua>Kit Menu <gray>(Page " + (page + 1) + ")"));
        populate();
    }

    private void populate() {
        int start = page * KITS_PER_PAGE;
        int end = Math.min(start + KITS_PER_PAGE, kits.size());

        for (int index = start; index < end; index++) {
            Kit kit = kits.get(index);
            inventory.setItem(index - start, kitIcon(kit));
        }

        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = end - start; slot < KITS_PER_PAGE; slot++) {
            inventory.setItem(slot, filler);
        }

        if (page > 0) {
            inventory.setItem(PREV_SLOT, controlItem(Material.ARROW, "<yellow>Previous Page", List.of()));
        } else {
            inventory.setItem(PREV_SLOT, filler);
        }

        if (end < kits.size()) {
            inventory.setItem(NEXT_SLOT, controlItem(Material.ARROW, "<yellow>Next Page", List.of()));
        } else {
            inventory.setItem(NEXT_SLOT, filler);
        }

        for (int slot = 46; slot < 53; slot++) {
            if (slot != PREV_SLOT && slot != NEXT_SLOT) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack kitIcon(Kit kit) {
        ItemStack icon = new ItemStack(kit.iconMaterial());
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }

        meta.displayName(MessageUtil.parse("<aqua>" + kit.resolvedDisplayName()));

        List<Component> lore = new ArrayList<>();
        for (String line : kit.description()) {
            lore.add(MessageUtil.parse(line));
        }

        if (kit.starter()) {
            lore.add(MessageUtil.parse("<green>Starter Kit <gray>(unlimited)"));
        } else {
            boolean hasAccess = player.hasPermission(kit.permission());
            lore.add(MessageUtil.parse(hasAccess ? "<green>Unlocked" : "<red>Locked"));
            if (kit.cooldownSeconds() > 0L) {
                long remaining = cooldownManager.getRemainingSeconds(
                        player.getUniqueId(),
                        kit.name(),
                        kit.cooldownSeconds()
                );
                if (remaining > 0L) {
                    lore.add(MessageUtil.parse("<yellow>Cooldown: <white>" + DurationParser.formatSeconds(remaining)));
                } else {
                    lore.add(MessageUtil.parse("<gray>Cooldown: <white>" + DurationParser.formatSeconds(kit.cooldownSeconds())));
                }
            }
        }

        lore.add(MessageUtil.parse("<dark_gray>---"));
        lore.add(MessageUtil.parse("<gray>Left-click: <white>Claim all"));
        lore.add(MessageUtil.parse(customizeMode
                ? "<gray>Right-click: <white>Edit layout <dark_gray>(even on cooldown)"
                : "<gray>Right-click: <white>Preview"));
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack controlItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Kit> getKits() {
        return kits;
    }

    public int getPage() {
        return page;
    }

    public Kit kitAtSlot(int menuSlot) {
        int index = page * KITS_PER_PAGE + menuSlot;
        if (index < 0 || index >= kits.size()) {
            return null;
        }
        return kits.get(index);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
