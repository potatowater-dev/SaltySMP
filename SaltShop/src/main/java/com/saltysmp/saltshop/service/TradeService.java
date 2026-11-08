package com.saltysmp.saltshop.service;

import com.saltysmp.saltcoins.api.CoinService;
import com.saltysmp.saltshop.model.ShopTrade;
import com.saltysmp.saltshop.util.CoinBridge;
import com.saltysmp.saltshop.util.ItemStackUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;

public final class TradeService {

    public boolean canAfford(Player player, ShopTrade trade) {
        CoinService coins = CoinBridge.get();
        if (trade.getCoinCost() > 0) {
            if (coins == null || !coins.has(player, trade.getCoinCost())) {
                return false;
            }
        }
        for (ItemStack cost : trade.getCosts()) {
            if (countSimilar(player.getInventory(), cost) < cost.getAmount()) {
                return false;
            }
        }
        return true;
    }

    public boolean hasSpace(Player player, ItemStack result) {
        if (ItemStackUtil.isAir(result)) {
            return false;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(result.clone());
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getInventory().removeItem(item);
            }
            return false;
        }
        player.getInventory().removeItem(result.clone());
        return true;
    }

    public boolean execute(Player player, ShopTrade trade) {
        ItemStack result = trade.getResult();
        if (ItemStackUtil.isAir(result)) {
            return false;
        }
        if (!canAfford(player, trade)) {
            return false;
        }
        if (!hasSpace(player, result)) {
            return false;
        }
        charge(player, trade);
        deliver(player, result.clone());
        return true;
    }

    public void charge(Player player, ShopTrade trade) {
        CoinService coins = CoinBridge.get();
        if (trade.getCoinCost() > 0 && coins != null) {
            coins.withdraw(player, trade.getCoinCost());
        }
        chargeItems(player, trade.getCosts());
    }

    private void chargeItems(Player player, List<ItemStack> costs) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack cost : costs) {
            removeSimilar(inventory, cost);
        }
    }

    private void deliver(Player player, ItemStack result) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private int countSimilar(PlayerInventory inventory, ItemStack target) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (matchesCost(item, target)) {
                total += item.getAmount();
            }
        }
        ItemStack offhand = inventory.getItemInOffHand();
        if (matchesCost(offhand, target)) {
            total += offhand.getAmount();
        }
        return total;
    }

    private void removeSimilar(PlayerInventory inventory, ItemStack target) {
        int remaining = target.getAmount();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (!matchesCost(item, target)) {
                continue;
            }
            int remove = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            if (item.getAmount() <= 0) {
                contents[slot] = null;
            }
            remaining -= remove;
        }
        inventory.setContents(contents);

        if (remaining > 0) {
            ItemStack offhand = inventory.getItemInOffHand();
            if (matchesCost(offhand, target)) {
                int remove = Math.min(remaining, offhand.getAmount());
                offhand.setAmount(offhand.getAmount() - remove);
                if (offhand.getAmount() <= 0) {
                    inventory.setItemInOffHand(null);
                }
            }
        }
    }

    private boolean matchesCost(ItemStack item, ItemStack target) {
        if (ItemStackUtil.isAir(item) || ItemStackUtil.isAir(target)) {
            return false;
        }
        ItemStack left = item.clone();
        ItemStack right = target.clone();
        left.setAmount(1);
        right.setAmount(1);
        return left.isSimilar(right);
    }
}
