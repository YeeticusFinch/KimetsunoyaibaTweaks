package com.lerdorf.kimetsunoyaibamultiplayer.mixin;

import com.lerdorf.kimetsunoyaibamultiplayer.util.SunlightImmunityHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {
    "net.mcreator.kimetsunoyaiba.procedures.PlayerBreathSunProcedure",
    "net.mcreator.kimetsunoyaiba.procedures.PlayerBreathHinokamiKaguraProcedure"
})
public abstract class BaseSunBreathingSunlightImmunityMixin {
    @Redirect(
        method = "execute",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerAdvancements;award(Lnet/minecraft/advancements/Advancement;Ljava/lang/String;)Z"
        ),
        require = 0
    )
    private static boolean kimetsunoyaibamultiplayer$blockOvercomeSunlightAward(
            PlayerAdvancements advancements,
            Advancement advancement,
            String criterion) {
        if (SunlightImmunityHelper.isBaseSunBreathingSunlightImmunityDisabled()
            && SunlightImmunityHelper.isOvercomeSunlightAdvancement(advancement)) {
            return false;
        }
        return advancements.award(advancement, criterion);
    }
}
