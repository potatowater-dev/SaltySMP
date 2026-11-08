package com.potatowater.airreset.mixin;

import com.potatowater.airreset.AirResetClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WindChargeItem.class)
public class WindChargeItemMixin {

    @Inject(method = "use", at = @At("TAIL"))
    private void airreset$afterUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!level.isClientSide()) {
            return;
        }
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }
        AirResetClient.tryActivate(localPlayer);
    }
}
