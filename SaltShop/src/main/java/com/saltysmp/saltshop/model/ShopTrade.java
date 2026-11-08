package com.saltysmp.saltshop.model;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopTrade {

    private ItemStack result;
    private final List<ItemStack> costs = new ArrayList<>();
    private int coinCost;
    private int maxUses;

    public ShopTrade(ItemStack result, List<ItemStack> costs, int coinCost, int maxUses) {
        this.result = result == null ? null : result.clone();
        this.coinCost = Math.max(0, coinCost);
        if (costs != null) {
            for (ItemStack cost : costs) {
                if (cost != null && !cost.getType().isAir()) {
                    this.costs.add(cost.clone());
                }
            }
        }
        this.maxUses = Math.max(1, maxUses);
    }

    public ShopTrade(ItemStack result, List<ItemStack> costs, int maxUses) {
        this(result, costs, 0, maxUses);
    }

    public ItemStack getResult() {
        return result == null ? null : result.clone();
    }

    public void setResult(ItemStack result) {
        this.result = result == null ? null : result.clone();
    }

    public List<ItemStack> getCosts() {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack cost : costs) {
            copy.add(cost.clone());
        }
        return copy;
    }

    public void setCosts(List<ItemStack> newCosts) {
        costs.clear();
        if (newCosts != null) {
            for (ItemStack cost : newCosts) {
                if (cost != null && !cost.getType().isAir()) {
                    costs.add(cost.clone());
                }
            }
        }
    }

    public int getCoinCost() {
        return coinCost;
    }

    public void setCoinCost(int coinCost) {
        this.coinCost = Math.max(0, coinCost);
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = Math.max(1, maxUses);
    }

    public ShopTrade copy() {
        ShopTrade copy = new ShopTrade(result, costs, coinCost, maxUses);
        return copy;
    }
}
