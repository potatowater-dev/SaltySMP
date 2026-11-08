package com.saltysmp.saltkoth.listener;

import com.saltysmp.saltkoth.listener.KothZoneListener;
import com.saltysmp.saltkoth.service.CrateRewardService;
import com.saltysmp.saltkoth.service.KothGameService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerLifecycleListener implements Listener {

    private final KothGameService gameService;
    private final CrateRewardService rewardService;
    private final KothZoneListener zoneListener;

    public PlayerLifecycleListener(
            KothGameService gameService,
            CrateRewardService rewardService,
            KothZoneListener zoneListener
    ) {
        this.gameService = gameService;
        this.rewardService = rewardService;
        this.zoneListener = zoneListener;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        rewardService.deliverPending(event.getPlayer());
        rewardService.flushPending();
        gameService.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        zoneListener.clear(event.getPlayer());
        gameService.onPlayerQuit(event.getPlayer());
    }
}
