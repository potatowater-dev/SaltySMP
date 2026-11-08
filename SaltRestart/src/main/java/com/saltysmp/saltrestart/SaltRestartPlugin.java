package com.saltysmp.saltrestart;

import com.saltysmp.saltrestart.command.SaltRestartCommand;
import com.saltysmp.saltrestart.config.MessagesConfig;
import com.saltysmp.saltrestart.service.RestartService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltRestartPlugin extends JavaPlugin {

    private MessagesConfig messagesConfig;
    private RestartService restartService;

    @Override
    public void onEnable() {
        messagesConfig = new MessagesConfig(this);
        messagesConfig.load();
        restartService = new RestartService(this, messagesConfig);
        registerCommand();
        getLogger().info("SaltRestart enabled.");
    }

    @Override
    public void onDisable() {
        if (restartService != null) {
            restartService.shutdown();
        }
    }

    private void registerCommand() {
        SaltRestartCommand executor = new SaltRestartCommand(messagesConfig, restartService);
        PluginCommand command = getCommand("saltrestart");
        if (command == null) {
            getLogger().severe("Failed to register /saltrestart — command missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public RestartService getRestartService() {
        return restartService;
    }
}
