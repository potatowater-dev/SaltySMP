package com.saltysmp.saltplots.listener;

import com.saltysmp.saltplots.service.PlotService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class HubWandListener implements Listener {

    private final PlotService plotService;

    public HubWandListener(PlotService plotService) {
        this.plotService = plotService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onWandUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("saltplots.admin")) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.GOLDEN_HOE) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        event.setCancelled(true);
        if (action == Action.LEFT_CLICK_BLOCK) {
            plotService.setHubPos1(player, event.getClickedBlock().getLocation());
        } else {
            plotService.setHubPos2(player, event.getClickedBlock().getLocation());
        }
    }
}
