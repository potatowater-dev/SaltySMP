package com.saltysmp.saltkits.listener;

import com.saltysmp.saltkits.SaltKitsPlugin;
import com.saltysmp.saltkits.kit.CooldownManager;
import com.saltysmp.saltkits.kit.KitManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public final class KitJoinListener implements Listener {

    private final SaltKitsPlugin plugin;
    private final KitManager kitManager;
    private final CooldownManager cooldownManager;

    public KitJoinListener(SaltKitsPlugin plugin, KitManager kitManager, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        cooldownManager.loadPlayer(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    kitManager.tryAutoClaimStarter(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
