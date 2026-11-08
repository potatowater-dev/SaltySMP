package com.saltysmp.saltshop.service;

import com.saltysmp.saltshop.config.PluginConfig;
import com.saltysmp.saltshop.config.ShopStorage;
import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.model.SpawnedShopNpc;
import com.saltysmp.saltshop.util.MessageUtil;
import com.saltysmp.saltshop.util.PluginKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ShopNpcService {

    private final PluginConfig config;
    private final ShopStorage storage;
    private final PluginKeys keys;
    private final MerchantRecipeService recipeService;

    public ShopNpcService(
            PluginConfig config,
            ShopStorage storage,
            PluginKeys keys,
            MerchantRecipeService recipeService
    ) {
        this.config = config;
        this.storage = storage;
        this.keys = keys;
        this.recipeService = recipeService;
    }

    public void restoreAll() {
        for (SpawnedShopNpc record : new ArrayList<>(storage.getNpcs())) {
            Location location = record.toLocation();
            if (location == null) {
                continue;
            }
            Entity entity = Bukkit.getEntity(record.getEntityId());
            if (entity instanceof Villager villager && villager.isValid()) {
                configure(villager, storage.get(record.getShopId()));
                continue;
            }
            ShopDefinition shop = storage.get(record.getShopId());
            if (shop == null) {
                continue;
            }
            Villager spawned = spawnAt(location, shop);
            if (spawned != null) {
                storage.removeNpc(record.getEntityId());
                storage.addNpc(new SpawnedShopNpc(shop.getId(), spawned.getUniqueId(), location));
            }
        }
    }

    public Villager spawn(Player player, ShopDefinition shop) {
        Villager villager = spawnAt(player.getLocation(), shop);
        if (villager != null) {
            storage.addNpc(new SpawnedShopNpc(shop.getId(), villager.getUniqueId(), player.getLocation()));
        }
        return villager;
    }

    public Villager spawnAt(Location location, ShopDefinition shop) {
        Location spawn = location.clone();
        if (spawn.getWorld() == null) {
            return null;
        }
        return spawn.getWorld().spawn(spawn, Villager.class, entity -> configure(entity, shop));
    }

    public void configure(Villager villager, ShopDefinition shop) {
        villager.getPersistentDataContainer().set(keys.shopNpc, PersistentDataType.BYTE, (byte) 1);
        if (shop == null) {
            villager.setRecipes(List.of());
            return;
        }
        villager.getPersistentDataContainer().set(keys.shopId, PersistentDataType.STRING, shop.getId());
        villager.customName(MessageUtil.parse(shop.getDisplayName()));
        villager.setCustomNameVisible(true);
        villager.setProfession(shop.getProfession() != Villager.Profession.NONE
                ? shop.getProfession()
                : config.getDefaultProfession());
        villager.setVillagerType(shop.getVillagerType() != null
                ? shop.getVillagerType()
                : config.getDefaultType());
        villager.setVillagerLevel(shop.getVillagerLevel() > 0
                ? shop.getVillagerLevel()
                : config.getVillagerLevel());
        villager.setAI(!config.isVillagerNoAi());
        villager.setInvulnerable(config.isVillagerInvulnerable());
        villager.setSilent(config.isVillagerSilent());
        villager.setRemoveWhenFarAway(false);

        List<MerchantRecipe> recipes = recipeService.buildRecipes(shop);
        villager.setRecipes(recipes);
    }

    public String shopIdOf(Villager villager) {
        return villager.getPersistentDataContainer().get(keys.shopId, PersistentDataType.STRING);
    }

    public boolean isShopNpc(Villager villager) {
        Byte marker = villager.getPersistentDataContainer().get(keys.shopNpc, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public void refreshNpc(UUID entityId) {
        Entity entity = Bukkit.getEntity(entityId);
        if (!(entity instanceof Villager villager)) {
            return;
        }
        String shopId = shopIdOf(villager);
        if (shopId == null) {
            return;
        }
        configure(villager, storage.get(shopId));
    }

    public void refreshShop(String shopId) {
        for (SpawnedShopNpc npc : storage.getNpcs()) {
            if (npc.getShopId().equalsIgnoreCase(shopId)) {
                refreshNpc(npc.getEntityId());
            }
        }
    }
}
