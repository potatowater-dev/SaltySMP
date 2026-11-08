package com.saltysmp.saltshop.util;

import com.saltysmp.saltshop.model.ShopTrade;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemStackUtil {

    private ItemStackUtil() {
    }

    public static ItemStack loadItem(Object raw) {
        if (raw instanceof ItemStack stack) {
            return stack.clone();
        }
        if (raw instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> serialized = (Map<String, Object>) map;
            return ItemStack.deserialize(serialized);
        }
        return null;
    }

    public static List<ItemStack> loadCostList(ConfigurationSection section, String path) {
        List<ItemStack> costs = new ArrayList<>();
        if (section == null) {
            return costs;
        }
        List<?> list = section.getList(path);
        if (list == null) {
            return costs;
        }
        for (Object entry : list) {
            ItemStack item = loadItem(entry);
            if (item != null && !item.getType().isAir()) {
                costs.add(item);
            }
        }
        return costs;
    }

    public static boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    public static String describeCost(ItemStack cost) {
        if (isAir(cost)) {
            return "";
        }
        return cost.getAmount() + "x " + MessageUtil.plainItemName(cost);
    }

    public static String describeCosts(List<ItemStack> costs) {
        if (costs.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < costs.size(); index++) {
            if (index > 0) {
                builder.append(" + ");
            }
            builder.append(describeCost(costs.get(index)));
        }
        return builder.toString();
    }

    public static String describeTradeCost(ShopTrade trade) {
        List<String> parts = new ArrayList<>();
        if (trade.getCoinCost() > 0) {
            parts.add(trade.getCoinCost() + " Coins");
        }
        String items = describeCosts(trade.getCosts());
        if (!items.isEmpty()) {
            parts.add(items);
        }
        if (parts.isEmpty()) {
            return "free";
        }
        return String.join(" + ", parts);
    }
}
