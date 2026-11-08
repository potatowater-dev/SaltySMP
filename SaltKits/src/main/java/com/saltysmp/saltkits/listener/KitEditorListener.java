package com.saltysmp.saltkits.listener;

import com.saltysmp.saltkits.config.MessagesConfig;
import com.saltysmp.saltkits.gui.KitEditorGui;
import com.saltysmp.saltkits.gui.KitInventoryLayout;
import com.saltysmp.saltkits.kit.KitManager;
import com.saltysmp.saltkits.kit.KitSessionRegistry;
import com.saltysmp.saltkits.model.Kit;
import com.saltysmp.saltkits.util.DurationParser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KitEditorListener implements Listener {

    private final KitManager kitManager;
    private final KitSessionRegistry sessionRegistry;
    private final ChatInputListener chatInput;
    private final MessagesConfig messages;

    public KitEditorListener(
            KitManager kitManager,
            KitSessionRegistry sessionRegistry,
            ChatInputListener chatInput,
            MessagesConfig messages
    ) {
        this.kitManager = kitManager;
        this.sessionRegistry = sessionRegistry;
        this.chatInput = chatInput;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(holder(top) instanceof KitEditorGui editor)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }

        if (!(holder(clicked) instanceof KitEditorGui)) {
            event.setCancelled(false);
            return;
        }

        int slot = event.getSlot();
        if (KitInventoryLayout.isKitGuiSlot(slot)) {
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true);
        if (slot == KitEditorGui.SLOT_SAVE) {
            kitManager.saveEditor(player, editor);
            sessionRegistry.clear(player.getUniqueId());
            player.closeInventory();
        } else if (slot == KitEditorGui.SLOT_CANCEL) {
            sessionRegistry.markDiscard(player.getUniqueId());
            messages.send(player, "editor-cancelled");
            player.closeInventory();
        } else if (KitEditorGui.isSettingsSlot(slot)) {
            handleSettingsClick(player, editor, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(holder(top) instanceof KitEditorGui)) {
            return;
        }

        boolean hitsDecoration = false;
        for (int slot : event.getRawSlots()) {
            Inventory dest = event.getView().getInventory(slot);
            if (dest == null || !(holder(dest) instanceof KitEditorGui)) {
                continue;
            }
            if (!KitInventoryLayout.isKitGuiSlot(slot)) {
                hitsDecoration = true;
                break;
            }
        }

        event.setCancelled(hitsDecoration);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(holder(inventory) instanceof KitEditorGui)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        sessionRegistry.getSession(player.getUniqueId()).ifPresent(session -> {
            boolean discard = session.discardOnClose();
            kitManager.finishEditorSession(player.getUniqueId(), !discard);
            if (!discard) {
                messages.send(player, "editor-saved", "kit", ((KitEditorGui) session.gui()).getKit().resolvedDisplayName());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        chatInput.clear(event.getPlayer());
        kitManager.finishEditorSession(event.getPlayer().getUniqueId(), true);
        sessionRegistry.clear(event.getPlayer().getUniqueId());
    }

    private static InventoryHolder holder(Inventory inventory) {
        return inventory.getHolder(false);
    }

    private void handleSettingsClick(Player player, KitEditorGui editor, int slot) {
        Kit kit = editor.getKit();
        switch (slot) {
            case KitEditorGui.SLOT_DISPLAY_NAME -> chatInput.requestInput(
                    player,
                    "<yellow>Enter display name (MiniMessage):",
                    input -> {
                        Kit updated = kit.withDisplayName(input);
                        kitManager.saveKit(updated);
                        editor.setKit(updated);
                        messages.send(player, "settings-display-updated");
                    }
            );
            case KitEditorGui.SLOT_STARTER -> {
                Kit updated = kit.withStarter(!kit.starter());
                kitManager.saveKit(updated);
                editor.setKit(updated);
                messages.send(player, "settings-starter-toggled", "value", String.valueOf(updated.starter()));
            }
            case KitEditorGui.SLOT_COOLDOWN -> chatInput.requestInput(
                    player,
                    "<yellow>Enter cooldown (e.g. 24h, 30m, 0):",
                    input -> {
                        try {
                            long seconds = DurationParser.parseSeconds(input);
                            Kit updated = editor.getKit().withCooldownSeconds(seconds);
                            kitManager.saveKit(updated);
                            editor.setKit(updated);
                            messages.send(player, "settings-cooldown-updated", "time", DurationParser.formatSeconds(seconds));
                        } catch (IllegalArgumentException exception) {
                            messages.send(player, "invalid-duration");
                        }
                    }
            );
            case KitEditorGui.SLOT_PERMISSION -> chatInput.requestInput(
                    player,
                    "<yellow>Enter permission node:",
                    input -> {
                        Kit updated = editor.getKit().withPermission(input);
                        kitManager.saveKit(updated);
                        editor.setKit(updated);
                        messages.send(player, "settings-permission-updated", "permission", input);
                    }
            );
            case KitEditorGui.SLOT_MENU_ORDER -> chatInput.requestInput(
                    player,
                    "<yellow>Enter menu list order (lower = shown earlier):",
                    input -> {
                        try {
                            int order = Integer.parseInt(input.trim());
                            Kit updated = editor.getKit().withMenuOrder(order);
                            kitManager.saveKit(updated);
                            editor.setKit(updated);
                            messages.send(player, "settings-menu-order-updated", "order", String.valueOf(order));
                        } catch (NumberFormatException exception) {
                            messages.send(player, "invalid-number");
                        }
                    }
            );
            default -> {
            }
        }
    }
}
