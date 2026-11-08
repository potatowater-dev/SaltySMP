package com.saltysmp.saltmine;

import com.saltysmp.saltmine.command.MineCommand;
import com.saltysmp.saltmine.config.MessagesConfig;
import com.saltysmp.saltmine.config.PluginConfig;
import com.saltysmp.saltmine.item.MineWandFactory;
import com.saltysmp.saltmine.listener.MineBreakListener;
import com.saltysmp.saltmine.listener.MineWandListener;
import com.saltysmp.saltmine.service.MineRegenService;
import com.saltysmp.saltmine.service.WandSelectionService;
import com.saltysmp.saltmine.placeholder.SaltMineExpansion;
import com.saltysmp.saltmine.storage.MineStorage;
import com.saltysmp.saltmine.util.PluginKeys;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltMinePlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;
    private PluginKeys pluginKeys;
    private MineStorage mineStorage;
    private WandSelectionService selectionService;
    private MineWandFactory wandFactory;
    private MineRegenService regenService;

    @Override
    public void onEnable() {
        pluginKeys = new PluginKeys(this);
        pluginConfig = new PluginConfig(this);
        messagesConfig = new MessagesConfig(this);
        mineStorage = new MineStorage(this);
        selectionService = new WandSelectionService();
        reloadAll();

        wandFactory = new MineWandFactory(pluginConfig, pluginKeys);
        regenService = new MineRegenService(this, pluginConfig, messagesConfig, mineStorage);
        regenService.start();

        registerCommand();
        getServer().getPluginManager().registerEvents(
                new MineWandListener(messagesConfig, wandFactory, selectionService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new MineBreakListener(pluginConfig, messagesConfig, mineStorage),
                this
        );
        registerPlaceholderApi();
        getLogger().info("SaltMine enabled.");
    }

    private void registerPlaceholderApi() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI not found — %saltmine_timer_<id>% unavailable.");
            return;
        }
        if (new SaltMineExpansion(this).register()) {
            getLogger().info("PlaceholderAPI expansion registered (%saltmine_timer_<id>%).");
        } else {
            getLogger().warning("Failed to register PlaceholderAPI expansion.");
        }
    }

    @Override
    public void onDisable() {
        if (regenService != null) {
            regenService.stop();
        }
    }

    private void registerCommand() {
        MineCommand executor = new MineCommand(
                this,
                messagesConfig,
                mineStorage,
                wandFactory,
                selectionService,
                regenService
        );
        PluginCommand command = getCommand("mine");
        if (command == null) {
            getLogger().severe("Failed to register /mine — command missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void reloadAll() {
        messagesConfig.load();
        pluginConfig.load();
        mineStorage.load();
        if (regenService != null) {
            regenService.restart();
        }
    }

    public MineStorage getMineStorage() {
        return mineStorage;
    }

    public MineRegenService getRegenService() {
        return regenService;
    }
}
