package com.saltysmp.saltkits.kit;

import com.saltysmp.saltkits.gui.KitEditorGui;
import org.bukkit.entity.Player;

public record KitEditorSession(String kitName, KitEditorGui editor, boolean isNew) {
}
