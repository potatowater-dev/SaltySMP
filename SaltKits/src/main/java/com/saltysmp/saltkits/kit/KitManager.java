package com.saltysmp.saltkits.kit;

import com.saltysmp.saltkits.SaltKitsPlugin;
import com.saltysmp.saltkits.config.MessagesConfig;
import com.saltysmp.saltkits.gui.KitClaimMenu;
import com.saltysmp.saltkits.gui.KitEditorGui;
import com.saltysmp.saltkits.gui.KitPreviewGui;
import com.saltysmp.saltkits.model.Kit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.Comparator;
import java.util.stream.Collectors;

public final class KitManager {

    private static final Pattern KIT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final SaltKitsPlugin plugin;
    private final CooldownManager cooldownManager;
    private final KitGiveService giveService;
    private final KitSessionRegistry sessionRegistry;
    private final MessagesConfig messages;
    private final File kitsFolder;
    private final PlayerLayoutStore layoutStore;
    private final Map<String, Kit> kits = new HashMap<>();

    public KitManager(
            SaltKitsPlugin plugin,
            CooldownManager cooldownManager,
            KitGiveService giveService,
            KitSessionRegistry sessionRegistry,
            MessagesConfig messages
    ) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.giveService = giveService;
        this.sessionRegistry = sessionRegistry;
        this.messages = messages;
        this.kitsFolder = new File(plugin.getDataFolder(), "kits");
        this.layoutStore = new PlayerLayoutStore(plugin);
        if (!kitsFolder.exists() && !kitsFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create kits folder: " + kitsFolder.getAbsolutePath());
        }
        loadAll();
    }

    public void loadAll() {
        kits.clear();
        File[] files = kitsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4);
            loadKitFromFile(name, file).ifPresent(kit -> kits.put(kit.name().toLowerCase(Locale.ROOT), kit));
        }
    }

    public List<String> getKitNames() {
        List<String> names = new ArrayList<>();
        for (Kit kit : kits.values()) {
            names.add(kit.name());
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<Kit> getVisibleKits(Player player) {
        return kits.values().stream()
                .filter(kit -> kit.starter() || player.hasPermission(kit.permission()) || player.hasPermission("saltkits.admin"))
                .sorted(Comparator.comparingInt(Kit::menuOrder)
                        .thenComparing(kit -> kit.resolvedDisplayName(), String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public Optional<Kit> getKit(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(kits.get(name.toLowerCase(Locale.ROOT)));
    }

    public boolean kitExists(String name) {
        return getKit(name).isPresent();
    }

    public boolean isValidKitName(String name) {
        return name != null && KIT_NAME_PATTERN.matcher(name).matches();
    }

    public boolean openEditor(Player player, String kitName, boolean isNew) {
        if (sessionRegistry.hasSession(player.getUniqueId())) {
            messages.send(player, "editor-already-open");
            return false;
        }
        if (!isValidKitName(kitName)) {
            messages.send(player, "editor-invalid-name");
            return false;
        }
        if (isNew && kitExists(kitName)) {
            messages.send(player, "editor-kit-exists", "kit", kitName);
            return false;
        }

        Kit kit;
        if (isNew) {
            kit = new Kit(
                    kitName,
                    new ItemStack[Kit.INVENTORY_SIZE],
                    false,
                    plugin.getDefaultPaidCooldownSeconds(),
                    plugin.getPermissionPrefix() + kitName.toLowerCase(Locale.ROOT),
                    kitName,
                    Material.CHEST,
                    List.of(),
                    "",
                    false,
                    Kit.DEFAULT_MENU_ORDER
            );
        } else {
            Optional<Kit> existing = getKit(kitName);
            if (existing.isEmpty()) {
                messages.send(player, "editor-kit-missing", "kit", kitName);
                return false;
            }
            kit = existing.get();
        }

        KitEditorGui gui = new KitEditorGui(kit);
        sessionRegistry.setEditor(player, gui);
        player.openInventory(gui.getInventory());
        player.updateInventory();
        messages.send(player, isNew ? "editor-opened-create" : "editor-opened-edit", "kit", kit.resolvedDisplayName());
        return true;
    }

    public void openClaimMenu(Player player, int page) {
        if (sessionRegistry.hasSession(player.getUniqueId())) {
            messages.send(player, "editor-already-open");
            return;
        }
        List<Kit> visible = getVisibleKits(player);
        if (visible.isEmpty()) {
            messages.send(player, "list-empty");
            return;
        }
        KitClaimMenu menu = new KitClaimMenu(player, visible, cooldownManager, page, plugin.isCustomizeClaimGui());
        sessionRegistry.setMenu(player, menu);
        player.openInventory(menu.getInventory());
        messages.send(player, "menu-opened");
        playSound(player, plugin.getMenuClickSound(), plugin.getMenuClickVolume(), plugin.getMenuClickPitch());
    }

    public void openPreview(Player player, Kit kit) {
        if (plugin.isCustomizeClaimGui()) {
            ClaimResult result = openCustomize(player, kit, false);
            if (!result.success()) {
                messages.send(player, "menu-claim-failed", "reason", result.message());
            }
            return;
        }
        KitPreviewGui preview = new KitPreviewGui(kit, false);
        sessionRegistry.setPreview(player, preview);
        player.openInventory(preview.getInventory());
        playSound(player, plugin.getMenuClickSound(), plugin.getMenuClickVolume(), plugin.getMenuClickPitch());
    }

    public ClaimResult openCustomize(Player player, Kit kit, boolean bypassChecks) {
        if (!bypassChecks && !kit.starter() && !player.hasPermission(kit.permission())
                && !player.hasPermission("saltkits.admin")) {
            playSound(player, plugin.getClaimDeniedSound(), plugin.getClaimDeniedVolume(), plugin.getClaimDeniedPitch());
            return ClaimResult.failure(messages.format("claim-no-access", "kit", kit.resolvedDisplayName()));
        }
        ItemStack[] layout = itemsFor(player, kit);
        KitPreviewGui gui = new KitPreviewGui(kit, true, layout);
        sessionRegistry.setPreview(player, gui);
        player.openInventory(gui.getInventory());
        player.updateInventory();
        messages.send(player, "customize-opened", "kit", kit.resolvedDisplayName());
        playSound(player, plugin.getMenuClickSound(), plugin.getMenuClickVolume(), plugin.getMenuClickPitch());
        return ClaimResult.success("");
    }

    public void finishCustomize(Player player, KitPreviewGui gui) {
        if (gui.isSettled()) {
            sessionRegistry.clear(player.getUniqueId());
            return;
        }
        gui.markSettled();
        gui.restoreCursor(player);
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            player.setItemOnCursor(null);
        }
        gui.captureTemplate();
        layoutStore.save(player.getUniqueId(), gui.getKit().name(), gui.getTemplate());
        sessionRegistry.clear(player.getUniqueId());
        messages.send(player, "layout-saved", "kit", gui.getKit().resolvedDisplayName());
    }

    public void saveEditor(Player player, KitEditorGui editor) {
        Kit updated = editor.getKit().withItems(editor.collectItems());
        saveKit(updated);
        editor.setKit(updated);
        messages.send(player, "editor-saved", "kit", updated.resolvedDisplayName());
    }

    public void finishEditorSession(UUID playerId, boolean save) {
        sessionRegistry.getSession(playerId).ifPresent(session -> {
            if (session.type() == KitSessionRegistry.SessionType.EDITOR && session.gui() instanceof KitEditorGui editor) {
                if (save && !session.discardOnClose()) {
                    Kit updated = editor.getKit().withItems(editor.collectItems());
                    saveKit(updated);
                }
            }
            sessionRegistry.clear(playerId);
        });
    }

    public ClaimResult claimKit(Player player, String kitName, boolean bypassChecks) {
        Optional<Kit> kitOptional = getKit(kitName);
        if (kitOptional.isEmpty()) {
            return ClaimResult.failure(messages.format("claim-kit-missing", "kit", kitName));
        }

        Kit kit = kitOptional.get();
        ClaimResult allowed = checkClaim(player, kit, bypassChecks);
        if (!allowed.success()) {
            return allowed;
        }

        KitGiveService.GiveResult giveResult = giveService.give(player, itemsFor(player, kit));
        consumeClaim(player, kit, bypassChecks);
        announceGive(player, giveResult);
        return ClaimResult.success(messages.format("claim-success", "kit", kit.resolvedDisplayName()));
    }

    private ClaimResult checkClaim(Player player, Kit kit, boolean bypassChecks) {
        if (giveService.wouldOverflow(player, itemsFor(player, kit))) {
            playSound(player, plugin.getClaimDeniedSound(), plugin.getClaimDeniedVolume(), plugin.getClaimDeniedPitch());
            return ClaimResult.failure(messages.format("inventory-full", "kit", kit.resolvedDisplayName()));
        }
        if (bypassChecks) {
            return ClaimResult.success("");
        }
        if (!kit.starter() && !player.hasPermission(kit.permission())) {
            playSound(player, plugin.getClaimDeniedSound(), plugin.getClaimDeniedVolume(), plugin.getClaimDeniedPitch());
            return ClaimResult.failure(messages.format("claim-no-access", "kit", kit.resolvedDisplayName()));
        }
        if (!kit.starter() && cooldownManager.isOnCooldown(player.getUniqueId(), kit.name(), kit.cooldownSeconds())) {
            long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId(), kit.name(), kit.cooldownSeconds());
            playSound(player, plugin.getClaimDeniedSound(), plugin.getClaimDeniedVolume(), plugin.getClaimDeniedPitch());
            return ClaimResult.failure(messages.format(
                    "claim-cooldown",
                    "kit", kit.resolvedDisplayName(),
                    "time", cooldownManager.formatRemaining(remaining)
            ));
        }
        return ClaimResult.success("");
    }

    private void consumeClaim(Player player, Kit kit, boolean bypassChecks) {
        if (!bypassChecks && !kit.starter() && kit.cooldownSeconds() > 0L) {
            cooldownManager.recordClaim(player.getUniqueId(), kit.name());
        }
        playSound(player, plugin.getClaimSuccessSound(), plugin.getClaimSuccessVolume(), plugin.getClaimSuccessPitch());
        if (plugin.isClaimTitleEnabled()) {
            messages.actionBar(player, "claim-success", "kit", kit.resolvedDisplayName());
            com.saltysmp.saltkits.util.MessageUtil.title(
                    player,
                    messages.get("title-claim"),
                    messages.format("subtitle-claim", "kit", kit.resolvedDisplayName())
            );
        }
        messages.send(player, "claim-success", "kit", kit.resolvedDisplayName());
    }

    private void announceGive(Player player, KitGiveService.GiveResult giveResult) {
        if (giveResult.itemCount() > 0) {
            String itemSummary = giveResult.itemNames().isEmpty()
                    ? ""
                    : " — " + String.join(", ", giveResult.itemNames());
            messages.send(player, "claim-success-detail", "count", String.valueOf(giveResult.itemCount()), "items", itemSummary);
        }
        if (!giveResult.equippedArmor().isEmpty()) {
            messages.send(player, "claim-equipped", "items", String.join(", ", giveResult.equippedArmor()));
        }
    }

    public void tryAutoClaimStarter(Player player) {
        for (Kit kit : kits.values()) {
            if (!kit.starter() || !kit.autoClaimOnJoin()) {
                continue;
            }
            if (cooldownManager.hasAutoClaimed(player.getUniqueId(), kit.name())) {
                continue;
            }
            if (giveService.wouldOverflow(player, kit.items())) {
                continue;
            }
            giveService.give(player, kit.items());
            cooldownManager.markAutoClaimed(player.getUniqueId(), kit.name());
            messages.send(player, "join-auto-claim", "kit", kit.resolvedDisplayName());
        }
    }

    public boolean deleteKit(String kitName) {
        Optional<Kit> kit = getKit(kitName);
        if (kit.isEmpty()) {
            return false;
        }
        kits.remove(kit.get().name().toLowerCase(Locale.ROOT));
        File file = getKitFile(kit.get().name());
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete kit file: " + file.getName());
        }
        return true;
    }

    public boolean cloneKit(String from, String to) {
        Optional<Kit> source = getKit(from);
        if (source.isEmpty() || !isValidKitName(to) || kitExists(to)) {
            return false;
        }
        Kit cloned = new Kit(
                to,
                cloneItems(source.get().items()),
                source.get().starter(),
                source.get().cooldownSeconds(),
                plugin.getPermissionPrefix() + to.toLowerCase(Locale.ROOT),
                source.get().displayName(),
                source.get().iconMaterial(),
                source.get().description(),
                source.get().category(),
                source.get().autoClaimOnJoin(),
                source.get().menuOrder()
        );
        saveKit(cloned);
        return true;
    }

    public void saveKit(Kit kit) {
        kits.put(kit.name().toLowerCase(Locale.ROOT), kit);
        File file = getKitFile(kit.name());
        FileConfiguration config = new YamlConfiguration();
        config.set("starter", kit.starter());
        config.set("cooldown-seconds", kit.cooldownSeconds());
        config.set("permission", kit.permission());
        config.set("display-name", kit.displayName());
        config.set("icon-material", kit.iconMaterial().name());
        config.set("description", kit.description());
        config.set("category", kit.category());
        config.set("auto-claim-on-join", kit.autoClaimOnJoin());
        config.set("menu-order", kit.menuOrder());
        config.set("items", Arrays.asList(cloneItems(kit.items())));
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save kit \"" + kit.name() + "\": " + exception.getMessage());
        }
    }

    public boolean setStarter(String kitName, boolean starter) {
        return updateKit(kitName, kit -> kit.withStarter(starter));
    }

    public boolean setCooldown(String kitName, long cooldownSeconds) {
        return updateKit(kitName, kit -> kit.withCooldownSeconds(cooldownSeconds));
    }

    public boolean setPermission(String kitName, String permission) {
        return updateKit(kitName, kit -> kit.withPermission(permission));
    }

    public boolean updateKitMetadata(String kitName, java.util.function.UnaryOperator<Kit> updater) {
        return updateKit(kitName, updater);
    }

    private boolean updateKit(String kitName, java.util.function.UnaryOperator<Kit> updater) {
        Optional<Kit> kitOptional = getKit(kitName);
        if (kitOptional.isEmpty()) {
            return false;
        }
        saveKit(updater.apply(kitOptional.get()));
        return true;
    }

    public void resetPlayerLayout(Player player, KitPreviewGui gui) {
        layoutStore.delete(player.getUniqueId(), gui.getKit().name());
        gui.resetTemplateToDefault();
        messages.send(player, "layout-reset", "kit", gui.getKit().resolvedDisplayName());
    }

    public ItemStack[] itemsFor(Player player, Kit kit) {
        ItemStack[] saved = layoutStore.load(player.getUniqueId(), kit.name());
        return saved != null ? saved : kit.items();
    }

    public SaltKitsPlugin getPlugin() {
        return plugin;
    }

    private Optional<Kit> loadKitFromFile(String name, File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean starter = config.getBoolean("starter", false);
        long cooldownSeconds = config.getLong("cooldown-seconds", plugin.getDefaultPaidCooldownSeconds());
        String permission = config.getString("permission", plugin.getPermissionPrefix() + name.toLowerCase(Locale.ROOT));
        String displayName = config.getString("display-name", name);
        Material iconMaterial = Material.matchMaterial(config.getString("icon-material", "CHEST"));
        if (iconMaterial == null) {
            iconMaterial = Material.CHEST;
        }
        List<String> description = config.getStringList("description");
        String category = config.getString("category", "");
        boolean autoClaimOnJoin = config.getBoolean("auto-claim-on-join", false);
        int menuOrder = config.getInt("menu-order", Kit.DEFAULT_MENU_ORDER);
        ItemStack[] items = readItems(config);
        return Optional.of(new Kit(
                name, items, starter, cooldownSeconds, permission, displayName,
                iconMaterial, description, category, autoClaimOnJoin, menuOrder
        ));
    }

    @SuppressWarnings("unchecked")
    private ItemStack[] readItems(FileConfiguration config) {
        List<ItemStack> serialized = (List<ItemStack>) config.getList("items");
        if (serialized == null) {
            return new ItemStack[Kit.INVENTORY_SIZE];
        }
        ItemStack[] raw = serialized.toArray(new ItemStack[0]);
        return Kit.migrateItems(raw);
    }

    private File getKitFile(String name) {
        return new File(kitsFolder, name.toLowerCase(Locale.ROOT) + ".yml");
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] cloned = new ItemStack[Kit.INVENTORY_SIZE];
        for (int index = 0; index < items.length && index < cloned.length; index++) {
            ItemStack item = items[index];
            cloned[index] = item == null ? null : item.clone();
        }
        return cloned;
    }

    private void playSound(Player player, Sound sound, float volume, float pitch) {
        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public record ClaimResult(boolean success, String message) {
        public static ClaimResult success(String message) {
            return new ClaimResult(true, message);
        }

        public static ClaimResult failure(String message) {
            return new ClaimResult(false, message);
        }
    }
}
