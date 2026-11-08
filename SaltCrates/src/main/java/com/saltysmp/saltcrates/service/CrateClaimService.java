package com.saltysmp.saltcrates.service;

import com.saltysmp.saltcrates.SaltCratesPlugin;
import com.saltysmp.saltcrates.config.MessagesConfig;
import com.saltysmp.saltcrates.config.PluginConfig;
import com.saltysmp.saltcrates.gui.CrateClaimGui;
import com.saltysmp.saltcrates.model.CrateDefinition;
import com.saltysmp.saltcrates.model.LootEntry;
import com.saltysmp.saltcrates.util.BlockMaterialUtil;
import com.saltysmp.saltcrates.util.CooldownManager;
import com.saltysmp.saltcrates.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CrateClaimService {

    private final SaltCratesPlugin plugin;
    private final PluginConfig config;
    private final KeyService keyService;
    private final LootService lootService;
    private final CooldownManager cooldownManager;
    private final HologramService hologramService;
    private final PermissionLookupService permissionLookup;
    private final MessagesConfig messages;
    private final ClaimSessionRegistry sessions = new ClaimSessionRegistry();

    public CrateClaimService(
            SaltCratesPlugin plugin,
            PluginConfig config,
            MessagesConfig messages,
            KeyService keyService,
            LootService lootService,
            CooldownManager cooldownManager,
            HologramService hologramService,
            PermissionLookupService permissionLookup
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.keyService = keyService;
        this.lootService = lootService;
        this.cooldownManager = cooldownManager;
        this.hologramService = hologramService;
        this.permissionLookup = permissionLookup;
    }

    public ClaimSessionRegistry getSessions() {
        return sessions;
    }

    public void claim(Player player, CrateDefinition definition, String cooldownKey) {
        ClaimSessionRegistry.Session active = sessions.get(player);
        if (active != null && active.isRolling()) {
            return;
        }
        if (active != null) {
            active.cancelCloseTask();
            sessions.clearSession(player, active);
        }

        long cooldownMs = CrateCooldownService.resolveOpenCooldownMs(player, definition);
        if (cooldownMs > 0L && cooldownManager.isOnCooldown(cooldownKey)) {
            long remaining = cooldownManager.getRemainingMs(cooldownKey);
            messages.send(player, "cooldown-active", "time", MessageUtil.formatDuration(remaining));
            return;
        }
        if (!definition.hasLoot()) {
            MessageUtil.send(player, "<red>This crate has no loot configured.</red>");
            return;
        }
        if (!keyService.tryConsume(player, definition.getId())) {
            String message = definition.getNoKeyMessage() != null
                    ? definition.getNoKeyMessage()
                    : config.getDefaultNoKeyMessage();
            Component component = MessageUtil.parse(config.getPrefix() + message);
            player.sendMessage(component);
            if (config.isActionBarNoKey()) {
                player.sendActionBar(component);
            }
            return;
        }
        hologramService.refreshKeysFor(player);
        LootEntry reward = lootService.roll(definition);
        if (reward == null) {
            keyService.give(player, definition.getId(), 1);
            hologramService.refreshKeysFor(player);
            MessageUtil.send(player, "<red>Failed to roll loot. Your key was refunded.</red>");
            return;
        }
        if (shouldSkipAnimation(player)) {
            applyOpenCooldown(cooldownKey, cooldownMs);
            lootService.giveReward(player, reward.getItem());
            lootService.maybeBroadcast(player, definition, reward);
            player.playSound(player.getLocation(), config.getClaimSuccessSound(), config.getClaimSuccessVolume(), config.getClaimSuccessPitch());
            hologramService.refreshKeysFor(player);
            return;
        }
        startClaim(player, definition, reward, cooldownKey, cooldownMs);
    }

    private boolean shouldSkipAnimation(Player player) {
        if (!config.isClaimAnimationEnabled()) {
            return true;
        }
        if (!config.isClaimAnimationAllowSkipPermission()) {
            return false;
        }
        if (permissionLookup.isLuckPermsAvailable()) {
            return permissionLookup.hasPermission(player.getUniqueId(), "saltcrates.skip-animation");
        }
        return player.hasPermission("saltcrates.skip-animation");
    }

    public void startClaim(Player player, CrateDefinition definition, LootEntry reward, String cooldownKey, long cooldownMs) {
        applyOpenCooldown(cooldownKey, cooldownMs);
        CrateClaimGui gui = new CrateClaimGui(definition);
        ClaimSessionRegistry.Session session = sessions.start(player, gui, definition, reward);
        long sessionId = session.getSessionId();
        player.openInventory(gui.getInventory());

        Component rollingBar = MessageUtil.parse(BlockMaterialUtil.rollingActionBarTag(definition.getDisplayBlock()));
        player.sendActionBar(rollingBar);

        List<ItemStack> scrollPool = buildScrollPool(definition, reward);
        scrollPool.add(reward.getItem().clone());
        int rewardIndex = scrollPool.size() - 1;
        int pointerIndex = CrateClaimGui.POINTER_SLOT - CrateClaimGui.SPIN_START;
        int targetOffset = Math.max(0, rewardIndex - pointerIndex);
        int startOffset = Math.max(0, targetOffset - config.getClaimScrollDistance());
        int totalTicks = config.getClaimDurationTicks();

        gui.syncSpinRow(player, readWindow(scrollPool, startOffset));

        BukkitRunnable animation = new BukkitRunnable() {
            int tick = 0;
            double lastProgress = 0;

            @Override
            public void run() {
                ClaimSessionRegistry.Session current = sessions.get(player);
                if (current == null || current.getSessionId() != sessionId) {
                    cancel();
                    return;
                }
                if (!player.isOnline()) {
                    grantPendingReward(player, current);
                    sessions.end(player.getUniqueId(), sessionId);
                    cancel();
                    return;
                }
                if (!current.isRolling()) {
                    cancel();
                    return;
                }
                CrateClaimGui openGui = current.getGui();

                if (tick >= totalTicks) {
                    openGui.syncSpinRow(player, readWindow(scrollPool, targetOffset));
                    openGui.syncHighlightWinner(player);
                    completeClaim(player, definition, reward, openGui, sessionId);
                    cancel();
                    return;
                }

                player.sendActionBar(rollingBar);

                double progress = (double) tick / totalTicks;
                double eased = easeOutQuint(progress);
                int currentOffset = startOffset + (int) Math.round((targetOffset - startOffset) * eased);
                openGui.syncSpinRow(player, readWindow(scrollPool, currentOffset));

                double velocity = eased - lastProgress;
                lastProgress = eased;
                float pitch = 0.6f + (float) (velocity * 20);
                float volume = 0.25f + (float) Math.min(0.35, velocity * 3);
                if (velocity > 0.001) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch);
                }

                tick++;
            }
        };
        session.setAnimationTask(animation);
        animation.runTaskTimer(plugin, 2L, 1L);
    }

    private void completeClaim(
            Player player,
            CrateDefinition definition,
            LootEntry reward,
            CrateClaimGui gui,
            long sessionId
    ) {
        ClaimSessionRegistry.Session session = sessions.get(player);
        if (session == null || session.getSessionId() != sessionId || !session.isRolling()) {
            return;
        }
        grantPendingReward(player, session);
        lootService.maybeBroadcast(player, definition, reward);
        player.playSound(player.getLocation(), config.getClaimSuccessSound(), config.getClaimSuccessVolume(), config.getClaimSuccessPitch());
        hologramService.refreshKeysFor(player);
        session.finishRolling();
        player.sendActionBar(MessageUtil.parse(definition.getDisplayName()));

        BukkitRunnable closeTask = new BukkitRunnable() {
            @Override
            public void run() {
                ClaimSessionRegistry.Session current = sessions.get(player);
                if (current == null || current.getSessionId() != sessionId) {
                    return;
                }
                if (player.isOnline() && gui.isOpenFor(player)) {
                    player.closeInventory();
                }
                sessions.end(player.getUniqueId(), sessionId);
            }
        };
        session.setCloseTask(closeTask);
        closeTask.runTaskLater(plugin, 40L);
    }

    private void grantPendingReward(Player player, ClaimSessionRegistry.Session session) {
        if (session.isRewardGranted() || player == null) {
            return;
        }
        lootService.giveReward(player, session.getReward().getItem());
        session.markRewardGranted();
    }

    private void applyOpenCooldown(String cooldownKey, long cooldownMs) {
        if (cooldownMs > 0L) {
            cooldownManager.setCooldown(cooldownKey, cooldownMs);
        }
    }

    /** Ends an active roll immediately — used when the player enters combat. */
    public void interruptForCombat(Player player) {
        ClaimSessionRegistry.Session session = sessions.get(player);
        if (session == null || !session.isRolling()) {
            return;
        }
        finishSession(player, session);
    }

    public void handlePlayerQuit(Player player) {
        ClaimSessionRegistry.Session session = sessions.get(player);
        if (session != null && session.isRolling()) {
            grantPendingReward(player, session);
            session.finishRolling();
        }
        if (session != null) {
            session.cancelAllTasks();
        }
        sessions.end(player);
    }

    public void cancelAllSessions(boolean refundKeys) {
        for (java.util.UUID playerId : new java.util.ArrayList<>(sessions.getActivePlayers())) {
            Player player = plugin.getServer().getPlayer(playerId);
            ClaimSessionRegistry.Session session = sessions.get(playerId);
            if (session == null) {
                sessions.end(playerId, null);
                continue;
            }
            session.cancelAllTasks();
        if (refundKeys && player != null && !session.isRewardGranted()) {
                keyService.give(player, session.getDefinition().getId(), 1);
                hologramService.refreshKeysFor(player);
            } else if (player != null && !session.isRewardGranted()) {
                grantPendingReward(player, session);
            }
            session.finishRolling();
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
            sessions.end(playerId, session.getSessionId());
        }
    }

    private void finishSession(Player player, ClaimSessionRegistry.Session session) {
        session.cancelAnimation();
        session.cancelCloseTask();
        grantPendingReward(player, session);
        session.finishRolling();
        if (player.isOnline()) {
            player.closeInventory();
        }
        sessions.end(player.getUniqueId(), session.getSessionId());
    }

    private ItemStack[] readWindow(List<ItemStack> pool, int offset) {
        ItemStack[] row = new ItemStack[7];
        for (int i = 0; i < row.length; i++) {
            int index = offset + i;
            row[i] = index >= 0 && index < pool.size() ? pool.get(index).clone() : new ItemStack(org.bukkit.Material.BARRIER);
        }
        return row;
    }

    private List<ItemStack> buildScrollPool(CrateDefinition definition, LootEntry reward) {
        List<ItemStack> pool = new ArrayList<>();
        for (int cycle = 0; cycle < 10; cycle++) {
            for (LootEntry entry : definition.getLoot()) {
                for (int i = 0; i < Math.min(entry.getWeight(), 5); i++) {
                    pool.add(entry.getItem().clone());
                }
            }
        }
        if (pool.isEmpty()) {
            for (int i = 0; i < 32; i++) {
                pool.add(reward.getItem().clone());
            }
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return pool;
    }

    private double easeOutQuint(double t) {
        return 1 - Math.pow(1 - t, 5);
    }
}
