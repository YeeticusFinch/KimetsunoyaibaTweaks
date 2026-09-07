package com.lerdorf.kimetsunoyaibamultiplayer.entities.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EntitySkinLayersConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Applies 3D Skin Layers' voxel meshes to GeckoLib biped models.
 *
 * The dependency is deliberately resolved at runtime because 3D Skin Layers is
 * a client-side optional mod. Its public mesh API and config are used when it
 * is present, rather than copying either implementation into this mod.
 */
public class SkinLayersGeoLayer<T extends net.minecraft.world.entity.LivingEntity & GeoAnimatable>
        extends GeoRenderLayer<T> {

    private static final String HEAD = "head";
    private static final String ARMOR_HEAD = "armorHead";
    private static final String TORSO = "torso";
    private static final String RIGHT_ARM = "right_arm";
    private static final String LEFT_ARM = "left_arm";
    private static final String RIGHT_LEG = "right_leg";
    private static final String LEFT_LEG = "left_leg";
    private static final int SIMPLE_RENDER = 0;
    private static final int INTEGER_COLOR_RENDER = 1;
    private static final int FLOAT_COLOR_RENDER = 2;

    private static final Map<MeshKey, MeshSet> MESHES = new HashMap<>();
    private static MeshApi meshApi;
    private static boolean apiChecked;
    private static boolean apiDiagnosticLogged;
    private static boolean renderDiagnosticLogged;
    private static boolean meshDiagnosticLogged;

    public SkinLayersGeoLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        // Bone-attached meshes are rendered from renderForBone, where GeckoLib
        // has already applied the active bone and parent transforms.
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        MeshApi api = getMeshApi();
        if (api == null || !withinRenderDistance(animatable)) {
            return;
        }

        ResourceLocation texture = getRenderer().getTextureLocation(animatable);
        if (!renderDiagnosticLogged) {
            renderDiagnosticLogged = true;
            Log.alwaysWarn("3D skin layer render reached for {} with texture {}",
                    animatable.getClass().getName(), texture);
        }
        BakedGeoModel model = getDefaultBakedModel(animatable);
        MeshSet meshes = getMeshes(texture, model, api);
        if (meshes == null) {
            return;
        }

        VertexConsumer skinBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        int overlay = packedOverlay == OverlayTexture.NO_OVERLAY
                ? OverlayTexture.NO_OVERLAY
                : LivingEntityRenderer.getOverlayCoords(animatable, 0.0F);

        switch (bone.getName()) {
            case ARMOR_HEAD -> renderHead(poseStack, meshes.head, skinBuffer, packedLight, overlay, api);
            case HEAD -> {
                if (model.getBone(ARMOR_HEAD).isEmpty()) {
                    renderHead(poseStack, meshes.head, skinBuffer, packedLight, overlay, api);
                }
            }
            case TORSO -> renderBodyPart(poseStack, TORSO, meshes.torso, skinBuffer, packedLight, overlay, api,
                    configBoolean("enableJacket", true), 0.0F, true);
            case RIGHT_ARM -> renderBodyPart(poseStack, RIGHT_ARM, meshes.rightArm, skinBuffer, packedLight, overlay, api,
                    configBoolean("enableRightSleeve", true), -0.998F, false);
            case LEFT_ARM -> renderBodyPart(poseStack, LEFT_ARM, meshes.leftArm, skinBuffer, packedLight, overlay, api,
                    configBoolean("enableLeftSleeve", true), 0.998F, false);
            case RIGHT_LEG -> renderBodyPart(poseStack, RIGHT_LEG, meshes.rightLeg, skinBuffer, packedLight, overlay, api,
                    configBoolean("enableRightPants", true), 0.0F, false);
            case LEFT_LEG -> renderBodyPart(poseStack, LEFT_LEG, meshes.leftLeg, skinBuffer, packedLight, overlay, api,
                    configBoolean("enableLeftPants", true), 0.0F, false);
            default -> {
            }
        }
    }

    private void renderHead(PoseStack poseStack, MeshPart mesh, VertexConsumer buffer,
                            int light, int overlay, MeshApi api) {
        if (mesh == null || !configBoolean("enableHat", true)) {
            return;
        }

        poseStack.pushPose();
        applyPartOffsets(poseStack, EntitySkinLayersConfig.HEAD);
        float size = configFloat("headVoxelSize", 1.18F);
        poseStack.translate(0.0F, -0.25F, 0.0F);
        poseStack.scale(size, size, size);
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.translate(0.0F, -0.04F, 0.0F);
        api.render(mesh.mesh, poseStack, buffer, light, overlay);
        poseStack.popPose();
    }

    private void renderBodyPart(PoseStack poseStack, String boneName, MeshPart mesh,
                                VertexConsumer buffer, int light, int overlay, MeshApi api, boolean enabled,
                                float x, boolean torso) {
        if (mesh == null || !enabled) {
            return;
        }

        poseStack.pushPose();
        applyPartOffsets(poseStack, configFor(boneName));
        float width = torso ? configFloat("bodyVoxelWidthSize", 1.05F)
                : configFloat("baseVoxelSize", 1.15F);
        poseStack.scale(width, 1.035F, configFloat("baseVoxelSize", 1.15F));
        float positionX = torso || boneName.endsWith("leg") ? 0.0F : armPosition(mesh.width, x);
        float positionY = boneName.equals(RIGHT_ARM) || boneName.equals(LEFT_ARM)
                ? -0.1F
                : -0.2F;
        api.setPosition(mesh.mesh, positionX, positionY, 0.0F);
        api.render(mesh.mesh, poseStack, buffer, light, overlay);
        poseStack.popPose();
    }

    private static EntitySkinLayersConfig.PartConfig configFor(String boneName) {
        return switch (boneName) {
            case TORSO -> EntitySkinLayersConfig.TORSO;
            case RIGHT_ARM -> EntitySkinLayersConfig.RIGHT_ARM;
            case LEFT_ARM -> EntitySkinLayersConfig.LEFT_ARM;
            case RIGHT_LEG -> EntitySkinLayersConfig.RIGHT_LEG;
            case LEFT_LEG -> EntitySkinLayersConfig.LEFT_LEG;
            default -> EntitySkinLayersConfig.TORSO;
        };
    }

    private static void applyPartOffsets(PoseStack poseStack, EntitySkinLayersConfig.PartConfig config) {
        poseStack.translate(config.translateX(), config.translateY(), config.translateZ());
        poseStack.mulPose(Axis.XP.rotationDegrees(config.rotateX()));
        poseStack.mulPose(Axis.YP.rotationDegrees(config.rotateY()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(config.rotateZ()));
    }

    private static float armPosition(int width, float standardPosition) {
        if (width == 4) {
            return standardPosition;
        }
        return standardPosition * ((width - 2.0F) / 2.0F);
    }

    private static boolean withinRenderDistance(net.minecraft.world.entity.LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getCameraEntity() == null) {
            return false;
        }
        int distance = configInt("renderDistanceLOD", 14);
        return entity.distanceToSqr(minecraft.getCameraEntity()) <= (double) distance * distance;
    }

    private static MeshSet getMeshes(ResourceLocation texture, BakedGeoModel model, MeshApi api) {
        synchronized (MESHES) {
            MeshKey key = new MeshKey(texture, model);
            MeshSet cached = MESHES.get(key);
            if (cached != null) {
                return cached;
            }

            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isEmpty()) {
                Log.alwaysWarn("3D skin layer texture resource is missing: {}", texture);
                return null;
            }

            try (NativeImage source = NativeImage.read(resource.get().open())) {
                NativeImage image = normalizeTexture(source);
                if (image == null) {
                    Log.alwaysWarn("3D skin layer texture dimensions are unsupported: {}x{} for {}",
                            source.getWidth(), source.getHeight(), texture);
                    return null;
                }
                try {
                    MeshSet created = createMeshes(image, model, api);
                    if (!meshDiagnosticLogged) {
                        meshDiagnosticLogged = true;
                        Log.alwaysWarn("3D skin layer meshes for {}: head={}, torso={}, rightArm={}, leftArm={}, rightLeg={}, leftLeg={}",
                                texture, created.head != null, created.torso != null, created.rightArm != null,
                                created.leftArm != null, created.rightLeg != null, created.leftLeg != null);
                    }
                    if (created.hasAny()) {
                        MESHES.put(key, created);
                        return created;
                    }
                } finally {
                    if (image != source) {
                        image.close();
                    }
                }
            } catch (Throwable throwable) {
                Log.alwaysWarn("Could not create 3D skin layer meshes for {}: {}", texture, throwable.toString());
            }
            return null;
        }
    }

    private static NativeImage normalizeTexture(NativeImage source) {
        if (source.getWidth() == 64 && source.getHeight() == 64) {
            return source;
        }
        if (source.getWidth() != source.getHeight() || source.getWidth() % 64 != 0) {
            return null;
        }

        int scale = source.getWidth() / 64;
        NativeImage normalized = new NativeImage(NativeImage.Format.RGBA, 64, 64, false);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                normalized.setPixelRGBA(x, y, source.getPixelRGBA(x * scale, y * scale));
            }
        }
        return normalized;
    }

    private static MeshSet createMeshes(NativeImage image, BakedGeoModel model, MeshApi api)
            throws ReflectiveOperationException {
        int armWidth = detectArmWidth(model);
        return new MeshSet(
                new MeshPart(api.create(image, 8, 8, 8, 32, 0, false, 0.6F), 8),
                new MeshPart(api.create(image, 8, 12, 4, 16, 32, true, 0.0F), 8),
                new MeshPart(api.create(image, armWidth, 12, 4, 40, 32, true, -2.0F), armWidth),
                new MeshPart(api.create(image, armWidth, 12, 4, 48, 48, true, -2.0F), armWidth),
                new MeshPart(api.create(image, 4, 12, 4, 0, 32, true, 0.0F), 4),
                new MeshPart(api.create(image, 4, 12, 4, 0, 48, true, 0.0F), 4));
    }

    private static int detectArmWidth(BakedGeoModel model) {
        GeoBone bone = model.getBone(RIGHT_ARM).orElse(null);
        if (bone != null) {
            for (GeoCube cube : bone.getCubes()) {
                int width = (int) Math.round(cube.size().x);
                if (width == 3 || width == 4) {
                    return width;
                }
            }
        }
        return 4;
    }

    private static MeshApi getMeshApi() {
        if (apiChecked) {
            return meshApi;
        }

        apiChecked = true;

        try {
            Class<?> apiClass =
                    Class.forName("dev.tr7zw.skinlayers.api.SkinLayersAPI");
            Class<?> helperClass =
                    Class.forName("dev.tr7zw.skinlayers.api.MeshHelper");
            Class<?> meshClass =
                    Class.forName("dev.tr7zw.skinlayers.api.Mesh");

            Object helper =
                    apiClass.getMethod("getMeshHelper").invoke(null);

            Method create;
            try {
                create = helperClass.getMethod(
                        "create3DMesh", NativeImage.class, int.class, int.class, int.class,
                        int.class, int.class, boolean.class, float.class);
            } catch (NoSuchMethodException exception) {
                create = helperClass.getMethod(
                        "create3DMesh", NativeImage.class, int.class, int.class, int.class,
                        int.class, int.class, boolean.class, float.class, boolean.class);
            }

            Method render;
            int renderMode;
            try {
                render = meshClass.getMethod("render", PoseStack.class, VertexConsumer.class,
                        int.class, int.class);
                renderMode = SIMPLE_RENDER;
            } catch (NoSuchMethodException simpleRenderMissing) {
                try {
                    render = meshClass.getMethod("render", net.minecraft.client.model.geom.ModelPart.class,
                            PoseStack.class, VertexConsumer.class, int.class, int.class, int.class);
                    renderMode = INTEGER_COLOR_RENDER;
                } catch (NoSuchMethodException integerRenderMissing) {
                    render = meshClass.getMethod("render", net.minecraft.client.model.geom.ModelPart.class,
                            PoseStack.class, VertexConsumer.class, int.class, int.class,
                            float.class, float.class, float.class, float.class);
                    renderMode = FLOAT_COLOR_RENDER;
                }
            }

            Method setPosition = meshClass.getMethod(
                    "setPosition",
                    float.class,
                    float.class,
                    float.class
            );

            meshApi = new MeshApi(helper, create, render, renderMode, setPosition);

            Log.alwaysWarn("3D Skin Layers API integration loaded: createArgs={}, renderArgs={}, renderMode={}",
                    create.getParameterCount(), render.getParameterCount(), renderMode);

        } catch (Throwable throwable) {
            meshApi = null;
            Log.alwaysWarn(
                    "3D Skin Layers API integration unavailable: {}",
                    throwable.toString()
            );
        }

        return meshApi;
    }

    private static Object configValue(String name) {
        try {
            Object config = getConfigObject();
            if (config == null) {
                return null;
            }
            Field field = config.getClass().getField(name);
            return field.get(config);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getConfigObject() {
        String[] configOwners = {
                "dev.tr7zw.skinlayers.SkinLayersModBase",
                "dev.tr7zw.skinlayers.versionless.ModBase"
        };
        for (String owner : configOwners) {
            try {
                Field field = Class.forName(owner).getField("config");
                Object config = field.get(null);
                if (config != null) {
                    return config;
                }
            } catch (Throwable ignored) {
                // Try the other version's config owner.
            }
        }
        return null;
    }

    private static boolean configBoolean(String name, boolean fallback) {
        Object value = configValue(name);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private static int configInt(String name, int fallback) {
        Object value = configValue(name);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static float configFloat(String name, float fallback) {
        Object value = configValue(name);
        return value instanceof Number ? ((Number) value).floatValue() : fallback;
    }

    private record MeshKey(ResourceLocation texture, BakedGeoModel model) {
    }

    private record MeshPart(Object mesh, int width) {
    }

    private record MeshSet(MeshPart head, MeshPart torso, MeshPart rightArm, MeshPart leftArm,
                            MeshPart rightLeg, MeshPart leftLeg) {
        private boolean hasAny() {
            return head != null || torso != null || rightArm != null || leftArm != null
                    || rightLeg != null || leftLeg != null;
        }
    }

    private record MeshApi(
            Object helper,
            Method create,
            Method render,
            int renderMode,
            Method setPosition
    ) {

        private Object create(
                NativeImage image,
                int width,
                int height,
                int depth,
                int u,
                int v,
                boolean topPivot,
                float rotationOffset
        ) throws ReflectiveOperationException {

            Object[] arguments = create.getParameterCount() == 9
                    ? new Object[]{image, width, height, depth, u, v, topPivot, rotationOffset, false}
                    : new Object[]{image, width, height, depth, u, v, topPivot, rotationOffset};
            return create.invoke(helper, arguments);
        }

        private void setPosition(
                Object mesh,
                float x,
                float y,
                float z
        ) {
            try {
                setPosition.invoke(mesh, x, y, z);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Unable to position 3D skin mesh",
                        exception
                );
            }
        }

        private void render(
                Object mesh,
                PoseStack poseStack,
                VertexConsumer buffer,
                int light,
            int overlay
        ) {
            try {
                if (renderMode == SIMPLE_RENDER) {
                    render.invoke(mesh, poseStack, buffer, light, overlay);
                } else if (renderMode == INTEGER_COLOR_RENDER) {
                    render.invoke(mesh, null, poseStack, buffer, light, overlay, 0xFFFFFFFF);
                } else {
                    render.invoke(mesh, null, poseStack, buffer, light, overlay,
                            1.0F, 1.0F, 1.0F, 1.0F);
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Unable to render 3D skin mesh",
                        exception
                );
            }
        }
    }
}
