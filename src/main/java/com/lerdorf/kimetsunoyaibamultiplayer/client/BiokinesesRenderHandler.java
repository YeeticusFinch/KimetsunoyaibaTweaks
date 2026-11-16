package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Renders Biokineses' left-hand companion without changing entity inventory. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BiokinesesRenderHandler {
    private static final String OFFHAND_MODEL_TAG = "kimetsunoyaibamultiplayer_biokineses_offhand";
    private static final double THIRD_PERSON_LEFT_HAND_OFFSET_X = 0.05D;
    private static final double THIRD_PERSON_LEFT_HAND_OFFSET_Y = 0.12D;
    private static final double THIRD_PERSON_LEFT_HAND_OFFSET_Z = -0.6D;
    private static final String[] MUZAN_HAND_ANIMATIONS = {
        "brambles",
        "left_brambles",
        "sword_to_right",
        "left_sword_to_left",
        "left_sword_to_right",
        "left_sword_overhead"
    };

    private BiokinesesRenderHandler() {
    }

    public static ItemStack createVirtualOffhandStack(ItemStack mainHand) {
        if (mainHand.isEmpty() || mainHand.getItem() != ModItems.BIOKINESES.get()) {
            return ItemStack.EMPTY;
        }
        ItemStack virtualStack = mainHand.copy();
        virtualStack.getOrCreateTag().putBoolean(OFFHAND_MODEL_TAG, true);
        return virtualStack;
    }

    public static ItemStack getVirtualOffhandStack(LivingEntity entity) {
        if (entity == null || !entity.getOffhandItem().isEmpty() || !shouldRenderVirtualOffhand(entity)) {
            return ItemStack.EMPTY;
        }
        return createVirtualOffhandStack(entity.getMainHandItem());
    }

    public static boolean shouldRenderVirtualOffhand(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (!(entity instanceof Player player)) {
            return false;
        }

        for (String animation : MUZAN_HAND_ANIMATIONS) {
            if (AnimationTracker.isAnimationActive(player.getUUID(), animation)
                || AnimationSyncHandler.isAnimationActive(player.getUUID(), animation)) {
                return true;
            }
        }
        return false;
    }

    public static void applyThirdPersonLeftHandOffset(PoseStack poseStack) {
        poseStack.translate(
            THIRD_PERSON_LEFT_HAND_OFFSET_X,
            THIRD_PERSON_LEFT_HAND_OFFSET_Y,
            THIRD_PERSON_LEFT_HAND_OFFSET_Z
        );
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            // Players use BiokinesesPlayerLayer while their arm pose is still active.
            return;
        }
        ItemStack virtualStack = getVirtualOffhandStack(entity);
        if (virtualStack.isEmpty() || !(event.getRenderer().getModel() instanceof HumanoidModel<?> model)) {
            return;
        }

        HumanoidArm offhandArm = entity instanceof net.minecraft.world.entity.player.Player player
            ? player.getMainArm().getOpposite() : HumanoidArm.LEFT;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        model.translateToHand(offhandArm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        if (offhandArm == HumanoidArm.LEFT) {
            applyThirdPersonLeftHandOffset(poseStack);
        }
        boolean leftHand = offhandArm == HumanoidArm.LEFT;
        renderItem(entity, virtualStack,
            leftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.OFF_HAND) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack virtualStack = getVirtualOffhandStack(player);
        if (virtualStack.isEmpty()) {
            return;
        }

        HumanoidArm offhandArm = player.getMainArm().getOpposite();
        boolean leftHand = offhandArm == HumanoidArm.LEFT;
        event.setCanceled(true);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        int side = leftHand ? -1 : 1;
        poseStack.translate(side * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
        renderItem(player, virtualStack,
            leftHand ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            poseStack, event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    private static void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                   PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(stack, context, packedLight, OverlayTexture.NO_OVERLAY,
            poseStack, buffer, entity.level(), entity.getId());
    }
}
