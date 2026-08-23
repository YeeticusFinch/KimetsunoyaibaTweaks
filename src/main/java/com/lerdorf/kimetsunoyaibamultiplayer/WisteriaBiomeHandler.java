package com.lerdorf.kimetsunoyaibamultiplayer;

import com.lerdorf.kimetsunoyaibamultiplayer.util.WisteriaResistanceHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles wisteria biome protection - prevents demons from entering wisteria forests
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class WisteriaBiomeHandler {

    // All 3 wisteria forest biome IDs
    private static final ResourceLocation WISTERIA_FOREST_CYAN =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest_cyan");
    private static final ResourceLocation WISTERIA_FOREST_CREAM =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest_cream");
    private static final ResourceLocation WISTERIA_FOREST =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "wisteria_forest");  // Default lavender+pink

    /**
     * Check if the entity is in any wisteria forest biome
     */
    private static boolean isInWisteriaForest(LivingEntity entity) {
        Holder<Biome> biome = entity.level().getBiome(entity.blockPosition());
        ResourceKey<Biome> biomeKey = biome.unwrapKey().orElse(null);

        if (biomeKey == null) {
            return false;
        }

        ResourceLocation biomeLoc = biomeKey.location();
        return biomeLoc.equals(WISTERIA_FOREST_CYAN) ||
               biomeLoc.equals(WISTERIA_FOREST_CREAM) ||
               biomeLoc.equals(WISTERIA_FOREST);
    }

    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % 20 != 0 || !Damager.isDemon(entity) || !isInWisteriaForest(entity)) {
            return;
        }

        WisteriaResistanceHelper.addWisteriaPoisonEffect(entity, 160, 0, false, true, true);
    }
}
