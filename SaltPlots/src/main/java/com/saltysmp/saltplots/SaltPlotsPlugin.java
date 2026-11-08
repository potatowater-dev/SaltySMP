package com.saltysmp.saltplots;

import com.saltysmp.saltplots.command.PlotCommand;
import com.saltysmp.saltplots.config.MessagesConfig;
import com.saltysmp.saltplots.config.PlotStorage;
import com.saltysmp.saltplots.config.PluginConfig;
import com.saltysmp.saltplots.listener.HubWandListener;
import com.saltysmp.saltplots.listener.PlotProtectionListener;
import com.saltysmp.saltplots.service.PlotBorderService;
import com.saltysmp.saltplots.service.PlotGenerationService;
import com.saltysmp.saltplots.service.PlotService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltPlotsPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;
    private PlotStorage plotStorage;
    private PlotService plotService;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);
        messagesConfig = new MessagesConfig(this);
        plotStorage = new PlotStorage(this);
        reloadAll();

        PlotGenerationService generationService = new PlotGenerationService(pluginConfig);
        PlotBorderService borderService = new PlotBorderService(pluginConfig);
        plotService = new PlotService(pluginConfig, plotStorage, messagesConfig, generationService, borderService);

        registerCommand();
        getServer().getPluginManager().registerEvents(
                new PlotProtectionListener(pluginConfig, plotService, messagesConfig),
                this
        );
        getServer().getPluginManager().registerEvents(new HubWandListener(plotService), this);
        getLogger().info("SaltPlots enabled.");
    }

    private void registerCommand() {
        PlotCommand executor = new PlotCommand(this, plotService, plotStorage, pluginConfig, messagesConfig);
        PluginCommand command = getCommand("plot");
        if (command == null) {
            getLogger().severe("Failed to register /plot — command missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void reloadAll() {
        messagesConfig.load();
        pluginConfig.load();
        plotStorage.load();
        PlotDataMigration.migrateIfNeeded(this, pluginConfig, plotStorage);
    }
}
