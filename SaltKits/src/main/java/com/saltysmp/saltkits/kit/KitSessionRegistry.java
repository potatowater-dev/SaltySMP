package com.saltysmp.saltkits.kit;

import com.saltysmp.saltkits.gui.KitClaimMenu;
import com.saltysmp.saltkits.gui.KitEditorGui;
import com.saltysmp.saltkits.gui.KitPreviewGui;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KitSessionRegistry {

    public enum SessionType {
        EDITOR,
        MENU,
        PREVIEW
    }

    public record Session(SessionType type, Object gui, boolean discardOnClose) {
        public Session(SessionType type, Object gui) {
            this(type, gui, false);
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public Optional<Session> getSession(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public void setEditor(Player player, KitEditorGui gui) {
        sessions.put(player.getUniqueId(), new Session(SessionType.EDITOR, gui));
    }

    public void setMenu(Player player, KitClaimMenu menu) {
        sessions.put(player.getUniqueId(), new Session(SessionType.MENU, menu));
    }

    public void setPreview(Player player, KitPreviewGui preview) {
        sessions.put(player.getUniqueId(), new Session(SessionType.PREVIEW, preview));
    }

    public void markDiscard(UUID playerId) {
        Session session = sessions.get(playerId);
        if (session != null) {
            sessions.put(playerId, new Session(session.type(), session.gui(), true));
        }
    }

    public void clear(UUID playerId) {
        sessions.remove(playerId);
    }

    public void clearAll() {
        sessions.clear();
    }
}
