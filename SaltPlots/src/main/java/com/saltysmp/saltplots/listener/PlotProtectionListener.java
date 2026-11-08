package com.saltysmp.saltplots.listener;

import com.saltysmp.saltplots.config.MessagesConfig;
import com.saltysmp.saltplots.config.PluginConfig;
import com.saltysmp.saltplots.model.Plot;
import com.saltysmp.saltplots.service.PlotService;
import com.saltysmp.saltplots.util.MessageThrottle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class PlotProtectionListener implements Listener {

    private final PluginConfig pluginConfig;
    private final PlotService plotService;
    private final MessagesConfig messages;
    private final MessageThrottle enterThrottle = new MessageThrottle(3000L);
    private final MessageThrottle buildThrottle = new MessageThrottle(2000L);
    private final MessageThrottle edgeThrottle = new MessageThrottle(2000L);

    public PlotProtectionListener(
            PluginConfig pluginConfig,
            PlotService plotService,
            MessagesConfig messages
    ) {
        this.pluginConfig = pluginConfig;
        this.plotService = plotService;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("saltplots.admin")) {
            return;
        }
        Plot plot = plotService.findPlotAt(event.getBlock().getLocation());
        if (plot == null && isPlotWorld(event.getBlock().getWorld().getName())) {
            event.setCancelled(true);
            if (buildThrottle.shouldSend(player.getUniqueId())) {
                messages.send(player, "protection-wilderness");
            }
            return;
        }
        if (plot != null && !plotService.canBuild(player, plot)) {
            event.setCancelled(true);
            if (buildThrottle.shouldSend(player.getUniqueId())) {
                messages.send(player, "protection-build", "owner", ownerName(plot));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("saltplots.admin")) {
            return;
        }
        Plot plot = plotService.findPlotAt(event.getBlock().getLocation());
        if (plot == null && isPlotWorld(event.getBlock().getWorld().getName())) {
            event.setCancelled(true);
            if (buildThrottle.shouldSend(player.getUniqueId())) {
                messages.send(player, "protection-wilderness");
            }
            return;
        }
        if (plot != null && !plotService.canBuild(player, plot)) {
            event.setCancelled(true);
            if (buildThrottle.shouldSend(player.getUniqueId())) {
                messages.send(player, "protection-build", "owner", ownerName(plot));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("saltplots.admin")) {
            return;
        }
        if (!isContainer(block.getType())) {
            return;
        }
        Plot plot = plotService.findPlotAt(block.getLocation());
        if (plot == null && isPlotWorld(block.getWorld().getName())) {
            event.setCancelled(true);
            if (buildThrottle.shouldSend(player.getUniqueId())) {
                messages.send(player, "protection-interact");
            }
            return;
        }
        if (plot != null && !plotService.canInteractContainer(player, plot)) {
            event.setCancelled(true);
            if (buildThrottle.shouldSend(player.getUniqueId())) {
                messages.send(player, "protection-interact");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("saltplots.admin")) {
            return;
        }
        if (!isPlotWorld(event.getTo().getWorld().getName())) {
            return;
        }
        keepPlayerOnBoundPlot(event, player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plotService.getBorderService().clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }
        if (!isPlotWorld(event.getTo().getWorld().getName())) {
            plotService.getBorderService().clear(event.getPlayer());
        }
    }

    private void keepPlayerOnBoundPlot(PlayerMoveEvent event, Player player) {
        Plot bound = plotService.getBorderService().getBoundPlot(player, plotService);
        if (bound == null) {
            Plot own = plotService.getPlot(player.getUniqueId());
            if (own == null) {
                return;
            }
            plotService.getBorderService().bindToPlot(player, own);
            bound = own;
        }

        int size = pluginConfig.getTierSize(bound.getTierIndex());
        int toX = event.getTo().getBlockX();
        int toZ = event.getTo().getBlockZ();
        boolean outsidePlot = !bound.contains(toX, toZ, size);
        boolean fellOff = event.getTo().getY() < pluginConfig.getPlatformY() - 4;

        if (!outsidePlot && !fellOff) {
            return;
        }

        Location safe = plotService.getPlotTeleportLocation(bound);
        if (safe == null) {
            return;
        }
        event.setTo(safe);
        plotService.getBorderService().applyBorder(player, bound);
        if (edgeThrottle.shouldSend(player.getUniqueId())) {
            messages.send(player, "plot-edge-safety");
        }
    }

    private String ownerName(Plot plot) {
        String name = Bukkit.getOfflinePlayer(plot.getOwnerId()).getName();
        return name == null ? "someone" : name;
    }

    private boolean isPlotWorld(String worldName) {
        return pluginConfig.getPlotWorld().equals(worldName);
    }

    private boolean isContainer(Material material) {
        return material == Material.CHEST
                || material == Material.TRAPPED_CHEST
                || material == Material.BARREL
                || material == Material.SHULKER_BOX
                || material == Material.HOPPER
                || material == Material.DISPENSER
                || material == Material.DROPPER
                || material == Material.FURNACE
                || material == Material.BLAST_FURNACE
                || material == Material.SMOKER
                || material.name().endsWith("_SHULKER_BOX");
    }
}
