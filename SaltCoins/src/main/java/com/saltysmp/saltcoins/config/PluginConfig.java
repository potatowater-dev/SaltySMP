package com.saltysmp.saltcoins.config;

import com.saltysmp.saltcoins.SaltCoinsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PluginConfig {

    private final SaltCoinsPlugin plugin;
    private Material killTokenMaterial;
    private String killTokenDisplayName;
    private boolean killTokenGlint;
    private int killTokenMaxStack;
    private float useCooldownSeconds;
    private String useCooldownGroup;
    private List<String> killTokenLore;
    private boolean onKillEnabled;
    private int onKillAmount;
    private boolean onKillNotify;
    private boolean onKillNotifyActionBar;
    private Set<String> onKillIgnoredWorlds;
    private boolean onKillRequirePermission;
    private boolean armorCoinsEnabled;
    private int diamondProtectionMin;
    private int diamondCoinsPerPiece;
    private int netheriteProtectionMin;
    private int netheriteCoinsPerPiece;
    private boolean overrideKeepInventory;

    public PluginConfig(SaltCoinsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        Material material = Material.matchMaterial(config.getString("coin.material",
                config.getString("kill-token.material", "SNOWBALL")));
        killTokenMaterial = material != null ? material : Material.SNOWBALL;
        killTokenDisplayName = config.getString("coin.display-name",
                config.getString("kill-token.display-name", "<gold><bold>Coin"));
        killTokenGlint = config.getBoolean("coin.glint",
                config.getBoolean("kill-token.glint", true));
        killTokenMaxStack = Math.max(1, config.getInt("coin.max-stack",
                config.getInt("kill-token.max-stack", 64)));
        useCooldownSeconds = (float) Math.max(0, config.getDouble("coin.use-cooldown-seconds", 999999D));
        useCooldownGroup = config.getString("coin.use-cooldown-group", "snowball");
        killTokenLore = new ArrayList<>(config.getStringList("coin.lore"));
        if (killTokenLore.isEmpty()) {
            killTokenLore = new ArrayList<>(config.getStringList("kill-token.lore"));
        }

        onKillEnabled = config.getBoolean("on-kill.enabled", true);
        onKillAmount = Math.max(1, config.getInt("on-kill.amount", 1));
        onKillNotify = config.getBoolean("on-kill.notify", true);
        onKillNotifyActionBar = config.getBoolean("on-kill.notify-actionbar", true);
        onKillIgnoredWorlds = new HashSet<>();
        for (String world : config.getStringList("on-kill.ignored-worlds")) {
            onKillIgnoredWorlds.add(world.toLowerCase());
        }
        onKillRequirePermission = config.getBoolean("on-kill.require-permission", false);
        overrideKeepInventory = config.getBoolean("on-kill.override-keep-inventory", true);

        armorCoinsEnabled = config.getBoolean("armor-coins.enabled", true);
        diamondProtectionMin = Math.max(1, config.getInt("armor-coins.diamond-protection-min", 4));
        diamondCoinsPerPiece = Math.max(0, config.getInt("armor-coins.diamond-coins-per-piece", 1));
        netheriteProtectionMin = Math.max(1, config.getInt("armor-coins.netherite-protection-min", 5));
        netheriteCoinsPerPiece = Math.max(0, config.getInt("armor-coins.netherite-coins-per-piece", 3));
    }

    public Material getKillTokenMaterial() {
        return killTokenMaterial;
    }

    public String getKillTokenDisplayName() {
        return killTokenDisplayName;
    }

    public boolean isKillTokenGlint() {
        return killTokenGlint;
    }

    public int getKillTokenMaxStack() {
        return killTokenMaxStack;
    }

    public List<String> getKillTokenLore() {
        return Collections.unmodifiableList(killTokenLore);
    }

    public float getUseCooldownSeconds() {
        return useCooldownSeconds;
    }

    public String getUseCooldownGroup() {
        return useCooldownGroup;
    }

    public boolean isOnKillEnabled() {
        return onKillEnabled;
    }

    public int getOnKillAmount() {
        return onKillAmount;
    }

    public boolean isOnKillNotify() {
        return onKillNotify;
    }

    public boolean isOnKillNotifyActionBar() {
        return onKillNotifyActionBar;
    }

    public boolean isWorldIgnored(String worldName) {
        return onKillIgnoredWorlds.contains(worldName.toLowerCase());
    }

    public boolean isOnKillRequirePermission() {
        return onKillRequirePermission;
    }

    public boolean isOverrideKeepInventory() {
        return overrideKeepInventory;
    }

    public boolean isArmorCoinsEnabled() {
        return armorCoinsEnabled;
    }

    public int getDiamondProtectionMin() {
        return diamondProtectionMin;
    }

    public int getDiamondCoinsPerPiece() {
        return diamondCoinsPerPiece;
    }

    public int getNetheriteProtectionMin() {
        return netheriteProtectionMin;
    }

    public int getNetheriteCoinsPerPiece() {
        return netheriteCoinsPerPiece;
    }
}
