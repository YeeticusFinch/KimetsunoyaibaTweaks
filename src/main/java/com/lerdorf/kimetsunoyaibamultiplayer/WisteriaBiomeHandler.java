package com.lerdorf.kimetsunoyaibamultiplayer;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

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

    /**
     * DISABLED: Biome-wide damage has been removed.
     * Only the wisteria blocks themselves (leaves and petals) should damage demons,
     * not the biome as a whole. This allows demons to safely fly above or dig under
     * wisteria forests without being affected.
     */
    // @SubscribeEvent
    // public static void onEntityTick(LivingEvent.LivingTickEvent event) {
    //     // This event handler has been disabled.
    //     // Wisteria protection now only comes from direct contact with wisteria blocks.
    // }
}
