package com.saltysmp.saltshop.config;

import com.saltysmp.saltshop.SaltShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Villager;

public final class PluginConfig {

    private final SaltShopPlugin plugin;
    private boolean villagerNoAi;
    private boolean villagerInvulnerable;
    private boolean villagerSilent;
    private int villagerLevel;
    private Villager.Profession defaultProfession;
    private Villager.Type defaultType;
    private String guiTitle;
    private Material borderMaterial;
    private int tradesPerPage;
    private int defaultMaxUses;
    private boolean merchantMode;

    public PluginConfig(SaltShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        villagerNoAi = config.getBoolean("villager.no-ai", true);
        villagerInvulnerable = config.getBoolean("villager.invulnerable", true);
        villagerSilent = config.getBoolean("villager.silent", true);
        villagerLevel = Math.max(1, Math.min(5, config.getInt("villager.level", 5)));
        defaultProfession = parseProfession(config.getString("villager.profession", "NONE"));
        defaultType = parseType(config.getString("villager.type", "PLAINS"));

        guiTitle = config.getString("gui.title", "<light_purple>{shop}");
        borderMaterial = Material.matchMaterial(config.getString("gui.border-material", "PURPLE_STAINED_GLASS_PANE"));
        if (borderMaterial == null) {
            borderMaterial = Material.PURPLE_STAINED_GLASS_PANE;
        }
        tradesPerPage = Math.max(1, Math.min(45, config.getInt("gui.trades-per-page", 28)));
        defaultMaxUses = Math.max(1, config.getInt("trade.max-uses-default", Integer.MAX_VALUE));
        merchantMode = !config.getString("trade.mode", "MERCHANT").equalsIgnoreCase("CHEST");
    }

    private Villager.Profession parseProfession(String value) {
        if (value == null || value.isBlank()) {
            return Villager.Profession.NONE;
        }
        try {
            return Villager.Profession.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Villager.Profession.NONE;
        }
    }

    private Villager.Type parseType(String value) {
        if (value == null || value.isBlank()) {
            return Villager.Type.PLAINS;
        }
        try {
            return Villager.Type.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Villager.Type.PLAINS;
        }
    }

    public boolean isVillagerNoAi() {
        return villagerNoAi;
    }

    public boolean isVillagerInvulnerable() {
        return villagerInvulnerable;
    }

    public boolean isVillagerSilent() {
        return villagerSilent;
    }

    public int getVillagerLevel() {
        return villagerLevel;
    }

    public Villager.Profession getDefaultProfession() {
        return defaultProfession;
    }

    public Villager.Type getDefaultType() {
        return defaultType;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    public Material getBorderMaterial() {
        return borderMaterial;
    }

    public int getTradesPerPage() {
        return tradesPerPage;
    }

    public int getDefaultMaxUses() {
        return defaultMaxUses;
    }

    public boolean isMerchantMode() {
        return merchantMode;
    }
}
