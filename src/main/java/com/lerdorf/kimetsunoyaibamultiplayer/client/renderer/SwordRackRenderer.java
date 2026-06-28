package com.lerdorf.kimetsunoyaibamultiplayer.client.renderer;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.SwordRackBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.SwordRackBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SheathModelRenderer;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordSheathRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordRackConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordLoveAnimated;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the sword rack block model and overlays the stored swords.
 *
 * The rack models in this repo are vanilla Blockbench block models, not GeckoLib geo
 * assets, so we render them through the block renderer and keep sword placement as a
 * separate pass.
 */
public class SwordRackRenderer implements BlockEntityRenderer<SwordRackBlockEntity> {
    public SwordRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SwordRackBlockEntity rack, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = rack.getBlockState();

        poseStack.pushPose();
        applyRackOrientation(state, poseStack);
        renderRackModel(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        for (int slot = 0; slot < rack.getContainerSize() && slot < 3; slot++) {
            ItemStack swordStack = rack.getItem(slot);
            if (swordStack.isEmpty()) {
                continue;
            }
            if (!com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingInfoDetector.isNichirinSword(swordStack)) {
                continue;
            }

            renderSwordSlot(rack, slot, swordStack, state, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderRackModel(BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(),
            bufferSource.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)),
            state,
            minecraft.getBlockRenderer().getBlockModel(state),
            1.0F,
            1.0F,
            1.0F,
            packedLight,
            packedOverlay
        );
    }

    private static void renderSwordSlot(SwordRackBlockEntity rack, int slot, ItemStack swordStack, BlockState state,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        SwordRackConfig.RackTransform transform = SwordRackConfig.getTransform(
            rack.getBlockState().getValue(SwordRackBlock.WALL),
            slot
        );

        poseStack.pushPose();
        applyRackOrientation(state, poseStack);
        poseStack.translate(transform.translateX(), transform.translateY(), transform.translateZ());
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) transform.rotateZ()));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) transform.rotateY()));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) transform.rotateX()));

        applySwordDisplayOffsets(poseStack, swordStack);

        Item sheathItem = SwordSheathRegistry.getSheathItem(swordStack);
        ItemStack swordForDisplay = SwordSheathRegistry.getSheathDisplayItem(swordStack);
        if (swordForDisplay.isEmpty()) {
            swordForDisplay = swordStack;
        }
        swordForDisplay = prepareRackSwordStack(swordForDisplay);
        if (isKokushiboSword(swordStack)) {
            Item kokushiboDisplay = resolveKokushiboDisplayItem();
            if (kokushiboDisplay != null) {
                swordForDisplay = new ItemStack(kokushiboDisplay);
                swordForDisplay = prepareRackSwordStack(swordForDisplay);
                Item kokushiboSheath = SwordSheathRegistry.getSheathItem(swordForDisplay);
                if (kokushiboSheath != null) {
                    sheathItem = kokushiboSheath;
                }
            }
        }

        if (SwordDisplayConfig.renderSheaths && sheathItem != null && !SwordParticleMapping.isSheathExempt(swordStack)) {
            SheathModelRenderer.renderSheath(sheathItem, poseStack, bufferSource, packedLight, rack.getBlockPos().hashCode() + slot);
        }

        Minecraft.getInstance().getItemRenderer().renderStatic(
            swordForDisplay,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            bufferSource,
            Minecraft.getInstance().level,
            rack.getBlockPos().hashCode() * 31 + slot
        );
        poseStack.popPose();
    }

    private static void applySwordDisplayOffsets(PoseStack poseStack, ItemStack swordStack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordStack.getItem());
        SwordDisplayConfig.SwordOffsets offsets = itemId == null ? null : SwordDisplayConfig.getSwordOffsets(itemId.toString());
        if (offsets != null) {
            poseStack.translate(offsets.translateX, offsets.translateY, offsets.translateZ);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) offsets.rotateZ));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) offsets.rotateY));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) offsets.rotateX));
        }

        poseStack.scale((float) SwordDisplayConfig.scale, (float) SwordDisplayConfig.scale, (float) SwordDisplayConfig.scale);
    }

    private static void applyRackOrientation(BlockState state, PoseStack poseStack) {
        poseStack.translate(0.5D, 0.5D, 0.5D);

        if (state.hasProperty(SwordRackBlock.WALL) && state.getValue(SwordRackBlock.WALL)
            && state.hasProperty(SwordRackBlock.FACING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(wallVisualYaw(state.getValue(SwordRackBlock.FACING))));
        } else if (state.hasProperty(SwordRackBlock.ROTATION)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(floorVisualYaw(state.getValue(SwordRackBlock.ROTATION))));
        }

        poseStack.translate(-0.5D, -0.5D, -0.5D);
    }

    private static float floorVisualYaw(int rotation) {
        return 180.0F - (rotation * 22.5F);
    }

    private static float wallVisualYaw(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case SOUTH -> 180.0F;
            case EAST -> 270.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static ItemStack prepareRackSwordStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        if (copy.getItem() instanceof NichirinSwordLoveAnimated) {
            NichirinSwordLoveAnimated.setAnimationOnStack(copy, "sheath");
            return copy;
        }
        if (copy.getItem() instanceof NichirinSwordKanrojiAnimated) {
            NichirinSwordKanrojiAnimated.setAnimationOnStack(copy, "sheath");
            return copy;
        }

        return copy;
    }

    private static boolean isKokushiboSword(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null
            && "kimetsunoyaiba".equals(id.getNamespace())
            && id.getPath().startsWith("sword_kokushibo");
    }

    private static Item resolveKokushiboDisplayItem() {
        Item kokushiboSword = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sword_kokushibo_1"));
        if (kokushiboSword != null) {
            return kokushiboSword;
        }

        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "sword_kokushibo_2"));
    }
}
