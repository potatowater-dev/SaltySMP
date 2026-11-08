package com.saltysmp.saltkits.util;

public final class DurationParser {

    private DurationParser() {
    }

    public static long parseSeconds(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Duration cannot be null.");
        }

        String trimmed = input.trim().toLowerCase();
        if (trimmed.isEmpty() || trimmed.equals("0") || trimmed.equals("none")) {
            return 0L;
        }

        if (trimmed.chars().allMatch(Character::isDigit)) {
            return Long.parseLong(trimmed);
        }

        char suffix = trimmed.charAt(trimmed.length() - 1);
        String numberPart = trimmed.substring(0, trimmed.length() - 1);
        if (!numberPart.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid duration: " + input);
        }

        long value = Long.parseLong(numberPart);
        return switch (suffix) {
            case 's' -> value;
            case 'm' -> value * 60L;
            case 'h' -> value * 3600L;
            case 'd' -> value * 86400L;
            default -> throw new IllegalArgumentException("Invalid duration suffix: " + suffix);
        };
    }

    public static String formatSeconds(long seconds) {
        if (seconds <= 0L) {
            return "none";
        }

        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        StringBuilder builder = new StringBuilder();
        if (days > 0L) {
            builder.append(days).append('d');
        }
        if (hours > 0L) {
            builder.append(hours).append('h');
        }
        if (minutes > 0L) {
            builder.append(minutes).append('m');
        }
        if (seconds > 0L) {
            builder.append(seconds).append('s');
        }
        return builder.isEmpty() ? "0s" : builder.toString();
    }
}
