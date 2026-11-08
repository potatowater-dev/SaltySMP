package com.saltysmp.saltkits.cleanup;

import com.saltysmp.saltkits.SaltKitsPlugin;
import com.saltysmp.saltkits.config.MessagesConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BlockCleanupService {

    private final SaltKitsPlugin plugin;
    private final MessagesConfig messages;
    private final Map<String, ConfiguredRegion> configuredRegions = new LinkedHashMap<>();
    private final Set<Material> blocks = EnumSet.noneOf(Material.class);
    private int blocksPerTick = 4000;
    private boolean running;

    public BlockCleanupService(SaltKitsPlugin plugin, MessagesConfig messages) {
        this.plugin = plugin;
        this.messages = messages;
        reload();
    }

    public void reload() {
        configuredRegions.clear();
        blocks.clear();
        var config = plugin.getConfig();
        blocksPerTick = Math.max(100, config.getInt("block-cleanup.blocks-per-tick", 4000));
        for (String name : config.getStringList("block-cleanup.blocks")) {
            Material material = parseMaterial(name);
            if (material == null) {
                plugin.getLogger().warning("Unknown cleanup block: " + name);
                continue;
            }
            blocks.add(material);
        }
        ConfigurationSection section = config.getConfigurationSection("block-cleanup.regions");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection region = section.getConfigurationSection(id);
            if (region == null) {
                continue;
            }
            configuredRegions.put(id.toLowerCase(Locale.ROOT), new ConfiguredRegion(
                    id,
                    region.getString("world", "world"),
                    region.getString("worldguard"),
                    parseCoord(region, "min"),
                    parseCoord(region, "max")
            ));
        }
    }

    public List<String> regionIds() {
        return new ArrayList<>(configuredRegions.keySet());
    }

    public void cleanup(CommandSender sender, String regionId) {
        if (running) {
            messages.send(sender, "cleanup-running");
            return;
        }
        if (blocks.isEmpty()) {
            messages.send(sender, "cleanup-no-blocks");
            return;
        }
        List<ConfiguredRegion> selected = new ArrayList<>();
        if (regionId == null || regionId.isBlank()) {
            selected.addAll(configuredRegions.values());
        } else {
            ConfiguredRegion match = configuredRegions.get(regionId.toLowerCase(Locale.ROOT));
            if (match == null) {
                messages.send(sender, "cleanup-unknown-region", "region", regionId);
                return;
            }
            selected.add(match);
        }
        if (selected.isEmpty()) {
            messages.send(sender, "cleanup-no-regions");
            return;
        }

        List<CleanupRegion> resolved = new ArrayList<>();
        for (ConfiguredRegion configured : selected) {
            CleanupRegion volume = resolve(configured);
            if (volume == null) {
                continue;
            }
            resolved.add(volume);
        }
        if (resolved.isEmpty()) {
            messages.send(sender, "cleanup-resolve-failed");
            return;
        }

        running = true;
        messages.send(sender, "cleanup-started", "count", String.valueOf(resolved.size()));
        runNext(sender, resolved, 0, 0);
    }

    private void runNext(CommandSender sender, List<CleanupRegion> regions, int index, int removedTotal) {
        if (index >= regions.size()) {
            running = false;
            messages.send(sender, "cleanup-done", "removed", String.valueOf(removedTotal));
            return;
        }
        CleanupRegion region = regions.get(index);
        messages.send(sender, "cleanup-region-started", "region", region.id(), "world", region.world().getName());
        scanRegion(sender, regions, index, region, region.minX(), region.minY(), region.minZ(), 0, removedTotal);
    }

    private void scanRegion(
            CommandSender sender,
            List<CleanupRegion> regions,
            int index,
            CleanupRegion region,
            int x,
            int y,
            int z,
            int removed,
            int removedTotal
    ) {
        World world = region.world();
        int processed = 0;
        int currentX = x;
        int currentY = y;
        int currentZ = z;
        int extraRemoved = 0;

        while (currentX <= region.maxX()) {
            while (currentY <= region.maxY()) {
                while (currentZ <= region.maxZ()) {
                    if (processed >= blocksPerTick) {
                        final int resumeX = currentX;
                        final int resumeY = currentY;
                        final int resumeZ = currentZ;
                        final int nextRemoved = removed + extraRemoved;
                        Bukkit.getScheduler().runTask(plugin, () -> scanRegion(
                                sender, regions, index, region, resumeX, resumeY, resumeZ, nextRemoved, removedTotal
                        ));
                        return;
                    }
                    processed++;
                    if (region.contains(currentX, currentY, currentZ)) {
                        Block block = world.getBlockAt(currentX, currentY, currentZ);
                        if (blocks.contains(block.getType())) {
                            block.setType(Material.AIR, false);
                            extraRemoved++;
                        }
                    }
                    currentZ++;
                }
                currentZ = region.minZ();
                currentY++;
            }
            currentY = region.minY();
            currentX++;
        }

        int regionTotal = removed + extraRemoved;
        messages.send(sender, "cleanup-region-done", "region", region.id(), "removed", String.valueOf(regionTotal));
        Bukkit.getScheduler().runTask(plugin, () -> runNext(sender, regions, index + 1, removedTotal + regionTotal));
    }

    private CleanupRegion resolve(ConfiguredRegion configured) {
        World world = Bukkit.getWorld(configured.worldName());
        if (world == null) {
            plugin.getLogger().warning("Cleanup region \"" + configured.id() + "\" world not found: " + configured.worldName());
            return null;
        }
        if (configured.worldGuard() != null && !configured.worldGuard().isBlank()) {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
                plugin.getLogger().warning("Cleanup region \"" + configured.id() + "\" needs WorldGuard, but it is not loaded.");
                return null;
            }
            CleanupRegion volume = WorldGuardHook.resolve(configured.id(), world, configured.worldGuard());
            if (volume == null) {
                plugin.getLogger().warning("WorldGuard region not found: " + configured.worldGuard() + " in " + world.getName());
            }
            return volume;
        }
        if (configured.min() == null || configured.max() == null) {
            plugin.getLogger().warning("Cleanup region \"" + configured.id() + "\" needs worldguard or min/max.");
            return null;
        }
        int minX = Math.min(configured.min()[0], configured.max()[0]);
        int minY = Math.min(configured.min()[1], configured.max()[1]);
        int minZ = Math.min(configured.min()[2], configured.max()[2]);
        int maxX = Math.max(configured.min()[0], configured.max()[0]);
        int maxY = Math.max(configured.min()[1], configured.max()[1]);
        int maxZ = Math.max(configured.min()[2], configured.max()[2]);
        return new CleanupRegion(configured.id(), world, minX, minY, minZ, maxX, maxY, maxZ, CleanupRegion.always());
    }

    private static Material parseMaterial(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim().toUpperCase(Locale.ROOT).replace("MINECRAFT:", "");
        if (key.equals("FLOWING_WATER") || key.equals("STATIONARY_WATER")) {
            return Material.WATER;
        }
        return Material.matchMaterial(key);
    }

    private static int[] parseCoord(ConfigurationSection section, String key) {
        if (section.isList(key)) {
            List<Integer> list = section.getIntegerList(key);
            if (list.size() >= 3) {
                return new int[]{list.get(0), list.get(1), list.get(2)};
            }
        }
        String raw = section.getString(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length < 3) {
            return null;
        }
        try {
            return new int[]{
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            };
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record ConfiguredRegion(String id, String worldName, String worldGuard, int[] min, int[] max) {
    }
}
