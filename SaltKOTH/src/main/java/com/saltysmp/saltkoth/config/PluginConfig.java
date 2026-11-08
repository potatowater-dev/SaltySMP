package com.saltysmp.saltkoth.config;



import com.saltysmp.saltkoth.SaltKothPlugin;

import org.bukkit.Sound;



public final class PluginConfig {



    private final SaltKothPlugin plugin;

    private long delayBetweenEventsMs;

    private long eventDurationMs;

    private String crateId;

    private int keysPerWin;

    private int tickIntervalTicks;

    private int minimumWinSeconds;

    private boolean broadcastStart;

    private boolean broadcastEnd;

    private boolean leaderChangeBroadcast;

    private boolean contestedActionBar;

    private boolean titlesEnabled;

    private boolean captureSoundEnabled;

    private Sound captureSound;

    private boolean captureBroadcastEnabled;

    private int captureBroadcastIntervalSeconds;

    private boolean bossbarEnabled;

    private boolean bossbarZoneOnly;

    private boolean hideTabInZone;

    private boolean scoreboardEnabled;

    private boolean borderParticlesEnabled;

    private boolean borderParticlesActiveOnly;

    private int borderParticleSpacing;

    private int borderParticleViewDistance;

    private double borderParticleYOffset;

    private float borderParticleSize;

    private int borderParticleCount;

    private int borderParticleRed;

    private int borderParticleGreen;

    private int borderParticleBlue;

    private int borderParticleIntervalTicks;

    private int borderPreviewSeconds;
    private String titleHologramText;
    private float titleHologramScale;
    private double titleHologramHeight;



    public PluginConfig(SaltKothPlugin plugin) {

        this.plugin = plugin;

    }



    public void load() {

        plugin.saveDefaultConfig();

        plugin.reloadConfig();

        delayBetweenEventsMs = plugin.getConfig().getLong("delay-between-events-ms", 10_800_000L);

        eventDurationMs = plugin.getConfig().getLong("event-duration-ms", 1_800_000L);

        crateId = plugin.getConfig().getString("crate-id", "koth").toLowerCase();

        keysPerWin = Math.max(1, plugin.getConfig().getInt("keys-per-win", 1));

        tickIntervalTicks = Math.max(1, plugin.getConfig().getInt("tick-interval-ticks", 20));

        minimumWinSeconds = Math.max(0, plugin.getConfig().getInt("minimum-win-seconds", 30));

        broadcastStart = plugin.getConfig().getBoolean("broadcast-start", true);

        broadcastEnd = plugin.getConfig().getBoolean("broadcast-end", true);

        leaderChangeBroadcast = plugin.getConfig().getBoolean("leader-change-broadcast", true);

        contestedActionBar = plugin.getConfig().getBoolean("contested-action-bar", true);

        titlesEnabled = plugin.getConfig().getBoolean("titles-enabled", true);

        captureSoundEnabled = plugin.getConfig().getBoolean("capture-sound-enabled", false);

        captureSound = parseSound(plugin.getConfig().getString("capture-sound", "BLOCK_NOTE_BLOCK_PLING"));

        captureBroadcastEnabled = plugin.getConfig().getBoolean("capture-broadcast-enabled", true);
        captureBroadcastIntervalSeconds = Math.max(1, plugin.getConfig().getInt("capture-broadcast-interval-seconds", 15));

        bossbarEnabled = plugin.getConfig().getBoolean("bossbar-enabled", true);

        bossbarZoneOnly = plugin.getConfig().getBoolean("bossbar-zone-only", true);
        hideTabInZone = plugin.getConfig().getBoolean("hide-tab-in-zone", true);
        scoreboardEnabled = plugin.getConfig().getBoolean("scoreboard-enabled", false);

        borderParticlesEnabled = plugin.getConfig().getBoolean("border-particles.enabled", true);

        borderParticlesActiveOnly = plugin.getConfig().getBoolean("border-particles.active-only", false);

        borderParticleSpacing = Math.max(1, plugin.getConfig().getInt("border-particles.spacing", 1));

        borderParticleViewDistance = Math.max(16, plugin.getConfig().getInt("border-particles.view-distance", 96));

        borderParticleYOffset = plugin.getConfig().getDouble("border-particles.y-offset", 1.05);

        borderParticleSize = (float) plugin.getConfig().getDouble("border-particles.size", 1.5);

        borderParticleCount = Math.max(1, plugin.getConfig().getInt("border-particles.count", 2));

        borderParticleRed = plugin.getConfig().getInt("border-particles.color.red", 255);

        borderParticleGreen = plugin.getConfig().getInt("border-particles.color.green", 0);

        borderParticleBlue = plugin.getConfig().getInt("border-particles.color.blue", 0);

        borderParticleIntervalTicks = Math.max(1, plugin.getConfig().getInt("border-particles.interval-ticks", 2));

        borderPreviewSeconds = Math.max(3, plugin.getConfig().getInt("border-particles.preview-seconds", 15));
        titleHologramText = plugin.getConfig().getString("title-hologram.text", "<red><bold>KOTH");
        titleHologramScale = (float) plugin.getConfig().getDouble("title-hologram.scale", 20.0);
        titleHologramHeight = plugin.getConfig().getDouble("title-hologram.height", 3.0);
    }



    private Sound parseSound(String name) {

        try {

            return Sound.valueOf(name.toUpperCase());

        } catch (IllegalArgumentException exception) {

            return Sound.BLOCK_NOTE_BLOCK_PLING;

        }

    }



    public long getDelayBetweenEventsMs() {

        return delayBetweenEventsMs;

    }



    public long getEventDurationMs() {

        return eventDurationMs;

    }



    public String getCrateId() {

        return crateId;

    }



    public int getKeysPerWin() {

        return keysPerWin;

    }



    public int getTickIntervalTicks() {

        return tickIntervalTicks;

    }



    public int getMinimumWinSeconds() {

        return minimumWinSeconds;

    }



    public boolean isBroadcastStart() {

        return broadcastStart;

    }



    public boolean isBroadcastEnd() {

        return broadcastEnd;

    }



    public boolean isLeaderChangeBroadcast() {

        return leaderChangeBroadcast;

    }



    public boolean isContestedActionBar() {

        return contestedActionBar;

    }



    public boolean isTitlesEnabled() {

        return titlesEnabled;

    }



    public boolean isCaptureSoundEnabled() {

        return captureSoundEnabled;

    }



    public Sound getCaptureSound() {

        return captureSound;

    }



    public boolean isCaptureBroadcastEnabled() {

        return captureBroadcastEnabled;

    }



    public int getCaptureBroadcastIntervalSeconds() {

        return captureBroadcastIntervalSeconds;

    }



    public boolean isBossbarEnabled() {

        return bossbarEnabled;

    }



    public boolean isBossbarZoneOnly() {
        return bossbarZoneOnly;
    }

    public boolean isHideTabInZone() {
        return hideTabInZone;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }



    public boolean isBorderParticlesEnabled() {

        return borderParticlesEnabled;

    }



    public boolean isBorderParticlesActiveOnly() {

        return borderParticlesActiveOnly;

    }



    public int getBorderParticleSpacing() {

        return borderParticleSpacing;

    }



    public int getBorderParticleViewDistance() {

        return borderParticleViewDistance;

    }



    public double getBorderParticleYOffset() {

        return borderParticleYOffset;

    }



    public float getBorderParticleSize() {

        return borderParticleSize;

    }



    public int getBorderParticleCount() {

        return borderParticleCount;

    }



    public int getBorderParticleRed() {

        return borderParticleRed;

    }



    public int getBorderParticleGreen() {

        return borderParticleGreen;

    }



    public int getBorderParticleBlue() {

        return borderParticleBlue;

    }



    public int getBorderParticleIntervalTicks() {

        return borderParticleIntervalTicks;

    }



    public int getBorderPreviewSeconds() {
        return borderPreviewSeconds;
    }

    public String getTitleHologramText() {
        return titleHologramText;
    }

    public float getTitleHologramScale() {
        return titleHologramScale;
    }

    public double getTitleHologramHeight() {
        return titleHologramHeight;
    }
}

