package com.saltysmp.saltkits.kit;

import com.saltysmp.saltkits.SaltKitsPlugin;
import com.saltysmp.saltkits.model.Kit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PlayerLayoutStore {

    private final SaltKitsPlugin plugin;
    private final File layoutsFolder;

    public PlayerLayoutStore(SaltKitsPlugin plugin) {
        this.plugin = plugin;
        this.layoutsFolder = new File(plugin.getDataFolder(), "layouts");
        if (!layoutsFolder.exists() && !layoutsFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create layouts folder: " + layoutsFolder.getAbsolutePath());
        }
    }

    @SuppressWarnings("unchecked")
    public ItemStack[] load(UUID playerId, String kitName) {
        File file = fileFor(playerId, kitName);
        if (!file.exists()) {
            return null;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> serialized = (List<ItemStack>) config.getList("items");
        if (serialized == null) {
            return null;
        }
        return Kit.migrateItems(serialized.toArray(new ItemStack[0]));
    }

    public void save(UUID playerId, String kitName, ItemStack[] items) {
        File file = fileFor(playerId, kitName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Could not create layout folder: " + parent.getAbsolutePath());
            return;
        }
        FileConfiguration config = new YamlConfiguration();
        ItemStack[] cloned = new ItemStack[Kit.INVENTORY_SIZE];
        for (int i = 0; i < cloned.length && items != null && i < items.length; i++) {
            cloned[i] = items[i] == null ? null : items[i].clone();
        }
        config.set("items", Arrays.asList(cloned));
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save layout for " + playerId + " / " + kitName + ": " + exception.getMessage());
        }
    }

    public void delete(UUID playerId, String kitName) {
        File file = fileFor(playerId, kitName);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete layout: " + file.getName());
        }
    }

    private File fileFor(UUID playerId, String kitName) {
        File playerFolder = new File(layoutsFolder, playerId.toString());
        return new File(playerFolder, kitName.toLowerCase(Locale.ROOT) + ".yml");
    }
}
