package com.saltysmp.saltrestart.command;

import com.saltysmp.saltrestart.config.MessagesConfig;
import com.saltysmp.saltrestart.service.RestartService;
import com.saltysmp.saltrestart.util.DurationParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

public final class SaltRestartCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("cancel", "status", "now");
    private static final List<String> DURATION_HINTS = List.of("10s", "30s", "1m", "5m", "10m", "30m", "1h");

    private final MessagesConfig messages;
    private final RestartService restartService;

    public SaltRestartCommand(MessagesConfig messages, RestartService restartService) {
        this.messages = messages;
        this.restartService = restartService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "usage");
            return true;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        switch (first) {
            case "cancel" -> handleCancel(sender);
            case "status" -> handleStatus(sender);
            case "now" -> handleNow(sender);
            default -> handleSchedule(sender, args[0]);
        }
        return true;
    }

    private void handleCancel(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (!restartService.cancel()) {
            messages.send(sender, "no-schedule");
            return;
        }
        messages.broadcast("cancelled");
    }

    private void handleStatus(CommandSender sender) {
        OptionalLong remaining = restartService.remainingSeconds();
        if (remaining.isEmpty()) {
            messages.send(sender, "no-schedule");
            return;
        }
        messages.send(sender, "status", "time", DurationParser.format(remaining.getAsLong()));
    }

    private void handleNow(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        restartService.executeNow();
    }

    private void handleSchedule(CommandSender sender, String durationRaw) {
        if (!requireAdmin(sender)) {
            return;
        }
        OptionalLong seconds = DurationParser.parseSeconds(durationRaw);
        if (seconds.isEmpty()) {
            messages.send(sender, "invalid-duration");
            return;
        }
        if (restartService.isScheduled()) {
            OptionalLong remaining = restartService.remainingSeconds();
            String time = remaining.isPresent()
                    ? DurationParser.format(remaining.getAsLong())
                    : "?";
            messages.send(sender, "already-scheduled", "time", time);
            return;
        }
        restartService.schedule(seconds.getAsLong());
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("saltrestart.admin")) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (sender.hasPermission("saltrestart.admin")) {
            options.addAll(SUBS);
            options.addAll(DURATION_HINTS);
        } else {
            options.add("status");
        }
        return filter(prefix, options);
    }

    private static List<String> filter(String prefix, List<String> options) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(option);
            }
        }
        return result;
    }
}
