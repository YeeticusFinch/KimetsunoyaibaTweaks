package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Legacy event-based renderer for entity sword/sheath display (disabled).
 * Replaced by GeoSwordDisplayLayer on GeckoLib entity renderers.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntitySwordDisplayRenderer {

    // Temporarily store swords that we hide during rendering
    private static final ThreadLocal<ItemStack> hiddenSword = ThreadLocal.withInitial(() -> ItemStack.EMPTY);
    // Legacy event-based renderer disabled in favor of GeoRenderLayer integration.
    private static final boolean ENABLE_LEGACY_EVENT_RENDERER = false;

    /**
     * BEFORE rendering: Hide sword from hand if entity is not in combat.
     * We temporarily clear the mainhand slot so the renderer doesn't draw it.
     */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<LivingEntity, ?> event) {
        if (!ENABLE_LEGACY_EVENT_RENDERER) {
            hiddenSword.set(ItemStack.EMPTY);
            return;
        }

        LivingEntity entity = event.getEntity();

        // CRITICAL: Skip players - they have their own sword display system
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            hiddenSword.set(ItemStack.EMPTY);
            return;
        }

        // Check if entity has a nichirin sword
        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!SwordParticleMapping.isKimetsunoyaibaSword(mainHand)) {
            hiddenSword.set(ItemStack.EMPTY);
            return;
        }

        // Update combat state
        EntityCombatStateTracker.updateCombatState(entity);
        boolean inCombat = EntityCombatStateTracker.isInCombat(entity);

        // If not in combat, hide the sword from hand
        if (!inCombat) {
            Log.debug("Hiding sword from {} hand (not in combat)", entity.getType().getDescriptionId());
            hiddenSword.set(mainHand);
            entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        } else {
            hiddenSword.set(ItemStack.EMPTY);
        }
    }

    /**
     * AFTER rendering: Restore the sword if we hid it, and render sword/sheath on back/hip.
     */
    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<LivingEntity, ?> event) {
        if (!ENABLE_LEGACY_EVENT_RENDERER) {
            return;
        }

        LivingEntity entity = event.getEntity();

        // CRITICAL: Skip players - they have their own sword display system
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return;
        }

        // REMOVED GeoAnimatable check - base mod entities aren't GeoAnimatable!
        // Now we process ANY entity with a nichirin sword

        // Only process if sword display is enabled
        if (!SwordDisplayConfig.enabled) {
            Log.debug("  Sword display disabled in config");
            return;
        }

        // Only process entities with nichirin swords
        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!SwordParticleMapping.isKimetsunoyaibaSword(mainHand)) {
            //Log.debug("  Not a KnY sword: {}", mainHand.isEmpty() ? "empty" : mainHand.getDescriptionId());
            return;
        }

        Log.debug("EntitySwordDisplayRenderer: Rendering for {} with sword {}",
            entity.getType().getDescriptionId(), mainHand.getDescriptionId());

        // Update combat state for this entity
        EntityCombatStateTracker.updateCombatState(entity);
        boolean inCombat = EntityCombatStateTracker.isInCombat(entity);

        Log.debug("  Combat state: {}", inCombat);

        // Get sword position (hip or back) from config
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(mainHand.getItem());
        SwordDisplayConfig.SwordDisplayPosition position =
            SwordDisplayConfig.getPositionForSword(itemId.toString());

        Log.debug("  Position: {}", position);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();

        poseStack.pushPose();

        // Position the sword/sheath on the entity's body
        // The pose stack is already positioned at the entity's location
        if (inCombat) {
            Log.debug("  Rendering sheath only (in combat)");
            renderSheathOnly(poseStack, bufferSource, packedLight, entity, mainHand, position);
        } else {
            Log.debug("  Rendering sword + sheath (not in combat)");
            renderSwordWithSheath(poseStack, bufferSource, packedLight, entity, mainHand, position);
        }

        poseStack.popPose();
    }

    /**
     * Renders a sword with its sheath on entity back/hip.
     * Uses the EXACT same positioning as the player system.
     */
    private static void renderSwordWithSheath(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                             LivingEntity entity, ItemStack sword,
                                             SwordDisplayConfig.SwordDisplayPosition position) {
        poseStack.pushPose();

        // Position on back or hip (using right side config, since entities don't have left/right)
        if (position == SwordDisplayConfig.SwordDisplayPosition.HIP) {
            applyHipPosition(poseStack);
        } else {
            applyBackPosition(poseStack);
        }

        // Apply scale from config
        float scale = (float) SwordDisplayConfig.scale;
        poseStack.scale(scale, scale, scale);

        // Render the sheath first (behind the sword) if enabled
        if (SwordDisplayConfig.renderSheaths) {
            Item sheathItem = SwordSheathRegistry.getSheathItem(sword);
            if (sheathItem != null) {
                SheathModelRenderer.renderSheath(sheathItem, poseStack, buffer, packedLight, entity.getId());
            }
        }

        // Render the sword
        Minecraft.getInstance().getItemRenderer().renderStatic(
            sword,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            entity.level(),
            entity.getId()
        );

        poseStack.popPose();
    }

    /**
     * Renders just a sheath (when sword is drawn but sheath persists).
     */
    private static void renderSheathOnly(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                        LivingEntity entity, ItemStack sword,
                                        SwordDisplayConfig.SwordDisplayPosition position) {
        if (!SwordDisplayConfig.renderSheaths) {
            return;
        }

        // Check if this sheath persists when sword is drawn
        SwordSheathRegistry.SheathInfo sheathInfo = SwordSheathRegistry.getSheathInfo(sword);
        if (sheathInfo == null || !sheathInfo.persistsWhenDrawn()) {
            return; // Don't render temporary sheaths (already hidden with particles)
        }

        Item sheathItem = SwordSheathRegistry.getSheathItem(sword);
        if (sheathItem == null) {
            return;
        }

        poseStack.pushPose();

        // Position on back or hip
        if (position == SwordDisplayConfig.SwordDisplayPosition.HIP) {
            applyHipPosition(poseStack);
        } else {
            applyBackPosition(poseStack);
        }

        // Apply scale from config
        float scale = (float) SwordDisplayConfig.scale;
        poseStack.scale(scale, scale, scale);

        // Render the sheath
        SheathModelRenderer.renderSheath(sheathItem, poseStack, buffer, packedLight, entity.getId());

        poseStack.popPose();
    }

    /**
     * Applies hip position transformation.
     * EXACT SAME as player system - uses right hip config values.
     */
    private static void applyHipPosition(PoseStack poseStack) {
        poseStack.translate(
            SwordDisplayConfig.hipRightTranslateX,
            SwordDisplayConfig.hipRightTranslateY,
            SwordDisplayConfig.hipRightTranslateZ
        );
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) SwordDisplayConfig.hipRightRotateZ));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) SwordDisplayConfig.hipRightRotateY));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) SwordDisplayConfig.hipRightRotateX));
    }

    /**
     * Applies back position transformation.
     * EXACT SAME as player system - uses right back config values.
     */
    private static void applyBackPosition(PoseStack poseStack) {
        poseStack.translate(
            SwordDisplayConfig.backRightTranslateX,
            SwordDisplayConfig.backRightTranslateY,
            SwordDisplayConfig.backRightTranslateZ
        );
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) SwordDisplayConfig.backRightRotateZ));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) SwordDisplayConfig.backRightRotateY));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) SwordDisplayConfig.backRightRotateX));
    }
}
