package com.saltysmp.saltnewbie.command;

import com.saltysmp.saltnewbie.config.MessagesConfig;
import com.saltysmp.saltnewbie.service.NewbieService;
import com.saltysmp.saltnewbie.util.DurationFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

public final class NewbieCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("status", "remove");

    private final MessagesConfig messages;
    private final NewbieService newbieService;

    public NewbieCommand(MessagesConfig messages, NewbieService newbieService) {
        this.messages = messages;
        this.newbieService = newbieService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        if (!player.hasPermission("saltnewbie.use")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            handleActivate(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> handleStatus(player);
            case "remove" -> handleRemove(player);
            default -> messages.send(player, "usage");
        }
        return true;
    }

    private void handleActivate(Player player) {
        if (newbieService.activate(player.getUniqueId())) {
            messages.send(player, "activated");
            return;
        }
        OptionalLong remaining = newbieService.remainingSeconds(player.getUniqueId());
        String time = remaining.isPresent()
                ? DurationFormatter.formatRemaining(remaining.getAsLong())
                : "0s";
        messages.send(player, "already-active", "time", time);
    }

    private void handleStatus(Player player) {
        OptionalLong remaining = newbieService.remainingSeconds(player.getUniqueId());
        if (remaining.isEmpty()) {
            messages.send(player, "status-none");
            return;
        }
        messages.send(player, "status-active", "time", DurationFormatter.formatRemaining(remaining.getAsLong()));
    }

    private void handleRemove(Player player) {
        NewbieService.RemoveResult result = newbieService.tryRemove(player.getUniqueId());
        switch (result) {
            case NOT_PROTECTED -> messages.send(player, "status-none");
            case CONFIRMED -> messages.send(player, "removed");
            case EXPIRED -> messages.send(player, "remove-expired");
            case AWAITING_CONFIRM -> messages.send(player, "remove-confirm");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : SUBS) {
            if (option.startsWith(prefix)) {
                result.add(option);
            }
        }
        return result;
    }
}
