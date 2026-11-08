package com.saltysmp.saltplots.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Plot {

    private final UUID ownerId;
    private int originX;
    private int originZ;
    private int tierIndex;
    private boolean isPrivate;
    private final List<UUID> trusted;

    public Plot(UUID ownerId, int originX, int originZ, int tierIndex) {
        this.ownerId = ownerId;
        this.originX = originX;
        this.originZ = originZ;
        this.tierIndex = tierIndex;
        this.isPrivate = false;
        this.trusted = new ArrayList<>();
    }

    public Plot(
            UUID ownerId,
            int originX,
            int originZ,
            int tierIndex,
            boolean isPrivate,
            List<UUID> trusted
    ) {
        this.ownerId = ownerId;
        this.originX = originX;
        this.originZ = originZ;
        this.tierIndex = tierIndex;
        this.isPrivate = isPrivate;
        this.trusted = new ArrayList<>(trusted);
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginZ() {
        return originZ;
    }

    public int getTierIndex() {
        return tierIndex;
    }

    public void setTierIndex(int tierIndex) {
        this.tierIndex = tierIndex;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public List<UUID> getTrusted() {
        return Collections.unmodifiableList(trusted);
    }

    public boolean isTrusted(UUID playerId) {
        return trusted.contains(playerId);
    }

    public boolean addTrusted(UUID playerId) {
        return trusted.add(playerId);
    }

    public boolean removeTrusted(UUID playerId) {
        return trusted.remove(playerId);
    }

    public boolean contains(int x, int z, int size) {
        return x >= originX && x < originX + size
                && z >= originZ && z < originZ + size;
    }
}
