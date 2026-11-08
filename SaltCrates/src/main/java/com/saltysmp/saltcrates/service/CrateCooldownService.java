package com.saltysmp.saltcrates.service;

import com.saltysmp.saltcrates.model.CrateDefinition;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class CrateCooldownService {

    private CrateCooldownService() {
    }

    public static String cooldownKey(UUID playerId, String crateId) {
        return playerId + ":crate:" + crateId.toLowerCase();
    }

    public static long resolveOpenCooldownMs(Player player, CrateDefinition definition) {
        if (player.hasPermission("saltcrates.cooldown.bypass")) {
            return 0L;
        }
        return Math.max(0L, definition.getOpenCooldownMs());
    }
}
