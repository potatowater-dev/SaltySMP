package com.saltysmp.saltkoth;

import com.saltysmp.saltkoth.command.KothCommand;
import com.saltysmp.saltkoth.config.AreaStorage;
import com.saltysmp.saltkoth.config.KothDataStorage;
import com.saltysmp.saltkoth.config.MessagesConfig;
import com.saltysmp.saltkoth.config.PendingRewardStorage;
import com.saltysmp.saltkoth.config.PluginConfig;
import com.saltysmp.saltkoth.config.WinStatsStorage;
import com.saltysmp.saltkoth.listener.KothZoneListener;
import com.saltysmp.saltkoth.listener.PlayerLifecycleListener;
import com.saltysmp.saltkoth.service.CrateRewardService;
import com.saltysmp.saltkoth.service.KothBorderService;
import com.saltysmp.saltkoth.service.KothBossBarService;
import com.saltysmp.saltkoth.service.KothGameService;
import com.saltysmp.saltkoth.service.KothHologramService;
import com.saltysmp.saltkoth.service.SelectionService;
import com.saltysmp.saltkoth.placeholder.SaltKothExpansion;
import com.saltysmp.saltkoth.util.PluginKeys;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltKothPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;
    private AreaStorage areaStorage;
    private KothDataStorage dataStorage;
    private PendingRewardStorage pendingRewardStorage;
    private WinStatsStorage winStatsStorage;
    private SelectionService selectionService;
    private CrateRewardService rewardService;
    private KothBorderService borderService;
    private KothBossBarService bossBarService;
    private KothGameService gameService;
    private KothHologramService hologramService;
    private PluginKeys pluginKeys;
    private KothZoneListener zoneListener;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();
        messagesConfig = new MessagesConfig(this);
        messagesConfig.load();
        areaStorage = new AreaStorage(this);
        areaStorage.load();
        dataStorage = new KothDataStorage(this);
        dataStorage.load();
        pendingRewardStorage = new PendingRewardStorage(this);
        pendingRewardStorage.load();
        winStatsStorage = new WinStatsStorage(this);
        winStatsStorage.load();

        pluginKeys = new PluginKeys(this);
        selectionService = new SelectionService();
        rewardService = new CrateRewardService(this, pluginConfig, pendingRewardStorage);
        borderService = new KothBorderService(this, pluginConfig);
        bossBarService = new KothBossBarService(this, pluginConfig);
        gameService = new KothGameService(
                this,
                pluginConfig,
                messagesConfig,
                areaStorage,
                dataStorage,
                winStatsStorage,
                rewardService,
                borderService,
                bossBarService
        );
        hologramService = new KothHologramService(this, pluginConfig, pluginKeys);
        hologramService.load();
        zoneListener = new KothZoneListener(areaStorage, gameService);

        rewardService.flushPending();
        gameService.startScheduler();

        registerCommand();
        getServer().getPluginManager().registerEvents(
                new PlayerLifecycleListener(gameService, rewardService, zoneListener),
                this
        );
        getServer().getPluginManager().registerEvents(zoneListener, this);
        registerPlaceholderApi();
        getLogger().info("SaltKOTH v" + getPluginMeta().getVersion() + " enabled.");
    }

    private void registerPlaceholderApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI not found — %saltkoth_*% placeholders unavailable.");
            return;
        }
        if (new SaltKothExpansion(this).register()) {
            getLogger().info("PlaceholderAPI expansion registered (%saltkoth_*%).");
        } else {
            getLogger().warning("Failed to register PlaceholderAPI expansion.");
        }
    }

    @Override
    public void onDisable() {
        if (gameService != null) {
            gameService.stopScheduler();
            if (gameService.isActive()) {
                gameService.forceStop();
            }
        }
        if (bossBarService != null) {
            bossBarService.hideAll();
        }
        getLogger().info("SaltKOTH disabled.");
    }

    public void reloadPlugin() {
        gameService.stopScheduler();
        pluginConfig.load();
        messagesConfig.load();
        areaStorage.load();
        dataStorage.load();
        pendingRewardStorage.load();
        winStatsStorage.load();
        hologramService.load();
        rewardService.flushPending();
        gameService.reloadUi();
        gameService.startScheduler();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public AreaStorage getAreaStorage() {
        return areaStorage;
    }

    public KothGameService getGameService() {
        return gameService;
    }

    public KothBorderService getBorderService() {
        return borderService;
    }

    public WinStatsStorage getWinStatsStorage() {
        return winStatsStorage;
    }

    public KothHologramService getHologramService() {
        return hologramService;
    }

    private void registerCommand() {
        PluginCommand command = getCommand("koth");
        if (command == null) {
            getLogger().severe("Command /koth missing from plugin.yml");
            return;
        }
        KothCommand executor = new KothCommand(
                this, messagesConfig, areaStorage, selectionService, gameService, borderService
        );
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
