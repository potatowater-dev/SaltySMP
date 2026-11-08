package com.saltysmp.saltkoth.placeholder;

import com.saltysmp.saltkoth.SaltKothPlugin;
import com.saltysmp.saltkoth.service.KothGameService;
import com.saltysmp.saltkoth.util.MessageUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI identifiers (prefix {@code saltkoth}):
 * <ul>
 *   <li>{@code %saltkoth_in_zone%} — yes/no</li>
 *   <li>{@code %saltkoth_active%} — yes/no</li>
 *   <li>{@code %saltkoth_paused%} — yes/no</li>
 *   <li>{@code %saltkoth_time%} — time left (active) or until next event (idle)</li>
 *   <li>{@code %saltkoth_time_left%} — formatted active event time remaining</li>
 *   <li>{@code %saltkoth_next_time%} — formatted time until next event (idle only)</li>
 *   <li>{@code %saltkoth_leader%} / {@code %saltkoth_leader_seconds%}</li>
 *   <li>{@code %saltkoth_capturer%} / {@code %saltkoth_capturer_seconds%}</li>
 *   <li>{@code %saltkoth_zone_line%} — sidebar line when inside the KOTH zone (empty outside)</li>
 *   <li>{@code %saltkoth_outside_line%} — sidebar line when outside the zone (empty inside during active)</li>
 * </ul>
 */
public final class SaltKothExpansion extends PlaceholderExpansion {

    private final SaltKothPlugin plugin;

    public SaltKothExpansion(SaltKothPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "saltkoth";
    }

    @Override
    public String getAuthor() {
        return "SaltySMP";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        KothGameService game = plugin.getGameService();
        if (game == null) {
            return "";
        }

        boolean inZone = player != null && game.isInZone(player);
        boolean eventActive = game.isActive();
        long timeLeft = game.getActiveTimeLeftMs();
        long nextTime = game.getNextEventTimeMs();
        String timeLeftFormatted = MessageUtil.formatDuration(timeLeft);
        String nextTimeFormatted = MessageUtil.formatDuration(nextTime);

        return switch (params.toLowerCase()) {
            case "in_zone" -> inZone ? "yes" : "no";
            case "active" -> eventActive ? "yes" : "no";
            case "paused" -> game.isPaused() ? "yes" : "no";
            case "time" -> eventActive ? timeLeftFormatted : nextTimeFormatted;
            case "time_left" -> timeLeftFormatted;
            case "next_time" -> eventActive ? "" : nextTimeFormatted;
            case "leader" -> game.getLeaderName();
            case "leader_seconds" -> String.valueOf(game.getLeaderSeconds());
            case "capturer" -> game.getCapturerName();
            case "capturer_seconds" -> String.valueOf(game.getCapturerSeconds());
            case "zone_line" -> buildZoneLine(inZone, eventActive, timeLeftFormatted, nextTimeFormatted, game);
            case "outside_line" -> buildOutsideLine(inZone, eventActive, timeLeftFormatted, nextTimeFormatted);
            default -> null;
        };
    }

    private String buildZoneLine(
            boolean inZone,
            boolean eventActive,
            String timeLeftFormatted,
            String nextTimeFormatted,
            KothGameService game
    ) {
        if (!inZone) {
            return "";
        }
        if (eventActive) {
            String capturer = game.getCapturerName();
            if ("—".equals(capturer)) {
                return "KOTH | " + timeLeftFormatted + " left";
            }
            return "KOTH | " + timeLeftFormatted + " | " + capturer;
        }
        return "Next KOTH in " + nextTimeFormatted;
    }

    private String buildOutsideLine(
            boolean inZone,
            boolean eventActive,
            String timeLeftFormatted,
            String nextTimeFormatted
    ) {
        if (inZone && eventActive) {
            return "";
        }
        if (eventActive) {
            return "KOTH Active | " + timeLeftFormatted + " left";
        }
        return "Next KOTH in " + nextTimeFormatted;
    }
}
