package com.potatowater.airreset;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.potatowater.airreset.mixin.AbstractWindChargeInvoker;

public final class AirResetClient implements ClientModInitializer {

    public static final String MOD_ID = "airreset";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static int settleTicksRemaining;
    private static double savedFallDistance;
    private static boolean lastAllowed = false;
    private static boolean pendingWindChargeBurst;

    @Override
    public void onInitializeClient() {
        AirResetConfig.load();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> evaluateServerGate(client, true)));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            settleTicksRemaining = 0;
            pendingWindChargeBurst = false;
            lastAllowed = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            if (pendingWindChargeBurst) {
                burstOwnWindCharges(client.player);
                pendingWindChargeBurst = false;
            }

            if (settleTicksRemaining <= 0) {
                return;
            }

            applySettle(client.player);
            settleTicksRemaining--;
        });

        LOGGER.info("AirReset client loaded");
    }

    public static boolean isEffectivelyEnabled() {
        return AirResetConfig.INSTANCE.enabled && ServerGate.isCurrentServerAllowed();
    }

    public static void evaluateServerGate(Minecraft client, boolean announce) {
        boolean allowed = ServerGate.isCurrentServerAllowed();
        if (announce && client.player != null && AirResetConfig.INSTANCE.enabled && allowed != lastAllowed) {
            if (!allowed) {
                client.player.displayClientMessage(
                        Component.literal("AirReset auto-disabled (server not on whitelist)"),
                        true
                );
            } else {
                client.player.displayClientMessage(
                        Component.literal("AirReset enabled for this server"),
                        true
                );
            }
        }
        lastAllowed = allowed;
        if (!allowed) {
            settleTicksRemaining = 0;
            pendingWindChargeBurst = false;
        }
    }

    public static boolean tryActivate(LocalPlayer player) {
        if (!isEffectivelyEnabled()) {
            return false;
        }

        if (player.onGround()) {
            return false;
        }

        if (player.getXRot() < AirResetConfig.INSTANCE.minPitch) {
            return false;
        }

        double vy = player.getDeltaMovement().y;
        if (vy > -AirResetConfig.INSTANCE.requiredDownwardVelocity) {
            return false;
        }

        savedFallDistance = Math.max(player.fallDistance, savedFallDistance);
        if (savedFallDistance <= 0.0D) {
            savedFallDistance = player.fallDistance;
        }

        var motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 0.0D, motion.z);
        player.fallDistance = Math.max(player.fallDistance, savedFallDistance);
        settleTicksRemaining = Math.max(1, AirResetConfig.INSTANCE.settleTicks);
        pendingWindChargeBurst = true;
        burstOwnWindCharges(player);
        LOGGER.debug("AirReset activated (vy was {}, fall {})", vy, savedFallDistance);

        return true;
    }

    private static void burstOwnWindCharges(LocalPlayer player) {
        var level = player.level();
        var searchBox = player.getBoundingBox().inflate(6.0D);
        for (AbstractWindCharge charge : level.getEntitiesOfClass(
                AbstractWindCharge.class,
                searchBox,
                entity -> entity.isAlive() && (entity.getOwner() == null || entity.getOwner() == player)
        )) {
            Vec3 hitPos = charge.position();
            ((AbstractWindChargeInvoker) charge).airreset$explode(hitPos);
            if (charge.isAlive()) {
                charge.discard();
            }
        }
    }

    private static void applySettle(LocalPlayer player) {
        var motion = player.getDeltaMovement();
        if (motion.y != 0.0D) {
            player.setDeltaMovement(motion.x, 0.0D, motion.z);
        }
        player.fallDistance = Math.max(player.fallDistance, savedFallDistance);
    }
}
