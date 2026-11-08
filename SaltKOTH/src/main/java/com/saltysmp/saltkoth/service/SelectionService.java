package com.saltysmp.saltkoth.service;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SelectionService {

    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public void setPos1(UUID playerId, Location location) {
        pos1.put(playerId, location.clone());
    }

    public void setPos2(UUID playerId, Location location) {
        pos2.put(playerId, location.clone());
    }

    public Location getPos1(UUID playerId) {
        Location location = pos1.get(playerId);
        return location == null ? null : location.clone();
    }

    public Location getPos2(UUID playerId) {
        Location location = pos2.get(playerId);
        return location == null ? null : location.clone();
    }

    public void clear(UUID playerId) {
        pos1.remove(playerId);
        pos2.remove(playerId);
    }
}
