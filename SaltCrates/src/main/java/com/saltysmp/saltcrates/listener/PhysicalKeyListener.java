package com.saltysmp.saltcrates.listener;

import com.saltysmp.saltcrates.config.MessagesConfig;
import com.saltysmp.saltcrates.gui.SaltGuiHolder;
import com.saltysmp.saltcrates.service.PhysicalKeyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhysicalKeyListener implements Listener {

    private final Plugin plugin;
    private final PhysicalKeyService physicalKeys;
    private final MessagesConfig messages;
    private final Map<UUID, Boolean> pending = new ConcurrentHashMap<>();

    public PhysicalKeyListener(Plugin plugin, PhysicalKeyService physicalKeys, MessagesConfig messages) {
        this.plugin = plugin;
        this.physicalKeys = physicalKeys;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleConvert(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!physicalKeys.isPhysicalKey(event.getItem().getItemStack())) {
            return;
        }
        scheduleConvert(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isSaltGui(event.getInventory().getHolder()) || isSaltGui(event.getView().getTopInventory().getHolder())) {
            return;
        }
        scheduleConvert(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (isSaltGui(event.getInventory().getHolder())) {
            return;
        }
        scheduleConvert(player);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        scheduleConvert(player);
    }

    private void scheduleConvert(Player player) {
        UUID id = player.getUniqueId();
        if (pending.putIfAbsent(id, Boolean.TRUE) != null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(id);
            Player online = Bukkit.getPlayer(id);
            if (online == null || !online.isOnline()) {
                return;
            }
            if (isSaltGui(online.getOpenInventory().getTopInventory().getHolder())) {
                return;
            }
            Map<String, Integer> converted = physicalKeys.convertAndCredit(online);
            if (converted.isEmpty()) {
                return;
            }
            messages.send(
                    online,
                    "physical-key-converted",
                    "details",
                    physicalKeys.formatConvertedSummary(converted)
            );
        }, 2L);
    }

    private static boolean isSaltGui(InventoryHolder holder) {
        return holder instanceof SaltGuiHolder;
    }
}
