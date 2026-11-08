package com.saltysmp.saltplots.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (input.contains("<") && input.contains(">")) {
            return MINI.deserialize(input);
        }
        return LEGACY.deserialize(input);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public static String format(String template, String... replacements) {
        String result = template;
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            result = result.replace("{" + replacements[index] + "}", replacements[index + 1]);
        }
        return result;
    }
}
