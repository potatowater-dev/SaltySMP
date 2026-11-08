package com.saltysmp.saltkits.listener;

import com.saltysmp.saltkits.gui.KitInventoryLayout;
import com.saltysmp.saltkits.gui.KitPreviewGui;
import com.saltysmp.saltkits.kit.KitManager;
import com.saltysmp.saltkits.kit.KitSessionRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class KitGuiListener implements Listener {

    private final KitManager kitManager;
    private final KitSessionRegistry sessionRegistry;

    public KitGuiListener(KitManager kitManager, KitSessionRegistry sessionRegistry) {
        this.kitManager = kitManager;
        this.sessionRegistry = sessionRegistry;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryClickCancel(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (holder(top) instanceof KitPreviewGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(holder(top) instanceof KitPreviewGui preview)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        event.setCancelled(true);

        if (preview.isCustomize()) {
            Inventory clicked = event.getClickedInventory();
            if (clicked != null && holder(clicked) instanceof KitPreviewGui) {
                int slot = event.getSlot();
                if (!KitInventoryLayout.isKitGuiSlot(slot)) {
                    if (slot == KitPreviewGui.SLOT_DONE) {
                        lockCursor(player, preview);
                        player.closeInventory();
                        return;
                    }
                    if (slot == KitPreviewGui.SLOT_RESET) {
                        kitManager.resetPlayerLayout(player, preview);
                    }
                } else if (!KitInventoryLayout.isArmorGuiSlot(slot)) {
                    preview.swapOrSelect(slot);
                }
            }
        }

        lockCursor(player, preview);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryDragCancel(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (holder(top) instanceof KitPreviewGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(holder(top) instanceof KitPreviewGui preview)) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            lockCursor(player, preview);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(holder(event.getInventory()) instanceof KitPreviewGui preview)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            sessionRegistry.clear(event.getPlayer().getUniqueId());
            return;
        }
        if (preview.isCustomize()) {
            kitManager.finishCustomize(player, preview);
            return;
        }
        sessionRegistry.clear(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionRegistry.getSession(event.getPlayer().getUniqueId()).ifPresent(session -> {
            if (session.gui() instanceof KitPreviewGui preview && preview.isCustomize()) {
                kitManager.finishCustomize(event.getPlayer(), preview);
            }
        });
    }

    private void lockCursor(Player player, KitPreviewGui preview) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            int guiSlot = KitInventoryLayout.kitIndexToGuiSlot(preview.getLastPickedKitSlot());
            ItemStack existing = guiSlot >= 0 ? preview.getInventory().getItem(guiSlot) : null;
            if (guiSlot >= 0 && (existing == null || existing.getType().isAir())) {
                preview.getInventory().setItem(guiSlot, cursor);
            }
            player.setItemOnCursor(null);
            preview.captureTemplate();
        }
        player.updateInventory();
    }

    private static InventoryHolder holder(Inventory inventory) {
        return inventory.getHolder(false);
    }
}
