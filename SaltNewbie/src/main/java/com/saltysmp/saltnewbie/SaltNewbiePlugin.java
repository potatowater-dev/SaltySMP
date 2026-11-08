package com.saltysmp.saltnewbie;

import com.saltysmp.saltnewbie.command.NewbieCommand;
import com.saltysmp.saltnewbie.config.MessagesConfig;
import com.saltysmp.saltnewbie.listener.NewbieProtectionListener;
import com.saltysmp.saltnewbie.service.NewbieService;
import com.saltysmp.saltnewbie.util.PluginKeys;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltNewbiePlugin extends JavaPlugin {

    private MessagesConfig messagesConfig;
    private NewbieService newbieService;
    private PluginKeys pluginKeys;

    @Override
    public void onEnable() {
        pluginKeys = new PluginKeys(this);
        messagesConfig = new MessagesConfig(this);
        messagesConfig.load();
        newbieService = new NewbieService(this);

        registerCommand();
        getServer().getPluginManager().registerEvents(
                new NewbieProtectionListener(newbieService, messagesConfig, pluginKeys),
                this
        );
        getLogger().info("SaltNewbie enabled.");
    }

    @Override
    public void onDisable() {
        if (newbieService != null) {
            newbieService.shutdown();
        }
    }

    private void registerCommand() {
        NewbieCommand executor = new NewbieCommand(messagesConfig, newbieService);
        PluginCommand command = getCommand("newbie");
        if (command == null) {
            getLogger().severe("Failed to register /newbie — command missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
