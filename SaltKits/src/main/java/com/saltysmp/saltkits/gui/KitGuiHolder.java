package com.saltysmp.saltkits.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface KitGuiHolder extends InventoryHolder {
    @Override
    Inventory getInventory();
}
