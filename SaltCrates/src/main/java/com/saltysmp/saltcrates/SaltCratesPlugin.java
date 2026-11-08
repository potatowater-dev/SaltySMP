package com.saltysmp.saltcrates;

import com.saltysmp.saltcrates.command.CrateCommand;
import com.saltysmp.saltcrates.config.AutoKeyGrantStorage;
import com.saltysmp.saltcrates.config.CrateDefinitionStorage;
import com.saltysmp.saltcrates.config.KeyStorage;
import com.saltysmp.saltcrates.config.MessagesConfig;
import com.saltysmp.saltcrates.config.PlacedCrateStorage;
import com.saltysmp.saltcrates.config.PluginConfig;
import com.saltysmp.saltcrates.config.RankRewardsConfig;
import com.saltysmp.saltcrates.listener.ChatInputListener;
import com.saltysmp.saltcrates.listener.CrateBlockListener;
import com.saltysmp.saltcrates.listener.CrateEditorListener;
import com.saltysmp.saltcrates.listener.CrateCombatListener;
import com.saltysmp.saltcrates.listener.CrateGuiListener;
import com.saltysmp.saltcrates.listener.CrateInteractListener;
import com.saltysmp.saltcrates.listener.PhysicalKeyListener;
import com.saltysmp.saltcrates.listener.WandListener;
import com.saltysmp.saltcrates.service.AutoKeyGrantService;
import com.saltysmp.saltcrates.service.CrateClaimService;
import com.saltysmp.saltcrates.service.CrateService;
import com.saltysmp.saltcrates.service.HologramService;
import com.saltysmp.saltcrates.service.KeyService;
import com.saltysmp.saltcrates.service.LootService;
import com.saltysmp.saltcrates.service.PermissionLookupService;
import com.saltysmp.saltcrates.service.PhysicalKeyService;
import com.saltysmp.saltcrates.service.PlacementService;
import com.saltysmp.saltcrates.util.CooldownManager;
import com.saltysmp.saltcrates.util.PluginKeys;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltCratesPlugin extends JavaPlugin {

    private MessagesConfig messagesConfig;
    private PluginConfig pluginConfig;
    private RankRewardsConfig rankRewardsConfig;
    private CrateDefinitionStorage definitionStorage;
    private PlacedCrateStorage placedCrateStorage;
    private KeyStorage keyStorage;
    private AutoKeyGrantStorage autoKeyGrantStorage;
    private PluginKeys pluginKeys;
    private CooldownManager cooldownManager;
    private KeyService keyService;
    private LootService lootService;
    private CrateService crateService;
    private HologramService hologramService;
    private PlacementService placementService;
    private CrateClaimService claimService;
    private PermissionLookupService permissionLookupService;
    private AutoKeyGrantService autoKeyGrantService;
    private PhysicalKeyService physicalKeyService;
    private ChatInputListener chatInputListener;

    @Override
    public void onEnable() {
        pluginKeys = new PluginKeys(this);
        cooldownManager = new CooldownManager(this);

        messagesConfig = new MessagesConfig(this);
        messagesConfig.load();
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();
        rankRewardsConfig = new RankRewardsConfig(this);
        rankRewardsConfig.load();
        definitionStorage = new CrateDefinitionStorage(this);
        placedCrateStorage = new PlacedCrateStorage(this);
        keyStorage = new KeyStorage(this);
        autoKeyGrantStorage = new AutoKeyGrantStorage(this);
        reloadAll();

        permissionLookupService = new PermissionLookupService(this);
        keyService = new KeyService(keyStorage);
        lootService = new LootService(pluginConfig);
        crateService = new CrateService(definitionStorage, placedCrateStorage);
        hologramService = new HologramService(
                this, pluginConfig, definitionStorage, placedCrateStorage, keyService, pluginKeys
        );
        PhysicalKeyService physicalKeyService = new PhysicalKeyService(
                pluginKeys, definitionStorage, keyService, hologramService
        );
        this.physicalKeyService = physicalKeyService;
        placementService = new PlacementService(this, placedCrateStorage, hologramService);
        placementService.setDefinitions(definitionStorage);
        claimService = new CrateClaimService(
                this,
                pluginConfig,
                messagesConfig,
                keyService,
                lootService,
                cooldownManager,
                hologramService,
                permissionLookupService
        );
        autoKeyGrantService = new AutoKeyGrantService(
                this,
                pluginConfig,
                rankRewardsConfig,
                messagesConfig,
                definitionStorage,
                autoKeyGrantStorage,
                permissionLookupService,
                keyService,
                hologramService
        );
        chatInputListener = new ChatInputListener(this, messagesConfig);

        int pruned = placementService.pruneInvalidPlacements();
        if (pruned > 0) {
            getLogger().info("Pruned " + pruned + " invalid crate placement(s).");
        }
        hologramService.cleanupOrphanHolograms();
        hologramService.respawnAll();
        hologramService.start();
        autoKeyGrantService.start();

        CrateCommand crateCommand = new CrateCommand(
                this,
                messagesConfig,
                definitionStorage,
                placedCrateStorage,
                keyService,
                claimService,
                autoKeyGrantService,
                physicalKeyService,
                pluginKeys
        );
        registerCrateCommand(crateCommand);
        registerListeners();
        getLogger().info("SaltCrates enabled. Command /crate registered.");
    }

    private void registerCrateCommand(CrateCommand crateCommand) {
        PluginCommand command = getCommand("crate");
        if (command == null) {
            getLogger().severe("Failed to register /crate — command missing from plugin.yml!");
            return;
        }
        command.setExecutor(crateCommand);
        command.setTabCompleter(crateCommand);
        command.setDescription("SaltCrates virtual key crate system");
        getLogger().info("Registered command: /crate (aliases: " + command.getAliases() + ")");
    }

    @Override
    public void onDisable() {
        if (claimService != null) {
            claimService.cancelAllSessions(false);
        }
        if (autoKeyGrantService != null) {
            autoKeyGrantService.stop();
        }
        if (hologramService != null) {
            hologramService.stop();
            hologramService.removeAll();
        }
        for (Player player : getServer().getOnlinePlayers()) {
            player.closeInventory();
        }
        if (cooldownManager != null) {
            cooldownManager.save();
        }
        if (keyStorage != null) {
            keyStorage.save();
        }
        if (autoKeyGrantStorage != null) {
            autoKeyGrantStorage.save();
        }
        if (placedCrateStorage != null) {
            placedCrateStorage.save();
        }
        getLogger().info("SaltCrates disabled.");
    }

    public void reloadAll() {
        messagesConfig.load();
        pluginConfig.load();
        if (rankRewardsConfig != null) {
            rankRewardsConfig.load();
        }
        cooldownManager.load();
        definitionStorage.load();
        placedCrateStorage.load();
        keyStorage.load();
        if (autoKeyGrantStorage != null) {
            autoKeyGrantStorage.load();
        }
    }

    public void safeReload() {
        if (claimService != null) {
            claimService.cancelAllSessions(true);
        }
        reloadAll();
        if (placementService != null) {
            placementService.setDefinitions(definitionStorage);
            int pruned = placementService.pruneInvalidPlacements();
            if (pruned > 0) {
                getLogger().info("Pruned " + pruned + " invalid crate placement(s).");
            }
        }
        if (hologramService != null) {
            hologramService.stop();
            hologramService.cleanupOrphanHolograms();
            hologramService.respawnAll();
            hologramService.start();
        }
        if (autoKeyGrantService != null) {
            autoKeyGrantService.stop();
            autoKeyGrantService.start();
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public CrateClaimService getClaimService() {
        return claimService;
    }

    public CrateDefinitionStorage getDefinitionStorage() {
        return definitionStorage;
    }

    public PlacedCrateStorage getPlacedCrateStorage() {
        return placedCrateStorage;
    }

    public CrateService getCrateService() {
        return crateService;
    }

    public KeyService getKeyService() {
        return keyService;
    }

    public PlacementService getPlacementService() {
        return placementService;
    }

    public HologramService getHologramService() {
        return hologramService;
    }

    public AutoKeyGrantService getAutoKeyGrantService() {
        return autoKeyGrantService;
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(chatInputListener, this);
        pluginManager.registerEvents(new CrateEditorListener(definitionStorage, chatInputListener, placementService), this);
        pluginManager.registerEvents(new CrateGuiListener(this, claimService), this);
        pluginManager.registerEvents(new CrateCombatListener(claimService), this);
        pluginManager.registerEvents(
                new CrateInteractListener(crateService, claimService, placementService, messagesConfig),
                this
        );
        pluginManager.registerEvents(new WandListener(pluginKeys, crateService, placementService, messagesConfig), this);
        pluginManager.registerEvents(new PhysicalKeyListener(this, physicalKeyService, messagesConfig), this);
        pluginManager.registerEvents(new CrateBlockListener(pluginKeys, crateService, placementService, messagesConfig), this);
        pluginManager.registerEvents(hologramService, this);
    }
}
