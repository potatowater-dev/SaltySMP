package com.saltysmp.airreset;

import java.util.UUID;

public record TechSession(UUID projectileId, FallSnapshot snapshot, long expiresAtTick, boolean applied) {

    public TechSession withApplied() {
        return new TechSession(projectileId, snapshot, expiresAtTick, true);
    }

    public boolean isExpired(long currentTick) {
        return currentTick >= expiresAtTick;
    }
}
