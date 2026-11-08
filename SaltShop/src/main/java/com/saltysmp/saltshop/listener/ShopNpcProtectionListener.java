package com.saltysmp.saltshop.listener;

import com.saltysmp.saltshop.config.ShopStorage;
import com.saltysmp.saltshop.service.ShopNpcService;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ShopNpcProtectionListener implements Listener {

    private final ShopStorage storage;
    private final ShopNpcService npcService;

    public ShopNpcProtectionListener(ShopStorage storage, ShopNpcService npcService) {
        this.storage = storage;
        this.npcService = npcService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (var entity : event.getChunk().getEntities()) {
            if (entity instanceof Villager villager && npcService.isShopNpc(villager)) {
                npcService.configure(villager, storage.get(npcService.shopIdOf(villager)));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager villager && npcService.isShopNpc(villager)) {
            event.setCancelled(true);
        }
    }
}
