package com.saltysmp.saltcrates.listener;

import com.saltysmp.saltcrates.config.MessagesConfig;
import com.saltysmp.saltcrates.gui.CratePreviewGui;
import com.saltysmp.saltcrates.model.CrateDefinition;
import com.saltysmp.saltcrates.model.PlacedCrate;
import com.saltysmp.saltcrates.service.CrateClaimService;
import com.saltysmp.saltcrates.service.CrateCooldownService;
import com.saltysmp.saltcrates.service.CrateService;
import com.saltysmp.saltcrates.service.PlacementService;
import com.saltysmp.saltcrates.util.BlockMaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CrateInteractListener implements Listener {

    private final CrateService crateService;
    private final CrateClaimService claimService;
    private final PlacementService placementService;
    private final MessagesConfig messages;

    public CrateInteractListener(
            CrateService crateService,
            CrateClaimService claimService,
            PlacementService placementService,
            MessagesConfig messages
    ) {
        this.crateService = crateService;
        this.claimService = claimService;
        this.placementService = placementService;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (crateService.getPlacedAt(block.getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrateBroken(BlockBreakEvent event) {
        PlacedCrate placed = crateService.getPlacedAt(event.getBlock().getLocation());
        if (placed != null) {
            placementService.unregisterAt(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        PlacedCrate placed = crateService.getPlacedAt(block.getLocation());
        if (placed != null) {
            event.setCancelled(true);
            if (event.getPlayer() instanceof Player player) {
                openPreview(player, placed);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        PlacedCrate placed = crateService.getPlacedAt(block.getLocation());
        if (placed == null) {
            return;
        }
        crateService.repairBlockIfNeeded(block);
        CrateDefinition definition = crateService.getDefinition(placed);
        if (definition == null) {
            return;
        }
        if (block.getType() != BlockMaterialUtil.normalizeDisplayBlock(definition.getDisplayBlock())) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            openPreview(player, placed);
            return;
        }

        openCrate(player, placed, definition);
    }

    private void openPreview(Player player, PlacedCrate placed) {
        if (!player.hasPermission("saltcrates.open")) {
            messages.send(player, "no-permission-open");
            return;
        }
        CrateDefinition definition = crateService.getDefinition(placed);
        if (definition == null) {
            messages.send(player, "preview-missing-definition");
            return;
        }
        player.openInventory(new CratePreviewGui(definition).getInventory());
    }

    private void openCrate(Player player, PlacedCrate placed, CrateDefinition definition) {
        if (!player.hasPermission("saltcrates.open")) {
            messages.send(player, "no-permission-open");
            return;
        }
        String cooldownKey = CrateCooldownService.cooldownKey(player.getUniqueId(), definition.getId());
        claimService.claim(player, definition, cooldownKey);
    }
}
