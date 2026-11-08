package com.saltysmp.saltshop.listener;

import com.saltysmp.saltshop.config.MessagesConfig;
import com.saltysmp.saltshop.config.PluginConfig;
import com.saltysmp.saltshop.config.ShopStorage;
import com.saltysmp.saltshop.gui.ShopBrowseGui;
import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.service.ShopNpcService;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class ShopInteractListener implements Listener {

    private final PluginConfig config;
    private final MessagesConfig messages;
    private final ShopStorage storage;
    private final ShopNpcService npcService;

    public ShopInteractListener(
            PluginConfig config,
            MessagesConfig messages,
            ShopStorage storage,
            ShopNpcService npcService
    ) {
        this.config = config;
        this.messages = messages;
        this.storage = storage;
        this.npcService = npcService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (!npcService.isShopNpc(villager)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("saltshop.use")) {
            messages.send(player, "no-permission");
            event.setCancelled(true);
            return;
        }

        String shopId = npcService.shopIdOf(villager);
        ShopDefinition shop = storage.get(shopId);
        if (shop == null) {
            messages.send(player, "unknown-shop", "shop", shopId == null ? "?" : shopId);
            event.setCancelled(true);
            return;
        }

        if (config.isMerchantMode()) {
            npcService.configure(villager, shop);
            return;
        }

        event.setCancelled(true);
        player.openInventory(new ShopBrowseGui(player, shop, config, 0).getInventory());
    }
}
