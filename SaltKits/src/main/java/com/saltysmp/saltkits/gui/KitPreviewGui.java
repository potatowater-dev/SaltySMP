package com.saltysmp.saltkits.gui;

import com.saltysmp.saltkits.model.Kit;
import com.saltysmp.saltkits.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class KitPreviewGui implements KitGuiHolder {

    public static final int SLOT_RESET = 47;
    public static final int SLOT_DONE = 49;

    private final Kit kit;
    private final boolean customize;
    private final Inventory inventory;
    private final ItemStack[] template;
    private final ItemStack[] ghosts;
    private int lastPickedKitSlot = -1;
    private int selectedGuiSlot = -1;
    private boolean settled;

    public KitPreviewGui(Kit kit, boolean customize) {
        this(kit, customize, kit.items());
    }

    public KitPreviewGui(Kit kit, boolean customize, ItemStack[] displayItems) {
        this.kit = kit;
        this.customize = customize;
        this.template = cloneItems(displayItems);
        this.ghosts = new ItemStack[Kit.INVENTORY_SIZE];
        this.inventory = Bukkit.createInventory(
                this,
                54,
                MessageUtil.parse(customize
                        ? "<green>Edit layout: <aqua>" + kit.resolvedDisplayName()
                        : "<aqua>" + kit.resolvedDisplayName() + " <gray>Preview")
        );
        KitInventoryLayout.loadKitItems(inventory, displayItems, !customize);

        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = KitInventoryLayout.LAYOUT_SLOTS; slot < 54; slot++) {
            inventory.setItem(slot, filler);
        }
        if (customize) {
            inventory.setItem(SLOT_RESET, resetButton());
            inventory.setItem(SLOT_DONE, doneButton());
        }
    }

    private ItemStack doneButton() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<green>Save Layout"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Saves slot order for the next claim."),
                    MessageUtil.parse("<gray>Does not give any items.")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack resetButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<red>Reset Layout"));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Next claim uses the default kit.")
            ));
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

    public Kit getKit() {
        return kit;
    }

    public boolean isCustomize() {
        return customize;
    }

    public boolean isSettled() {
        return settled;
    }

    public void markSettled() {
        this.settled = true;
    }

    public void restoreCursor(Player player) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return;
        }
        if (lastPickedKitSlot < 0) {
            return;
        }
        int guiSlot = KitInventoryLayout.kitIndexToGuiSlot(lastPickedKitSlot);
        ItemStack existing = guiSlot >= 0 ? inventory.getItem(guiSlot) : null;
        if (guiSlot >= 0 && (existing == null || existing.getType().isAir())) {
            inventory.setItem(guiSlot, cursor);
            player.setItemOnCursor(null);
            return;
        }
        rememberTaken(lastPickedKitSlot, cursor);
        player.setItemOnCursor(null);
    }

    public int getLastPickedKitSlot() {
        return lastPickedKitSlot;
    }

    public void setLastPickedKitSlot(int kitSlot) {
        this.lastPickedKitSlot = kitSlot;
    }

    public void swapOrSelect(int guiSlot) {
        if (!KitInventoryLayout.isMovableKitGuiSlot(guiSlot)) {
            return;
        }
        if (selectedGuiSlot < 0 || selectedGuiSlot == guiSlot) {
            selectedGuiSlot = selectedGuiSlot == guiSlot ? -1 : guiSlot;
            lastPickedKitSlot = KitInventoryLayout.guiSlotToKitIndex(guiSlot);
            return;
        }
        ItemStack first = inventory.getItem(selectedGuiSlot);
        ItemStack second = inventory.getItem(guiSlot);
        inventory.setItem(selectedGuiSlot, second);
        inventory.setItem(guiSlot, first);
        selectedGuiSlot = -1;
        captureTemplate();
    }

    public void rememberTaken(int kitSlot, ItemStack stack) {
        if (kitSlot < 0 || kitSlot >= Kit.INVENTORY_SIZE || stack == null || stack.getType().isAir()) {
            return;
        }
        ghosts[kitSlot] = stack.clone();
        template[kitSlot] = stack.clone();
    }

    public void forgetSlot(int kitSlot) {
        if (kitSlot < 0 || kitSlot >= Kit.INVENTORY_SIZE) {
            return;
        }
        ghosts[kitSlot] = null;
        template[kitSlot] = null;
    }

    public void captureTemplate() {
        ItemStack[] current = KitInventoryLayout.collectKitItems(inventory);
        for (int i = 0; i < template.length; i++) {
            ItemStack now = i < current.length ? current[i] : null;
            if (now != null && !now.getType().isAir()) {
                template[i] = now.clone();
                ghosts[i] = null;
            } else if (ghosts[i] != null) {
                template[i] = ghosts[i].clone();
            } else {
                template[i] = null;
            }
        }
    }

    public void resetTemplateToDefault() {
        ItemStack[] defaults = cloneItems(kit.items());
        System.arraycopy(defaults, 0, template, 0, template.length);
        for (int i = 0; i < ghosts.length; i++) {
            ghosts[i] = null;
        }
        selectedGuiSlot = -1;
    }

    public ItemStack[] getTemplate() {
        return cloneItems(template);
    }

    public ItemStack[] collectItems() {
        return KitInventoryLayout.collectKitItems(inventory);
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] cloned = new ItemStack[Kit.INVENTORY_SIZE];
        if (source == null) {
            return cloned;
        }
        for (int i = 0; i < cloned.length && i < source.length; i++) {
            cloned[i] = source[i] == null ? null : source[i].clone();
        }
        return cloned;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
