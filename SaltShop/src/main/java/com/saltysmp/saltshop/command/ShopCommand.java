package com.saltysmp.saltshop.command;

import com.saltysmp.saltshop.SaltShopPlugin;
import com.saltysmp.saltshop.config.MessagesConfig;
import com.saltysmp.saltshop.config.ShopStorage;
import com.saltysmp.saltshop.gui.ShopEditorGui;
import com.saltysmp.saltshop.model.ShopDefinition;
import com.saltysmp.saltshop.service.ShopNpcService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ShopCommand implements CommandExecutor, TabCompleter {

    private final SaltShopPlugin plugin;
    private final MessagesConfig messages;
    private final ShopStorage storage;
    private final ShopNpcService npcService;

    public ShopCommand(
            SaltShopPlugin plugin,
            MessagesConfig messages,
            ShopStorage storage,
            ShopNpcService npcService
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.storage = storage;
        this.npcService = npcService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "help" -> handleHelp(sender);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "spawn" -> handleSpawn(sender, args);
            case "remove", "removenpc" -> handleRemove(sender);
            case "editor", "edit" -> handleEditor(sender, args);
            default -> {
                messages.send(sender, "usage");
                yield true;
            }
        };
    }

    private boolean handleHelp(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        messages.send(sender, "help-header");
        for (int line = 1; line <= 6; line++) {
            messages.send(sender, "help-line-" + line);
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        plugin.reloadAll();
        npcService.restoreAll();
        messages.send(sender, "shop-reloaded");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        List<String> ids = storage.getIds();
        if (ids.isEmpty()) {
            messages.send(sender, "shop-list-empty");
            return true;
        }
        messages.send(sender, "shop-list", "shops", String.join(", ", ids));
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (storage.get(id) != null) {
            messages.send(sender, "shop-exists", "shop", id);
            return true;
        }
        storage.getOrCreate(id);
        messages.send(sender, "shop-created", "shop", id);
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
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!storage.delete(id)) {
            messages.send(sender, "unknown-shop", "shop", id);
            return true;
        }
        messages.send(sender, "shop-deleted", "shop", id);
        return true;
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        ShopDefinition shop = storage.get(args[1].toLowerCase(Locale.ROOT));
        if (shop == null) {
            messages.send(sender, "unknown-shop", "shop", args[1]);
            return true;
        }
        npcService.spawn(player, shop);
        messages.send(player, "shop-spawned", "shop", shop.getId());
        return true;
    }

    private boolean handleRemove(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        RayTraceResult trace = player.rayTraceEntities(5);
        if (trace == null || !(trace.getHitEntity() instanceof Villager villager)) {
            messages.send(player, "no-target");
            return true;
        }
        if (!npcService.isShopNpc(villager)) {
            messages.send(player, "no-target-not-shop");
            return true;
        }
        storage.removeNpc(villager.getUniqueId());
        villager.remove();
        messages.send(player, "shop-removed");
        return true;
    }

    private boolean handleEditor(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (args.length < 2) {
            messages.send(sender, "usage");
            return true;
        }
        ShopDefinition shop = storage.get(args[1].toLowerCase(Locale.ROOT));
        if (shop == null) {
            messages.send(sender, "unknown-shop", "shop", args[1]);
            return true;
        }
        player.openInventory(new ShopEditorGui(player, shop).getInventory());
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("saltshop.admin")) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("saltshop.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filterPrefix(Arrays.asList(
                    "help", "create", "delete", "list", "spawn", "remove", "removenpc", "editor", "edit", "reload"
            ), args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("list")
                && !args[0].equalsIgnoreCase("help")) {
            return filterPrefix(storage.getIds(), args[1]);
        }
        return List.of();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
