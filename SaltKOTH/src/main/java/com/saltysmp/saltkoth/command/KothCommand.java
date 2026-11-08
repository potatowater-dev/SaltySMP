package com.saltysmp.saltkoth.command;

import com.saltysmp.saltkoth.SaltKothPlugin;
import com.saltysmp.saltkoth.config.AreaStorage;
import com.saltysmp.saltkoth.config.MessagesConfig;
import com.saltysmp.saltkoth.model.KothArea;
import com.saltysmp.saltkoth.service.KothBorderService;
import com.saltysmp.saltkoth.service.KothGameService;
import com.saltysmp.saltkoth.service.KothHologramService;
import com.saltysmp.saltkoth.service.SelectionService;
import com.saltysmp.saltkoth.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class KothCommand implements CommandExecutor, TabCompleter {

    private final SaltKothPlugin plugin;
    private final MessagesConfig messages;
    private final AreaStorage areaStorage;
    private final SelectionService selectionService;
    private final KothGameService gameService;
    private final KothBorderService borderService;

    public KothCommand(
            SaltKothPlugin plugin,
            MessagesConfig messages,
            AreaStorage areaStorage,
            SelectionService selectionService,
            KothGameService gameService,
            KothBorderService borderService
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.areaStorage = areaStorage;
        this.selectionService = selectionService;
        this.gameService = gameService;
        this.borderService = borderService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "pos1" -> handlePos1(sender);
            case "pos2" -> handlePos2(sender);
            case "setarea" -> handleSetArea(sender);
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "pause" -> handlePause(sender);
            case "status" -> handleStatus(sender);
            case "info" -> handleInfo(sender);
            case "tp" -> handleTp(sender);
            case "top" -> handleTop(sender);
            case "preview" -> handlePreview(sender);
            case "reload" -> handleReload(sender);
            case "hologram" -> handleHologram(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("status", "info", "top"));
            if (sender.hasPermission("saltkoth.admin")) {
                options.addAll(List.of("pos1", "pos2", "setarea", "start", "stop", "pause", "tp", "preview", "hologram", "reload"));
            }
            return filter(args[0], options);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("hologram") && sender.hasPermission("saltkoth.admin")) {
            return filter(args[1], "remove");
        }
        return List.of();
    }

    private void handlePos1(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Location location = player.getLocation();
        selectionService.setPos1(player.getUniqueId(), location);
        messages.send(sender, "pos1-set",
                "x", String.valueOf(location.getBlockX()),
                "y", String.valueOf(location.getBlockY()),
                "z", String.valueOf(location.getBlockZ()));
    }

    private void handlePos2(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Location location = player.getLocation();
        selectionService.setPos2(player.getUniqueId(), location);
        messages.send(sender, "pos2-set",
                "x", String.valueOf(location.getBlockX()),
                "y", String.valueOf(location.getBlockY()),
                "z", String.valueOf(location.getBlockZ()));
    }

    private void handleSetArea(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Location first = selectionService.getPos1(player.getUniqueId());
        Location second = selectionService.getPos2(player.getUniqueId());
        if (first == null || second == null) {
            messages.send(sender, "setarea-missing");
            return;
        }
        if (first.getWorld() == null || second.getWorld() == null
                || !first.getWorld().equals(second.getWorld())) {
            messages.send(sender, "setarea-world");
            return;
        }
        KothArea area = new KothArea(
                first.getWorld().getName(),
                first.getBlockX(), first.getBlockY(), first.getBlockZ(),
                second.getBlockX(), second.getBlockY(), second.getBlockZ()
        );
        areaStorage.save(area);
        selectionService.clear(player.getUniqueId());
        messages.send(sender, "setarea-success", "volume", String.valueOf(area.getVolume()));
        borderService.preview(area);
        messages.send(sender, "preview-started");
    }

    private void handleStart(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (!areaStorage.hasArea()) {
            messages.send(sender, "no-area");
            return;
        }
        if (gameService.isActive()) {
            messages.send(sender, "already-active");
            return;
        }
        gameService.forceStart();
        messages.send(sender, "start-forced");
    }

    private void handleStop(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (!gameService.isActive()) {
            messages.send(sender, "not-active");
            return;
        }
        gameService.forceStop();
        messages.send(sender, "stop-forced");
    }

    private void handlePause(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (!gameService.isActive()) {
            messages.send(sender, "not-active");
            return;
        }
        boolean paused = gameService.togglePause();
        messages.send(sender, paused ? "pause-on" : "pause-off");
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("saltkoth.status")) {
            messages.send(sender, "no-permission-status");
            return;
        }
        if (!areaStorage.hasArea()) {
            messages.send(sender, "no-area");
            return;
        }
        if (gameService.isPaused()) {
            messages.send(sender, "status-paused", "time", MessageUtil.formatDuration(gameService.getTimeUntilNextEvent()));
            return;
        }
        if (gameService.isActive()) {
            String leader = gameService.getLeaderName();
            if ("—".equals(leader)) {
                messages.send(sender, "status-active-no-leader", "time", MessageUtil.formatDuration(gameService.getTimeUntilNextEvent()));
            } else {
                messages.send(sender, "status-active",
                        "time", MessageUtil.formatDuration(gameService.getTimeUntilNextEvent()),
                        "leader", leader,
                        "seconds", String.valueOf(gameService.getLeaderSeconds()));
            }
            return;
        }
        messages.send(sender, "status-idle", "time", MessageUtil.formatDuration(gameService.getTimeUntilNextEvent()));
    }

    private void handleInfo(CommandSender sender) {
        if (!sender.hasPermission("saltkoth.status")) {
            messages.send(sender, "no-permission-status");
            return;
        }
        KothArea area = areaStorage.getArea();
        if (area == null || !area.isConfigured()) {
            messages.send(sender, "no-area");
            return;
        }
        messages.send(sender, "info-header");
        messages.send(sender, "info-world", "world", area.getWorldName());
        messages.send(sender, "info-bounds",
                "minx", String.valueOf(area.getMinX()),
                "miny", String.valueOf(area.getMinY()),
                "minz", String.valueOf(area.getMinZ()),
                "maxx", String.valueOf(area.getMaxX()),
                "maxy", String.valueOf(area.getMaxY()),
                "maxz", String.valueOf(area.getMaxZ()));
        messages.send(sender, "info-volume", "volume", String.valueOf(area.getVolume()));
        messages.send(sender, "info-last-winner",
                "player", plugin.getWinStatsStorage().getLastWinnerName(),
                "seconds", String.valueOf(plugin.getWinStatsStorage().getLastWinnerSeconds()));
    }

    private void handleTp(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        KothArea area = areaStorage.getArea();
        if (area == null || area.getWorld() == null) {
            messages.send(sender, "no-area");
            return;
        }
        Location center = new Location(
                area.getWorld(),
                (area.getMinX() + area.getMaxX()) / 2.0 + 0.5,
                area.getMinY() + 1.0,
                (area.getMinZ() + area.getMaxZ()) / 2.0 + 0.5
        );
        player.teleport(center);
        messages.send(sender, "tp-success");
    }

    private void handleTop(CommandSender sender) {
        if (!sender.hasPermission("saltkoth.status")) {
            messages.send(sender, "no-permission-status");
            return;
        }
        List<Map.Entry<UUID, Integer>> top = plugin.getWinStatsStorage().topWins(10);
        if (top.isEmpty()) {
            messages.send(sender, "top-empty");
            return;
        }
        messages.send(sender, "top-header");
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());
            String name = offline.getName() != null ? offline.getName() : entry.getKey().toString();
            messages.send(sender, "top-entry", "rank", String.valueOf(rank++), "player", name, "wins", String.valueOf(entry.getValue()));
        }
    }

    private void handlePreview(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        KothArea area = areaStorage.getArea();
        if (area == null) {
            messages.send(sender, "no-area");
            return;
        }
        borderService.preview(area);
        messages.send(sender, "preview-started");
    }

    private void handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        plugin.reloadPlugin();
        messages.send(sender, "reload-success");
    }

    private void handleHologram(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        KothHologramService hologramService = plugin.getHologramService();
        if (args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
            if (hologramService.removeTitle()) {
                messages.send(sender, "hologram-removed");
            } else {
                messages.send(sender, "hologram-none");
            }
            return;
        }
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (hologramService.spawnTitle(player.getLocation())) {
            messages.send(sender, "hologram-spawned");
        } else {
            messages.send(sender, "hologram-failed");
        }
    }

    private void sendHelp(CommandSender sender) {
        messages.send(sender, "help-header");
        if (sender.hasPermission("saltkoth.status")) {
            messages.send(sender, "help-status");
            messages.send(sender, "help-info");
            messages.send(sender, "help-top");
        }
        if (sender.hasPermission("saltkoth.admin")) {
            messages.send(sender, "help-pos1");
            messages.send(sender, "help-pos2");
            messages.send(sender, "help-setarea");
            messages.send(sender, "help-start");
            messages.send(sender, "help-stop");
            messages.send(sender, "help-pause");
            messages.send(sender, "help-tp");
            messages.send(sender, "help-preview");
            messages.send(sender, "help-hologram");
            messages.send(sender, "help-reload");
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("saltkoth.admin")) {
            return true;
        }
        messages.send(sender, "no-permission-admin");
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        messages.send(sender, "players-only");
        return null;
    }

    private List<String> filter(String input, List<String> options) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private List<String> filter(String input, String... options) {
        return filter(input, Arrays.asList(options));
    }
}
