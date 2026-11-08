package com.saltysmp.saltcrates.listener;

import com.saltysmp.saltcrates.SaltCratesPlugin;
import com.saltysmp.saltcrates.gui.CrateClaimGui;
import com.saltysmp.saltcrates.gui.CratePreviewGui;
import com.saltysmp.saltcrates.gui.SaltGuiHolder;
import com.saltysmp.saltcrates.service.CrateClaimService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public final class CrateGuiListener implements Listener {

    private final SaltCratesPlugin plugin;
    private final CrateClaimService claimService;

    public CrateGuiListener(SaltCratesPlugin plugin, CrateClaimService claimService) {
        this.plugin = plugin;
        this.claimService = claimService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CratePreviewGui
                || event.getView().getTopInventory().getHolder() instanceof CrateClaimGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SaltGuiHolder holder
                && (holder instanceof CratePreviewGui || holder instanceof CrateClaimGui)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof CrateClaimGui)) {
            return;
        }
        if (!claimService.getSessions().isRolling(player)) {
            return;
        }
        var session = claimService.getSessions().get(player);
        if (session == null) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !claimService.getSessions().isRolling(player)) {
                    return;
                }
                player.openInventory(session.getGui().getInventory());
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        claimService.handlePlayerQuit(event.getPlayer());
    }
}
