package com.saltysmp.saltmine.listener;

import com.saltysmp.saltmine.config.MessagesConfig;
import com.saltysmp.saltmine.config.PluginConfig;
import com.saltysmp.saltmine.model.MineDefinition;
import com.saltysmp.saltmine.storage.MineStorage;
import com.saltysmp.saltmine.util.MessageThrottle;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class MineBreakListener implements Listener {

    private final PluginConfig pluginConfig;
    private final MessagesConfig messages;
    private final MineStorage mineStorage;
    private final MessageThrottle breakThrottle = new MessageThrottle(2500L);

    public MineBreakListener(PluginConfig pluginConfig, MessagesConfig messages, MineStorage mineStorage) {
        this.pluginConfig = pluginConfig;
        this.messages = messages;
        this.mineStorage = mineStorage;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!pluginConfig.isCancelBreakOutsideMines()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Block block = event.getBlock();
        MineDefinition mine = findContainingMine(block);
        if (mine != null) {
            if (!canUseMine(player, mine)) {
                event.setCancelled(true);
                if (breakThrottle.shouldSend(player.getUniqueId())) {
                    messages.send(player, "mine-access-denied", "id", mine.getId());
                }
            }
            return;
        }
        event.setCancelled(true);
        if (breakThrottle.shouldSend(player.getUniqueId())) {
            messages.send(player, "break-outside-denied");
        }
    }

    private MineDefinition findContainingMine(Block block) {
        for (MineDefinition mine : mineStorage.getAll()) {
            if (mine.getRegion() != null && mine.getRegion().contains(block)) {
                return mine;
            }
        }
        return null;
    }

    private boolean canUseMine(Player player, MineDefinition mine) {
        String mineId = mine.getId();
        return player.hasPermission("saltmine.admin")
                || player.hasPermission("saltmine.use." + mineId)
                || player.hasPermission("saltmine.use.*");
    }
}
