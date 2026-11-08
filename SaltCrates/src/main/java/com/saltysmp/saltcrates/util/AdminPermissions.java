package com.saltysmp.saltcrates.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AdminPermissions {

    private AdminPermissions() {
    }

    public static boolean isAdmin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.hasPermission("saltcrates.admin");
    }
}
