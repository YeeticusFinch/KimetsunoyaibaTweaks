package com.lerdorf.kimetsunoyaibamultiplayer;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles sword clashing mechanics based on the original KnY mod's guard system.
 *
 * This system allows attacks to be deflected or mitigated when entities are:
 * - Using breathing techniques or blood demon arts
 * - Actively swinging weapons
 *
 * The defensive power comes from the "Damage" NBT field, which serves a dual purpose:
 * - Offensive damage when attacking
 * - Defensive power when being attacked
 *
 * See KnY_Sword_Combat.md for full documentation.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class SwordClashingHandler {

    private static final ThreadLocal<Boolean> IS_PROCESSING_GUARD = ThreadLocal.withInitial(() -> false);

    /**
     * Check if an item is considered a sword for clashing mechanics.
     * Matches the original KnY mod's LogicSwordProcedure.
     */
    private static boolean isSword(ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }

        Item itemType = item.getItem();

        // Check if it's a SwordItem, AxeItem, PickaxeItem, ShovelItem, or HoeItem
        if (itemType instanceof SwordItem || itemType instanceof AxeItem ||
            itemType instanceof PickaxeItem || itemType instanceof ShovelItem ||
            itemType instanceof HoeItem) {
            return true;
        }

        // Check item tags (forge:sword, forge:whip, etc.)
        return item.is(net.minecraft.tags.ItemTags.create(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "sword")));
    }

    /**
     * Main event handler for sword clashing mechanics.
     * Reduces incoming damage based on defender's guard state.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        // Check if sword clashing is enabled in config
        if (!Config.enableSwordClashing) {
            return; // Sword clashing is disabled, process normally
        }

        // Prevent recursion
        if (IS_PROCESSING_GUARD.get()) {
            return;
        }

        try {
            IS_PROCESSING_GUARD.set(true);

            LivingEntity defender = event.getEntity();
            DamageSource damageSource = event.getSource();
            float incomingDamage = event.getAmount();

            // Get attacker entity (may be null for environmental damage)
            if (!(damageSource.getEntity() instanceof LivingEntity attacker)) {
                return; // No attacker entity, can't clash
            }

            // Check if defender is in guard state
            if (!GuardStateHelper.isGuarding(defender, attacker)) {
                return; // Not guarding, process normally
            }

            // Get defensive power
            double defensivePower = GuardStateHelper.getDefensivePower(defender);

            // Calculate reduced damage
            float reducedDamage = (float) Math.max(incomingDamage - defensivePower, 0.0);

            if (Config.logDebug) {
                Log.debug("Sword Clash: {} defending against {} - Damage: {} -> {} (blocked: {})",
                    defender.getName().getString(),
                    attacker.getName().getString(),
                    incomingDamage,
                    reducedDamage,
                    defensivePower);
            }

            // Cancel original damage event
            event.setCanceled(true);

            // Play guard effects (sounds and particles)
            playGuardEffects(defender, attacker, reducedDamage <= 0);

            // Reapply with reduced damage if any remains
            if (reducedDamage > 0) {
                // Use a custom damage source to maintain proper attribution
                defender.hurt(DamageCalculator.getDamageSource(attacker), reducedDamage);
            }

        } catch (Exception e) {
            // Catch exceptions to prevent crashes
            if (Config.logError) {
                Log.error("Error in SwordClashingHandler: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            IS_PROCESSING_GUARD.set(false);
        }
    }

    /**
     * Play visual and audio feedback for successful guard.
     * Matches the original KnY mod's GuardEffectProcedure.
     */
    private static void playGuardEffects(LivingEntity defender, LivingEntity attacker, boolean completeBlock) {
        if (defender.level().isClientSide()) {
            return; // Only play effects on server side
        }

        ServerLevel serverLevel = (ServerLevel) defender.level();
        Vec3 defenderPos = defender.position().add(0, defender.getEyeHeight() / 2, 0);

        // Get weapons
        ItemStack attackerWeapon = attacker.getMainHandItem();
        ItemStack defenderWeapon = defender.getMainHandItem();

        boolean attackerHasSword = isSword(attackerWeapon);
        boolean defenderHasSword = isSword(defenderWeapon);

        if (attackerHasSword && defenderHasSword) {
            // Both have swords - classic sword clash
            serverLevel.playSound(null, defender.blockPosition(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                completeBlock ? 1.2F : 0.8F,
                completeBlock ? 1.2F : 1.0F);

            // Spawn fire spark particles
            for (int i = 0; i < (completeBlock ? 15 : 8); i++) {
                double offsetX = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                double offsetY = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 0.5;

                serverLevel.sendParticles(ParticleTypes.CRIT,
                    defenderPos.x + offsetX,
                    defenderPos.y + offsetY,
                    defenderPos.z + offsetZ,
                    1, 0, 0.1, 0, 0.1);

                if (completeBlock) {
                    serverLevel.sendParticles(ParticleTypes.FLASH,
                        defenderPos.x + offsetX,
                        defenderPos.y + offsetY,
                        defenderPos.z + offsetZ,
                        1, 0, 0, 0, 0);
                }
            }

        } else if (attackerHasSword) {
            // Attacker has sword, defender doesn't - blocked with body/hands
            serverLevel.playSound(null, defender.blockPosition(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                completeBlock ? 1.0F : 0.7F,
                completeBlock ? 1.1F : 0.9F);

            // Spawn crit particles
            for (int i = 0; i < (completeBlock ? 10 : 5); i++) {
                double offsetX = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                double offsetY = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 0.5;

                serverLevel.sendParticles(ParticleTypes.CRIT,
                    defenderPos.x + offsetX,
                    defenderPos.y + offsetY,
                    defenderPos.z + offsetZ,
                    1, 0, 0.1, 0, 0.05);
            }

        } else {
            // No swords - generic attack block
            serverLevel.playSound(null, defender.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
                0.8F, 1.0F);

            // Spawn basic crit particles
            for (int i = 0; i < 5; i++) {
                double offsetX = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                double offsetY = (serverLevel.random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 0.5;

                serverLevel.sendParticles(ParticleTypes.CRIT,
                    defenderPos.x + offsetX,
                    defenderPos.y + offsetY,
                    defenderPos.z + offsetZ,
                    1, 0, 0, 0, 0);
            }
        }

        // Complete block gets extra flash effect
        if (completeBlock) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                defenderPos.x, defenderPos.y, defenderPos.z,
                1, 0, 0, 0, 0);
        }
    }
}
