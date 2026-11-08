package com.saltysmp.saltkoth.service;

import com.saltysmp.saltkoth.SaltKothPlugin;
import com.saltysmp.saltkoth.config.PluginConfig;
import com.saltysmp.saltkoth.model.KothArea;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class KothBorderService {

    private final SaltKothPlugin plugin;
    private final PluginConfig config;
    private BukkitTask particleTask;
    private Runnable renderCallback;

    public KothBorderService(SaltKothPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start(Runnable renderCallback) {
        this.renderCallback = renderCallback;
        stop();
        particleTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (renderCallback != null) {
                        renderCallback.run();
                    }
                },
                1L,
                Math.max(1L, config.getBorderParticleIntervalTicks())
        );
    }

    public void stop() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    public void preview(KothArea area) {
        if (!config.isBorderParticlesEnabled() || area == null || !area.isConfigured()) {
            return;
        }
        int durationTicks = config.getBorderPreviewSeconds() * 20;
        long interval = Math.max(1L, config.getBorderParticleIntervalTicks());
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> render(area, true), 0L, interval);
        plugin.getServer().getScheduler().runTaskLater(plugin, task::cancel, durationTicks);
    }

    public void render(KothArea area, boolean eventActive) {
        if (!config.isBorderParticlesEnabled()) {
            return;
        }
        if (config.isBorderParticlesActiveOnly() && !eventActive) {
            return;
        }
        World world = area.getWorld();
        if (world == null) {
            return;
        }

        double y = area.getMinY() + config.getBorderParticleYOffset();
        int spacing = Math.max(1, config.getBorderParticleSpacing());
        long viewDistanceSquared = (long) config.getBorderParticleViewDistance() * config.getBorderParticleViewDistance();

        Particle.DustOptions dust = new Particle.DustOptions(
                Color.fromRGB(config.getBorderParticleRed(), config.getBorderParticleGreen(), config.getBorderParticleBlue()),
                config.getBorderParticleSize()
        );

        List<Player> viewers = viewersNearArea(world, area, viewDistanceSquared);
        if (viewers.isEmpty()) {
            return;
        }

        for (int x = area.getMinX(); x <= area.getMaxX(); x += spacing) {
            spawn(world, viewers, x + 0.5, y, area.getMinZ() + 0.5, dust);
            spawn(world, viewers, x + 0.5, y, area.getMaxZ() + 0.5, dust);
        }
        for (int z = area.getMinZ() + spacing; z < area.getMaxZ(); z += spacing) {
            spawn(world, viewers, area.getMinX() + 0.5, y, z + 0.5, dust);
            spawn(world, viewers, area.getMaxX() + 0.5, y, z + 0.5, dust);
        }
    }

    private List<Player> viewersNearArea(World world, KothArea area, long viewDistanceSquared) {
        List<Player> viewers = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (distanceSquaredToArea(player.getLocation(), area) <= viewDistanceSquared) {
                viewers.add(player);
            }
        }
        return viewers;
    }

    private double distanceSquaredToArea(Location location, KothArea area) {
        double closestX = Math.max(area.getMinX(), Math.min(location.getX(), area.getMaxX() + 1.0));
        double closestY = Math.max(area.getMinY(), Math.min(location.getY(), area.getMaxY() + 1.0));
        double closestZ = Math.max(area.getMinZ(), Math.min(location.getZ(), area.getMaxZ() + 1.0));
        double dx = location.getX() - closestX;
        double dy = location.getY() - closestY;
        double dz = location.getZ() - closestZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private void spawn(World world, List<Player> viewers, double x, double y, double z, Particle.DustOptions dust) {
        Location location = new Location(world, x, y, z);
        int count = config.getBorderParticleCount();
        world.spawnParticle(Particle.DUST, location, count, 0.02, 0.02, 0.02, 0, dust, true);
        for (Player player : viewers) {
            player.spawnParticle(Particle.DUST, location, count, 0.02, 0.02, 0.02, 0, dust);
        }
    }
}
