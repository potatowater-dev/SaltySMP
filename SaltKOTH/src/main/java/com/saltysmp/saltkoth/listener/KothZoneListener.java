package com.saltysmp.saltkoth.listener;

import com.saltysmp.saltkoth.config.AreaStorage;
import com.saltysmp.saltkoth.model.KothArea;
import com.saltysmp.saltkoth.service.KothGameService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KothZoneListener implements Listener {

    private final AreaStorage areaStorage;
    private final KothGameService gameService;
    private final Map<UUID, Boolean> insideZone = new HashMap<>();

    public KothZoneListener(AreaStorage areaStorage, KothGameService gameService) {
        this.areaStorage = areaStorage;
        this.gameService = gameService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        KothArea area = areaStorage.getArea();
        if (area == null || !area.isConfigured()) {
            return;
        }
        Player player = event.getPlayer();
        boolean nowInside = area.contains(event.getTo());
        boolean wasInside = insideZone.getOrDefault(player.getUniqueId(), false);
        if (nowInside == wasInside) {
            return;
        }
        insideZone.put(player.getUniqueId(), nowInside);
        if (nowInside) {
            gameService.onEnterZone(player);
        } else {
            gameService.onExitZone(player);
        }
    }

    public void clear(Player player) {
        insideZone.remove(player.getUniqueId());
    }
}
