package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.KanrojiEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side handler that synchronizes KanrojiEntity animations with the Kanroji sword item animations.
 *
 * This watches all KanrojiEntity instances and triggers matching animations on their held Kanroji sword
 * whenever the entity's animation changes (e.g., when attacking, walking, or using abilities).
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityKanrojiSwordAnimationSync {

    // Track last known animation for each entity to detect changes
    private static final Map<UUID, String> lastKnownAnimation = new HashMap<>();

    /**
     * Called every client tick to check for animation changes on KanrojiEntity instances.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        // Check all KanrojiEntity instances in the level
        mc.level.getEntitiesOfClass(KanrojiEntity.class, mc.player.getBoundingBox().inflate(64))
            .forEach(EntityKanrojiSwordAnimationSync::checkEntityAnimation);
    }

    /**
     * Check if the entity's animation has changed and trigger sword animation if needed.
     */
    private static void checkEntityAnimation(KanrojiEntity entity) {
        UUID entityUUID = entity.getUUID();
        String currentAnimation = entity.getCurrentAnimation();

        // Check if animation changed
        String lastAnimation = lastKnownAnimation.get(entityUUID);
        if (currentAnimation.equals(lastAnimation)) {
            return; // No change
        }

        // Update tracking
        lastKnownAnimation.put(entityUUID, currentAnimation);

        // Check if entity is holding Kanroji sword
        ItemStack mainHand = entity.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(mainHand.getItem() instanceof NichirinSwordKanrojiAnimated)) {
            return;
        }

        Log.debug("[EntityKanrojiSwordAnimationSync] Animation changed for entity {} from {} to {}",
                  entity.getName().getString(), lastAnimation, currentAnimation);

        // Trigger matching sword animation
        KanrojiSwordAnimationTrigger.triggerAnimation(entity, currentAnimation);

        Log.debug("[EntityKanrojiSwordAnimationSync] Triggered sword animation: {}", currentAnimation);
    }

    /**
     * Clean up tracking when entities are removed.
     */
    public static void cleanup(UUID entityUUID) {
        lastKnownAnimation.remove(entityUUID);
    }
}
