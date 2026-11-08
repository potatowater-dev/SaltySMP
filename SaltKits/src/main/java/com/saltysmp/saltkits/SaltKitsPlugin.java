package com.saltysmp.saltkits;

import com.saltysmp.saltkits.cleanup.BlockCleanupService;
import com.saltysmp.saltkits.command.KitCommand;
import com.saltysmp.saltkits.config.MessagesConfig;
import com.saltysmp.saltkits.kit.CooldownManager;
import com.saltysmp.saltkits.kit.KitGiveService;
import com.saltysmp.saltkits.kit.KitManager;
import com.saltysmp.saltkits.kit.KitSessionRegistry;
import com.saltysmp.saltkits.gui.KitGuiKeys;
import com.saltysmp.saltkits.listener.ChatInputListener;
import com.saltysmp.saltkits.listener.KitEditorListener;
import com.saltysmp.saltkits.listener.KitGuiListener;
import com.saltysmp.saltkits.listener.KitJoinListener;
import com.saltysmp.saltkits.listener.KitMenuListener;
import com.saltysmp.saltkits.util.DurationParser;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltKitsPlugin extends JavaPlugin {

    private long defaultPaidCooldownSeconds;
    private String permissionPrefix;
    private boolean warnOnFullInventory;
    private boolean claimTitleEnabled;
    private boolean customizeClaimGui;
    private int chatInputTimeoutSeconds;

    private Sound claimSuccessSound;
    private Sound claimDeniedSound;
    private Sound menuClickSound;
    private float claimSuccessVolume;
    private float claimSuccessPitch;
    private float claimDeniedVolume;
    private float claimDeniedPitch;
    private float menuClickVolume;
    private float menuClickPitch;

    private MessagesConfig messagesConfig;
    private KitSessionRegistry sessionRegistry;
    private CooldownManager cooldownManager;
    private KitGiveService giveService;
    private KitManager kitManager;
    private ChatInputListener chatInputListener;
    private BlockCleanupService cleanupService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        KitGuiKeys.register(this);
        messagesConfig = new MessagesConfig(this);
        messagesConfig.load();
        loadSettings();

        sessionRegistry = new KitSessionRegistry();
        cooldownManager = new CooldownManager(this);
        giveService = new KitGiveService();
        kitManager = new KitManager(this, cooldownManager, giveService, sessionRegistry, messagesConfig);
        chatInputListener = new ChatInputListener(this, messagesConfig);
        cleanupService = new BlockCleanupService(this, messagesConfig);

        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(chatInputListener, this);
        pluginManager.registerEvents(new KitEditorListener(kitManager, sessionRegistry, chatInputListener, messagesConfig), this);
        pluginManager.registerEvents(new KitMenuListener(kitManager, sessionRegistry, messagesConfig), this);
        pluginManager.registerEvents(new KitGuiListener(kitManager, sessionRegistry), this);
        pluginManager.registerEvents(new KitJoinListener(this, kitManager, cooldownManager), this);

        KitCommand kitCommand = new KitCommand(this, kitManager, cooldownManager, messagesConfig, cleanupService);
        PluginCommand command = getCommand("kit");
        if (command != null) {
            command.setExecutor(kitCommand);
            command.setTabCompleter(kitCommand);
        } else {
            getLogger().severe("Command \"kit\" is missing from plugin.yml.");
        }
    }

    @Override
    public void onDisable() {
        if (sessionRegistry != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                player.closeInventory();
            }
            sessionRegistry.clearAll();
        }
    }

    public void reloadAll() {
        reloadConfig();
        messagesConfig.load();
        loadSettings();
        if (cleanupService != null) {
            cleanupService.reload();
        }
        kitManager.loadAll();
    }

    public void loadSettings() {
        FileConfiguration config = getConfig();

        try {
            defaultPaidCooldownSeconds = DurationParser.parseSeconds(config.getString("default-paid-cooldown", "24h"));
        } catch (IllegalArgumentException exception) {
            defaultPaidCooldownSeconds = 86400L;
            getLogger().warning("Invalid default-paid-cooldown; using 24h.");
        }

        permissionPrefix = config.getString("permission-prefix", "saltkits.kit.");
        warnOnFullInventory = config.getBoolean("warn-on-full-inventory", true);
        claimTitleEnabled = config.getBoolean("claim-title-enabled", true);
        customizeClaimGui = !"PREVIEW".equalsIgnoreCase(config.getString("claim-gui-mode", "CUSTOMIZE"));
        chatInputTimeoutSeconds = config.getInt("chat-input-timeout-seconds", 30);

        claimSuccessSound = parseSound(config.getString("sounds.claim-success"), Sound.ENTITY_PLAYER_LEVELUP);
        claimDeniedSound = parseSound(config.getString("sounds.claim-denied"), Sound.BLOCK_NOTE_BLOCK_BASS);
        menuClickSound = parseSound(config.getString("sounds.menu-click"), Sound.UI_BUTTON_CLICK);
        claimSuccessVolume = (float) config.getDouble("sounds.claim-success-volume", 1.0);
        claimSuccessPitch = (float) config.getDouble("sounds.claim-success-pitch", 1.2);
        claimDeniedVolume = (float) config.getDouble("sounds.claim-denied-volume", 0.8);
        claimDeniedPitch = (float) config.getDouble("sounds.claim-denied-pitch", 0.5);
        menuClickVolume = (float) config.getDouble("sounds.menu-click-volume", 0.5);
        menuClickPitch = (float) config.getDouble("sounds.menu-click-pitch", 1.0);
    }

    private Sound parseSound(String name, Sound fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException exception) {
            getLogger().warning("Invalid sound: " + name);
            return fallback;
        }
    }

    public long getDefaultPaidCooldownSeconds() {
        return defaultPaidCooldownSeconds;
    }

    public String getPermissionPrefix() {
        return permissionPrefix;
    }

    public boolean isWarnOnFullInventory() {
        return warnOnFullInventory;
    }

    public boolean isClaimTitleEnabled() {
        return claimTitleEnabled;
    }

    public boolean isCustomizeClaimGui() {
        return customizeClaimGui;
    }

    public int getChatInputTimeoutSeconds() {
        return chatInputTimeoutSeconds;
    }

    public Sound getClaimSuccessSound() {
        return claimSuccessSound;
    }

    public Sound getClaimDeniedSound() {
        return claimDeniedSound;
    }

    public Sound getMenuClickSound() {
        return menuClickSound;
    }

    public float getClaimSuccessVolume() {
        return claimSuccessVolume;
    }

    public float getClaimSuccessPitch() {
        return claimSuccessPitch;
    }

    public float getClaimDeniedVolume() {
        return claimDeniedVolume;
    }

    public float getClaimDeniedPitch() {
        return claimDeniedPitch;
    }

    public float getMenuClickVolume() {
        return menuClickVolume;
    }

    public float getMenuClickPitch() {
        return menuClickPitch;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }
}
