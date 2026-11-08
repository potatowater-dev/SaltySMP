package com.saltysmp.saltkits.listener;

import com.saltysmp.saltkits.config.MessagesConfig;
import com.saltysmp.saltkits.gui.KitClaimMenu;
import com.saltysmp.saltkits.kit.KitManager;
import com.saltysmp.saltkits.kit.KitSessionRegistry;
import com.saltysmp.saltkits.model.Kit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class KitMenuListener implements Listener {

    private final KitManager kitManager;
    private final KitSessionRegistry sessionRegistry;
    private final MessagesConfig messages;

    public KitMenuListener(
            KitManager kitManager,
            KitSessionRegistry sessionRegistry,
            MessagesConfig messages
    ) {
        this.kitManager = kitManager;
        this.sessionRegistry = sessionRegistry;
        this.messages = messages;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof KitClaimMenu menu)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(top)) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        int rawSlot = event.getRawSlot();

        if (rawSlot == KitClaimMenu.PREV_SLOT && menu.getPage() > 0) {
            player.closeInventory();
            kitManager.openClaimMenu(player, menu.getPage() - 1);
            return;
        }
        if (rawSlot == KitClaimMenu.NEXT_SLOT) {
            int nextStart = (menu.getPage() + 1) * KitClaimMenu.KITS_PER_PAGE;
            if (nextStart < menu.getKits().size()) {
                player.closeInventory();
                kitManager.openClaimMenu(player, menu.getPage() + 1);
            }
            return;
        }

        Kit kit = menu.kitAtSlot(rawSlot);
        if (kit == null) {
            return;
        }

        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            player.closeInventory();
            sessionRegistry.clear(player.getUniqueId());
            kitManager.openPreview(player, kit);
            return;
        }

        KitManager.ClaimResult result = kitManager.claimKit(player, kit.name(), false);
        if (!result.success()) {
            messages.send(player, "menu-claim-failed", "reason", result.message());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof KitClaimMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof KitClaimMenu) {
            sessionRegistry.clear(event.getPlayer().getUniqueId());
        }
    }
}
