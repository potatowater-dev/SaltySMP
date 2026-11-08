package com.saltysmp.saltkoth.service;

import com.saltysmp.saltkoth.SaltKothPlugin;
import com.saltysmp.saltkoth.config.AreaStorage;
import com.saltysmp.saltkoth.config.KothDataStorage;
import com.saltysmp.saltkoth.config.MessagesConfig;
import com.saltysmp.saltkoth.config.PluginConfig;
import com.saltysmp.saltkoth.config.WinStatsStorage;
import com.saltysmp.saltkoth.model.KothArea;
import com.saltysmp.saltkoth.util.MessageUtil;
import com.saltysmp.saltkoth.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KothGameService {

    private final SaltKothPlugin plugin;
    private final PluginConfig config;
    private final MessagesConfig messages;
    private final AreaStorage areaStorage;
    private final KothDataStorage dataStorage;
    private final WinStatsStorage winStats;
    private final CrateRewardService rewardService;
    private final KothBorderService borderService;
    private final KothBossBarService bossBarService;

    private final Map<UUID, Integer> captureSeconds = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    private final Set<UUID> playersInZone = new HashSet<>();
    private final LinkedHashSet<UUID> zonePresenceOrder = new LinkedHashSet<>();

    private BukkitTask tickTask;
    private boolean active;
    private boolean paused;
    private String currentLeaderName;
    private UUID currentCapturer;
    private int captureBroadcastCounter;

    public KothGameService(
            SaltKothPlugin plugin,
            PluginConfig config,
            MessagesConfig messages,
            AreaStorage areaStorage,
            KothDataStorage dataStorage,
            WinStatsStorage winStats,
            CrateRewardService rewardService,
            KothBorderService borderService,
            KothBossBarService bossBarService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.areaStorage = areaStorage;
        this.dataStorage = dataStorage;
        this.winStats = winStats;
        this.rewardService = rewardService;
        this.borderService = borderService;
        this.bossBarService = bossBarService;
    }

    public void startScheduler() {
        stopScheduler();
        bossBarService.start();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, config.getTickIntervalTicks());
        borderService.start(() -> {
            if (!areaStorage.hasArea()) {
                return;
            }
            borderService.render(areaStorage.getArea(), active);
        });
    }

    public void stopScheduler() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        borderService.stop();
        bossBarService.stopEnforce();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPaused() {
        return paused;
    }

    public long getTimeUntilNextEvent() {
        if (active) {
            return Math.max(0L, dataStorage.getEventEndMs() - TimeUtil.now());
        }
        return Math.max(0L, dataStorage.getNextEventStartMs() - TimeUtil.now());
    }

    public String getLeaderName() {
        LeaderInfo leader = getLeader();
        return leader == null ? "—" : leader.name();
    }

    public int getLeaderSeconds() {
        LeaderInfo leader = getLeader();
        return leader == null ? 0 : leader.seconds();
    }

    public boolean isInZone(Player player) {
        KothArea area = areaStorage.getArea();
        return area != null && area.contains(player.getLocation());
    }

    public long getActiveTimeLeftMs() {
        if (!active) {
            return 0L;
        }
        return Math.max(0L, dataStorage.getEventEndMs() - TimeUtil.now());
    }

    public long getNextEventTimeMs() {
        if (active) {
            return 0L;
        }
        return Math.max(0L, dataStorage.getNextEventStartMs() - TimeUtil.now());
    }

    public String getCapturerName() {
        if (currentCapturer == null) {
            return "—";
        }
        Player capturer = Bukkit.getPlayer(currentCapturer);
        if (capturer != null) {
            return capturer.getName();
        }
        return playerNames.getOrDefault(currentCapturer, "—");
    }

    public int getCapturerSeconds() {
        if (currentCapturer == null) {
            return 0;
        }
        return captureSeconds.getOrDefault(currentCapturer, 0);
    }

    public boolean forceStart() {
        if (!areaStorage.hasArea()) {
            return false;
        }
        if (active) {
            return false;
        }
        paused = false;
        beginEvent(true);
        return true;
    }

    public boolean forceStop() {
        if (!active) {
            return false;
        }
        endEvent(true);
        return true;
    }

    public boolean togglePause() {
        if (!active) {
            return false;
        }
        paused = !paused;
        return paused;
    }

    public void reloadUi() {
        bossBarService.hideAll();
        if (active) {
            refreshUi();
        }
    }

    public void onEnterZone(Player player) {
        playersInZone.add(player.getUniqueId());
        zonePresenceOrder.add(player.getUniqueId());
        if (active && currentCapturer == null) {
            setCapturer(player.getUniqueId());
        }
        if (active) {
            refreshPlayerUi(player);
        }
    }

    public void onExitZone(Player player) {
        playersInZone.remove(player.getUniqueId());
        zonePresenceOrder.remove(player.getUniqueId());
        if (player.getUniqueId().equals(currentCapturer)) {
            currentCapturer = null;
            pickNextCapturer();
        }
        player.setGlowing(false);
        if (active) {
            bossBarService.onQuit(player);
            refreshUi();
        }
    }

    public void onPlayerJoin(Player player) {
        KothArea area = areaStorage.getArea();
        boolean inZone = area != null && area.contains(player.getLocation());
        if (inZone) {
            playersInZone.add(player.getUniqueId());
            zonePresenceOrder.add(player.getUniqueId());
            if (currentCapturer == null) {
                pickNextCapturer();
            }
        }
        if (active) {
            refreshPlayerUi(player);
        }
    }

    public void onPlayerQuit(Player player) {
        playersInZone.remove(player.getUniqueId());
        zonePresenceOrder.remove(player.getUniqueId());
        if (player.getUniqueId().equals(currentCapturer)) {
            currentCapturer = null;
        }
        player.setGlowing(false);
        bossBarService.onQuit(player);
        if (!active) {
            captureSeconds.remove(player.getUniqueId());
            playerNames.remove(player.getUniqueId());
        }
    }

    private void tick() {
        if (!areaStorage.hasArea()) {
            if (active) {
                endEvent(true);
            }
            return;
        }

        long now = TimeUtil.now();
        if (!active) {
            if (now >= dataStorage.getNextEventStartMs()) {
                beginEvent(false);
            }
            return;
        }

        if (paused) {
            dataStorage.extendEventEnd(config.getTickIntervalTicks() * 50L);
            refreshUi();
            return;
        }

        if (now >= dataStorage.getEventEndMs()) {
            endEvent(false);
            return;
        }

        processCaptureTick();
        refreshUi();
    }

    private void beginEvent(boolean forced) {
        bossBarService.hideAll();
        active = true;
        paused = false;
        currentLeaderName = null;
        currentCapturer = null;
        captureBroadcastCounter = 0;
        zonePresenceOrder.clear();
        clearAllGlow();
        captureSeconds.clear();
        playerNames.clear();

        long now = TimeUtil.now();
        dataStorage.setEventEndMs(now + config.getEventDurationMs());

        if (config.isBroadcastStart()) {
            messages.broadcast(
                    "reset-broadcast",
                    "duration", MessageUtil.formatDuration(config.getEventDurationMs())
            );
        }
        if (config.isTitlesEnabled()) {
            for (Player player : playersInZone()) {
                MessageUtil.title(player, "<gold>⚔ KOTH", "<yellow>Capture the hill!");
            }
        }
        for (Player player : playersInZone()) {
            zonePresenceOrder.add(player.getUniqueId());
        }
        pickNextCapturer();
        refreshUi();
    }

    private void endEvent(boolean forced) {
        active = false;
        paused = false;
        List<LeaderInfo> winners = findWinners(forced);

        if (!winners.isEmpty()) {
            if (config.isBroadcastEnd() || forced) {
                if (winners.size() == 1) {
                    LeaderInfo winner = winners.get(0);
                    messages.broadcast(
                            forced ? "event-end-forced" : "event-end-broadcast",
                            "player", winner.name(),
                            "seconds", String.valueOf(winner.seconds())
                    );
                } else {
                    String names = String.join(", ", winners.stream().map(LeaderInfo::name).toList());
                    messages.broadcast("event-end-tie", "players", names);
                }
            }
            for (LeaderInfo winner : winners) {
                winStats.recordWin(winner.uuid(), winner.name(), winner.seconds());
                rewardService.rewardWinner(winner.uuid(), winner.name());
            }
        } else if (config.isBroadcastEnd() || forced) {
            messages.broadcast("event-end-no-winner");
        }

        if (config.isTitlesEnabled()) {
            for (Player player : playersInZone()) {
                MessageUtil.title(player, "<gray>KOTH Ended", "<white>Thanks for fighting!");
            }
        }

        captureSeconds.clear();
        playerNames.clear();
        currentLeaderName = null;
        currentCapturer = null;
        captureBroadcastCounter = 0;
        zonePresenceOrder.clear();
        clearAllGlow();
        dataStorage.setNextEventStartMs(TimeUtil.now() + config.getDelayBetweenEventsMs());
        dataStorage.setEventEndMs(0L);

        bossBarService.hideAll();
        refreshIdleUi();
    }

    private void processCaptureTick() {
        KothArea area = areaStorage.getArea();
        if (area == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (area.contains(player.getLocation())) {
                zonePresenceOrder.add(player.getUniqueId());
            }
        }
        if (currentCapturer == null) {
            pickNextCapturer();
        }
        if (currentCapturer == null) {
            captureBroadcastCounter = 0;
            return;
        }
        Player capturer = Bukkit.getPlayer(currentCapturer);
        if (capturer == null || !area.contains(capturer.getLocation())) {
            currentCapturer = null;
            pickNextCapturer();
            capturer = currentCapturer != null ? Bukkit.getPlayer(currentCapturer) : null;
        }
        if (capturer == null) {
            captureBroadcastCounter = 0;
            return;
        }

        UUID uuid = capturer.getUniqueId();
        playerNames.put(uuid, capturer.getName());
        captureSeconds.merge(uuid, 1, Integer::sum);
        capturer.setGlowing(true);

        LeaderInfo leader = getLeader();
        if (leader != null && !leader.name().equals(currentLeaderName)) {
            currentLeaderName = leader.name();
            if (config.isLeaderChangeBroadcast()) {
                messages.broadcast("leader-change", "player", leader.name(), "seconds", String.valueOf(leader.seconds()));
            }
        }
        if (config.isCaptureSoundEnabled()) {
            capturer.playSound(capturer.getLocation(), config.getCaptureSound(), 0.35f, 1.2f);
        }
        if (config.isCaptureBroadcastEnabled()) {
            captureBroadcastCounter++;
            if (captureBroadcastCounter >= config.getCaptureBroadcastIntervalSeconds()) {
                captureBroadcastCounter = 0;
                messages.broadcast("capturing-broadcast", "player", capturer.getName());
            }
        }
    }

    private void pickNextCapturer() {
        KothArea area = areaStorage.getArea();
        if (area == null) {
            return;
        }
        for (UUID candidateId : zonePresenceOrder) {
            Player candidate = Bukkit.getPlayer(candidateId);
            if (candidate != null && area.contains(candidate.getLocation())) {
                setCapturer(candidateId);
                return;
            }
        }
        currentCapturer = null;
    }

    private void setCapturer(UUID playerId) {
        if (playerId.equals(currentCapturer)) {
            return;
        }
        clearAllGlow();
        currentCapturer = playerId;
        captureBroadcastCounter = 0;
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.setGlowing(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false, false));
        }
    }

    private void clearAllGlow() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isGlowing()) {
                player.setGlowing(false);
            }
        }
    }

    private void refreshUi() {
        long remaining = Math.max(0L, dataStorage.getEventEndMs() - TimeUtil.now());
        LeaderInfo leader = getLeader();
        String capturerText;
        if (currentCapturer != null) {
            Player capturer = Bukkit.getPlayer(currentCapturer);
            String name = capturer != null ? capturer.getName() : playerNames.getOrDefault(currentCapturer, "Unknown");
            int seconds = captureSeconds.getOrDefault(currentCapturer, 0);
            capturerText = name + " capturing (" + seconds + "s)";
        } else {
            capturerText = leader == null ? "Nobody capturing" : leader.name() + " (" + leader.seconds() + "s)";
        }
        Set<Player> zonePlayers = playersInZone();

        bossBarService.showActive(remaining, config.getEventDurationMs(), capturerText, zonePlayers);
    }

    private void refreshIdleUi() {
        bossBarService.hideAll();
    }

    private void refreshPlayerUi(Player player) {
        if (!active) {
            return;
        }
        refreshUi();
    }

    private Set<Player> playersInZone() {
        Set<Player> players = new HashSet<>();
        KothArea area = areaStorage.getArea();
        if (area == null) {
            return players;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (area.contains(player.getLocation())) {
                players.add(player);
            }
        }
        return players;
    }

    private List<LeaderInfo> findWinners() {
        return findWinners(false);
    }

    private List<LeaderInfo> findWinners(boolean forced) {
        if (captureSeconds.isEmpty()) {
            return List.of();
        }
        int best = captureSeconds.values().stream().max(Integer::compareTo).orElse(0);
        if (!forced) {
            int minimum = config.getMinimumWinSeconds();
            if (best < minimum) {
                return List.of();
            }
        } else if (best <= 0) {
            return List.of();
        }
        List<LeaderInfo> winners = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : captureSeconds.entrySet()) {
            if (entry.getValue() == best) {
                String name = playerNames.getOrDefault(entry.getKey(), entry.getKey().toString());
                winners.add(new LeaderInfo(entry.getKey(), name, entry.getValue()));
            }
        }
        winners.sort(Comparator.comparing(LeaderInfo::name));
        return winners;
    }

    private LeaderInfo getLeader() {
        return captureSeconds.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new LeaderInfo(
                        entry.getKey(),
                        playerNames.getOrDefault(entry.getKey(), "Unknown"),
                        entry.getValue()
                ))
                .orElse(null);
    }

    public WinStatsStorage getWinStats() {
        return winStats;
    }

    private record LeaderInfo(UUID uuid, String name, int seconds) {
    }
}
