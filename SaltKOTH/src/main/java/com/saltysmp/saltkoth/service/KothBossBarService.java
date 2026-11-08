package com.saltysmp.saltkoth.service;

import com.saltysmp.saltkoth.SaltKothPlugin;
import com.saltysmp.saltkoth.config.PluginConfig;
import com.saltysmp.saltkoth.util.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class KothBossBarService {

    private final SaltKothPlugin plugin;
    private final PluginConfig config;
    private BossBar bossBar;
    private final Set<UUID> viewers = new HashSet<>();
    private BukkitTask enforceTask;

    public KothBossBarService(SaltKothPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stopEnforce();
        enforceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::enforceVisibility, 5L, 5L);
    }

    public void stopEnforce() {
        if (enforceTask != null) {
            enforceTask.cancel();
            enforceTask = null;
        }
    }

    public void showIdle(long remainingMs) {
        hideAll();
    }

    public void showActive(long remainingMs, long totalMs, String capturerText, Set<Player> zonePlayers) {
        if (!config.isBossbarEnabled()) {
            hideAll();
            return;
        }
        ensureBossBar(BossBar.Color.RED);
        bossBar.color(BossBar.Color.RED);
        float progress = totalMs <= 0 ? 0f : Math.min(1f, Math.max(0f, remainingMs / (float) totalMs));
        bossBar.progress(progress);
        bossBar.name(MessageUtil.parse(
                "<gold><bold>KOTH <gray>| <yellow>" + MessageUtil.formatDuration(remainingMs) + " left"
                        + " <gray>| <aqua>" + capturerText
        ));

        Set<UUID> desired = new HashSet<>();
        for (Player player : zonePlayers) {
            desired.add(player.getUniqueId());
        }
        syncViewers(desired);
    }

    public void hideAll() {
        if (bossBar == null) {
            viewers.clear();
            return;
        }
        for (UUID viewerId : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null) {
                player.hideBossBar(bossBar);
            }
        }
        viewers.clear();
        bossBar = null;
    }

    public void onQuit(Player player) {
        viewers.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private void ensureBossBar(BossBar.Color color) {
        if (bossBar == null) {
            bossBar = BossBar.bossBar(Component.text("KOTH"), 1.0f, color, BossBar.Overlay.PROGRESS);
        }
    }

    private void syncViewers(Set<UUID> desired) {
        for (UUID viewerId : new HashSet<>(viewers)) {
            if (!desired.contains(viewerId)) {
                Player player = Bukkit.getPlayer(viewerId);
                if (player != null && bossBar != null) {
                    player.hideBossBar(bossBar);
                }
                viewers.remove(viewerId);
            }
        }
        for (UUID viewerId : desired) {
            if (viewers.contains(viewerId)) {
                continue;
            }
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null && bossBar != null) {
                player.showBossBar(bossBar);
                viewers.add(viewerId);
            }
        }
    }

    private Set<UUID> allOnlineIds() {
        Set<UUID> ids = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ids.add(player.getUniqueId());
        }
        return ids;
    }

    private void enforceVisibility() {
        if (bossBar == null) {
            return;
        }
        for (UUID viewerId : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player == null || !player.isOnline()) {
                viewers.remove(viewerId);
                continue;
            }
            player.showBossBar(bossBar);
        }
    }
}
