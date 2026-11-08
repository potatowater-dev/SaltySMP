package com.saltysmp.saltcrates.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface SaltGuiHolder extends InventoryHolder {
    Inventory getInventory();
}
