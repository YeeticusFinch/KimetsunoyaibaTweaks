package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SunlightImmunityHelper;
import net.mcreator.kimetsunoyaiba.procedures.NikkoyakeProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the base mod's sunlight procedure evaluate mounted demons using both
 * the rider and vehicle positions. The base procedure otherwise only checks
 * the position supplied by its caller, which can be inside a seat entity.
 */
@Mixin(targets = "net.mcreator.kimetsunoyaiba.procedures.TestNikkoProcedure")
public abstract class BaseSunlightVehicleMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void kimetsunoyaibamultiplayer$handleMountedDemon(
            LevelAccessor world,
            double x,
            double y,
            double z,
            Entity entity,
            CallbackInfo callbackInfo) {
        if (!(entity instanceof LivingEntity living)
            || !entity.isPassenger()
            || !Damager.isDemon(living)) {
            return;
        }

        if (SunlightImmunityHelper.isEnclosedVehicle(entity.getVehicle())) {
            callbackInfo.cancel();
            return;
        }

        if (living instanceof net.minecraft.world.entity.player.Player) {
            return;
        }

        if (SunlightImmunityHelper.isInBurningSunlight(living)) {
            NikkoyakeProcedure.execute(world, x, y, z, entity);
            callbackInfo.cancel();
        }
    }
}
