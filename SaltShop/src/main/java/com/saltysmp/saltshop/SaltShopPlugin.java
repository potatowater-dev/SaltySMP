package com.saltysmp.saltshop;

import com.saltysmp.saltshop.command.ShopCommand;
import com.saltysmp.saltshop.config.MessagesConfig;
import com.saltysmp.saltshop.config.PluginConfig;
import com.saltysmp.saltshop.config.ShopStorage;
import com.saltysmp.saltshop.listener.ShopGuiListener;
import com.saltysmp.saltshop.listener.ShopInteractListener;
import com.saltysmp.saltshop.listener.ShopNpcProtectionListener;
import com.saltysmp.saltshop.service.MerchantRecipeService;
import com.saltysmp.saltshop.service.ShopNpcService;
import com.saltysmp.saltshop.service.TradeService;
import com.saltysmp.saltshop.util.GuiItems;
import com.saltysmp.saltshop.util.PluginKeys;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltShopPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;
    private ShopStorage shopStorage;
    private PluginKeys pluginKeys;
    private ShopNpcService npcService;
    private TradeService tradeService;

    @Override
    public void onEnable() {
        pluginKeys = new PluginKeys(this);
        GuiItems.init(this);
        pluginConfig = new PluginConfig(this);
        messagesConfig = new MessagesConfig(this);
        shopStorage = new ShopStorage(this, pluginConfig);
        reloadAll();

        MerchantRecipeService recipeService = new MerchantRecipeService();
        tradeService = new TradeService();
        npcService = new ShopNpcService(pluginConfig, shopStorage, pluginKeys, recipeService);
        npcService.restoreAll();

        registerCommand();
        getServer().getPluginManager().registerEvents(
                new ShopInteractListener(pluginConfig, messagesConfig, shopStorage, npcService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new ShopGuiListener(pluginConfig, messagesConfig, shopStorage, tradeService, npcService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new ShopNpcProtectionListener(shopStorage, npcService),
                this
        );
        getLogger().info("SaltShop enabled.");
    }

    private void registerCommand() {
        ShopCommand executor = new ShopCommand(this, messagesConfig, shopStorage, npcService);
        PluginCommand command = getCommand("shop");
        if (command == null) {
            getLogger().severe("Failed to register /shop — command missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public void reloadAll() {
        pluginConfig.load();
        messagesConfig.load();
        shopStorage.load();
    }
}
