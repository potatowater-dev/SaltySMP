package com.saltysmp.saltcrates.service;

import com.saltysmp.saltcrates.gui.CrateClaimGui;
import com.saltysmp.saltcrates.model.CrateDefinition;
import com.saltysmp.saltcrates.model.LootEntry;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ClaimSessionRegistry {

    private static final AtomicLong SESSION_ID = new AtomicLong();

    public static final class Session {
        private final long sessionId;
        private final CrateClaimGui gui;
        private final CrateDefinition definition;
        private final LootEntry reward;
        private volatile boolean rolling;
        private volatile boolean rewardGranted;
        private BukkitRunnable animationTask;
        private BukkitRunnable closeTask;

        public Session(CrateClaimGui gui, CrateDefinition definition, LootEntry reward) {
            this.sessionId = SESSION_ID.incrementAndGet();
            this.gui = gui;
            this.definition = definition;
            this.reward = reward;
            this.rolling = true;
        }

        public long getSessionId() {
            return sessionId;
        }

        public CrateClaimGui getGui() {
            return gui;
        }

        public CrateDefinition getDefinition() {
            return definition;
        }

        public LootEntry getReward() {
            return reward;
        }

        public boolean isRolling() {
            return rolling;
        }

        public boolean isRewardGranted() {
            return rewardGranted;
        }

        public void markRewardGranted() {
            rewardGranted = true;
        }

        public void finishRolling() {
            rolling = false;
        }

        public void setAnimationTask(BukkitRunnable animationTask) {
            this.animationTask = animationTask;
        }

        public void setCloseTask(BukkitRunnable closeTask) {
            cancelCloseTask();
            this.closeTask = closeTask;
        }

        public void cancelAnimation() {
            if (animationTask != null) {
                animationTask.cancel();
                animationTask = null;
            }
        }

        public void cancelCloseTask() {
            if (closeTask != null) {
                closeTask.cancel();
                closeTask = null;
            }
        }

        public void cancelAllTasks() {
            cancelAnimation();
            cancelCloseTask();
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public Session start(Player player, CrateClaimGui gui, CrateDefinition definition, LootEntry reward) {
        Session session = new Session(gui, definition, reward);
        sessions.put(player.getUniqueId(), session);
        return session;
    }

    public Session get(Player player) {
        if (player == null) {
            return null;
        }
        return sessions.get(player.getUniqueId());
    }

    public Session get(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean isRolling(Player player) {
        Session session = sessions.get(player.getUniqueId());
        return session != null && session.isRolling();
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void end(Player player) {
        if (player == null) {
            return;
        }
        end(player.getUniqueId(), null);
    }

    public void end(UUID playerId, Long expectedSessionId) {
        Session session = sessions.get(playerId);
        if (session == null) {
            return;
        }
        if (expectedSessionId != null && session.getSessionId() != expectedSessionId) {
            return;
        }
        sessions.remove(playerId);
        session.cancelAllTasks();
    }

    public void clearSession(Player player, Session session) {
        if (player == null || session == null) {
            return;
        }
        Session current = sessions.get(player.getUniqueId());
        if (current == session) {
            session.cancelAllTasks();
            sessions.remove(player.getUniqueId());
        }
    }

    public java.util.Collection<UUID> getActivePlayers() {
        return java.util.List.copyOf(sessions.keySet());
    }
}
