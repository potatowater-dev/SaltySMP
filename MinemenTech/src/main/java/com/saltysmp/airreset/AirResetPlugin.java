package com.saltysmp.airreset;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class AirResetPlugin extends JavaPlugin {

    private static final int WIND_CHARGE_COOLDOWN_TICKS = 10;

    private AirResetListener listener;
    private boolean enabled;
    private float minPitch;
    private double requiredDownwardVelocity;
    private int sessionTimeoutTicks;
    private int recordDurationTicks;
    private double spawnBelowFeet;
    private double riseSpeed;
    private double descentSpawnHeight;
    private double descentSpeed;
    private double hitProximity;
    private double riseOvershoot;
    private int postResetGuardTicks;
    private double postResetMaxDownwardVelocity;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        listener = new AirResetListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        listener.startCleanupTask();
    }

    public void loadSettings() {
        reloadConfig();

        enabled = getConfig().getBoolean("enabled", true);
        minPitch = (float) getConfig().getDouble("min-pitch", 80.0);
        requiredDownwardVelocity = getConfig().getDouble("required-downward-velocity", 0.8);
        sessionTimeoutTicks = getConfig().getInt("session-timeout-ticks", 20);
        recordDurationTicks = getConfig().getInt("record-duration-ticks", 20);
        spawnBelowFeet = getConfig().getDouble("spawn-below-feet", 1.2);
        riseSpeed = getConfig().getDouble("rise-speed", 1.5);
        descentSpawnHeight = getConfig().getDouble("descent-spawn-height", 1.6);
        descentSpeed = getConfig().getDouble("descent-speed", 1.25);
        hitProximity = getConfig().getDouble("hit-proximity", 1.0);
        riseOvershoot = getConfig().getDouble("rise-overshoot", 0.35);
        postResetGuardTicks = getConfig().getInt("post-reset-guard-ticks", 8);
        postResetMaxDownwardVelocity = getConfig().getDouble("post-reset-max-downward-velocity", 0.55);
        debug = getConfig().getBoolean("debug", false);

        if (FallStateAccess.isAvailable()) {
            getLogger().info("NMS fallStartPos access enabled.");
        } else {
            getLogger().warning("Could not access ServerPlayer.fallStartPos; falling height preservation is approximate.");
        }
    }

    public boolean isTechEnabled() {
        return enabled;
    }

    public float getMinPitch() {
        return minPitch;
    }

    public double getRequiredDownwardVelocity() {
        return requiredDownwardVelocity;
    }

    public int getSessionTimeoutTicks() {
        return sessionTimeoutTicks;
    }

    public int getRecordDurationTicks() {
        return recordDurationTicks;
    }

    public double getSpawnBelowFeet() {
        return spawnBelowFeet;
    }

    public double getRiseSpeed() {
        return riseSpeed;
    }

    public double getDescentSpawnHeight() {
        return descentSpawnHeight;
    }

    public double getDescentSpeed() {
        return descentSpeed;
    }

    public double getHitProximity() {
        return hitProximity;
    }

    public double getRiseOvershoot() {
        return riseOvershoot;
    }

    public int getPostResetGuardTicks() {
        return postResetGuardTicks;
    }

    public double getPostResetMaxDownwardVelocity() {
        return postResetMaxDownwardVelocity;
    }

    public int getWindChargeCooldownTicks() {
        return WIND_CHARGE_COOLDOWN_TICKS;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setRequiredDownwardVelocity(double value) {
        requiredDownwardVelocity = Math.max(0.01D, value);
        getConfig().set("required-downward-velocity", requiredDownwardVelocity);
        saveConfig();
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!command.getName().equalsIgnoreCase("airreset")) {
            return false;
        }

        if (!sender.hasPermission("airreset.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            sender.sendMessage("AirReset config reloaded.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("record")) {
            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }

            listener.startVelocityRecording(sender, target);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setvelocity")) {
            try {
                double velocity = Double.parseDouble(args[1]);
                setRequiredDownwardVelocity(velocity);
                sender.sendMessage("AirReset required downward velocity set to " + requiredDownwardVelocity);
            } catch (NumberFormatException exception) {
                sender.sendMessage("Invalid velocity value: " + args[1]);
            }
            return true;
        }

        sender.sendMessage("Usage: /airreset reload | record <player> | setvelocity <value>");
        return true;
    }
}
