package com.saltysmp.saltrestart.util;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern TOKEN = Pattern.compile("(\\d+)\\s*([smhd])", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAIN_SECONDS = Pattern.compile("^(\\d+)$");

    private DurationParser() {
    }

    /**
     * Parses durations like {@code 5s}, {@code 10m}, {@code 1h}, {@code 1h30m}, {@code 90}.
     *
     * @return total seconds, or empty if invalid / non-positive
     */
    public static OptionalLong parseSeconds(String input) {
        if (input == null || input.isBlank()) {
            return OptionalLong.empty();
        }
        String raw = input.trim().toLowerCase(Locale.ROOT).replace(" ", "");

        Matcher plain = PLAIN_SECONDS.matcher(raw);
        if (plain.matches()) {
            long seconds = Long.parseLong(plain.group(1));
            return seconds > 0 ? OptionalLong.of(seconds) : OptionalLong.empty();
        }

        Matcher matcher = TOKEN.matcher(raw);
        long total = 0;
        int consumed = 0;
        while (matcher.find()) {
            if (matcher.start() != consumed) {
                return OptionalLong.empty();
            }
            long amount = Long.parseLong(matcher.group(1));
            total += switch (matcher.group(2).charAt(0)) {
                case 's' -> amount;
                case 'm' -> amount * 60L;
                case 'h' -> amount * 3600L;
                case 'd' -> amount * 86400L;
                default -> 0L;
            };
            consumed = matcher.end();
        }
        if (consumed == 0 || consumed != raw.length() || total <= 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(total);
    }

    /** Formats seconds as a short label: {@code 1h}, {@code 30m}, {@code 10s}, {@code 1h 5m}. */
    public static String format(long totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append('d');
        }
        if (hours > 0) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(hours).append('h');
        }
        if (minutes > 0) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(minutes).append('m');
        }
        if (seconds > 0 || builder.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(seconds).append('s');
        }
        return builder.toString();
    }
}
