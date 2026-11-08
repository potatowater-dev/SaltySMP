package com.saltysmp.saltshop.gui;

import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.model.ShopTrade;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface ShopGuiHolder extends InventoryHolder {

    Player getViewer();

    ShopDefinition getShop();

    @Override
    Inventory getInventory();

    default ShopTrade tradeAt(int menuSlot) {
        return null;
    }
}
