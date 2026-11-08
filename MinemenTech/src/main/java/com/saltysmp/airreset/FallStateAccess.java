package com.saltysmp.airreset;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reads/writes {@code ServerPlayer.fallStartPos}, which mace smash attacks use to
 * calculate fall height — not the Bukkit {@code fallDistance} field alone.
 */
public final class FallStateAccess {

    private static final String CRAFT_PLAYER = "org.bukkit.craftbukkit.entity.CraftPlayer";
    private static final String SERVER_PLAYER = "net.minecraft.server.level.ServerPlayer";

    private static final Method GET_HANDLE;
    private static final Field FALL_START_POS;
    private static final Constructor<?> VEC3_CONSTRUCTOR;
    private static final Field VEC_X;
    private static final Field VEC_Y;
    private static final Field VEC_Z;
    private static final boolean AVAILABLE;

    static {
        Method getHandle = null;
        Field fallStartPos = null;
        Constructor<?> vec3Constructor = null;
        Field vecX = null;
        Field vecY = null;
        Field vecZ = null;
        boolean available = false;

        try {
            Class<?> craftPlayer = Class.forName(CRAFT_PLAYER);
            getHandle = craftPlayer.getMethod("getHandle");

            Class<?> serverPlayer = Class.forName(SERVER_PLAYER);
            fallStartPos = serverPlayer.getDeclaredField("fallStartPos");
            fallStartPos.setAccessible(true);

            Class<?> vec3 = fallStartPos.getType();
            vec3Constructor = vec3.getConstructor(double.class, double.class, double.class);
            vecX = vec3.getDeclaredField("x");
            vecY = vec3.getDeclaredField("y");
            vecZ = vec3.getDeclaredField("z");
            vecX.setAccessible(true);
            vecY.setAccessible(true);
            vecZ.setAccessible(true);

            available = true;
        } catch (ReflectiveOperationException ignored) {
            // NMS layout unavailable — fall back to fallDistance only.
        }

        GET_HANDLE = getHandle;
        FALL_START_POS = fallStartPos;
        VEC3_CONSTRUCTOR = vec3Constructor;
        VEC_X = vecX;
        VEC_Y = vecY;
        VEC_Z = vecZ;
        AVAILABLE = available;
    }

    private FallStateAccess() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static FallSnapshot capture(Player player) {
        float fallDistance = player.getFallDistance();

        if (!AVAILABLE) {
            return estimateSnapshot(player, fallDistance);
        }

        try {
            Object handle = GET_HANDLE.invoke(player);
            Object fallStart = FALL_START_POS.get(handle);

            if (fallStart == null) {
                return estimateSnapshot(player, fallDistance);
            }

            return new FallSnapshot(
                    VEC_X.getDouble(fallStart),
                    VEC_Y.getDouble(fallStart),
                    VEC_Z.getDouble(fallStart),
                    fallDistance
            );
        } catch (ReflectiveOperationException exception) {
            return estimateSnapshot(player, fallDistance);
        }
    }

    public static void restore(Player player, FallSnapshot snapshot) {
        float targetFallDistance = Math.max(snapshot.fallDistance(), player.getFallDistance());
        player.setFallDistance(targetFallDistance);

        if (!AVAILABLE) {
            return;
        }

        try {
            Object handle = GET_HANDLE.invoke(player);
            Object vec = VEC3_CONSTRUCTOR.newInstance(
                    snapshot.fallStartX(),
                    snapshot.fallStartY(),
                    snapshot.fallStartZ()
            );
            FALL_START_POS.set(handle, vec);
        } catch (ReflectiveOperationException ignored) {
            // Best-effort; fallDistance restore still applied above.
        }
    }

    private static FallSnapshot estimateSnapshot(Player player, float fallDistance) {
        double y = player.getY() + Math.max(fallDistance, 0);
        return new FallSnapshot(player.getX(), y, player.getZ(), fallDistance);
    }
}
