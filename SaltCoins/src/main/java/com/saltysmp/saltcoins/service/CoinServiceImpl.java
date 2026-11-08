package com.saltysmp.saltcoins.service;

import com.saltysmp.saltcoins.api.CoinService;
import com.saltysmp.saltcoins.item.KillTokenFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CoinServiceImpl implements CoinService {

    private final KillTokenService killTokenService;
    private final KillTokenFactory factory;

    public CoinServiceImpl(KillTokenService killTokenService, KillTokenFactory factory) {
        this.killTokenService = killTokenService;
        this.factory = factory;
    }

    @Override
    public boolean isCoin(ItemStack item) {
        return factory.isKillToken(item);
    }

    @Override
    public int count(Player player) {
        return killTokenService.countInInventory(player);
    }

    @Override
    public boolean has(Player player, int amount) {
        return count(player) >= amount;
    }

    @Override
    public boolean withdraw(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (!has(player, amount)) {
            return false;
        }
        int removed = killTokenService.removeAmount(player, amount);
        return removed >= amount;
    }

    @Override
    public void deposit(Player player, int amount) {
        if (amount > 0) {
            killTokenService.give(player, amount);
        }
    }

    @Override
    public ItemStack create(int amount) {
        return factory.create(amount);
    }
}
