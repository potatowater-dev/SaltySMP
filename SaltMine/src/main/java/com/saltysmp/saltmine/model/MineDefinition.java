package com.saltysmp.saltmine.model;

import com.saltysmp.saltmine.util.RegionUtil;
import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class MineDefinition {

    private final String id;
    private RegionUtil.Cuboid region;
    private final Map<Material, Integer> blockWeights = new LinkedHashMap<>();
    private int resetIntervalMinutes;
    private boolean announceReset = true;
    private String resetBroadcast;

    public MineDefinition(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public RegionUtil.Cuboid getRegion() {
        return region;
    }

    public void setRegion(RegionUtil.Cuboid region) {
        this.region = region;
    }

    public Map<Material, Integer> getBlockWeights() {
        return Collections.unmodifiableMap(blockWeights);
    }

    public void setBlockWeight(Material material, int weight) {
        if (weight <= 0) {
            blockWeights.remove(material);
            return;
        }
        blockWeights.put(material, weight);
    }

    public boolean hasRegion() {
        return region != null;
    }

    public boolean hasBlocks() {
        return !blockWeights.isEmpty();
    }

    public long regionVolume() {
        return region == null ? 0 : region.volume();
    }

    public int getResetIntervalMinutes() {
        return resetIntervalMinutes;
    }

    public void setResetIntervalMinutes(int resetIntervalMinutes) {
        this.resetIntervalMinutes = Math.max(0, resetIntervalMinutes);
    }

    public int resolveResetIntervalMinutes(int globalDefault) {
        return resetIntervalMinutes > 0 ? resetIntervalMinutes : globalDefault;
    }

    public boolean isAnnounceReset() {
        return announceReset;
    }

    public void setAnnounceReset(boolean announceReset) {
        this.announceReset = announceReset;
    }

    public String getResetBroadcast() {
        return resetBroadcast;
    }

    public void setResetBroadcast(String resetBroadcast) {
        this.resetBroadcast = resetBroadcast;
    }

    public Material pickRandomBlock() {
        int totalWeight = blockWeights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            return Material.STONE;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (Map.Entry<Material, Integer> entry : blockWeights.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        return blockWeights.keySet().iterator().next();
    }
}
