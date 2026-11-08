package com.saltysmp.saltcoins.listener;

import com.saltysmp.saltcoins.config.MessagesConfig;
import com.saltysmp.saltcoins.config.PluginConfig;
import com.saltysmp.saltcoins.service.ArmorCoinService;
import com.saltysmp.saltcoins.service.KillTokenService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KillTokenListener implements Listener {

    private final PluginConfig config;
    private final MessagesConfig messages;
    private final KillTokenService killTokenService;
    private final ArmorCoinService armorCoinService;
    private final Set<UUID> pendingRespawnCleanup = ConcurrentHashMap.newKeySet();

    public KillTokenListener(
            PluginConfig config,
            MessagesConfig messages,
            KillTokenService killTokenService,
            ArmorCoinService armorCoinService
    ) {
        this.config = config;
        this.messages = messages;
        this.killTokenService = killTokenService;
        this.armorCoinService = armorCoinService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!config.isOnKillEnabled()) {
            return;
        }
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }
        if (config.isWorldIgnored(victim.getWorld().getName())) {
            return;
        }
        if (config.isOnKillRequirePermission() && !killer.hasPermission("saltcoins.token.receive")) {
            return;
        }

        int carriedTokens = killTokenService.countInInventory(victim);
        int armorCoins = armorCoinService.evaluateAndStrip(victim);
        int removedTokens = killTokenService.removeAllFromInventory(victim);
        carriedTokens = Math.max(carriedTokens, removedTokens);

        if (config.isOverrideKeepInventory()) {
            pendingRespawnCleanup.add(victim.getUniqueId());
        }

        int baseReward = config.getOnKillAmount();
        int total = baseReward + armorCoins + carriedTokens;
        if (total <= 0) {
            return;
        }

        killTokenService.give(killer, total);

        if (config.isOnKillNotify()) {
            if (armorCoins > 0 || carriedTokens > 0) {
                messages.send(
                        killer,
                        "token-looted",
                        "amount", String.valueOf(total),
                        "base", String.valueOf(baseReward),
                        "armor", String.valueOf(armorCoins),
                        "carried", String.valueOf(carriedTokens)
                );
            } else {
                messages.send(killer, "token-received", "amount", String.valueOf(total));
            }
        }
        if (config.isOnKillNotifyActionBar()) {
            messages.actionBar(killer, "token-received-actionbar", "amount", String.valueOf(total));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!config.isOverrideKeepInventory()) {
            return;
        }
        if (!pendingRespawnCleanup.remove(event.getPlayer().getUniqueId())) {
            return;
        }
        Player player = event.getPlayer();
        armorCoinService.evaluateAndStrip(player);
        killTokenService.removeAllFromInventory(player);
    }
}
