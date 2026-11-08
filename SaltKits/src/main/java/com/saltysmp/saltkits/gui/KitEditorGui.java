package com.saltysmp.saltkits.gui;

import com.saltysmp.saltkits.model.Kit;
import com.saltysmp.saltkits.util.DurationParser;
import com.saltysmp.saltkits.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class KitEditorGui implements KitGuiHolder {

    public static final int LAYOUT_SLOTS = KitInventoryLayout.LAYOUT_SLOTS;
    public static final int SLOT_HELP = 50;
    public static final int SLOT_DISPLAY_NAME = 45;
    public static final int SLOT_STARTER = 46;
    public static final int SLOT_COOLDOWN = 47;
    public static final int SLOT_PERMISSION = 48;
    public static final int SLOT_MENU_ORDER = 49;
    public static final int SLOT_CANCEL = 52;
    public static final int SLOT_SAVE = 53;

    private final String kitName;
    private Kit kit;
    private final Inventory inventory;

    public KitEditorGui(Kit kit) {
        this.kitName = kit.name();
        this.kit = kit;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                MessageUtil.parse("<dark_purple>Edit Kit: <aqua>" + kit.resolvedDisplayName())
        );
        loadItems();
        refreshDecorations();
    }

    private void loadItems() {
        KitInventoryLayout.loadKitItems(inventory, kit.items());
    }

    public void refreshDecorations() {
        ItemStack pane = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = LAYOUT_SLOTS; slot < 54; slot++) {
            if (isSettingsSlot(slot)) {
                continue;
            }
            inventory.setItem(slot, pane);
        }

        inventory.setItem(SLOT_HELP, helpBook());
        inventory.setItem(SLOT_DISPLAY_NAME, displayNameButton());
        inventory.setItem(SLOT_STARTER, starterButton());
        inventory.setItem(SLOT_COOLDOWN, cooldownButton());
        inventory.setItem(SLOT_PERMISSION, permissionButton());
        inventory.setItem(SLOT_MENU_ORDER, menuOrderButton());
        inventory.setItem(SLOT_CANCEL, controlItem(
                Material.RED_STAINED_GLASS_PANE,
                "<red>Cancel",
                List.of("<gray>Discard changes and close.")
        ));
        inventory.setItem(SLOT_SAVE, controlItem(
                Material.LIME_STAINED_GLASS_PANE,
                "<green>Save",
                List.of("<gray>Save kit and close.")
        ));
        KitInventoryLayout.applySpacers(inventory);
    }

    public static boolean isSettingsSlot(int slot) {
        return slot == SLOT_DISPLAY_NAME
                || slot == SLOT_STARTER
                || slot == SLOT_COOLDOWN
                || slot == SLOT_PERMISSION
                || slot == SLOT_MENU_ORDER
                || slot == SLOT_CANCEL
                || slot == SLOT_SAVE;
    }

    private ItemStack helpBook() {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Kit Editor Help"));
            List<Component> lore = new ArrayList<>();
            lore.add(MessageUtil.parse("<gray>ID: <white>" + kit.name()));
            lore.add(MessageUtil.parse("<gray>Top: offhand + armor"));
            lore.add(MessageUtil.parse("<gray>Middle: storage (27 slots)"));
            lore.add(MessageUtil.parse("<gray>Bottom row: hotbar"));
            lore.add(MessageUtil.parse("<gray>Bottom row: kit settings"));
            lore.add(MessageUtil.parse("<dark_gray>Menu order is hidden from players."));
            meta.lore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack displayNameButton() {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Display Name"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Current: <white>" + kit.resolvedDisplayName()),
                    Component.empty(),
                    MessageUtil.parse("<yellow>Click to change in chat.")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack starterButton() {
        boolean enabled = kit.starter();
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Starter Kit"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Status: " + (enabled ? "<green>Yes" : "<red>No")),
                    Component.empty(),
                    MessageUtil.parse("<yellow>Click to toggle.")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack cooldownButton() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Claim Cooldown"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Current: <white>" + DurationParser.formatSeconds(kit.cooldownSeconds())),
                    Component.empty(),
                    MessageUtil.parse("<yellow>Click to set in chat."),
                    MessageUtil.parse("<dark_gray>e.g. 24h, 30m, 0")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack permissionButton() {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Permission"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Current: <white>" + kit.permission()),
                    Component.empty(),
                    MessageUtil.parse("<yellow>Click to change in chat.")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack menuOrderButton() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<gold>Menu List Order"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Current: <white>" + kit.menuOrder()),
                    MessageUtil.parse("<dark_gray>Lower number = earlier in /kit menu."),
                    MessageUtil.parse("<dark_gray>Not shown to players."),
                    Component.empty(),
                    MessageUtil.parse("<yellow>Click to set in chat.")
            ));
            item.setItemMeta(meta);
        }
        return item;
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

    public String getKitName() {
        return kitName;
    }

    public Kit getKit() {
        return kit;
    }

    public void setKit(Kit kit) {
        this.kit = kit;
        refreshDecorations();
    }

    public ItemStack[] collectItems() {
        return KitInventoryLayout.collectKitItems(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
