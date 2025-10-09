package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles gun-specific animations for players and mobs holding rifles, pistols, or miniguns
 * Replaces idle/walk/attack animations with gun-specific versions
 */
public class GunAnimationHandler {

    // Track current gun type for each entity
    private static final Map<UUID, GunType> currentGunType = new HashMap<>();
    private static final Map<UUID, AnimationState> currentAnimationState = new HashMap<>();

    public enum GunType {
        RIFLE("rifle"),
        PISTOL("pistol"),
        MINIGUN("minigun"),
        NONE("none");

        public final String name;

        GunType(String name) {
            this.name = name;
        }
    }

    public enum AnimationState {
        IDLE,
        WALKING,
        SHOOTING
    }

    /**
     * Detect what type of gun the entity is holding
     */
    public static GunType getGunType(LivingEntity entity) {
        ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);

        String mainItemId = mainHand.getItem().toString();
        String offItemId = offHand.getItem().toString();

        // Check main hand first
        if (mainItemId.contains("rifle")) {
            return GunType.RIFLE;
        } else if (mainItemId.contains("minigun")) {
            return GunType.MINIGUN;
        } else if (mainItemId.contains("pistol")) {
            return GunType.PISTOL;
        }

        // Check off hand for pistol
        if (offItemId.contains("pistol")) {
            return GunType.PISTOL;
        }

        return GunType.NONE;
    }

    /**
     * Check if entity is holding a pistol (in either hand)
     */
    public static boolean isHoldingPistol(LivingEntity entity) {
        ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = entity.getItemInHand(InteractionHand.OFF_HAND);

        return mainHand.getItem().toString().contains("pistol") ||
               offHand.getItem().toString().contains("pistol");
    }

    /**
     * Check if item is a gun from kimetsunoyaiba mod
     */
    public static boolean isGun(ItemStack item) {
        ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem());
        if (itemId == null) return false;

        return itemId.getNamespace().equals("kimetsunoyaiba") &&
               (itemId.getPath().contains("rifle") ||
                itemId.getPath().contains("pistol") ||
                itemId.getPath().contains("minigun"));
    }

    /**
     * Update gun animations for a player
     * Called every tick to ensure correct animations are playing
     */
    public static void updatePlayerGunAnimation(AbstractClientPlayer player) {
        GunType gunType = getGunType(player);
        UUID playerId = player.getUUID();

        // Check if gun type changed
        GunType previousGun = currentGunType.get(playerId);
        if (previousGun != gunType) {
            currentGunType.put(playerId, gunType);
            if (Config.logDebug)
            	Log.info("Player {} gun type changed: {} -> {}", player.getName().getString(), previousGun, gunType);

            // If gun was unequipped, stop the gun animations
            if (gunType == GunType.NONE && previousGun != null) {
                stopGunAnimation(player);
                currentAnimationState.remove(playerId);
                return;
            }
        }

        if (gunType == GunType.NONE) {
            return; // No gun, use default animations
        }

        // Determine animation state
        AnimationState state;
        if (player.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            state = AnimationState.WALKING;
        } else {
            state = AnimationState.IDLE;
        }

        // Check if state changed
        AnimationState previousState = currentAnimationState.get(playerId);
        if (previousState != state) {
            currentAnimationState.put(playerId, state);
            playGunAnimation(player, gunType, state);
        }
    }

    /**
     * Update gun animations for all nearby entities (players and mobs)
     * Called every tick to ensure correct animations are playing for entities holding guns
     */
    public static void updateAllEntityGunAnimations() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        // Check all nearby living entities
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(64))) {

            // Skip local player (handled separately)
            if (entity.getUUID().equals(mc.player.getUUID())) {
                continue;
            }

            // Only process entities holding guns
            GunType gunType = getGunType(entity);
            if (gunType == GunType.NONE) {
                continue;
            }

            UUID entityId = entity.getUUID();

            // Check if gun type changed
            GunType previousGun = currentGunType.get(entityId);
            if (previousGun != gunType) {
                currentGunType.put(entityId, gunType);
                if (Config.logDebug) {
                    Log.info("Entity {} gun type changed: {} -> {}",
                            entity.getName().getString(), previousGun, gunType);
                }

                // If gun was unequipped, stop the gun animations
                if (gunType == GunType.NONE && previousGun != null) {
                    stopEntityGunAnimation(entity);
                    currentAnimationState.remove(entityId);
                    continue;
                }
            }

            // Determine animation state
            AnimationState state;
            if (entity.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                state = AnimationState.WALKING;
            } else {
                state = AnimationState.IDLE;
            }

            // Check if state changed
            AnimationState previousState = currentAnimationState.get(entityId);
            if (previousState != state) {
                currentAnimationState.put(entityId, state);

                // Apply animation based on entity type
                if (entity instanceof AbstractClientPlayer clientPlayer) {
                    playGunAnimation(clientPlayer, gunType, state);
                } else {
                    // For mobs, we'll use the mob animation handler
                    playMobGunAnimation(entity, gunType, state);
                }
            }
        }
    }

    /**
     * Play the appropriate gun animation for a player
     */
    private static void playGunAnimation(AbstractClientPlayer player, GunType gunType, AnimationState state) {
        String animationName;

        switch (state) {
            case IDLE -> animationName = "idle_" + gunType.name;
            case WALKING -> animationName = "walk_" + gunType.name;
            case SHOOTING -> animationName = "shoot_" + gunType.name;
            default -> {
                return;
            }
        }

        // Try to play the animation
        ResourceLocation animationId = ResourceLocation.tryBuild("kimetsunoyaibamultiplayer", animationName);
        if (animationId == null) {
        	if (Config.logDebug)
        		Log.warn("Failed to build ResourceLocation for animation: {}", animationName);
            return;
        }

        // Try multiple namespaces to find the animation
        KeyframeAnimation animation = null;
        ResourceLocation[] possibleLocations = {
            animationId, // kimetsunoyaibamultiplayer:idle_rifle
            ResourceLocation.tryBuild("kimetsunoyaiba", animationName), // kimetsunoyaiba:idle_rifle
            ResourceLocation.tryBuild("playeranimator", animationName) // playeranimator:idle_rifle
        };

        for (ResourceLocation loc : possibleLocations) {
            animation = PlayerAnimationRegistry.getAnimation(loc);
            if (animation != null) {
                if (Config.logDebug) {
                    Log.info("Found gun animation at: {}", loc);
                }
                break;
            }
        }

        if (animation == null) {
        	if (Config.logDebug)
        		Log.warn("Animation not found in registry: {} (tried {} namespaces)", animationName, possibleLocations.length);
            return;
        }

        var animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        if (animationStack != null) {
            // Use layer 3000 for idle/walk animations (base layer for guns)
            // Layer 1000 is reserved for normal animations
            animationStack.addAnimLayer(3000, new KeyframeAnimationPlayer(animation));
            if (Config.logDebug) {
                Log.info("Playing gun animation for {}: {}", player.getName().getString(), animationName);
            }
        } else {
        	if (Config.logDebug)
        		Log.warn("No animation stack available for player: {}", player.getName().getString());
        }
    }

    /**
     * Play the appropriate gun animation for a mob
     * Uses mobplayeranimator to apply PlayerAnimator animations to mobs
     */
    private static void playMobGunAnimation(LivingEntity entity, GunType gunType, AnimationState state) {
        String animationName;

        switch (state) {
            case IDLE -> animationName = "idle_" + gunType.name;
            case WALKING -> animationName = "walk_" + gunType.name;
            case SHOOTING -> animationName = "shoot_" + gunType.name;
            default -> {
                return;
            }
        }

        // Try to play the animation using PlayerAnimator with mobplayeranimator support
        ResourceLocation animationId = ResourceLocation.tryBuild("kimetsunoyaibamultiplayer", animationName);
        if (animationId == null) {
        	if (Config.logDebug)
        		Log.warn("Failed to build ResourceLocation for animation: {}", animationName);
            return;
        }

        KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(animationId);
        if (animation == null) {
            if (Config.logDebug) {
                Log.debug("Animation not found in registry for mob: {}", animationId);
            }
            return;
        }

        // With mobplayeranimator, PlayerAnimationAccess.getPlayerAnimLayer() works with any LivingEntity
        // However, at compile time it still requires AbstractClientPlayer, so we use reflection
        try {
            // Try to get the animation layer using reflection to bypass compile-time type checking
            // mobplayeranimator adds support for LivingEntity at runtime
            java.lang.reflect.Method method = PlayerAnimationAccess.class.getMethod("getPlayerAnimLayer", LivingEntity.class);
            Object animationStackObj = method.invoke(null, entity);

            if (animationStackObj != null && animationStackObj instanceof AnimationStack) {
                // Cast to AnimationStack and add the animation
                AnimationStack animationStack = (AnimationStack) animationStackObj;
                animationStack.addAnimLayer(1000, new KeyframeAnimationPlayer(animation));

                if (Config.logDebug) {
                    Log.info("Playing gun animation for mob {}: {}", entity.getName().getString(), animationName);
                }
            } else if (Config.logDebug) {
                Log.debug("No animation stack available for entity: {}", entity.getName().getString());
            }
        } catch (NoSuchMethodException e) {
            // mobplayeranimator not present or wrong version
            if (Config.logDebug) {
                Log.debug("mobplayeranimator not available - cannot animate mobs");
            }
        } catch (Exception e) {
            if (Config.logDebug) {
                Log.debug("Could not apply gun animation to mob {}: {}",
                        entity.getName().getString(), e.getMessage());
            }
        }
    }

    /**
     * Play shoot animation and effects when player attacks with gun
     * Uses layer 4000 so it plays ON TOP of idle/walk animations
     */
    public static void playShootAnimation(Player player, GunType gunType) {
        String animationName = "shoot_" + gunType.name;
        ResourceLocation animationId = ResourceLocation.tryBuild("kimetsunoyaibamultiplayer", animationName);

        if (animationId == null) {
        	if (Config.logDebug)
        		Log.warn("Failed to build ResourceLocation for shoot animation: {}", animationName);
            return;
        }

        // Try multiple namespaces
        KeyframeAnimation animation = null;
        ResourceLocation[] possibleLocations = {
            animationId,
            ResourceLocation.tryBuild("kimetsunoyaiba", animationName),
            ResourceLocation.tryBuild("playeranimator", animationName)
        };

        for (ResourceLocation loc : possibleLocations) {
            animation = PlayerAnimationRegistry.getAnimation(loc);
            if (animation != null) {
                break;
            }
        }

        if (animation != null && player instanceof AbstractClientPlayer clientPlayer) {
            var animationStack = PlayerAnimationAccess.getPlayerAnimLayer(clientPlayer);
            if (animationStack != null) {
                // Layer 4000 = HIGH PRIORITY - plays ON TOP of idle/walk (layer 3000)
                // This allows the upper body to shoot while legs continue walking
                animationStack.addAnimLayer(4000, new KeyframeAnimationPlayer(animation));
                if (Config.logDebug) {
                    Log.info("Playing shoot animation for {}: {} (layer 4000)",
                            player.getName().getString(), animationName);
                }
            }
        }

        // Play sound and particles
        playShootEffects(player, gunType);
    }

    /**
     * Play shoot animation and effects when a mob attacks with gun
     */
    public static void playMobShootAnimation(LivingEntity entity, GunType gunType) {
        String animationName = "shoot_" + gunType.name;
        ResourceLocation animationId = ResourceLocation.tryBuild("kimetsunoyaibamultiplayer", animationName);

        if (animationId == null) {
        	if (Config.logDebug)
        		Log.warn("Failed to build ResourceLocation for shoot animation: {}", animationName);
            playShootEffectsForEntity(entity, gunType);
            return;
        }

        KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(animationId);
        if (animation != null) {
            // Use reflection to access mobplayeranimator's LivingEntity support
            try {
                java.lang.reflect.Method method = PlayerAnimationAccess.class.getMethod("getPlayerAnimLayer", LivingEntity.class);
                Object animationStackObj = method.invoke(null, entity);

                if (animationStackObj != null && animationStackObj instanceof AnimationStack) {
                    AnimationStack animationStack = (AnimationStack) animationStackObj;
                    animationStack.addAnimLayer(1000, new KeyframeAnimationPlayer(animation));

                    if (Config.logDebug) {
                        Log.info("Playing shoot animation for mob {}: {}", entity.getName().getString(), animationName);
                    }
                }
            } catch (Exception e) {
                if (Config.logDebug) {
                    Log.debug("Could not apply shoot animation to mob {}: {}",
                            entity.getName().getString(), e.getMessage());
                }
            }
        }

        // Play sound and particles
        playShootEffectsForEntity(entity, gunType);
    }

    /**
     * Play sound and particle effects for gun shooting (player-specific)
     */
    private static void playShootEffects(Player player, GunType gunType) {
        playShootEffectsForEntity(player, gunType);
    }

    /**
     * Play sound and particle effects for gun shooting (works for any LivingEntity)
     * MUST be called from client thread only
     */
    private static void playShootEffectsForEntity(LivingEntity entity, GunType gunType) {
        // Make sure we're on the client side
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Vec3 eyePos = entity.getEyePosition();
        Vec3 lookVec = entity.getLookAngle();
        Vec3 particlePos = eyePos.add(lookVec.scale(1.5));

        // Use a thread-safe random source
        java.util.Random random = new java.util.Random();

        switch (gunType) {
            case RIFLE -> {
                // Thunder sound (high pitched) - use local sound
                mc.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL,
                        0.5f, 1.8f, false);

                // Smoke particles - use client level
                for (int i = 0; i < 10; i++) {
                    mc.level.addParticle(ParticleTypes.SMOKE,
                            particlePos.x + (random.nextDouble() - 0.5) * 0.2,
                            particlePos.y + (random.nextDouble() - 0.5) * 0.2,
                            particlePos.z + (random.nextDouble() - 0.5) * 0.2,
                            lookVec.x * 0.1, lookVec.y * 0.1, lookVec.z * 0.1);
                }

                // Flash particle
                mc.level.addParticle(ParticleTypes.FLASH,
                        particlePos.x, particlePos.y, particlePos.z,
                        0, 0, 0);
            }
            case MINIGUN -> {
                // Rapid fire sound (thunder, slightly lower pitch)
                mc.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL,
                        0.4f, 1.5f, false);

                // More particles for minigun
                for (int i = 0; i < 15; i++) {
                    mc.level.addParticle(ParticleTypes.SMOKE,
                            particlePos.x + (random.nextDouble() - 0.5) * 0.3,
                            particlePos.y + (random.nextDouble() - 0.5) * 0.3,
                            particlePos.z + (random.nextDouble() - 0.5) * 0.3,
                            lookVec.x * 0.15, lookVec.y * 0.15, lookVec.z * 0.15);
                }

                // Flash particle
                mc.level.addParticle(ParticleTypes.FLASH,
                        particlePos.x, particlePos.y, particlePos.z,
                        0, 0, 0);
            }
        }
    }

    /**
     * Play pistol shot particles only (no sound - sound is played by the gun itself)
     * This is called when pistol fires (release right-click)
     */
    public static void playPistolParticles(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Vec3 eyePos = entity.getEyePosition();
        Vec3 lookVec = entity.getLookAngle();
        Vec3 particlePos = eyePos.add(lookVec.scale(1.5));

        // Use a thread-safe random source
        java.util.Random random = new java.util.Random();

        // Smoke particles
        for (int i = 0; i < 5; i++) {
            mc.level.addParticle(ParticleTypes.SMOKE,
                    particlePos.x + (random.nextDouble() - 0.5) * 0.2,
                    particlePos.y + (random.nextDouble() - 0.5) * 0.2,
                    particlePos.z + (random.nextDouble() - 0.5) * 0.2,
                    lookVec.x * 0.1, lookVec.y * 0.1, lookVec.z * 0.1);
        }

        // Flash particle
        mc.level.addParticle(ParticleTypes.FLASH,
                particlePos.x, particlePos.y, particlePos.z,
                0, 0, 0);
    }

    /**
     * Stop gun animations for a player
     * Clears animation layer 3000 (gun idle/walk) and layer 4000 (gun shoot)
     */
    private static void stopGunAnimation(AbstractClientPlayer player) {
        var animationStack = PlayerAnimationAccess.getPlayerAnimLayer(player);
        if (animationStack != null) {
            // Remove gun animation layers
            animationStack.removeLayer(3000);  // Idle/walk layer
            animationStack.removeLayer(4000);  // Shoot layer
            if (Config.logDebug) {
                Log.info("Stopped gun animations for player: {}", player.getName().getString());
            }
        }
    }

    /**
     * Stop gun animations for any living entity (using reflection for mob support)
     */
    private static void stopEntityGunAnimation(LivingEntity entity) {
        if (entity instanceof AbstractClientPlayer clientPlayer) {
            stopGunAnimation(clientPlayer);
            return;
        }

        // Try to stop animations for mobs using reflection
        try {
            java.lang.reflect.Method method = PlayerAnimationAccess.class.getMethod("getPlayerAnimLayer", LivingEntity.class);
            Object animationStackObj = method.invoke(null, entity);

            if (animationStackObj != null && animationStackObj instanceof AnimationStack) {
                AnimationStack animationStack = (AnimationStack) animationStackObj;
                animationStack.removeLayer(3000);  // Idle/walk layer
                animationStack.removeLayer(4000);  // Shoot layer

                if (Config.logDebug) {
                    Log.info("Stopped gun animations for entity: {}", entity.getName().getString());
                }
            }
        } catch (Exception e) {
            if (Config.logDebug) {
                Log.debug("Could not stop gun animations for entity {}: {}",
                        entity.getName().getString(), e.getMessage());
            }
        }
    }

    /**
     * Clear stored data for a player/entity
     */
    public static void clearEntity(UUID entityId) {
        currentGunType.remove(entityId);
        currentAnimationState.remove(entityId);
    }

    /**
     * Clear all stored data
     */
    public static void clearAll() {
        currentGunType.clear();
        currentAnimationState.clear();
        if (Config.logDebug)
        	Log.info("Cleared all gun animation data");
    }
}