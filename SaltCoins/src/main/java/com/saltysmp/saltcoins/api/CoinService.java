package com.saltysmp.saltcoins.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface CoinService {

    boolean isCoin(ItemStack item);

    int count(Player player);

    boolean has(Player player, int amount);

    boolean withdraw(Player player, int amount);

    void deposit(Player player, int amount);

    ItemStack create(int amount);
}
