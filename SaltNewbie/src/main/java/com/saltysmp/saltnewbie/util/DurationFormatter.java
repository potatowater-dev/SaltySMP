package com.saltysmp.saltnewbie.util;

import java.util.OptionalLong;

public final class DurationFormatter {

    private DurationFormatter() {
    }

  public static String formatRemaining(long totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
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

    public static OptionalLong remainingSeconds(long expiryMillis) {
        if (expiryMillis <= 0) {
            return OptionalLong.empty();
        }
        long remaining = (expiryMillis - System.currentTimeMillis() + 999L) / 1000L;
        if (remaining <= 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(remaining);
    }
}
