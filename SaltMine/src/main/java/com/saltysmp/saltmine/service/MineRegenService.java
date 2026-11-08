package com.saltysmp.saltmine.service;

import com.saltysmp.saltmine.SaltMinePlugin;
import com.saltysmp.saltmine.config.MessagesConfig;
import com.saltysmp.saltmine.config.PluginConfig;
import com.saltysmp.saltmine.model.MineDefinition;
import com.saltysmp.saltmine.storage.MineStorage;
import com.saltysmp.saltmine.util.MessageUtil;
import com.saltysmp.saltmine.util.RegionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MineRegenService {

    private final SaltMinePlugin plugin;
    private final PluginConfig pluginConfig;
    private final MessagesConfig messages;
    private final MineStorage mineStorage;
    private BukkitTask checkTask;
    private BukkitTask placementTask;
    private final ConcurrentLinkedQueue<PendingPlacement> pendingPlacements = new ConcurrentLinkedQueue<>();
    private final Map<String, Long> lastResetAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> pendingByMine = new ConcurrentHashMap<>();

    public MineRegenService(
            SaltMinePlugin plugin,
            PluginConfig pluginConfig,
            MessagesConfig messages,
            MineStorage mineStorage
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messages = messages;
        this.mineStorage = mineStorage;
    }

    public void start() {
        stop();
        long now = System.currentTimeMillis();
        for (MineDefinition mine : mineStorage.getAll()) {
            lastResetAt.putIfAbsent(mine.getId(), now);
        }
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkResets, 20L, 20L);
        placementTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processPlacementQueue, 1L, 1L);
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        if (placementTask != null) {
            placementTask.cancel();
            placementTask = null;
        }
        pendingPlacements.clear();
        pendingByMine.clear();
    }

    public void restart() {
        start();
    }

    public long getRemainingMs(String mineId) {
        MineDefinition mine = mineStorage.get(mineId);
        if (mine == null) {
            return -1L;
        }
        int intervalMinutes = mine.resolveResetIntervalMinutes(pluginConfig.getResetIntervalMinutes());
        long intervalMs = intervalMinutes * 60_000L;
        long lastReset = lastResetAt.getOrDefault(mine.getId(), System.currentTimeMillis());
        return Math.max(0L, intervalMs - (System.currentTimeMillis() - lastReset));
    }

    public String formatRemainingMinutesSeconds(String mineId) {
        long remainingMs = getRemainingMs(mineId);
        if (remainingMs < 0L) {
            return "";
        }
        long totalSeconds = remainingMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + String.format("%02d", seconds);
    }

    public void regenerate(String mineId) {
        MineDefinition mine = mineStorage.get(mineId);
        if (mine == null || !mine.hasRegion() || !mine.hasBlocks()) {
            return;
        }
        scheduleFullReset(mine, true);
    }

    private void checkResets() {
        long now = System.currentTimeMillis();
        int globalIntervalMinutes = pluginConfig.getResetIntervalMinutes();
        for (MineDefinition mine : mineStorage.getAll()) {
            if (!mine.hasRegion() || !mine.hasBlocks()) {
                continue;
            }
            if (pendingByMine.getOrDefault(mine.getId(), 0) > 0) {
                continue;
            }
            int intervalMinutes = mine.resolveResetIntervalMinutes(globalIntervalMinutes);
            long intervalMs = intervalMinutes * 60_000L;
            long lastReset = lastResetAt.getOrDefault(mine.getId(), now);
            if (now - lastReset >= intervalMs) {
                scheduleFullReset(mine, false);
            }
        }
    }

    private void scheduleFullReset(MineDefinition mine, boolean manual) {
        RegionUtil.Cuboid region = mine.getRegion();
        if (region == null) {
            return;
        }
        World world = Bukkit.getWorld(region.worldName());
        if (world == null) {
            plugin.getLogger().warning("World not loaded for mine " + mine.getId() + ": " + region.worldName());
            return;
        }
        if (pendingByMine.getOrDefault(mine.getId(), 0) > 0) {
            return;
        }

        lastResetAt.put(mine.getId(), System.currentTimeMillis());

        broadcastReset(mine, manual);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<RegionUtil.BlockSnapshot> positions = RegionUtil.collectAllBlocks(region);
            List<PendingPlacement> placements = new ArrayList<>(positions.size());
            for (RegionUtil.BlockSnapshot snapshot : positions) {
                Material material = mine.pickRandomBlock();
                placements.add(new PendingPlacement(
                        mine.getId(),
                        world.getName(),
                        snapshot.x(),
                        snapshot.y(),
                        snapshot.z(),
                        material
                ));
            }
            pendingPlacements.addAll(placements);
            pendingByMine.merge(mine.getId(), placements.size(), Integer::sum);
        });
    }

    private void broadcastReset(MineDefinition mine, boolean manual) {
        if (!pluginConfig.isAnnounceResets() || !mine.isAnnounceReset()) {
            return;
        }
        String minutes = String.valueOf(mine.resolveResetIntervalMinutes(pluginConfig.getResetIntervalMinutes()));
        String text = resolveBroadcastText(mine, manual, minutes);
        if (text == null || text.isBlank()) {
            return;
        }
        Bukkit.broadcast(MessageUtil.parse(text));
    }

    private String resolveBroadcastText(MineDefinition mine, boolean manual, String minutes) {
        String custom = mine.getResetBroadcast();
        if (custom != null && !custom.isBlank()) {
            String trimmed = custom.trim();
            if (isMessageKey(trimmed)) {
                return messages.format(trimmed, "id", mine.getId(), "minutes", minutes);
            }
            String text = trimmed.replace("{prefix}", messages.get("prefix"));
            return MessageUtil.format(text, "id", mine.getId(), "minutes", minutes);
        }
        String key = manual ? "reset-manual-broadcast" : "reset-broadcast";
        return messages.format(key, "id", mine.getId(), "minutes", minutes);
    }

    private boolean isMessageKey(String value) {
        if (value.contains("<") || value.contains(" ") || value.contains("&")) {
            return false;
        }
        return messages.has(value);
    }

    private void processPlacementQueue() {
        int budget = pluginConfig.getBlocksPerTick();
        int placed = 0;
        PendingPlacement next;
        while (placed < budget && (next = pendingPlacements.poll()) != null) {
            World world = Bukkit.getWorld(next.worldName());
            if (world != null) {
                world.getBlockAt(next.x(), next.y(), next.z()).setType(next.material(), false);
            }
            pendingByMine.computeIfPresent(next.mineId(), (id, count) -> count <= 1 ? null : count - 1);
            placed++;
        }
    }

    private record PendingPlacement(
            String mineId,
            String worldName,
            int x,
            int y,
            int z,
            Material material
    ) {
    }
}
