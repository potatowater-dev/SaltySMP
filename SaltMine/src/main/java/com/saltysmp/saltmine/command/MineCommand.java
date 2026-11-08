package com.saltysmp.saltmine.command;

import com.saltysmp.saltmine.SaltMinePlugin;
import com.saltysmp.saltmine.config.MessagesConfig;
import com.saltysmp.saltmine.item.MineWandFactory;
import com.saltysmp.saltmine.model.MineDefinition;
import com.saltysmp.saltmine.service.MineRegenService;
import com.saltysmp.saltmine.service.WandSelectionService;
import com.saltysmp.saltmine.storage.MineStorage;
import com.saltysmp.saltmine.util.MessageUtil;
import com.saltysmp.saltmine.util.RegionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class MineCommand implements CommandExecutor, TabCompleter {

    private static final Pattern VALID_ID = Pattern.compile("^[a-z0-9][a-z0-9_-]*$");

    private final SaltMinePlugin plugin;
    private final MessagesConfig messages;
    private final MineStorage mineStorage;
    private final MineWandFactory wandFactory;
    private final WandSelectionService selectionService;
    private final MineRegenService regenService;

    public MineCommand(
            SaltMinePlugin plugin,
            MessagesConfig messages,
            MineStorage mineStorage,
            MineWandFactory wandFactory,
            WandSelectionService selectionService,
            MineRegenService regenService
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.mineStorage = mineStorage;
        this.wandFactory = wandFactory;
        this.selectionService = selectionService;
        this.regenService = regenService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            if (sender.hasPermission("saltmine.admin")) {
                return handleHelp(sender);
            }
            messages.send(sender, "usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "help" -> handleHelp(sender);
            case "wand" -> handleWand(sender);
            case "create" -> handleCreate(sender, args);
            case "setregion" -> handleSetRegion(sender, args);
            case "addblock" -> handleAddBlock(sender, args);
            case "list" -> handleList(sender);
            case "delete" -> handleDelete(sender, args);
            case "info" -> handleInfo(sender, args);
            case "regen" -> handleRegen(sender, args);
            case "settime" -> handleSetTime(sender, args);
            case "broadcast" -> handleBroadcast(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                messages.send(sender, "usage");
                yield true;
            }
        };
    }

    private boolean handleHelp(CommandSender sender) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "usage");
            return true;
        }
        messages.send(sender, "help-header");
        for (int line = 1; line <= 9; line++) {
            messages.send(sender, "help-line-" + line);
        }
        return true;
    }

    private boolean handleWand(CommandSender sender) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        player.getInventory().addItem(wandFactory.createWand());
        messages.send(sender, "wand-given");
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        String id = normalizeId(args[1]);
        if (!isValidId(id)) {
            messages.send(sender, "invalid-id");
            return true;
        }
        if (mineStorage.exists(id)) {
            messages.send(sender, "mine-exists", "id", id);
            return true;
        }
        mineStorage.getOrCreate(id);
        messages.send(sender, "mine-created", "id", id);
        return true;
    }

    private boolean handleSetRegion(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        String id = normalizeId(args[1]);
        MineDefinition mine = mineStorage.get(id);
        if (mine == null) {
            messages.send(sender, "mine-not-found", "id", id);
            return true;
        }
        Location pos1 = selectionService.getPos1(player.getUniqueId());
        Location pos2 = selectionService.getPos2(player.getUniqueId());
        if (pos1 == null || pos2 == null) {
            messages.send(sender, "pos-missing");
            return true;
        }
        if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
            messages.send(sender, "pos-world-mismatch");
            return true;
        }
        try {
            RegionUtil.Cuboid region = RegionUtil.Cuboid.fromLocations(pos1, pos2);
            mine.setRegion(region);
            mineStorage.save(mine);
            messages.send(sender, "region-set", "id", id, "volume", String.valueOf(region.volume()));
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "pos-world-mismatch");
        }
        return true;
    }

    private boolean handleAddBlock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 4) {
            messages.send(sender, "usage");
            return true;
        }
        String id = normalizeId(args[1]);
        MineDefinition mine = mineStorage.get(id);
        if (mine == null) {
            messages.send(sender, "mine-not-found", "id", id);
            return true;
        }
        Material material = Material.matchMaterial(args[2]);
        if (material == null || !material.isBlock()) {
            messages.send(sender, "invalid-material", "material", args[2]);
            return true;
        }
        int weight;
        try {
            weight = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            messages.send(sender, "invalid-weight");
            return true;
        }
        if (weight <= 0) {
            messages.send(sender, "invalid-weight");
            return true;
        }
        mine.setBlockWeight(material, weight);
        mineStorage.save(mine);
        messages.send(sender, "block-added", "id", id, "material", material.name(), "weight", String.valueOf(weight));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        List<String> ids = mineStorage.getIds();
        if (ids.isEmpty()) {
            messages.send(sender, "mine-list-empty");
            return true;
        }
        messages.send(sender, "mine-list-header", "count", String.valueOf(ids.size()));
        for (String id : ids) {
            MineDefinition mine = mineStorage.get(id);
            if (mine == null) {
                continue;
            }
            MessageUtil.send(sender, messages.format(
                    "mine-list-entry",
                    "id", id,
                    "blocks", String.valueOf(mine.getBlockWeights().size()),
                    "volume", String.valueOf(mine.regionVolume())
            ));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        String id = normalizeId(args[1]);
        if (!mineStorage.delete(id)) {
            messages.send(sender, "mine-not-found", "id", id);
            return true;
        }
        messages.send(sender, "mine-deleted", "id", id);
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        String id = normalizeId(args[1]);
        MineDefinition mine = mineStorage.get(id);
        if (mine == null) {
            messages.send(sender, "mine-not-found", "id", id);
            return true;
        }
        messages.send(sender, "mine-info-header", "id", id);
        RegionUtil.Cuboid region = mine.getRegion();
        if (region == null) {
            messages.send(sender, "mine-info-no-region");
        } else {
            MessageUtil.send(sender, messages.format("mine-info-world", "world", region.worldName()));
            MessageUtil.send(sender, messages.format(
                    "mine-info-region",
                    "min", region.minLabel(),
                    "max", region.maxLabel()
            ));
            MessageUtil.send(sender, messages.format("mine-info-volume", "volume", String.valueOf(region.volume())));
        }
        MessageUtil.send(sender, messages.format("mine-info-blocks"));
        if (mine.getBlockWeights().isEmpty()) {
            MessageUtil.send(sender, messages.format("mine-info-blocks-empty"));
        }
        for (Map.Entry<Material, Integer> entry : mine.getBlockWeights().entrySet()) {
            MessageUtil.send(sender, messages.format(
                    "mine-info-block-entry",
                    "material", entry.getKey().name(),
                    "weight", String.valueOf(entry.getValue())
            ));
        }
        int globalMinutes = plugin.getConfig().getInt("reset-interval-minutes", 15);
        MessageUtil.send(sender, messages.format(
                "mine-info-reset",
                "minutes", String.valueOf(mine.resolveResetIntervalMinutes(globalMinutes)),
                "announce", mine.isAnnounceReset() ? "yes" : "no"
        ));
        if (mine.getResetBroadcast() != null && !mine.getResetBroadcast().isBlank()) {
            MessageUtil.send(sender, messages.format(
                    "mine-info-broadcast",
                    "message", mine.getResetBroadcast()
            ));
        }
        return true;
    }

    private boolean handleSetTime(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "settime-usage");
            return true;
        }

        if (args.length == 2) {
            int minutes;
            try {
                minutes = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                messages.send(sender, "settime-invalid");
                return true;
            }
            if (minutes < 1) {
                messages.send(sender, "settime-invalid");
                return true;
            }
            plugin.getConfig().set("reset-interval-minutes", minutes);
            plugin.saveConfig();
            for (MineDefinition mine : mineStorage.getAll()) {
                mine.setResetIntervalMinutes(0);
                mineStorage.save(mine);
            }
            plugin.reloadAll();
            messages.send(sender, "settime-all", "minutes", String.valueOf(minutes));
            return true;
        }

        String id = normalizeId(args[1]);
        MineDefinition mine = mineStorage.get(id);
        if (mine == null) {
            messages.send(sender, "mine-not-found", "id", id);
            return true;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            messages.send(sender, "settime-invalid");
            return true;
        }
        if (minutes < 1) {
            messages.send(sender, "settime-invalid");
            return true;
        }
        mine.setResetIntervalMinutes(minutes);
        mineStorage.save(mine);
        messages.send(sender, "settime-mine", "id", id, "minutes", String.valueOf(minutes));
        return true;
    }

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "broadcast-usage");
            return true;
        }
        String id = normalizeId(args[1]);
        MineDefinition mine = mineStorage.get(id);
        if (mine == null) {
            messages.send(sender, "mine-not-found", "id", id);
            return true;
        }

        String mode = args[2].toLowerCase(Locale.ROOT);
        if (mode.equals("off") || mode.equals("none") || mode.equals("false")) {
            mine.setAnnounceReset(false);
            mine.setResetBroadcast(null);
            mineStorage.save(mine);
            messages.send(sender, "broadcast-off", "id", id);
            return true;
        }
        if (mode.equals("default") || mode.equals("on") || mode.equals("true")) {
            mine.setAnnounceReset(true);
            mine.setResetBroadcast(null);
            mineStorage.save(mine);
            messages.send(sender, "broadcast-default", "id", id);
            return true;
        }

        StringBuilder message = new StringBuilder(args[2]);
        for (int index = 3; index < args.length; index++) {
            message.append(' ').append(args[index]);
        }
        mine.setAnnounceReset(true);
        mine.setResetBroadcast(message.toString());
        mineStorage.save(mine);
        messages.send(sender, "broadcast-set", "id", id, "message", message.toString());
        return true;
    }

    private boolean handleRegen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        String id = normalizeId(args[1]);
        MineDefinition mine = mineStorage.get(id);
        if (mine == null) {
            messages.send(sender, "mine-not-found", "id", id);
            return true;
        }
        if (!mine.hasRegion()) {
            messages.send(sender, "regen-no-region", "id", id);
            return true;
        }
        if (!mine.hasBlocks()) {
            messages.send(sender, "regen-no-blocks", "id", id);
            return true;
        }
        regenService.regenerate(id);
        messages.send(sender, "regen-started", "id", id);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("saltmine.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }
        plugin.reloadAll();
        messages.send(sender, "reload-success");
        return true;
    }

    private static String normalizeId(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    private static boolean isValidId(String id) {
        return VALID_ID.matcher(id).matches();
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("saltmine.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filterPrefix(args[0], "help", "wand", "create", "setregion", "addblock", "list", "delete", "info", "regen", "settime", "broadcast", "reload");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("setregion") || sub.equals("addblock") || sub.equals("delete")
                    || sub.equals("info") || sub.equals("regen") || sub.equals("broadcast")) {
                return filterPrefix(args[1], mineStorage.getIds());
            }
            if (sub.equals("settime")) {
                List<String> options = new ArrayList<>(mineStorage.getIds());
                options.addAll(List.of("5", "10", "15", "30", "60"));
                return filterPrefix(args[1], options);
            }
            if (sub.equals("create")) {
                return List.of("<id>");
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("broadcast")) {
            return filterPrefix(args[2], List.of("off", "default", "<gold>Mine {id} is resetting!"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("settime")) {
            return filterPrefix(args[2], List.of("5", "10", "15", "30", "60"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("addblock")) {
            return filterPrefix(args[2], Arrays.stream(Material.values())
                    .filter(Material::isBlock)
                    .map(Enum::name)
                    .toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("addblock")) {
            return filterPrefix(args[3], List.of("1", "5", "10", "25", "50", "100"));
        }
        return List.of();
    }

    private static List<String> filterPrefix(String input, String... options) {
        return filterPrefix(input, Arrays.asList(options));
    }

    private static List<String> filterPrefix(String input, List<String> options) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                results.add(option);
            }
        }
        return results;
    }
}
