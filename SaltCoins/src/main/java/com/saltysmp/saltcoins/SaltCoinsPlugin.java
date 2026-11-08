package com.saltysmp.saltcoins;

import com.saltysmp.saltcoins.api.CoinService;
import com.saltysmp.saltcoins.command.KillTokenCommand;
import com.saltysmp.saltcoins.config.MessagesConfig;
import com.saltysmp.saltcoins.config.PluginConfig;
import com.saltysmp.saltcoins.item.KillTokenFactory;
import com.saltysmp.saltcoins.listener.CoinUseListener;
import com.saltysmp.saltcoins.listener.KillTokenListener;
import com.saltysmp.saltcoins.service.ArmorCoinService;
import com.saltysmp.saltcoins.service.CoinServiceImpl;
import com.saltysmp.saltcoins.service.KillTokenService;
import com.saltysmp.saltcoins.util.PluginKeys;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltCoinsPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;
    private PluginKeys pluginKeys;
    private KillTokenFactory killTokenFactory;
    private KillTokenService killTokenService;
    private CoinServiceImpl coinService;
    private ArmorCoinService armorCoinService;

    @Override
    public void onEnable() {
        pluginKeys = new PluginKeys(this);
        pluginConfig = new PluginConfig(this);
        messagesConfig = new MessagesConfig(this);
        reloadAll();

        killTokenFactory = new KillTokenFactory(pluginConfig, pluginKeys);
        killTokenService = new KillTokenService(pluginConfig, killTokenFactory);
        coinService = new CoinServiceImpl(killTokenService, killTokenFactory);
        armorCoinService = new ArmorCoinService(pluginConfig);

        getServer().getServicesManager().register(CoinService.class, coinService, this, ServicePriority.Normal);

        registerCommand();
        getServer().getPluginManager().registerEvents(
                new KillTokenListener(pluginConfig, messagesConfig, killTokenService, armorCoinService),
                this
        );
        getServer().getPluginManager().registerEvents(new CoinUseListener(killTokenFactory), this);
        getLogger().info("SaltCoins enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
    }

    private void registerCommand() {
        KillTokenCommand executor = new KillTokenCommand(this, messagesConfig, killTokenService);
        PluginCommand command = getCommand("killtoken");
        if (command == null) {
            getLogger().severe("Failed to register /killtoken — command missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        PluginCommand coin = getCommand("coin");
        if (coin != null) {
            coin.setExecutor(executor);
            coin.setTabCompleter(executor);
        }
    }

    public void reloadAll() {
        messagesConfig.load();
        pluginConfig.load();
    }

    public CoinService getCoinService() {
        return coinService;
    }
}
