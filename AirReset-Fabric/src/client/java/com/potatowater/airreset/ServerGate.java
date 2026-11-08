package com.potatowater.airreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

public final class ServerGate {

    private ServerGate() {
    }

    public static boolean isCurrentServerAllowed() {
        var whitelist = AirResetConfig.INSTANCE.serverWhitelist;
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.isSingleplayer()) {
            return matchesAny(whitelist, "singleplayer", "localhost", "local", "127.0.0.1");
        }

        ServerData server = client.getCurrentServer();
        if (server == null) {
            return false;
        }

        String address = server.ip == null ? "" : server.ip.trim();
        if (address.isEmpty()) {
            return false;
        }

        String host = stripPort(address);
        for (String entry : whitelist) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String normalized = entry.trim();
            if (equalsIgnoreCase(normalized, address) || equalsIgnoreCase(normalized, host)
                    || equalsIgnoreCase(stripPort(normalized), host)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAny(java.util.List<String> whitelist, String... aliases) {
        for (String entry : whitelist) {
            if (entry == null) {
                continue;
            }
            for (String alias : aliases) {
                if (equalsIgnoreCase(entry.trim(), alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String stripPort(String address) {
        int bracket = address.lastIndexOf(']');
        if (address.startsWith("[") && bracket > 0) {
            return address.substring(1, bracket);
        }
        int colon = address.lastIndexOf(':');
        if (colon > 0 && address.indexOf(':') == colon) {
            return address.substring(0, colon);
        }
        return address;
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a.toLowerCase(Locale.ROOT).equals(b.toLowerCase(Locale.ROOT));
    }
}
