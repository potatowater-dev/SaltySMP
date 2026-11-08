package com.saltysmp.saltrestart.service;

import com.saltysmp.saltrestart.SaltRestartPlugin;
import com.saltysmp.saltrestart.config.MessagesConfig;
import com.saltysmp.saltrestart.util.DurationParser;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public final class RestartService {

    /** Reminder offsets (seconds before restart). No 5s / 3s per request. */
    private static final long[] REMINDERS_SECONDS = {
            3600L, 1800L, 900L, 600L, 300L, 60L, 30L, 10L, 1L
    };

    private final SaltRestartPlugin plugin;
    private final MessagesConfig messages;

    private long restartAtMillis = -1L;
    private final List<BukkitTask> tasks = new ArrayList<>();

    public RestartService(SaltRestartPlugin plugin, MessagesConfig messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public boolean isScheduled() {
        return restartAtMillis > 0 && System.currentTimeMillis() < restartAtMillis;
    }

    public OptionalLong remainingSeconds() {
        if (restartAtMillis <= 0) {
            return OptionalLong.empty();
        }
        long remaining = (restartAtMillis - System.currentTimeMillis() + 999L) / 1000L;
        if (remaining < 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(remaining);
    }

    public boolean schedule(long seconds) {
        if (seconds <= 0) {
            return false;
        }
        if (isScheduled()) {
            return false;
        }
        cancelTasksOnly();
        restartAtMillis = System.currentTimeMillis() + (seconds * 1000L);

        messages.broadcast("scheduled", "time", DurationParser.format(seconds));
        scheduleReminders(seconds);
        scheduleFinal(seconds);
        return true;
    }

    public boolean cancel() {
        if (restartAtMillis <= 0) {
            return false;
        }
        cancelTasksOnly();
        restartAtMillis = -1L;
        return true;
    }

    public void executeNow() {
        cancelTasksOnly();
        restartAtMillis = -1L;
        performRestart();
    }

    public void shutdown() {
        cancelTasksOnly();
        restartAtMillis = -1L;
    }

    private void scheduleReminders(long totalSeconds) {
        for (long reminder : REMINDERS_SECONDS) {
            if (reminder >= totalSeconds) {
                continue;
            }
            long delayTicks = (totalSeconds - reminder) * 20L;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActiveSchedule()) {
                    return;
                }
                messages.broadcast("reminder", "time", DurationParser.format(reminder));
            }, delayTicks);
            tasks.add(task);
        }
    }

    private void scheduleFinal(long totalSeconds) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (restartAtMillis <= 0) {
                return;
            }
            restartAtMillis = -1L;
            performRestart();
        }, totalSeconds * 20L);
        tasks.add(task);
    }

    private boolean isActiveSchedule() {
        return restartAtMillis > 0;
    }

    private void performRestart() {
        messages.broadcast("restarting");
        plugin.getLogger().info("Clearing combat tags via /cc removeTag all");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cc removeTag all");
        plugin.getLogger().info("Dispatching /restart");
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart"), 2L);
    }

    private void cancelTasksOnly() {
        for (BukkitTask task : tasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();
    }
}
