package com.saltysmp.saltshop.gui;

import com.saltysmp.saltshop.config.PluginConfig;
import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.model.ShopTrade;
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

public final class ShopBrowseGui implements ShopGuiHolder {

    public static final int TRADE_START = 10;
    public static final int TRADE_COLUMNS = 7;
    public static final int TRADE_ROWS = 4;
    public static final int PREV_SLOT = 45;
    public static final int INFO_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final Player viewer;
    private final ShopDefinition shop;
    private final PluginConfig config;
    private final int page;
    private final Inventory inventory;
    private final List<Integer> tradeIndexes = new ArrayList<>();

    public ShopBrowseGui(Player viewer, ShopDefinition shop, PluginConfig config, int page) {
        this.viewer = viewer;
        this.shop = shop;
        this.config = config;
        this.page = page;
        String title = MessageUtil.format(config.getGuiTitle(), "shop", stripTags(shop.getDisplayName()));
        this.inventory = Bukkit.createInventory(this, 54, MessageUtil.parse(title));
        populate();
    }

    private void populate() {
        fillBorder();
        List<ShopTrade> trades = shop.getTrades();
        int perPage = config.getTradesPerPage();
        int start = page * perPage;
        int end = Math.min(start + perPage, trades.size());

        int menuSlot = TRADE_START;
        for (int index = start; index < end; index++) {
            if (menuSlot % 9 == 8) {
                menuSlot += 2;
            }
            if (menuSlot >= 44) {
                break;
            }
            tradeIndexes.add(index);
            inventory.setItem(menuSlot, tradeIcon(trades.get(index)));
            menuSlot++;
        }

        inventory.setItem(INFO_SLOT, infoItem());

        int totalPages = Math.max(1, (int) Math.ceil(trades.size() / (double) perPage));
        int currentPage = page + 1;
        ItemStack filler = pane(config.getBorderMaterial());
        if (trades.isEmpty()) {
            inventory.setItem(TRADE_START + 4, control(
                    Material.BARRIER,
                    "<red>No trades yet",
                    List.of("<gray>Ask an admin to stock this shop")
            ));
        }
        if (page > 0) {
            inventory.setItem(PREV_SLOT, control(Material.ARROW, "<yellow>Previous Page",
                    List.of("<gray>Page " + (currentPage - 1) + " of " + totalPages)));
        } else {
            inventory.setItem(PREV_SLOT, filler);
        }
        if (end < trades.size()) {
            inventory.setItem(NEXT_SLOT, control(Material.ARROW, "<yellow>Next Page",
                    List.of("<gray>Page " + (currentPage + 1) + " of " + totalPages)));
        } else {
            inventory.setItem(NEXT_SLOT, filler);
        }
    }

    private ItemStack tradeIcon(ShopTrade trade) {
        ItemStack icon = trade.getResult().clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(MessageUtil.parse("<dark_gray>────────────"));
            lore.add(MessageUtil.parse("<gray>Price: <white>" + ItemStackUtil.describeTradeCost(trade)));
            lore.add(MessageUtil.parse("<green>Click to purchase"));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack infoItem() {
        int totalPages = Math.max(1, (int) Math.ceil(shop.getTrades().size() / (double) config.getTradesPerPage()));
        return control(
                Material.EMERALD,
                shop.getDisplayName(),
                List.of(
                        "<gray>Trades: <white>" + shop.getTrades().size(),
                        "<gray>Page: <white>" + (page + 1) + "/" + totalPages,
                        "<green>Left-click an item to buy"
                )
        );
    }

    private void fillBorder() {
        ItemStack pane = pane(config.getBorderMaterial());
        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, pane);
        }
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, pane);
            inventory.setItem(row * 9 + 8, pane);
        }
        for (int slot = 45; slot < 54; slot++) {
            if (slot != PREV_SLOT && slot != INFO_SLOT && slot != NEXT_SLOT) {
                inventory.setItem(slot, pane);
            }
        }
    }

    private ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            pane.setItemMeta(meta);
        }
        return pane;
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

    private String stripTags(String input) {
        return input.replaceAll("<[^>]+>", "");
    }

    public int tradeIndexAtMenuSlot(int menuSlot) {
        int tradeAreaIndex = tradeSlotIndex(menuSlot);
        if (tradeAreaIndex < 0 || tradeAreaIndex >= tradeIndexes.size()) {
            return -1;
        }
        return tradeIndexes.get(tradeAreaIndex);
    }

    private int tradeSlotIndex(int menuSlot) {
        if (menuSlot < TRADE_START || menuSlot > 43) {
            return -1;
        }
        int row = menuSlot / 9;
        int col = menuSlot % 9;
        if (col == 0 || col == 8) {
            return -1;
        }
        return (row - 1) * TRADE_COLUMNS + (col - 1);
    }

    @Override
    public Player getViewer() {
        return viewer;
    }

    @Override
    public ShopDefinition getShop() {
        return shop;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
