package com.saltysmp.saltshop.listener;

import com.saltysmp.saltshop.config.MessagesConfig;
import com.saltysmp.saltshop.config.PluginConfig;
import com.saltysmp.saltshop.config.ShopStorage;
import com.saltysmp.saltshop.gui.ShopBrowseGui;
import com.saltysmp.saltshop.gui.ShopEditorGui;
import com.saltysmp.saltshop.gui.ShopGuiHolder;
import com.saltysmp.saltshop.gui.TradeEditGui;
import com.saltysmp.saltshop.model.ShopTrade;
import com.saltysmp.saltshop.service.ShopNpcService;
import com.saltysmp.saltshop.service.TradeService;
import com.saltysmp.saltshop.util.CoinBridge;
import com.saltysmp.saltshop.util.ItemStackUtil;
import com.saltysmp.saltshop.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopGuiListener implements Listener {

    private final PluginConfig config;
    private final MessagesConfig messages;
    private final ShopStorage storage;
    private final TradeService tradeService;
    private final ShopNpcService npcService;
    private final Map<UUID, PendingDraft> pendingDrafts = new HashMap<>();
    private final Set<UUID> skipCloseReturn = ConcurrentHashMap.newKeySet();

    public ShopGuiListener(
            PluginConfig config,
            MessagesConfig messages,
            ShopStorage storage,
            TradeService tradeService,
            ShopNpcService npcService
    ) {
        this.config = config;
        this.messages = messages;
        this.storage = storage;
        this.tradeService = tradeService;
        this.npcService = npcService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopGuiHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (holder instanceof ShopBrowseGui browse) {
            handleBrowseClick(event, player, browse);
        } else if (holder instanceof ShopEditorGui editor) {
            handleEditorClick(event, player, editor);
        } else if (holder instanceof TradeEditGui tradeEditor) {
            handleTradeEditClick(event, player, tradeEditor);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof TradeEditGui editor)) {
            return;
        }
        if (skipCloseReturn.remove(player.getUniqueId())) {
            return;
        }
        PendingDraft draft = pendingDrafts.remove(player.getUniqueId());
        if (draft != null && draft.awaitingSave()) {
            editor.getShop().removeTrade(draft.tradeIndex());
            storage.save(editor.getShop());
            npcService.refreshShop(editor.getShop().getId());
        }
        editor.returnEditableItems(player);
        if (draft != null && draft.returnedItems()) {
            messages.send(player, "editor-items-returned");
        }
    }

    private void handleBrowseClick(InventoryClickEvent event, Player player, ShopBrowseGui browse) {
        int rawSlot = event.getRawSlot();
        if (rawSlot >= browse.getInventory().getSize()) {
            return;
        }
        event.setCancelled(true);

        if (browse.getShop().getTrades().isEmpty()) {
            messages.send(player, "browse-empty");
            return;
        }

        if (rawSlot == ShopBrowseGui.PREV_SLOT && browse.getPage() > 0) {
            player.openInventory(new ShopBrowseGui(player, browse.getShop(), config, browse.getPage() - 1).getInventory());
            return;
        }
        if (rawSlot == ShopBrowseGui.NEXT_SLOT) {
            int nextPage = browse.getPage() + 1;
            int maxPage = (int) Math.ceil(browse.getShop().getTrades().size() / (double) config.getTradesPerPage());
            if (nextPage < maxPage) {
                player.openInventory(new ShopBrowseGui(player, browse.getShop(), config, nextPage).getInventory());
            }
            return;
        }

        int tradeIndex = browse.tradeIndexAtMenuSlot(rawSlot);
        if (tradeIndex < 0) {
            return;
        }
        ShopTrade trade = browse.getShop().getTrade(tradeIndex);
        if (trade == null) {
            return;
        }
        if (!tradeService.canAfford(player, trade)) {
            messages.send(player, "trade-fail-cost", "cost", ItemStackUtil.describeTradeCost(trade));
            return;
        }
        if (!tradeService.execute(player, trade)) {
            messages.send(player, "trade-fail-space");
            return;
        }
        messages.send(
                player,
                "trade-success",
                "item",
                MessageUtil.plainItemName(trade.getResult())
        );
    }

    private void handleEditorClick(InventoryClickEvent event, Player player, ShopEditorGui editor) {
        int rawSlot = event.getRawSlot();
        if (rawSlot >= editor.getInventory().getSize()) {
            return;
        }

        if (rawSlot >= ShopEditorGui.TRADE_SLOTS) {
            event.setCancelled(true);
            if (rawSlot == ShopEditorGui.SLOT_VILLAGER_TYPE) {
                editor.cycleVillagerType();
                editor.refresh();
            } else if (rawSlot == ShopEditorGui.SLOT_PROFESSION) {
                editor.cycleProfession();
                editor.refresh();
            } else if (rawSlot == ShopEditorGui.SLOT_LEVEL) {
                editor.cycleLevel();
                editor.refresh();
            } else if (rawSlot == ShopEditorGui.ADD_SLOT) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (ItemStackUtil.isAir(hand)) {
                    messages.send(player, "editor-need-item");
                    return;
                }
                int index = editor.getShop().getTrades().size();
                editor.getShop().addTrade(new ShopTrade(hand.clone(), java.util.List.of(), Integer.MAX_VALUE));
                pendingDrafts.put(player.getUniqueId(), new PendingDraft(index, true));
                messages.send(player, "editor-trade-added");
                player.openInventory(new TradeEditGui(
                        player,
                        editor.getShop(),
                        index,
                        true,
                        hand.clone()
                ).getInventory());
            } else if (rawSlot == ShopEditorGui.SAVE_SLOT) {
                storage.save(editor.getShop());
                npcService.refreshShop(editor.getShop().getId());
                messages.send(player, "editor-saved", "shop", editor.getShop().getId());
                player.closeInventory();
            }
            return;
        }

        event.setCancelled(true);

        int tradeIndex = editor.tradeIndexAtSlot(rawSlot);
        if (tradeIndex < 0) {
            return;
        }

        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            editor.getShop().removeTrade(tradeIndex);
            storage.save(editor.getShop());
            npcService.refreshShop(editor.getShop().getId());
            messages.send(player, "editor-trade-removed");
            editor.refresh();
            return;
        }

        if (event.getClick() == ClickType.LEFT) {
            pendingDrafts.remove(player.getUniqueId());
            player.openInventory(new TradeEditGui(player, editor.getShop(), tradeIndex, false).getInventory());
        }
    }

    private void handleTradeEditClick(InventoryClickEvent event, Player player, TradeEditGui editor) {
        int rawSlot = event.getRawSlot();
        if (rawSlot >= editor.getInventory().getSize()) {
            return;
        }

        if (rawSlot == TradeEditGui.SLOT_SAVE || rawSlot == TradeEditGui.SLOT_BACK) {
            event.setCancelled(true);
            if (rawSlot == TradeEditGui.SLOT_BACK) {
                skipCloseReturn.add(player.getUniqueId());
                editor.returnEditableItems(player);
                pendingDrafts.remove(player.getUniqueId());
                player.openInventory(new ShopEditorGui(player, editor.getShop()).getInventory());
                return;
            }
            ShopTrade trade = editor.collectTrade();
            if (trade == null) {
                if (ItemStackUtil.isAir(editor.getInventory().getItem(TradeEditGui.SLOT_RESULT))
                        || com.saltysmp.saltshop.util.GuiItems.isSlotLabel(editor.getInventory().getItem(TradeEditGui.SLOT_RESULT))) {
                    messages.send(player, "editor-need-result");
                } else {
                    ItemStack coinSlot = editor.getInventory().getItem(TradeEditGui.SLOT_COIN_COST);
                    if (!ItemStackUtil.isAir(coinSlot)
                            && !com.saltysmp.saltshop.util.GuiItems.isSlotLabel(coinSlot)
                            && CoinBridge.get() != null
                            && !CoinBridge.get().isCoin(coinSlot)) {
                        messages.send(player, "editor-invalid-coin-slot");
                    } else {
                        messages.send(player, "editor-need-cost");
                    }
                }
                return;
            }
            if (editor.isCreating()) {
                editor.getShop().setTrade(editor.getTradeIndex(), trade);
            } else {
                editor.getShop().setTrade(editor.getTradeIndex(), trade);
            }
            pendingDrafts.remove(player.getUniqueId());
            skipCloseReturn.add(player.getUniqueId());
            editor.returnEditableItems(player);
            storage.save(editor.getShop());
            npcService.refreshShop(editor.getShop().getId());
            messages.send(player, "editor-trade-saved");
            player.openInventory(new ShopEditorGui(player, editor.getShop()).getInventory());
            return;
        }

        if (rawSlot == TradeEditGui.SLOT_COIN_COST
                || rawSlot == TradeEditGui.SLOT_COST_ONE
                || rawSlot == TradeEditGui.SLOT_COST_TWO
                || rawSlot == TradeEditGui.SLOT_RESULT) {
            if (event.getClick().isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopGuiHolder holder)) {
            return;
        }
        if (holder instanceof ShopBrowseGui) {
            event.setCancelled(true);
            return;
        }
        if (holder instanceof ShopEditorGui) {
            for (int slot : event.getRawSlots()) {
                if (slot < top.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }
        if (holder instanceof TradeEditGui) {
            for (int slot : event.getRawSlots()) {
                if (slot < top.getSize()
                        && slot != TradeEditGui.SLOT_COIN_COST
                        && slot != TradeEditGui.SLOT_COST_ONE
                        && slot != TradeEditGui.SLOT_COST_TWO
                        && slot != TradeEditGui.SLOT_RESULT) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private record PendingDraft(int tradeIndex, boolean awaitingSave) {
        boolean returnedItems() {
            return awaitingSave;
        }
    }
}
