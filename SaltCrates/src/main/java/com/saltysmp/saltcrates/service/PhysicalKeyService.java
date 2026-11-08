package com.saltysmp.saltcrates.service;

import com.saltysmp.saltcrates.config.CrateDefinitionStorage;
import com.saltysmp.saltcrates.model.CrateDefinition;
import com.saltysmp.saltcrates.util.BlockMaterialUtil;
import com.saltysmp.saltcrates.util.MessageUtil;
import com.saltysmp.saltcrates.util.PluginKeys;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PhysicalKeyService {

    private final PluginKeys keys;
    private final CrateDefinitionStorage definitions;
    private final KeyService keyService;
    private final HologramService hologramService;

    public PhysicalKeyService(
            PluginKeys keys,
            CrateDefinitionStorage definitions,
            KeyService keyService,
            HologramService hologramService
    ) {
        this.keys = keys;
        this.definitions = definitions;
        this.keyService = keyService;
        this.hologramService = hologramService;
    }

    public ItemStack createKeyItem(CrateDefinition definition, int amount) {
        ItemStack stack = new ItemStack(
                BlockMaterialUtil.shulkerToCandle(definition.getDisplayBlock()),
                Math.max(1, amount)
        );
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String color = BlockMaterialUtil.miniMessageColor(definition.getDisplayBlock());
            String plainName = PlainTextComponentSerializer.plainText()
                    .serialize(MessageUtil.parse(definition.getDisplayName()));
            if (plainName.isBlank()) {
                plainName = definition.getId();
            }
            String keyLabel = stripCrateSuffix(plainName) + " Key";
            meta.displayName(MessageUtil.parse("<" + color + ">" + keyLabel));
            meta.lore(List.of(
                    MessageUtil.parse("<gray>Physical key for <aqua>" + plainName),
                    MessageUtil.parse("<dark_gray>Converts to a virtual key in your inventory.")
            ));
            meta.getPersistentDataContainer().set(keys.physicalKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(
                    keys.crateId,
                    PersistentDataType.STRING,
                    definition.getId().toLowerCase(Locale.ROOT)
            );
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isPhysicalKey(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(keys.physicalKey, PersistentDataType.BYTE);
    }

    public String getCrateId(ItemStack stack) {
        if (!isPhysicalKey(stack)) {
            return null;
        }
        String crateId = stack.getItemMeta().getPersistentDataContainer().get(keys.crateId, PersistentDataType.STRING);
        return crateId == null ? null : crateId.toLowerCase(Locale.ROOT);
    }

    /**
     * Removes physical keys from the player's inventory and credits virtual keys.
     * @return crateId → amount converted (empty if nothing converted)
     */
    public Map<String, Integer> convertAndCredit(Player player) {
        Map<String, Integer> converted = new HashMap<>();
        PlayerInventory inventory = player.getInventory();

        ItemStack[] storage = inventory.getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            final int index = slot;
            consumeStack(storage[slot], converted, () -> inventory.setItem(index, null));
        }

        ItemStack offhand = inventory.getItemInOffHand();
        consumeStack(offhand, converted, () -> inventory.setItemInOffHand(null));

        ItemStack[] armor = inventory.getArmorContents();
        boolean armorChanged = false;
        for (int i = 0; i < armor.length; i++) {
            final int index = i;
            if (consumeStack(armor[i], converted, () -> armor[index] = null)) {
                armorChanged = true;
            }
        }
        if (armorChanged) {
            inventory.setArmorContents(armor);
        }

        if (converted.isEmpty()) {
            return converted;
        }
        for (Map.Entry<String, Integer> entry : converted.entrySet()) {
            keyService.give(player, entry.getKey(), entry.getValue());
        }
        keyService.flush();
        hologramService.refreshKeysFor(player);
        return converted;
    }

    private boolean consumeStack(ItemStack stack, Map<String, Integer> converted, Runnable clearSlot) {
        String crateId = getCrateId(stack);
        if (crateId == null || definitions.get(crateId) == null) {
            return false;
        }
        converted.merge(crateId, stack.getAmount(), Integer::sum);
        clearSlot.run();
        return true;
    }

    public String formatConvertedSummary(Map<String, Integer> converted) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : converted.entrySet()) {
            CrateDefinition definition = definitions.get(entry.getKey());
            String name = definition != null
                    ? PlainTextComponentSerializer.plainText().serialize(MessageUtil.parse(definition.getDisplayName()))
                    : entry.getKey();
            if (name.isBlank()) {
                name = entry.getKey();
            }
            parts.add(entry.getValue() + " " + stripCrateSuffix(name));
        }
        return String.join(", ", parts);
    }

    private static String stripCrateSuffix(String name) {
        String trimmed = name.trim();
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(" crate")) {
            return trimmed.substring(0, trimmed.length() - " crate".length()).trim();
        }
        return trimmed;
    }
}
