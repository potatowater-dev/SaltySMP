package com.saltysmp.saltmine.listener;

import com.saltysmp.saltmine.config.MessagesConfig;
import com.saltysmp.saltmine.item.MineWandFactory;
import com.saltysmp.saltmine.service.WandSelectionService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class MineWandListener implements Listener {

    private final MessagesConfig messages;
    private final MineWandFactory wandFactory;
    private final WandSelectionService selectionService;

    public MineWandListener(
            MessagesConfig messages,
            MineWandFactory wandFactory,
            WandSelectionService selectionService
    ) {
        this.messages = messages;
        this.wandFactory = wandFactory;
        this.selectionService = selectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWandInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!wandFactory.isWand(event.getItem())) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            if (action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR) {
                event.setCancelled(true);
                messages.send(event.getPlayer(), "wand-click-air");
            }
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (action == Action.LEFT_CLICK_BLOCK) {
            selectionService.setPos1(player.getUniqueId(), block.getLocation());
            messages.send(player, "pos1-set",
                    "world", block.getWorld().getName(),
                    "x", String.valueOf(block.getX()),
                    "y", String.valueOf(block.getY()),
                    "z", String.valueOf(block.getZ()));
        } else {
            selectionService.setPos2(player.getUniqueId(), block.getLocation());
            messages.send(player, "pos2-set",
                    "world", block.getWorld().getName(),
                    "x", String.valueOf(block.getX()),
                    "y", String.valueOf(block.getY()),
                    "z", String.valueOf(block.getZ()));
        }
    }
}
