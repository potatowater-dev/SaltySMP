package com.saltysmp.saltplots.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MessageThrottle {

    private final long cooldownMs;
    private final Map<UUID, Long> lastSent = new ConcurrentHashMap<>();

    public MessageThrottle(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public boolean shouldSend(UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = lastSent.get(playerId);
        if (last != null && now - last < cooldownMs) {
            return false;
        }
        lastSent.put(playerId, now);
        return true;
    }
}
