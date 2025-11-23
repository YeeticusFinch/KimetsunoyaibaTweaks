package com.lerdorf.kimetsunoyaibamultiplayer.integration.customnpcs;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomNPCConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles basic attack animations for Custom NPCs.
 * Plays random sword swing animations (sword_to_left, sword_to_right, sword_overhead)
 * when NPCs make normal attacks.
 */
@Mod.EventBusSubscriber
public class CustomNPCAttackAnimationHandler {

    // Thread-local flag to prevent recursion
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    // Animation names for basic sword swings
    private static final String SWORD_TO_LEFT = "sword_to_left";
    private static final String SWORD_TO_RIGHT = "sword_to_right";
    private static final String SWORD_OVERHEAD = "sword_overhead";

    @SubscribeEvent(priority = EventPriority.LOW) // Lower priority than ability handler
    public static void onNPCAttack(LivingAttackEvent event) {
        // Prevent recursion
        if (IS_PROCESSING.get()) {
            return;
        }

        try {
            IS_PROCESSING.set(true);

            // Check if feature is enabled
            if (!CustomNPCConfig.isEnabled() || !CustomNPCConfig.isAttackAnimationsEnabled()) {
                return;
            }

            // Only process on server side
            if (event.getEntity().level().isClientSide) {
                return;
            }

            // Get attacker
            DamageSource source = event.getSource();
            if (source.getEntity() == null || !(source.getEntity() instanceof LivingEntity)) {
                return;
            }

            LivingEntity attacker = (LivingEntity) source.getEntity();

            // Check if attacker is a Custom NPC
            if (!CustomNPCHelper.canUseAbilities(attacker)) {
                return;
            }

            // Get held item - must be a sword
            ItemStack heldItem = attacker.getMainHandItem();
            if (heldItem.isEmpty() || !(heldItem.getItem() instanceof SwordItem)) {
                return;
            }

            // Select weighted random swing animation
            // 45% sword_to_left, 45% sword_to_right, 10% sword_overhead
            String animationName;
            double roll = attacker.getRandom().nextDouble();
            if (roll < 0.45) {
                animationName = SWORD_TO_LEFT;
            } else if (roll < 0.90) {
                animationName = SWORD_TO_RIGHT;
            } else {
                animationName = SWORD_OVERHEAD;
            }

            if (CustomNPCConfig.isDebugEnabled()) {
                Log.debug("[KnY Custom NPCs] [Animation] Playing attack animation: " + animationName +
                                 " on NPC: " + attacker.getName().getString() + " (ID: " + attacker.getId() + ")");
            }

            // Send animation packet to all clients
            // MobPlayerAnimator 1.3.3+ supports Custom NPCs
            try {
                com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket packet =
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket(
                        attacker.getId(),
                        animationName
                    );

                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(packet);

                if (CustomNPCConfig.isDebugEnabled()) {
                    Log.debug("[KnY Custom NPCs] [Animation] Sent animation packet to all clients");
                }
            } catch (Exception e) {
                if (CustomNPCConfig.isDebugEnabled()) {
                    System.err.println("[KnY Custom NPCs] [Animation] Failed to send animation packet: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("[KnY Custom NPCs] Error in attack animation handler:");
            e.printStackTrace();
        } finally {
            IS_PROCESSING.set(false);
        }
    }
}
