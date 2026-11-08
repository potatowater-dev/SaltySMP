package com.saltysmp.saltshop.service;

import com.saltysmp.saltcoins.api.CoinService;
import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.model.ShopTrade;
import com.saltysmp.saltshop.util.CoinBridge;
import com.saltysmp.saltshop.util.GuiItems;
import com.saltysmp.saltshop.util.ItemStackUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public final class MerchantRecipeService {

    public List<MerchantRecipe> buildRecipes(ShopDefinition shop) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        CoinService coins = CoinBridge.get();
        for (ShopTrade trade : shop.getTrades()) {
            if (ItemStackUtil.isAir(trade.getResult()) || GuiItems.isGlassPane(trade.getResult())) {
                continue;
            }
            List<ItemStack> ingredients = resolveIngredients(trade, coins);
            if (ingredients.isEmpty()) {
                continue;
            }
            MerchantRecipe recipe = new MerchantRecipe(trade.getResult().clone(), 0, trade.getMaxUses(), false);
            for (ItemStack ingredient : ingredients) {
                recipe.addIngredient(ingredient);
            }
            recipe.setExperienceReward(false);
            recipes.add(recipe);
        }
        return recipes;
    }

    private List<ItemStack> resolveIngredients(ShopTrade trade, CoinService coins) {
        List<ItemStack> ingredients = new ArrayList<>();
        if (trade.getCoinCost() > 0 && coins != null) {
            ingredients.add(coins.create(trade.getCoinCost()));
        }
        for (ItemStack cost : trade.getCosts()) {
            if (ItemStackUtil.isAir(cost) || GuiItems.isFiller(cost) || GuiItems.isGlassPane(cost)) {
                continue;
            }
            ingredients.add(cost.clone());
            if (ingredients.size() >= 2) {
                break;
            }
        }
        return ingredients;
    }
}
