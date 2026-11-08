package com.saltysmp.saltnewbie.listener;

import com.saltysmp.saltnewbie.config.MessagesConfig;
import com.saltysmp.saltnewbie.service.NewbieService;
import com.saltysmp.saltnewbie.util.ItemTagUtil;
import com.saltysmp.saltnewbie.util.PluginKeys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class NewbieProtectionListener implements Listener {

    private final NewbieService newbieService;
    private final MessagesConfig messages;
    private final PluginKeys keys;

    public NewbieProtectionListener(NewbieService newbieService, MessagesConfig messages, PluginKeys keys) {
        this.newbieService = newbieService;
        this.messages = messages;
        this.keys = keys;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (newbieService.isProtected(victim.getUniqueId()) || newbieService.isProtected(attacker.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (!newbieService.isProtected(player.getUniqueId())) {
            return;
        }
        UUID miner = player.getUniqueId();
        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            ItemTagUtil.markMinedBy(stack, keys, miner);
            item.setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        for (ItemStack stack : event.getDrops()) {
            ItemTagUtil.markDeathLoot(stack, keys);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        ItemTagUtil.markPlayerDropped(stack, keys);
        event.getItemDrop().setItemStack(stack);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!newbieService.isProtected(player.getUniqueId())) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (ItemTagUtil.canProtectedNewbiePickup(stack, keys, player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        messages.send(player, "pickup-denied");
    }

    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
