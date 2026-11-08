package com.saltysmp.saltkoth.service;

import com.saltysmp.saltcrates.SaltCratesPlugin;
import com.saltysmp.saltkoth.SaltKothPlugin;
import com.saltysmp.saltkoth.config.PendingRewardStorage;
import com.saltysmp.saltkoth.config.PluginConfig;
import com.saltysmp.saltcrates.service.KeyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

public final class CrateRewardService {

    private final SaltKothPlugin plugin;
    private final PluginConfig config;
    private final PendingRewardStorage pendingRewards;

    public CrateRewardService(SaltKothPlugin plugin, PluginConfig config, PendingRewardStorage pendingRewards) {
        this.plugin = plugin;
        this.config = config;
        this.pendingRewards = pendingRewards;
    }

    public void rewardWinner(UUID playerId, String playerName) {
        int amount = config.getKeysPerWin();
        String crateId = config.getCrateId();
        if (giveKeys(playerId, crateId, amount)) {
            plugin.getLogger().info("Awarded " + amount + " " + crateId + " key(s) to " + playerName + " (" + playerId + ")");
            return;
        }
        pendingRewards.add(playerId, amount);
        plugin.getLogger().warning("SaltCrates unavailable — stored " + amount + " pending key(s) for " + playerName);
    }

    public void deliverPending(Player player) {
        int pending = pendingRewards.takeAll(player.getUniqueId());
        if (pending <= 0) {
            return;
        }
        if (giveKeys(player.getUniqueId(), config.getCrateId(), pending)) {
            plugin.getMessagesConfig().send(
                    player,
                    "winner-reward-offline",
                    "amount", String.valueOf(pending),
                    "crate", config.getCrateId()
            );
        } else {
            pendingRewards.add(player.getUniqueId(), pending);
        }
    }

    public void flushPending() {
        for (Map.Entry<UUID, Integer> entry : pendingRewards.snapshot().entrySet()) {
            UUID uuid = entry.getKey();
            int amount = entry.getValue();
            if (amount <= 0) {
                continue;
            }
            if (giveKeys(uuid, config.getCrateId(), amount)) {
                pendingRewards.takeAll(uuid);
            }
        }
    }

    private boolean giveKeys(UUID playerId, String crateId, int amount) {
        if (amount <= 0) {
            return false;
        }
        SaltCratesPlugin crates = resolveSaltCrates();
        if (crates == null) {
            return false;
        }
        if (crates.getDefinitionStorage().get(crateId) == null) {
            plugin.getLogger().warning(
                    "Crate '" + crateId + "' is not defined in SaltCrates. "
                            + "Create it with /crate create " + crateId
            );
            return false;
        }
        KeyService keyService = crates.getKeyService();
        keyService.give(playerId, crateId, amount);
        int balance = keyService.get(playerId, crateId);
        plugin.getLogger().info("SaltCrates balance for " + playerId + " (" + crateId + "): " + balance);

        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            plugin.getMessagesConfig().send(
                    online,
                    "winner-reward",
                    "amount", String.valueOf(amount),
                    "crate", crateId
            );
        }
        return true;
    }

    private SaltCratesPlugin resolveSaltCrates() {
        Plugin cratesPlugin = Bukkit.getPluginManager().getPlugin("SaltCrates");
        if (cratesPlugin instanceof SaltCratesPlugin crates && crates.isEnabled()) {
            return crates;
        }
        return null;
    }
}
