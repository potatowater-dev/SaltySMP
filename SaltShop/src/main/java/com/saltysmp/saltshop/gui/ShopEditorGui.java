package com.saltysmp.saltshop.gui;

import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.model.ShopTrade;
import com.saltysmp.saltshop.util.GuiItems;
import com.saltysmp.saltshop.util.ItemStackUtil;
import com.saltysmp.saltshop.util.MessageUtil;
import com.saltysmp.saltshop.util.VillagerLabels;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ShopEditorGui implements ShopGuiHolder {

    public static final int TRADE_SLOTS = 45;
    public static final int SLOT_VILLAGER_TYPE = 45;
    public static final int SLOT_PROFESSION = 46;
    public static final int SLOT_LEVEL = 47;
    public static final int ADD_SLOT = 49;
    public static final int SAVE_SLOT = 53;

    private static final Villager.Type[] TYPES = Villager.Type.values();
    private static final Villager.Profession[] PROFESSIONS = Villager.Profession.values();

    private final Player viewer;
    private final ShopDefinition shop;
    private final Inventory inventory;

    public ShopEditorGui(Player viewer, ShopDefinition shop) {
        this.viewer = viewer;
        this.shop = shop;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                MessageUtil.parse("<gold>Edit Shop: <aqua>" + shop.getId())
        );
        populate();
    }

    private void populate() {
        List<ShopTrade> trades = shop.getTrades();
        for (int index = 0; index < Math.min(TRADE_SLOTS, trades.size()); index++) {
            inventory.setItem(index, tradePreview(trades.get(index), index));
        }

        ItemStack filler = GuiItems.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = trades.size(); slot < TRADE_SLOTS; slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(SLOT_VILLAGER_TYPE, control(
                Material.LEATHER_CHESTPLATE,
                "<aqua>Style: <white>" + VillagerLabels.friendlyType(shop.getVillagerType()),
                List.of("<gray>Click to cycle villager outfit")
        ));
        inventory.setItem(SLOT_PROFESSION, control(
                Material.ANVIL,
                "<aqua>Job: <white>" + VillagerLabels.friendlyProfession(shop.getProfession()),
                List.of("<gray>Click to cycle profession")
        ));
        inventory.setItem(SLOT_LEVEL, control(
                Material.EXPERIENCE_BOTTLE,
                "<aqua>Level: <white>" + shop.getVillagerLevel(),
                List.of("<gray>Click to cycle badge level 1-5")
        ));

        inventory.setItem(ADD_SLOT, control(
                Material.LIME_STAINED_GLASS_PANE,
                "<green>Add Trade",
                List.of(
                        "<gray>Click with result item in hand",
                        "<gray>Set costs in the trade editor"
                )
        ));
        inventory.setItem(SAVE_SLOT, control(
                Material.EMERALD_BLOCK,
                "<green>Save & Close",
                List.of("<gray>Writes shop to disk")
        ));

        for (int slot = 48; slot < 54; slot++) {
            if (slot != ADD_SLOT && slot != SAVE_SLOT
                    && slot != SLOT_VILLAGER_TYPE && slot != SLOT_PROFESSION && slot != SLOT_LEVEL) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack tradePreview(ShopTrade trade, int index) {
        ItemStack icon = trade.getResult().clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(MessageUtil.parse("<dark_gray>────────────"));
            lore.add(MessageUtil.parse("<gray>Price: <white>" + ItemStackUtil.describeTradeCost(trade)));
            lore.add(MessageUtil.parse("<yellow>Left-click: <white>Edit trade"));
            lore.add(MessageUtil.parse("<red>Shift-right-click: <white>Remove"));
            lore.add(MessageUtil.parse("<dark_gray>#" + (index + 1)));
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    public void refresh() {
        populate();
    }

    public void cycleVillagerType() {
        Villager.Type current = shop.getVillagerType();
        int index = 0;
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i] == current) {
                index = i;
                break;
            }
        }
        shop.setVillagerType(TYPES[(index + 1) % TYPES.length]);
    }

    public void cycleProfession() {
        Villager.Profession current = shop.getProfession();
        int index = 0;
        for (int i = 0; i < PROFESSIONS.length; i++) {
            if (PROFESSIONS[i] == current) {
                index = i;
                break;
            }
        }
        shop.setProfession(PROFESSIONS[(index + 1) % PROFESSIONS.length]);
    }

    public void cycleLevel() {
        int next = shop.getVillagerLevel() >= 5 ? 1 : shop.getVillagerLevel() + 1;
        shop.setVillagerLevel(next);
    }

    public int tradeIndexAtSlot(int slot) {
        if (slot < 0 || slot >= TRADE_SLOTS) {
            return -1;
        }
        if (slot >= shop.getTrades().size()) {
            return -1;
        }
        return slot;
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
