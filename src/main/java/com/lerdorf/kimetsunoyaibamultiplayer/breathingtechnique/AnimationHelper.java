package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.SpeedControlledAnimation;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.DistExecutor;

/**
 * Helper class for playing animations on players and entities
 * For players: Uses PlayerAnimator library
 * For mobs: Uses MobPlayerAnimator library (supports Custom NPCs and other humanoid mobs)
 * For custom entities: May use GeckoLib animations separately
 */
public class AnimationHelper {

    /**
     * Play an animation on any LivingEntity
     * Works for Player instances (uses PlayerAnimator)
     * Works for Custom NPCs and other humanoid mobs (uses MobPlayerAnimator via MobAnimationHelper)
     * Custom entities using GeckoLib should handle animations separately
     */
    public static void playAnimation(LivingEntity entity, String animationName) {
        SwordRegistry.RegisteredSword sword = SwordRegistry.getSword(entity.getMainHandItem().getItem());
        if (sword != null) {
            animationName = sword.getAnim(animationName);
        }

        // Trigger GeckoLib sword animation if holding Kanroji sword
        triggerKanrojiSwordAnimation(entity, animationName);

        if (entity instanceof Player player) {
            playAnimation(player, animationName, -1, 1.0f);
        } else {
            // Mobs/NPCs: server broadcasts, clients apply via MobPlayerAnimator
            if (!entity.level().isClientSide) {
                ModNetworking.sendToAllClients(new MobAnimationSyncPacket(entity.getId(), animationName));
            } else {
                com.lerdorf.kimetsunoyaibamultiplayer.entities.MobAnimationHelper.playAnimation(entity, animationName);
            }
        }
    }

    /**
     * Play an animation on any LivingEntity with max duration
     */
    public static void playAnimation(LivingEntity entity, String animationName, int maxDurationTicks) {
        SwordRegistry.RegisteredSword sword = SwordRegistry.getSword(entity.getMainHandItem().getItem());
        if (sword != null) {
            animationName = sword.getAnim(animationName);
        }
        if (entity instanceof Player player) {
            playAnimation(player, animationName, maxDurationTicks, 1.0f);
        } else {
            // For mobs/NPCs: duration control not supported yet; broadcast simple play
            if (!entity.level().isClientSide) {
                ModNetworking.sendToAllClients(new MobAnimationSyncPacket(entity.getId(), animationName));
            } else {
                com.lerdorf.kimetsunoyaibamultiplayer.entities.MobAnimationHelper.playAnimation(entity, animationName);
            }
        }
    }

    /**
     * Play an animation on any LivingEntity with max duration, speed, and layer
     */
    public static void playAnimationOnLayer(LivingEntity entity, String animationName, int maxDurationTicks, float speed, int layerPriority) {
        SwordRegistry.RegisteredSword sword = SwordRegistry.getSword(entity.getMainHandItem().getItem());
        if (sword != null) {
            animationName = sword.getAnim(animationName);
        }
        if (entity instanceof Player player) {
            playAnimationOnLayer(player, animationName, maxDurationTicks, speed, layerPriority);
        } else {
            // For mobs/NPCs: layer & speed not yet supported; broadcast simple play
            if (!entity.level().isClientSide) {
                ModNetworking.sendToAllClients(new MobAnimationSyncPacket(entity.getId(), animationName));
            } else {
                com.lerdorf.kimetsunoyaibamultiplayer.entities.MobAnimationHelper.playAnimationOnLayer(entity, animationName, maxDurationTicks, speed, layerPriority);
            }
        }
    }

    /**
     * Play an animation on a player (both client and server)
     */
    public static void playAnimation(Player player, String animationName) {
    	SwordRegistry.RegisteredSword sword = SwordRegistry.getSword(player.getMainHandItem().getItem());
    	if (sword != null) {
    		animationName = sword.getAnim(animationName);
    	}
        playAnimation(player, animationName, -1, 1.0f);
    }

    /**
     * Play an animation on a player with a maximum duration in ticks
     * @param player The player to play the animation on
     * @param animationName The name of the animation
     * @param maxDurationTicks Maximum duration in ticks (-1 for full animation)
     */
    public static void playAnimation(Player player, String animationName, int maxDurationTicks) {
    	SwordRegistry.RegisteredSword sword = SwordRegistry.getSword(player.getMainHandItem().getItem());
    	if (sword != null) {
    		animationName = sword.getAnim(animationName);
    	}
        playAnimation(player, animationName, maxDurationTicks, 1.0f);
    }
    
    /**
     * Play an animation with optional max duration and playback speed.
     * @param player The player to animate
     * @param animationName The animation name or path
     * @param maxDurationTicks Maximum duration in ticks (-1 for full animation)
     * @param speed Playback speed multiplier (1.0 = normal, 2.0 = double speed, 0.5 = half speed)
     */
    public static void playAnimation(Player player, String animationName, int maxDurationTicks, float speed) {
    	SwordRegistry.RegisteredSword sword = SwordRegistry.getSword(player.getMainHandItem().getItem());
    	if (sword != null) {
    		animationName = sword.getAnim(animationName);
    	}
        playAnimationOnLayer(player, animationName, maxDurationTicks, speed, 3000);
    }

    /**
     * Play an animation on a specific layer with optional max duration and playback speed.
     * @param player The player to animate
     * @param animationName The animation name or path
     * @param maxDurationTicks Maximum duration in ticks (-1 for full animation)
     * @param speed Playback speed multiplier (1.0 = normal, 2.0 = double speed, 0.5 = half speed)
     * @param layerPriority The animation layer priority (default 3000, use higher for overlays)
     */
    public static void playAnimationOnLayer(Player player, String animationName, int maxDurationTicks, float speed, int layerPriority) {
        ResourceLocation animationLocation = parseAnimationName(animationName);

        // Trigger GeckoLib sword animation if holding Kanroji sword
        triggerKanrojiSwordAnimation(player, animationName);

        // CLIENT SIDE: Play locally
        if (player.level().isClientSide) {
            // Only look up animation on client side (PlayerAnimationRegistry is client-only)
            KeyframeAnimation animation = findAnimation(animationLocation);

            if (animation == null) {
                return; // Animation not found
            }

            DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                ModifierLayer<IAnimation> layer = com.lerdorf.kimetsunoyaibamultiplayer.client.ClientAnimationHelper.playAnimationOnPlayer(player, animation, speed, layerPriority);

                if (maxDurationTicks > 0 && layer != null) {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.ClientAnimationHelper.scheduleAnimationCancellation(layer, maxDurationTicks);
                }
            });

            // Send to server so other players see it
            AnimationSyncPacket packet = new AnimationSyncPacket(
                player.getUUID(),
                animationLocation,
                0,
                30,
                false,
                false,
                animation
            );
            ModNetworking.sendToServer(packet);
        }
        // SERVER SIDE: Send to all clients (including the player themselves)
        else if (!player.level().isClientSide) {
            // On server side, we don't need to look up the animation - just send the animation name
            // The clients will look it up themselves when they receive the packet
            AnimationSyncPacket packet = new AnimationSyncPacket(
                player.getUUID(),
                animationLocation,
                0,
                30,
                false,
                false,
                null,  // animation = null on server side (clients will look it up)
                speed,
                layerPriority
            );
            ModNetworking.sendToAllClients(packet);
        }
    }


    private static ResourceLocation parseAnimationName(String animationName) {
        if (animationName.contains(":")) {
            String[] parts = animationName.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }

        // Check if this is one of our custom animations (love forms, etc.)
        // These should use kimetsunoyaibamultiplayer namespace
        if (animationName.startsWith("love_") || animationName.equals("kanroji_sword_overhead")) {
            return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", animationName);
        }

        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
    }

    private static KeyframeAnimation findAnimation(ResourceLocation animationLocation) {
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animationLocation);
        if (anim != null) {
            return anim;
        }

        // Try alternative namespaces
        String path = animationLocation.getPath();
        ResourceLocation[] alternativeLocations = {
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", path), // Check our mod's namespace first
            ResourceLocation.fromNamespaceAndPath("playeranimator", path),
            ResourceLocation.fromNamespaceAndPath("minecraft", path),
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "animations/" + path)
        };

        for (ResourceLocation loc : alternativeLocations) {
            anim = PlayerAnimationRegistry.getAnimation(loc);
            if (anim != null) {
                return anim;
            }
        }

        return null;
    }

    /**
     * Trigger GeckoLib animation on the Kanroji sword if the entity is holding it
     */
    public static void triggerKanrojiSwordAnimation(LivingEntity entity, String animationName) {
        if (entity.level().isClientSide) {
            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[AnimationHelper] triggerKanrojiSwordAnimation(entity={}, anim={})", entity.getName().getString(), animationName);
            }
            DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                com.lerdorf.kimetsunoyaibamultiplayer.client.KanrojiSwordAnimationTrigger.triggerAnimation(entity, animationName);
            });
        }
    }

}
