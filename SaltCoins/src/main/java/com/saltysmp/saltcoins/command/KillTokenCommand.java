package com.saltysmp.saltcoins.command;

import com.saltysmp.saltcoins.SaltCoinsPlugin;
import com.saltysmp.saltcoins.config.MessagesConfig;
import com.saltysmp.saltcoins.service.KillTokenService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class KillTokenCommand implements CommandExecutor, TabCompleter {

    private final SaltCoinsPlugin plugin;
    private final MessagesConfig messages;
    private final KillTokenService killTokenService;

    public KillTokenCommand(SaltCoinsPlugin plugin, MessagesConfig messages, KillTokenService killTokenService) {
        this.plugin = plugin;
        this.messages = messages;
        this.killTokenService = killTokenService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "usage-give");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            default -> messages.send(sender, "usage-give");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("saltcoins.admin")) {
            return List.of();
        }
        if (args.length <= 1) {
            return filter(args.length == 0 ? "" : args[0], List.of("give", "reload"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(args[2], List.of("1", "5", "10"));
        }
        return List.of();
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("saltcoins.admin")) {
            messages.send(sender, "no-permission");
            return;
        }

        Player target;
        int amount = 1;

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "players-only");
                return;
            }
            target = player;
        } else if (args.length == 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                messages.send(sender, "player-not-found", "player", args[1]);
                return;
            }
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                messages.send(sender, "player-not-found", "player", args[1]);
                return;
            }
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) {
                    messages.send(sender, "invalid-number");
                    return;
                }
            } catch (NumberFormatException exception) {
                messages.send(sender, "invalid-number");
                return;
            }
        }

        killTokenService.give(target, amount);
        if (sender.equals(target)) {
            messages.send(sender, "token-given-self", "amount", String.valueOf(amount));
        } else {
            messages.send(sender, "token-given-other", "player", target.getName(), "amount", String.valueOf(amount));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("saltcoins.admin")) {
            messages.send(sender, "no-permission");
            return;
        }
        plugin.reloadAll();
        messages.send(sender, "reload-success");
    }

    private List<String> filter(String input, List<String> options) {
        String needle = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
