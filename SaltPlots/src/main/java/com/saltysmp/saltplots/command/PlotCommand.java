package com.saltysmp.saltplots.command;

import com.saltysmp.saltplots.SaltPlotsPlugin;
import com.saltysmp.saltplots.config.MessagesConfig;
import com.saltysmp.saltplots.config.PlotStorage;
import com.saltysmp.saltplots.config.PluginConfig;
import com.saltysmp.saltplots.service.PlotService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class PlotCommand implements CommandExecutor, TabCompleter {

    private static final List<String> USER_SUBCOMMANDS = List.of(
            "help", "home", "hub", "info", "create", "visit", "private", "public", "add", "remove", "upgrade"
    );
    private static final List<String> ADMIN_SUBCOMMANDS = List.of(
            "sethub", "setprice", "reload"
    );

    private final SaltPlotsPlugin plugin;
    private final PlotService plotService;
    private final PlotStorage plotStorage;
    private final PluginConfig pluginConfig;
    private final MessagesConfig messages;

    public PlotCommand(
            SaltPlotsPlugin plugin,
            PlotService plotService,
            PlotStorage plotStorage,
            PluginConfig pluginConfig,
            MessagesConfig messages
    ) {
        this.plugin = plugin;
        this.plotService = plotService;
        this.plotStorage = plotStorage;
        this.pluginConfig = pluginConfig;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            return handleHelp(sender);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "help" -> handleHelp(sender);
            case "home" -> handleTeleport(sender);
            case "hub" -> handleHub(sender);
            case "info" -> handleInfo(sender);
            case "create" -> handleCreate(sender);
            case "visit" -> handleVisit(sender, args);
            case "private" -> handlePrivacy(sender, true);
            case "public" -> handlePrivacy(sender, false);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "upgrade" -> handleUpgrade(sender);
            case "sethub" -> handleSetHub(sender, args);
            case "setprice" -> handleSetPrice(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                messages.send(sender, "usage");
                yield true;
            }
        };
    }

    private boolean handleHelp(CommandSender sender) {
        messages.send(sender, "help-header");
        for (int line = 1; line <= 9; line++) {
            messages.send(sender, "help-line-" + line);
        }
        if (sender.hasPermission("saltplots.admin")) {
            messages.send(sender, "admin-usage");
        }
        return true;
    }

    private boolean handleHub(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        return plotService.teleportToHub(player);
    }

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        plotService.sendPlotInfo(player);
        return true;
    }

    private boolean handleTeleport(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (!plotStorage.getHubRegion().isConfigured()) {
            messages.send(sender, "hub-not-set");
            return true;
        }
        if (!plotService.isInHub(player)) {
            messages.send(sender, "not-in-hub");
            return true;
        }
        return plotService.teleportToPlot(player);
    }

    private boolean handleCreate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (!plotStorage.getHubRegion().isConfigured()) {
            messages.send(sender, "hub-not-set");
            return true;
        }
        if (!plotService.isInHub(player)) {
            messages.send(sender, "not-in-hub");
            return true;
        }
        return plotService.createPlot(player);
    }

    private boolean handleVisit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "visit-usage");
            return true;
        }
        return plotService.visitPlot(player, args[1]);
    }

    private boolean handlePrivacy(CommandSender sender, boolean isPrivate) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        plotService.setPrivacy(player, isPrivate);
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        plotService.addTrusted(player, args[1]);
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        plotService.removeTrusted(player, args[1]);
        return true;
    }

    private boolean handleUpgrade(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltplots.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        return plotService.upgradePlot(player);
    }

    private boolean handleSetHub(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltplots.admin")) {
            messages.send(sender, "no-permission-admin");
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            return plotService.confirmHub(player);
        }
        messages.send(sender, "sethub-info");
        return true;
    }

    private boolean handleSetPrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltplots.admin")) {
            messages.send(sender, "no-permission-admin");
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "admin-usage");
            return true;
        }
        int tier;
        int cost;
        try {
            tier = Integer.parseInt(args[1]);
            cost = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            messages.send(sender, "setprice-invalid-tier", "max", String.valueOf(pluginConfig.getTierCount() - 1));
            return true;
        }
        if (!plotService.setUpgradePrice(tier, cost)) {
            messages.send(sender, "setprice-invalid-tier", "max", String.valueOf(pluginConfig.getTierCount() - 1));
            return true;
        }
        messages.send(sender, "setprice-success", "tier", String.valueOf(tier), "cost", String.valueOf(cost));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("saltplots.admin")) {
            messages.send(sender, "no-permission-admin");
            return true;
        }
        plugin.reloadAll();
        messages.send(sender, "reload-success");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(USER_SUBCOMMANDS);
            if (sender.hasPermission("saltplots.admin")) {
                options.addAll(ADMIN_SUBCOMMANDS);
            }
            return filter(options, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("add") || sub.equals("remove") || sub.equals("visit")) {
                return filter(
                        Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
                        args[1]
                );
            }
            if (sub.equals("sethub") && sender.hasPermission("saltplots.admin")) {
                return filter(List.of("confirm"), args[1]);
            }
            if (sub.equals("setprice") && sender.hasPermission("saltplots.admin")) {
                List<String> tiers = new ArrayList<>();
                for (int index = 1; index < pluginConfig.getTierCount(); index++) {
                    tiers.add(String.valueOf(index));
                }
                return filter(tiers, args[1]);
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
