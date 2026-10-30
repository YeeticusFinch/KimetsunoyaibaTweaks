package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/** Registers colored block lights when the optional Shimmer mod is installed. */
@Mod.EventBusSubscriber(
    modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class ShimmerCompat {
    private static final String SHIMMER_MOD_ID = "shimmer";
    private static final String LIGHT_MANAGER_CLASS = "com.lowdragmc.shimmer.client.light.LightManager";
    private static final String POST_PROCESSING_CLASS = "com.lowdragmc.shimmer.client.postprocessing.PostProcessing";

    private static final Map<Integer, Object> ENTITY_LIGHTS = new HashMap<>();

    private static Object lightManager;
    private static Object bloomPost;
    private static Method registerBlockLight;
    private static Method addLight;
    private static Method postEntityForce;
    private static Method setPos;
    private static Method isRemoved;
    private static Method remove;
    private static ClientLevel lastLevel;
    private static boolean apiFailureReported;

    private ShimmerCompat() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded(SHIMMER_MOD_ID)) {
            return;
        }

        event.enqueueWork(() -> {
            try {
                registerLights();
            } catch (Throwable throwable) {
                System.err.println("[Kimetsunoyaiba Tweaks] Shimmer light registration failed: "
                    + throwable.getMessage());
            }
        });
    }

    private static void registerLights() {
        if (!initializeApi()) {
            return;
        }

        // Lamp colors match the existing Shimmer config color references.
        register(ModBlocks.SIDEWAYS_LANTERN_1.get(), 0xDFD7CD, 14.0F);
        register(ModBlocks.SIDEWAYS_LANTERN_2.get(), 0xF3F3ED, 11.0F);
        register(ModBlocks.SIDEWAYS_LANTERN_2RED.get(), 0xFF5955, 12.0F);
        register(ModBlocks.SIDEWAYS_LANTERN_3.get(), 0xDBD0C3, 14.0F);
        register(ModBlocks.LANTERN_TSUZUMI.get(), 0xFF9C51, 18.0F);
        register(ModBlocks.HANGING_LAMP.get(), 0xFFFFFF, 14.0F);
        register(ModBlocks.DIM_ORANGE_LAMP.get(), 0xFF6128, 7.0F);

        // Only the glowing wisteria variants emit colored light.
        register(ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get(), 0xE7A2F2, 4.0F);
        register(ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get(), 0xA2D2F2, 4.0F);
        register(ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get(), 0xB9A3F2, 4.0F);
        register(ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get(), 0xF2F0A2, 4.0F);
        register(ModBlocks.GLOWING_WISTERIA_PETALS_PINK.get(), 0xE7A2F2, 10.0F);
        register(ModBlocks.GLOWING_WISTERIA_PETALS_CYAN.get(), 0xA2D2F2, 10.0F);
        register(ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get(), 0xB9A3F2, 10.0F);
        register(ModBlocks.GLOWING_WISTERIA_PETALS_CREAM.get(), 0xF2F0A2, 10.0F);
    }

    private static void register(Block block, int color, float radius) {
        try {
            registerBlockLight.invoke(lightManager, block, color, radius);
        } catch (ReflectiveOperationException exception) {
            reportApiFailure(exception);
        }
    }

    /** Adds this entity to Shimmer's bloom pass without making Shimmer mandatory. */
    public static void postEntityBloom(Consumer<MultiBufferSource> renderer) {
        if (!isShimmerLoaded() || !initializeApi()) {
            return;
        }

        try {
            postEntityForce.invoke(bloomPost, renderer);
        } catch (ReflectiveOperationException exception) {
            reportApiFailure(exception);
        }
    }

    /** Updates one short-lived colored light at the entity's interpolated position. */
    public static void updateEntityLight(Entity entity, float partialTick, int color, float radius) {
        if (!isShimmerLoaded() || !initializeApi()) {
            return;
        }

        cleanupEntityLights();
        try {
            Object light = ENTITY_LIGHTS.get(entity.getId());
            if (light == null || (boolean) isRemoved.invoke(light)) {
                Vec3Position position = new Vec3Position(entity.getPosition(partialTick));
                light = addLight.invoke(lightManager, new Vector3f(position.x, position.y, position.z), color, radius);
                if (light == null) {
                    return;
                }
                ENTITY_LIGHTS.put(entity.getId(), light);
            } else {
                Vec3Position position = new Vec3Position(entity.getPosition(partialTick));
                setPos.invoke(light, position.x, position.y, position.z);
            }
        } catch (ReflectiveOperationException exception) {
            reportApiFailure(exception);
        }
    }

    private static boolean isShimmerLoaded() {
        return ModList.get().isLoaded(SHIMMER_MOD_ID);
    }

    private static boolean initializeApi() {
        if (lightManager != null && bloomPost != null) {
            return true;
        }

        try {
            Class<?> lightManagerType = Class.forName(LIGHT_MANAGER_CLASS);
            Field lightManagerField = lightManagerType.getField("INSTANCE");
            lightManager = lightManagerField.get(null);
            registerBlockLight = lightManagerType.getMethod("registerBlockLight", Block.class, int.class, float.class);
            addLight = lightManagerType.getMethod("addLight", Vector3f.class, int.class, float.class);

            Class<?> postProcessingType = Class.forName(POST_PROCESSING_CLASS);
            Field bloomField = postProcessingType.getField("BLOOM_UNREAL");
            bloomPost = bloomField.get(null);
            postEntityForce = postProcessingType.getMethod("postEntityForce", Consumer.class);

            Class<?> colorPointLightType = Class.forName("com.lowdragmc.shimmer.client.light.ColorPointLight");
            setPos = colorPointLightType.getMethod("setPos", float.class, float.class, float.class);
            isRemoved = colorPointLightType.getMethod("isRemoved");
            remove = colorPointLightType.getMethod("remove");
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            reportApiFailure(exception);
            return false;
        }
    }

    private static void cleanupEntityLights() {
        Minecraft minecraft = Minecraft.getInstance();
        if (lastLevel != minecraft.level) {
            removeAllEntityLights();
            lastLevel = minecraft.level;
            return;
        }
        if (minecraft.level == null) {
            return;
        }

        Iterator<Map.Entry<Integer, Object>> iterator = ENTITY_LIGHTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Object> entry = iterator.next();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            try {
                if (entity == null || entity.isRemoved() || (boolean) isRemoved.invoke(entry.getValue())) {
                    remove.invoke(entry.getValue());
                    iterator.remove();
                }
            } catch (ReflectiveOperationException exception) {
                reportApiFailure(exception);
                iterator.remove();
            }
        }
    }

    private static void removeAllEntityLights() {
        for (Object light : ENTITY_LIGHTS.values()) {
            try {
                remove.invoke(light);
            } catch (ReflectiveOperationException exception) {
                reportApiFailure(exception);
            }
        }
        ENTITY_LIGHTS.clear();
    }

    private static void reportApiFailure(Throwable throwable) {
        if (!apiFailureReported) {
            apiFailureReported = true;
            System.err.println("[Kimetsunoyaiba Tweaks] Shimmer compatibility failed: "
                + throwable.getMessage());
        }
    }

    private static final class Vec3Position {
        private final float x;
        private final float y;
        private final float z;

        private Vec3Position(net.minecraft.world.phys.Vec3 position) {
            this.x = (float) position.x;
            this.y = (float) position.y;
            this.z = (float) position.z;
        }
    }
}
