package com.saltysmp.saltkits.gui;

import com.saltysmp.saltkits.model.Kit;
import com.saltysmp.saltkits.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Visual layout for kit editor/preview (mirrors player inventory).
 * Kit indices follow Bukkit {@link org.bukkit.inventory.PlayerInventory}.
 */
public final class KitInventoryLayout {

    public static final int LAYOUT_SLOTS = 45;

    private static final int[] GUI_TO_KIT = new int[LAYOUT_SLOTS];
    private static final int[] KIT_TO_GUI = new int[Kit.INVENTORY_SIZE];

    static {
        for (int i = 0; i < GUI_TO_KIT.length; i++) {
            GUI_TO_KIT[i] = -1;
        }
        for (int i = 0; i < KIT_TO_GUI.length; i++) {
            KIT_TO_GUI[i] = -1;
        }

        map(0, 40);
        map(5, 39);
        map(6, 38);
        map(7, 37);
        map(8, 36);
        for (int kit = 9; kit <= 35; kit++) {
            map(kit, kit);
        }
        for (int hotbar = 0; hotbar <= 8; hotbar++) {
            map(36 + hotbar, hotbar);
        }
    }

    private static void map(int guiSlot, int kitSlot) {
        GUI_TO_KIT[guiSlot] = kitSlot;
        KIT_TO_GUI[kitSlot] = guiSlot;
    }

    private KitInventoryLayout() {
    }

    public static boolean isKitGuiSlot(int guiSlot) {
        return guiSlot >= 0 && guiSlot < LAYOUT_SLOTS && GUI_TO_KIT[guiSlot] >= 0;
    }

    public static boolean isArmorKitIndex(int kitIndex) {
        return kitIndex >= 36 && kitIndex <= 39;
    }

    public static boolean isArmorGuiSlot(int guiSlot) {
        return isArmorKitIndex(guiSlotToKitIndex(guiSlot));
    }

    public static boolean isMovableKitGuiSlot(int guiSlot) {
        return isKitGuiSlot(guiSlot) && !isArmorGuiSlot(guiSlot);
    }

    public static int guiSlotToKitIndex(int guiSlot) {
        if (guiSlot < 0 || guiSlot >= LAYOUT_SLOTS) {
            return -1;
        }
        return GUI_TO_KIT[guiSlot];
    }

    public static int kitIndexToGuiSlot(int kitIndex) {
        if (kitIndex < 0 || kitIndex >= Kit.INVENTORY_SIZE) {
            return -1;
        }
        return KIT_TO_GUI[kitIndex];
    }

    public static void loadKitItems(Inventory inventory, ItemStack[] kitItems) {
        loadKitItems(inventory, kitItems, false);
    }

    public static void loadKitItems(Inventory inventory, ItemStack[] kitItems, boolean showEquipmentHints) {
        applySpacers(inventory);
        for (int kitIndex = 0; kitIndex < Kit.INVENTORY_SIZE; kitIndex++) {
            ItemStack item = kitItems[kitIndex];
            setKitItem(inventory, kitIndex, item);
        }
        if (showEquipmentHints) {
            placeEquipmentHints(inventory);
        }
    }

    public static void applySpacers(Inventory inventory) {
        inventory.setItem(1, labeledPane(
                Material.ORANGE_STAINED_GLASS_PANE,
                "<gold>← Offhand",
                List.of("<gray>Put offhand items in the empty slot to the left.")
        ));
        inventory.setItem(2, labeledPane(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray> ", List.of()));
        inventory.setItem(3, labeledPane(
                Material.CYAN_STAINED_GLASS_PANE,
                "<aqua>Armor →",
                List.of("<gray>Helmet, chestplate, leggings, boots.")
        ));
        inventory.setItem(4, labeledPane(
                Material.CYAN_STAINED_GLASS_PANE,
                "<aqua>→",
                List.of("<gray>Use the 4 empty slots on the right.")
        ));
    }

    public static void placeEquipmentHints(Inventory inventory) {
        applySpacers(inventory);
        hintIfEmpty(inventory, 40, Material.SHIELD, "<gold>Offhand");
        hintIfEmpty(inventory, 39, Material.CHAINMAIL_HELMET, "<aqua>Helmet");
        hintIfEmpty(inventory, 38, Material.CHAINMAIL_CHESTPLATE, "<blue>Chestplate");
        hintIfEmpty(inventory, 37, Material.CHAINMAIL_LEGGINGS, "<blue>Leggings");
        hintIfEmpty(inventory, 36, Material.CHAINMAIL_BOOTS, "<aqua>Boots");
    }

    public static ItemStack[] collectKitItems(Inventory inventory) {
        ItemStack[] items = new ItemStack[Kit.INVENTORY_SIZE];
        for (int kitIndex = 0; kitIndex < Kit.INVENTORY_SIZE; kitIndex++) {
            items[kitIndex] = readKitItem(inventory, kitIndex);
        }
        return items;
    }

    private static void setKitItem(Inventory inventory, int kitIndex, ItemStack item) {
        int guiSlot = kitIndexToGuiSlot(kitIndex);
        if (guiSlot < 0) {
            return;
        }
        if (item == null || item.getType().isAir()) {
            inventory.setItem(guiSlot, null);
            return;
        }
        inventory.setItem(guiSlot, item.clone());
    }

    private static ItemStack readKitItem(Inventory inventory, int kitIndex) {
        int guiSlot = kitIndexToGuiSlot(kitIndex);
        if (guiSlot < 0) {
            return null;
        }
        ItemStack item = inventory.getItem(guiSlot);
        if (item == null || item.getType().isAir() || isGuideItem(item)) {
            return null;
        }
        return item.clone();
    }

    private static void hintIfEmpty(Inventory inventory, int kitIndex, Material icon, String label) {
        if (readKitItem(inventory, kitIndex) != null) {
            return;
        }
        int guiSlot = kitIndexToGuiSlot(kitIndex);
        if (guiSlot < 0) {
            return;
        }
        inventory.setItem(guiSlot, hintIcon(icon, label));
    }

    private static ItemStack hintIcon(Material material, String label) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(label));
            meta.lore(List.of(MessageUtil.parse("<dark_gray>Drop an item here")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            if (KitGuiKeys.guidePane() != null) {
                meta.getPersistentDataContainer().set(KitGuiKeys.guidePane(), PersistentDataType.BYTE, (byte) 1);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack labeledPane(Material material, String label, List<String> loreLines) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(label));
            if (!loreLines.isEmpty()) {
                meta.lore(loreLines.stream().map(MessageUtil::parse).toList());
            }
            if (KitGuiKeys.guidePane() != null) {
                meta.getPersistentDataContainer().set(KitGuiKeys.guidePane(), PersistentDataType.BYTE, (byte) 1);
            }
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public static boolean isGuideItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || KitGuiKeys.guidePane() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(KitGuiKeys.guidePane(), PersistentDataType.BYTE);
    }
}
