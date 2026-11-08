package com.saltysmp.saltkits.command;

import com.saltysmp.saltkits.cleanup.BlockCleanupService;
import com.saltysmp.saltkits.SaltKitsPlugin;
import com.saltysmp.saltkits.config.MessagesConfig;
import com.saltysmp.saltkits.kit.CooldownManager;
import com.saltysmp.saltkits.kit.KitManager;
import com.saltysmp.saltkits.model.Kit;
import com.saltysmp.saltkits.util.DurationParser;
import com.saltysmp.saltkits.util.MessageUtil;
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

public final class KitCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "menu",
            "create",
            "edit",
            "claim",
            "preview",
            "list",
            "delete",
            "clone",
            "give",
            "reload",
            "resetcooldown",
            "setstarter",
            "setcooldown",
            "setpermission",
            "cleanup"
    );

    private final SaltKitsPlugin plugin;
    private final KitManager kitManager;
    private final CooldownManager cooldownManager;
    private final MessagesConfig messages;
    private final BlockCleanupService cleanupService;

    public KitCommand(
            SaltKitsPlugin plugin,
            KitManager kitManager,
            CooldownManager cooldownManager,
            MessagesConfig messages,
            BlockCleanupService cleanupService
    ) {
        this.plugin = plugin;
        this.kitManager = kitManager;
        this.cooldownManager = cooldownManager;
        this.messages = messages;
        this.cleanupService = cleanupService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            return handleMenu(sender);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "menu" -> handleMenu(sender);
            case "create" -> handleCreate(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "claim" -> handleClaim(sender, args);
            case "preview" -> handlePreview(sender, args);
            case "list" -> handleList(sender);
            case "delete" -> handleDelete(sender, args);
            case "clone" -> handleClone(sender, args);
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "resetcooldown" -> handleResetCooldown(sender, args);
            case "setstarter" -> handleSetStarter(sender, args);
            case "setcooldown" -> handleSetCooldown(sender, args);
            case "setpermission" -> handleSetPermission(sender, args);
            case "cleanup" -> handleCleanup(sender, args);
            default -> {
                messages.send(sender, "usage");
                yield true;
            }
        };
    }

    private boolean handleMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltkits.claim")) {
            messages.send(sender, "no-permission-claim");
            return true;
        }
        kitManager.openClaimMenu(player, 0);
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) {
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        kitManager.openEditor((Player) sender, args[1], true);
        return true;
    }

    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!requireAdmin(sender) || !requirePlayer(sender)) {
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        kitManager.openEditor((Player) sender, args[1], false);
        return true;
    }

    private boolean handleClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!sender.hasPermission("saltkits.claim")) {
            messages.send(sender, "no-permission-claim");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        KitManager.ClaimResult result = kitManager.claimKit(player, args[1], false);
        if (!result.success()) {
            messages.send(player, "menu-claim-failed", "reason", result.message());
        }
        return true;
    }

    private boolean handlePreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        kitManager.getKit(args[1]).ifPresentOrElse(
                kit -> kitManager.openPreview(player, kit),
                () -> messages.send(sender, "claim-kit-missing", "kit", args[1])
        );
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        List<String> kitNames = kitManager.getKitNames();
        if (kitNames.isEmpty()) {
            messages.send(sender, "list-empty");
            return true;
        }
        messages.send(sender, "list-header", "count", String.valueOf(kitNames.size()));
        for (String kitName : kitNames) {
            kitManager.getKit(kitName).ifPresent(kit -> messages.send(sender, "list-entry",
                    "kit", kit.resolvedDisplayName(),
                    "starter", String.valueOf(kit.starter()),
                    "cooldown", kit.starter() ? "none" : DurationParser.formatSeconds(kit.cooldownSeconds()),
                    "access", kit.starter() ? "starter" : kit.permission()
            ));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        if (!kitManager.deleteKit(args[1])) {
            messages.send(sender, "claim-kit-missing", "kit", args[1]);
            return true;
        }
        messages.send(sender, "delete-success", "kit", args[1]);
        return true;
    }

    private boolean handleClone(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "usage");
            return true;
        }
        if (!kitManager.getKit(args[1]).isPresent()) {
            messages.send(sender, "claim-kit-missing", "kit", args[1]);
            return true;
        }
        if (!kitManager.isValidKitName(args[2]) || kitManager.kitExists(args[2])) {
            messages.send(sender, "editor-kit-exists", "kit", args[2]);
            return true;
        }
        kitManager.cloneKit(args[1], args[2]);
        messages.send(sender, "clone-success", "from", args[1], "to", args[2]);
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "usage");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "<red>Player not found.");
            return true;
        }
        if (kitManager.getKit(args[2]).isEmpty()) {
            messages.send(sender, "claim-kit-missing", "kit", args[2]);
            return true;
        }
        KitManager.ClaimResult result = kitManager.claimKit(target, args[2], true);
        if (!result.success()) {
            messages.send(sender, "menu-claim-failed", "reason", result.message());
            return true;
        }
        messages.send(sender, "give-success", "player", target.getName(), "kit", args[2]);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        plugin.reloadAll();
        messages.send(sender, "reload-success");
        return true;
    }

    private boolean handleResetCooldown(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "usage");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "<red>Player not found.");
            return true;
        }
        cooldownManager.resetCooldown(target.getUniqueId(), args[2]);
        messages.send(sender, "reset-cooldown", "player", target.getName(), "kit", args[2]);
        return true;
    }

    private boolean handleSetStarter(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "usage");
            return true;
        }
        Boolean starter = parseBoolean(args[2]);
        if (starter == null) {
            messages.send(sender, "invalid-boolean");
            return true;
        }
        if (!kitManager.setStarter(args[1], starter)) {
            messages.send(sender, "claim-kit-missing", "kit", args[1]);
            return true;
        }
        messages.send(sender, "set-starter", "kit", args[1], "value", String.valueOf(starter));
        return true;
    }

    private boolean handleSetCooldown(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "usage");
            return true;
        }
        long cooldownSeconds;
        try {
            cooldownSeconds = DurationParser.parseSeconds(args[2]);
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "invalid-duration");
            return true;
        }
        if (!kitManager.setCooldown(args[1], cooldownSeconds)) {
            messages.send(sender, "claim-kit-missing", "kit", args[1]);
            return true;
        }
        messages.send(sender, "set-cooldown", "kit", args[1], "time", DurationParser.formatSeconds(cooldownSeconds));
        return true;
    }

    private boolean handleSetPermission(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 3) {
            messages.send(sender, "usage");
            return true;
        }
        if (!kitManager.setPermission(args[1], args[2])) {
            messages.send(sender, "claim-kit-missing", "kit", args[1]);
            return true;
        }
        messages.send(sender, "set-permission", "kit", args[1], "permission", args[2]);
        return true;
    }

    private boolean handleCleanup(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        String regionId = args.length >= 2 ? args[1] : null;
        cleanupService.cleanup(sender, regionId);
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("saltkits.admin")) {
            messages.send(sender, "no-permission-admin");
            return false;
        }
        return true;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            messages.send(sender, "players-only-editor");
            return false;
        }
        return true;
    }

    @Nullable
    private Boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase(Locale.ROOT);
            if (subcommand.equals("claim")
                    || subcommand.equals("edit")
                    || subcommand.equals("preview")
                    || subcommand.equals("delete")
                    || subcommand.equals("setstarter")
                    || subcommand.equals("setcooldown")
                    || subcommand.equals("setpermission")
                    || subcommand.equals("clone")) {
                return filterStartsWith(kitManager.getKitNames(), args[1]);
            }
            if (subcommand.equals("give") || subcommand.equals("resetcooldown")) {
                return filterStartsWith(getOnlinePlayerNames(), args[1]);
            }
            if (subcommand.equals("cleanup")) {
                return filterStartsWith(cleanupService.regionIds(), args[1]);
            }
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase(Locale.ROOT);
            if (subcommand.equals("give") || subcommand.equals("clone")) {
                return filterStartsWith(kitManager.getKitNames(), args[2]);
            }
            if (subcommand.equals("resetcooldown")) {
                List<String> options = new ArrayList<>(kitManager.getKitNames());
                options.add("all");
                return filterStartsWith(options, args[2]);
            }
            if (subcommand.equals("setstarter")) {
                return filterStartsWith(List.of("true", "false"), args[2]);
            }
        }

        return List.of();
    }

    private List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowerInput)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
