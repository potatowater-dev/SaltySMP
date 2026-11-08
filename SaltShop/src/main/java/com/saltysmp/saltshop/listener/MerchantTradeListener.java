package com.saltysmp.saltshop.listener;

import com.saltysmp.saltshop.config.MessagesConfig;
import com.saltysmp.saltshop.service.ShopNpcService;
import com.saltysmp.saltshop.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

public final class MerchantTradeListener implements Listener {

    private final MessagesConfig messages;
    private final ShopNpcService npcService;

    public MerchantTradeListener(MessagesConfig messages, ShopNpcService npcService) {
        this.messages = messages;
        this.npcService = npcService;
    }

    @EventHandler
    public void onMerchantTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory() instanceof MerchantInventory merchantInventory)) {
            return;
        }
        if (!(merchantInventory.getHolder() instanceof Villager villager)) {
            return;
        }
        if (!npcService.isShopNpc(villager)) {
            return;
        }
        if (event.getSlot() != 2) {
            return;
        }
        MerchantRecipe recipe = merchantInventory.getSelectedRecipe();
        if (recipe == null || recipe.getResult() == null) {
            return;
        }
        messages.send(
                player,
                "trade-success",
                "item",
                MessageUtil.plainItemName(recipe.getResult())
        );
    }
}
