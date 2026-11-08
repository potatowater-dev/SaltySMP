package com.potatowater.airreset.mixin;

import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractWindCharge.class)
public interface AbstractWindChargeInvoker {

    @Invoker("explode")
    void airreset$explode(Vec3 pos);
}
