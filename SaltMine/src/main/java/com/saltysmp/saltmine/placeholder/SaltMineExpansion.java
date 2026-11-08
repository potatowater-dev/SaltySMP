package com.saltysmp.saltmine.placeholder;

import com.saltysmp.saltmine.SaltMinePlugin;
import com.saltysmp.saltmine.service.MineRegenService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI identifiers (prefix {@code saltmine}):
 * <ul>
 *   <li>{@code %saltmine_timer_<id>%} — reset countdown as {@code m:ss}</li>
 *   <li>{@code %saltmine_countdown_<id>%} — same as timer</li>
 * </ul>
 */
public final class SaltMineExpansion extends PlaceholderExpansion {

    private final SaltMinePlugin plugin;

    public SaltMineExpansion(SaltMinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "saltmine";
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
        MineRegenService regen = plugin.getRegenService();
        if (regen == null || params == null || params.isBlank()) {
            return "";
        }

        String key = params.toLowerCase();
        String mineId = null;
        if (key.startsWith("timer_")) {
            mineId = params.substring("timer_".length());
        } else if (key.startsWith("countdown_")) {
            mineId = params.substring("countdown_".length());
        }
        if (mineId == null || mineId.isBlank()) {
            return null;
        }
        String formatted = regen.formatRemainingMinutesSeconds(mineId);
        return formatted.isEmpty() ? "" : formatted;
    }
}
