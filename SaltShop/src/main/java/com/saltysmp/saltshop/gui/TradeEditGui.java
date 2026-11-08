package com.saltysmp.saltshop.gui;

import com.saltysmp.saltcoins.api.CoinService;
import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.model.ShopTrade;
import com.saltysmp.saltshop.util.CoinBridge;
import com.saltysmp.saltshop.util.GuiItems;
import com.saltysmp.saltshop.util.ItemStackUtil;
import com.saltysmp.saltshop.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TradeEditGui implements ShopGuiHolder {

    public static final int SLOT_COIN_COST = 10;
    public static final int SLOT_COST_ONE = 11;
    public static final int SLOT_COST_TWO = 12;
    public static final int SLOT_RESULT = 15;
    public static final int SLOT_SAVE = 22;
    public static final int SLOT_BACK = 18;

    private final Player viewer;
    private final ShopDefinition shop;
    private final int tradeIndex;
    private final boolean creating;
    private final Inventory inventory;

    public TradeEditGui(Player viewer, ShopDefinition shop, int tradeIndex, boolean creating) {
        this.viewer = viewer;
        this.shop = shop;
        this.tradeIndex = tradeIndex;
        this.creating = creating;
        this.inventory = Bukkit.createInventory(
                this,
                27,
                MessageUtil.parse("<gold>Trade Editor <gray>#" + (tradeIndex + 1))
        );
        populate();
    }

    public TradeEditGui(Player viewer, ShopDefinition shop, int tradeIndex, boolean creating, ItemStack initialResult) {
        this(viewer, shop, tradeIndex, creating);
        if (!ItemStackUtil.isAir(initialResult)) {
            inventory.setItem(SLOT_RESULT, initialResult.clone());
        }
    }

    private void populate() {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, GuiItems.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        ShopTrade trade = creating ? null : shop.getTrade(tradeIndex);
        if (trade != null) {
            List<ItemStack> costs = trade.getCosts();
            if (!costs.isEmpty()) {
                inventory.setItem(SLOT_COST_ONE, costs.get(0).clone());
            }
            if (costs.size() > 1) {
                inventory.setItem(SLOT_COST_TWO, costs.get(1).clone());
            }
            inventory.setItem(SLOT_RESULT, trade.getResult().clone());
            if (trade.getCoinCost() > 0) {
                CoinService coins = CoinBridge.get();
                if (coins != null) {
                    inventory.setItem(SLOT_COIN_COST, coins.create(trade.getCoinCost()));
                }
            }
        }

        placeLabelIfEmpty(SLOT_COIN_COST, Material.GOLD_NUGGET, "<gold>Coin Price",
                List.of("<gray>Place Coin items here"));
        placeLabelIfEmpty(SLOT_COST_ONE, Material.IRON_INGOT, "<aqua>Item Cost 1",
                List.of("<gray>Optional item cost"));
        placeLabelIfEmpty(SLOT_COST_TWO, Material.COPPER_INGOT, "<aqua>Item Cost 2",
                List.of("<gray>Optional second cost"));
        placeLabelIfEmpty(SLOT_RESULT, Material.CHEST, "<green>Reward",
                List.of("<gray>What the player receives"));

        inventory.setItem(SLOT_BACK, control(
                Material.ARROW,
                "<yellow>Back",
                List.of("<gray>Return items & go back")
        ));
        inventory.setItem(SLOT_SAVE, control(
                Material.EMERALD_BLOCK,
                "<green>Save Trade",
                List.of("<gray>Saves this trade to the shop")
        ));
        inventory.setItem(4, control(
                Material.BOOK,
                "<aqua>Trade setup",
                List.of(
                        "<gray>Coin Price — coins required",
                        "<gray>Item costs — up to 2 items",
                        "<gray>Reward — what they get",
                        "<yellow>Click Save when done"
                )
        ));
    }

    private void placeLabelIfEmpty(int slot, Material icon, String name, List<String> loreLines) {
        ItemStack current = inventory.getItem(slot);
        if (!ItemStackUtil.isAir(current) && !GuiItems.isFiller(current)) {
            return;
        }
        inventory.setItem(slot, GuiItems.slotLabel(icon, name, loreLines));
    }

    public ShopTrade collectTrade() {
        List<ItemStack> costs = new ArrayList<>();
        ItemStack costOne = readEditableSlot(SLOT_COST_ONE);
        ItemStack costTwo = readEditableSlot(SLOT_COST_TWO);
        if (!ItemStackUtil.isAir(costOne)) {
            costs.add(costOne.clone());
        }
        if (!ItemStackUtil.isAir(costTwo)) {
            costs.add(costTwo.clone());
        }

        int coinCost = 0;
        ItemStack coinStack = readEditableSlot(SLOT_COIN_COST);
        CoinService coins = CoinBridge.get();
        if (!ItemStackUtil.isAir(coinStack)) {
            if (coins == null || !coins.isCoin(coinStack)) {
                return null;
            }
            coinCost = coinStack.getAmount();
        }

        ItemStack result = readEditableSlot(SLOT_RESULT);
        if (ItemStackUtil.isAir(result)) {
            return null;
        }

        if (coinCost <= 0 && costs.isEmpty()) {
            return null;
        }

        ShopTrade existing = creating ? null : shop.getTrade(tradeIndex);
        int maxUses = existing == null ? Integer.MAX_VALUE : existing.getMaxUses();
        return new ShopTrade(result, costs, coinCost, maxUses);
    }

    public void returnEditableItems(Player player) {
        returnSlot(player, SLOT_COIN_COST);
        returnSlot(player, SLOT_COST_ONE);
        returnSlot(player, SLOT_COST_TWO);
        returnSlot(player, SLOT_RESULT);
    }

    private void returnSlot(Player player, int slot) {
        ItemStack item = readEditableSlot(slot);
        if (ItemStackUtil.isAir(item)) {
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        inventory.setItem(slot, null);
    }

    private ItemStack readEditableSlot(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (ItemStackUtil.isAir(item) || GuiItems.isFiller(item) || GuiItems.isSlotLabel(item)) {
            return null;
        }
        return item;
    }

    private boolean isLabelItem(ItemStack item) {
        return GuiItems.isSlotLabel(item);
    }

    public int getTradeIndex() {
        return tradeIndex;
    }

    public boolean isCreating() {
        return creating;
    }

    private ItemStack control(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Player getViewer() {
        return viewer;
    }

    @Override
    public ShopDefinition getShop() {
        return shop;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
