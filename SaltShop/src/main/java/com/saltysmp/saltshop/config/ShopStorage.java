package com.saltysmp.saltshop.config;

import com.saltysmp.saltshop.SaltShopPlugin;
import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.model.ShopTrade;
import com.saltysmp.saltshop.model.SpawnedShopNpc;
import com.saltysmp.saltshop.util.GuiItems;
import com.saltysmp.saltshop.util.ItemStackUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ShopStorage {

    private final SaltShopPlugin plugin;
    private final PluginConfig pluginConfig;
    private final File shopsFolder;
    private final File npcsFile;
    private final Map<String, ShopDefinition> shops = new HashMap<>();
    private final List<SpawnedShopNpc> npcs = new ArrayList<>();

    public ShopStorage(SaltShopPlugin plugin, PluginConfig pluginConfig) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.shopsFolder = new File(plugin.getDataFolder(), "shops");
        this.npcsFile = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void load() {
        if (!shopsFolder.exists() && !shopsFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create shops folder.");
        }
        shops.clear();
        File[] files = shopsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
                shops.put(id, loadShopFile(id, file));
            }
        }
        loadNpcs();
    }

    private ShopDefinition loadShopFile(String id, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ShopDefinition definition = new ShopDefinition(id);
        definition.setDisplayName(yaml.getString("display-name", definition.getDisplayName()));
        definition.setProfession(parseProfession(yaml.getString("villager.profession")));
        definition.setVillagerType(parseType(yaml.getString("villager.type")));
        definition.setVillagerLevel(yaml.getInt("villager.level", 5));

        List<ShopTrade> trades = new ArrayList<>();
        List<?> tradeList = yaml.getList("trades");
        if (tradeList != null) {
            for (Object entry : tradeList) {
                if (entry instanceof Map<?, ?> map) {
                    YamlConfiguration tradeYaml = new YamlConfiguration();
                    for (Map.Entry<?, ?> mapEntry : map.entrySet()) {
                        tradeYaml.set(String.valueOf(mapEntry.getKey()), mapEntry.getValue());
                    }
                    ShopTrade trade = sanitizeTrade(loadTrade(tradeYaml, pluginConfig.getDefaultMaxUses()));
                    if (trade != null) {
                        trades.add(trade);
                    }
                }
            }
        }

        if (!trades.isEmpty()) {
            definition.setTrades(trades);
        }
        return definition;
    }

    private ShopTrade loadTrade(ConfigurationSection section, int defaultMaxUses) {
        ItemStack result = ItemStackUtil.loadItem(section.get("result"));
        List<ItemStack> costs = ItemStackUtil.loadCostList(section, "cost");
        int coinCost = section.getInt("coin-cost", 0);
        int maxUses = section.getInt("max-uses", defaultMaxUses);
        return new ShopTrade(result, costs, coinCost, maxUses);
    }

    private ShopTrade sanitizeTrade(ShopTrade trade) {
        if (trade == null || ItemStackUtil.isAir(trade.getResult()) || GuiItems.isGlassPane(trade.getResult())) {
            return null;
        }
        if (trade.getCoinCost() <= 0 && trade.getCosts().isEmpty()) {
            return null;
        }
        List<ItemStack> cleaned = new ArrayList<>();
        for (ItemStack cost : trade.getCosts()) {
            if (ItemStackUtil.isAir(cost) || GuiItems.isGlassPane(cost)) {
                continue;
            }
            cleaned.add(cost);
        }
        return new ShopTrade(trade.getResult(), cleaned, trade.getCoinCost(), trade.getMaxUses());
    }

    private Villager.Profession parseProfession(String value) {
        if (value == null || value.isBlank()) {
            return Villager.Profession.NONE;
        }
        try {
            return Villager.Profession.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Villager.Profession.NONE;
        }
    }

    private Villager.Type parseType(String value) {
        if (value == null || value.isBlank()) {
            return Villager.Type.PLAINS;
        }
        try {
            return Villager.Type.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Villager.Type.PLAINS;
        }
    }

    public void save(ShopDefinition definition) {
        File file = new File(shopsFolder, definition.getId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("display-name", definition.getDisplayName());
        yaml.set("villager.profession", definition.getProfession().name());
        yaml.set("villager.type", definition.getVillagerType().name());
        yaml.set("villager.level", definition.getVillagerLevel());

        List<Map<String, Object>> tradeMaps = new ArrayList<>();
        for (ShopTrade trade : definition.getTrades()) {
            Map<String, Object> tradeMap = new HashMap<>();
            tradeMap.put("result", trade.getResult());
            tradeMap.put("cost", trade.getCosts());
            if (trade.getCoinCost() > 0) {
                tradeMap.put("coin-cost", trade.getCoinCost());
            }
            tradeMap.put("max-uses", trade.getMaxUses());
            tradeMaps.add(tradeMap);
        }
        yaml.set("trades", tradeMaps);

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save shop " + definition.getId(), exception);
        }
        shops.put(definition.getId(), definition);
    }

    private void loadNpcs() {
        npcs.clear();
        if (!npcsFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(npcsFile);
        List<?> list = yaml.getList("npcs");
        if (list == null) {
            return;
        }
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String shopId = String.valueOf(map.get("shop-id"));
            UUID entityId = UUID.fromString(String.valueOf(map.get("entity-id")));
            String world = String.valueOf(map.get("world"));
            double x = toDouble(map.get("x"));
            double y = toDouble(map.get("y"));
            double z = toDouble(map.get("z"));
            float yaw = (float) toDouble(map.get("yaw"));
            float pitch = (float) toDouble(map.get("pitch"));
            npcs.add(new SpawnedShopNpc(shopId, entityId, world, x, y, z, yaw, pitch));
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0D;
    }

    public void saveNpcs() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> entries = new ArrayList<>();
        for (SpawnedShopNpc npc : npcs) {
            Map<String, Object> map = new HashMap<>();
            map.put("shop-id", npc.getShopId());
            map.put("entity-id", npc.getEntityId().toString());
            LocationFields fields = locationFields(npc);
            if (fields == null) {
                continue;
            }
            map.put("world", fields.worldName);
            map.put("x", fields.x);
            map.put("y", fields.y);
            map.put("z", fields.z);
            map.put("yaw", fields.yaw);
            map.put("pitch", fields.pitch);
            entries.add(map);
        }
        yaml.set("npcs", entries);
        try {
            yaml.save(npcsFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save npcs.yml", exception);
        }
    }

    private LocationFields locationFields(SpawnedShopNpc npc) {
        var location = npc.toLocation();
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new LocationFields(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public ShopDefinition get(String id) {
        return shops.get(id.toLowerCase());
    }

    public ShopDefinition getOrCreate(String id) {
        String key = id.toLowerCase();
        ShopDefinition existing = shops.get(key);
        if (existing != null) {
            return existing;
        }
        ShopDefinition created = new ShopDefinition(key);
        shops.put(key, created);
        save(created);
        return created;
    }

    public boolean delete(String id) {
        String key = id.toLowerCase();
        if (!shops.containsKey(key)) {
            return false;
        }
        shops.remove(key);
        File file = new File(shopsFolder, key + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete shop file: " + file.getName());
        }
        npcs.removeIf(npc -> npc.getShopId().equals(key));
        saveNpcs();
        return true;
    }

    public Collection<ShopDefinition> getAll() {
        return Collections.unmodifiableCollection(shops.values());
    }

    public List<String> getIds() {
        return shops.keySet().stream().sorted().toList();
    }

    public List<SpawnedShopNpc> getNpcs() {
        return Collections.unmodifiableList(npcs);
    }

    public void addNpc(SpawnedShopNpc npc) {
        npcs.removeIf(existing -> existing.getEntityId().equals(npc.getEntityId()));
        npcs.add(npc);
        saveNpcs();
    }

    public boolean removeNpc(UUID entityId) {
        boolean removed = npcs.removeIf(npc -> npc.getEntityId().equals(entityId));
        if (removed) {
            saveNpcs();
        }
        return removed;
    }

    public SpawnedShopNpc findNpc(UUID entityId) {
        for (SpawnedShopNpc npc : npcs) {
            if (npc.getEntityId().equals(entityId)) {
                return npc;
            }
        }
        return null;
    }

    private record LocationFields(String worldName, double x, double y, double z, float yaw, float pitch) {
    }
}
