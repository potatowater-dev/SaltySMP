package com.saltysmp.airreset;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AirResetListener implements Listener {

    private static final byte PHASE_RISE = 1;

    private final AirResetPlugin plugin;
    private final NamespacedKey customChargePhaseKey;
    private final Map<UUID, TechSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> recordings = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> riseProximityTrackers = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> postResetVelocityGuards = new ConcurrentHashMap<>();

    public AirResetListener(AirResetPlugin plugin) {
        this.plugin = plugin;
        this.customChargePhaseKey = new NamespacedKey(plugin, "custom_charge_phase");
    }

    public void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                purgeExpiredSessions();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void startVelocityRecording(org.bukkit.command.CommandSender sender, Player target) {
        BukkitTask existing = recordings.remove(target.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        int duration = Math.max(1, plugin.getRecordDurationTicks());
        sender.sendMessage("Recording downward velocity for " + target.getName() + " for " + duration + " ticks.");

        BukkitTask task = new BukkitRunnable() {
            private int ticks;
            private double mostNegativeY = 0.0D;

            @Override
            public void run() {
                if (!target.isOnline()) {
                    sender.sendMessage("Recording stopped: " + target.getName() + " went offline.");
                    recordings.remove(target.getUniqueId());
                    cancel();
                    return;
                }

                double yVelocity = target.getVelocity().getY();
                if (yVelocity < mostNegativeY) {
                    mostNegativeY = yVelocity;
                }

                ticks++;
                if (ticks < duration) {
                    return;
                }

                double recordedThreshold = Math.abs(mostNegativeY);
                plugin.setRequiredDownwardVelocity(recordedThreshold);
                sender.sendMessage(
                        "Recorded downward velocity for "
                                + target.getName()
                                + ": "
                                + recordedThreshold
                                + " (set as required-downward-velocity)"
                );
                recordings.remove(target.getUniqueId());
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);

        recordings.put(target.getUniqueId(), task);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileLaunch(PlayerLaunchProjectileEvent event) {
        if (!plugin.isTechEnabled()) {
            return;
        }

        if (event.getItemStack().getType() != Material.WIND_CHARGE) {
            return;
        }

        Player player = event.getPlayer();
        if (!qualifies(player)) {
            return;
        }

        event.setCancelled(true);
        consumeOneWindCharge(player);
        player.setCooldown(Material.WIND_CHARGE, plugin.getWindChargeCooldownTicks());

        FallSnapshot snapshot = FallStateAccess.capture(player);
        double fallSpeed = Math.abs(player.getVelocity().getY());
        WindCharge riseCharge = spawnRiseCharge(player, fallSpeed);
        long expiresAtTick = currentTick(player) + plugin.getSessionTimeoutTicks();
        sessions.put(player.getUniqueId(), new TechSession(riseCharge.getUniqueId(), snapshot, expiresAtTick, false));
        startRiseProximityTracker(player, riseCharge, fallSpeed);

        plugin.getServer().getScheduler().runTask(plugin, () -> tryForceRiseHit(player, riseCharge, fallSpeed));

        if (plugin.isDebug()) {
            plugin.getLogger().info(
                    "Spawned rise wind charge below "
                            + player.getName()
                            + " (vy="
                            + player.getVelocity().getY()
                            + ")"
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof WindCharge windCharge)) {
            return;
        }

        Byte phase = windCharge.getPersistentDataContainer().get(customChargePhaseKey, PersistentDataType.BYTE);
        if (phase == null) {
            return;
        }

        if (!(windCharge.getShooter() instanceof Player player)) {
            return;
        }

        if (phase != PHASE_RISE) {
            return;
        }

        if (event.getHitEntity() != player) {
            return;
        }

        TechSession session = sessions.get(player.getUniqueId());
        if (session == null || session.applied() || session.isExpired(currentTick(player))) {
            explodeAndRemove(windCharge);
            return;
        }

        if (!session.projectileId().equals(windCharge.getUniqueId())) {
            explodeAndRemove(windCharge);
            return;
        }

        applyHitEffect(player, session);
        cancelRiseProximityTracker(player.getUniqueId());
        explodeAndRemove(windCharge);

        if (plugin.isDebug()) {
            plugin.getLogger().info(
                    "Rise charge hit "
                            + player.getName()
                            + ": velocity cancelled, fall state preserved, charge exploded."
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackEvent event) {
        if (!plugin.isTechEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        TechSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.applied()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!sessions.containsKey(player.getUniqueId())) {
            return;
        }

        if (player.isOnGround()) {
            cancelRiseProximityTracker(player.getUniqueId());
            cancelPostResetVelocityGuard(player.getUniqueId());
            sessions.remove(player.getUniqueId());
        }
    }

    private boolean qualifies(Player player) {
        if (player.isOnGround()) {
            return false;
        }

        if (player.getPitch() < plugin.getMinPitch()) {
            return false;
        }

        return player.getVelocity().getY() <= -plugin.getRequiredDownwardVelocity();
    }

    private WindCharge spawnRiseCharge(Player player, double fallSpeed) {
        double riseSpeed = resolveRiseSpeed(fallSpeed);
        double spawnOffset = resolveSpawnOffset(fallSpeed);
        Location spawnLocation = player.getLocation().clone().subtract(0.0D, spawnOffset, 0.0D);
        WindCharge windCharge = (WindCharge) player.getWorld().spawnEntity(spawnLocation, EntityType.WIND_CHARGE);
        tagCharge(windCharge, PHASE_RISE);
        windCharge.setShooter(player);
        windCharge.setHasBeenShot(true);
        windCharge.setVelocity(new Vector(0.0D, riseSpeed, 0.0D));

        if (plugin.isDebug()) {
            plugin.getLogger().info(
                    "Rise charge for "
                            + player.getName()
                            + ": fallSpeed="
                            + fallSpeed
                            + ", riseSpeed="
                            + riseSpeed
                            + ", spawnOffset="
                            + spawnOffset
            );
        }

        return windCharge;
    }

    private double resolveRiseSpeed(double fallSpeed) {
        return Math.max(plugin.getRiseSpeed(), fallSpeed + plugin.getRiseOvershoot());
    }

    private double resolveSpawnOffset(double fallSpeed) {
        double maxOffset = plugin.getSpawnBelowFeet();
        if (fallSpeed <= 0.01D) {
            return maxOffset;
        }

        double scaled = maxOffset * (plugin.getRiseSpeed() / fallSpeed);
        return Math.max(0.35D, Math.min(maxOffset, scaled));
    }

    private double resolveHitProximity(double fallSpeed) {
        return plugin.getHitProximity() + Math.min(2.0D, fallSpeed * 0.25D);
    }

    private void explodeAndRemove(WindCharge windCharge) {
        if (!windCharge.isValid() || windCharge.isDead()) {
            return;
        }

        windCharge.explode();
        if (windCharge.isValid()) {
            windCharge.remove();
        }
    }

    private void startRiseProximityTracker(Player player, WindCharge riseCharge, double fallSpeed) {
        cancelRiseProximityTracker(player.getUniqueId());

        BukkitTask task = new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                ticks++;

                if (!player.isOnline() || !riseCharge.isValid() || riseCharge.isDead()) {
                    cancelRiseProximityTracker(player.getUniqueId());
                    cancel();
                    return;
                }

                TechSession session = sessions.get(player.getUniqueId());
                if (session == null
                        || session.applied()
                        || session.isExpired(currentTick(player))
                        || !session.projectileId().equals(riseCharge.getUniqueId())) {
                    cancelRiseProximityTracker(player.getUniqueId());
                    cancel();
                    return;
                }

                if (ticks > plugin.getSessionTimeoutTicks()) {
                    cancelRiseProximityTracker(player.getUniqueId());
                    cancel();
                    return;
                }

                if (tryForceRiseHit(player, riseCharge, fallSpeed)) {
                    cancelRiseProximityTracker(player.getUniqueId());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        riseProximityTrackers.put(player.getUniqueId(), task);
    }

    private boolean tryForceRiseHit(Player player, WindCharge riseCharge, double fallSpeed) {
        double proximity = resolveHitProximity(fallSpeed);
        double distanceSquared = riseCharge.getLocation().distanceSquared(player.getLocation());
        if (distanceSquared > proximity * proximity) {
            return false;
        }

        if (plugin.isDebug()) {
            plugin.getLogger().info(
                    "Forcing rise charge hit for "
                            + player.getName()
                            + " at distance "
                            + Math.sqrt(distanceSquared)
                            + " (fallSpeed="
                            + fallSpeed
                            + ")"
            );
        }

        riseCharge.hitEntity(player);
        return true;
    }

    private void cancelRiseProximityTracker(UUID playerId) {
        BukkitTask task = riseProximityTrackers.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void cancelPostResetVelocityGuard(UUID playerId) {
        BukkitTask task = postResetVelocityGuards.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void startPostResetVelocityGuard(Player player) {
        cancelPostResetVelocityGuard(player.getUniqueId());

        int guardTicks = Math.max(1, plugin.getPostResetGuardTicks());
        double maxDownward = Math.abs(plugin.getPostResetMaxDownwardVelocity());

        BukkitTask task = new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                ticks++;

                if (!player.isOnline() || player.isOnGround() || ticks > guardTicks) {
                    cancelPostResetVelocityGuard(player.getUniqueId());
                    cancel();
                    return;
                }

                TechSession session = sessions.get(player.getUniqueId());
                if (session == null || !session.applied()) {
                    cancelPostResetVelocityGuard(player.getUniqueId());
                    cancel();
                    return;
                }

                Vector velocity = player.getVelocity();
                if (velocity.getY() < -maxDownward) {
                    player.setVelocity(new Vector(velocity.getX(), -maxDownward, velocity.getZ()));
                    if (plugin.isDebug()) {
                        plugin.getLogger().info(
                                "Clamped post-reset downward velocity for "
                                        + player.getName()
                                        + " from "
                                        + velocity.getY()
                                        + " to "
                                        + (-maxDownward)
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        postResetVelocityGuards.put(player.getUniqueId(), task);
    }

    private void tagCharge(WindCharge windCharge, byte phase) {
        windCharge.getPersistentDataContainer().set(customChargePhaseKey, PersistentDataType.BYTE, phase);
    }

    private void applyHitEffect(Player player, TechSession session) {
        sessions.put(player.getUniqueId(), session.withApplied());

        Vector current = player.getVelocity();
        player.setVelocity(new Vector(current.getX(), 0.0D, current.getZ()));

        FallStateAccess.restore(player, session.snapshot());
        scheduleOneTickRestore(player, session.snapshot());
        startPostResetVelocityGuard(player);

        if (plugin.isDebug()) {
            plugin.getLogger().info(
                    "Cancelled player velocity for "
                            + player.getName()
                            + " (was "
                            + current.getY()
                            + ") while preserving fall state."
            );
        }
    }

    private void consumeOneWindCharge(Player player) {
        if (player.getInventory().getItemInMainHand().getType() == Material.WIND_CHARGE) {
            decrementItem(player.getInventory().getItemInMainHand());
            return;
        }

        if (player.getInventory().getItemInOffHand().getType() == Material.WIND_CHARGE) {
            decrementItem(player.getInventory().getItemInOffHand());
        }
    }

    private void decrementItem(org.bukkit.inventory.ItemStack itemStack) {
        itemStack.setAmount(Math.max(0, itemStack.getAmount() - 1));
    }

    private void scheduleOneTickRestore(Player player, FallSnapshot snapshot) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.isOnGround()) {
                return;
            }

            FallStateAccess.restore(player, snapshot);
        }, 1L);
    }

    private void purgeExpiredSessions() {
        Iterator<Map.Entry<UUID, TechSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TechSession> entry = iterator.next();
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || entry.getValue().isExpired(currentTick(player))) {
                cancelRiseProximityTracker(entry.getKey());
                cancelPostResetVelocityGuard(entry.getKey());
                iterator.remove();
            }
        }
    }

    private long currentTick(Player player) {
        return player.getWorld().getFullTime();
    }
}
