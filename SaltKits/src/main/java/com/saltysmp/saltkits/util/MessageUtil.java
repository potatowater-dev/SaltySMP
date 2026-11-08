package com.saltysmp.saltkits.util;

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

    public static void actionBar(Player player, String message) {
        player.sendActionBar(parse(message));
    }

    public static void title(Player player, String title, String subtitle) {
        player.showTitle(net.kyori.adventure.title.Title.title(
                parse(title),
                parse(subtitle),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(250),
                        java.time.Duration.ofMillis(1500),
                        java.time.Duration.ofMillis(500)
                )
        ));
    }

    public static String format(String template, String... replacements) {
        String result = template;
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            result = result.replace("{" + replacements[index] + "}", replacements[index + 1]);
        }
        return result;
    }
}
