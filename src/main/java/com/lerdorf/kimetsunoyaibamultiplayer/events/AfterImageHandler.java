package com.lerdorf.kimetsunoyaibamultiplayer.events;

import java.util.List;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handler for AfterImage entities that redirects mobs to target after images
 * instead of their real target when after images are present.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public class AfterImageHandler {

    /**
     * Redirect mobs to target after images instead of their owners
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (mob.level().isClientSide) {
            return;
        }

        // Only run every 10 ticks for performance
        if (mob.tickCount % 10 != 0) {
            return;
        }

        LivingEntity currentTarget = mob.getTarget();

        // Check if mob is targeting an entity that has an after image - redirect to the after image
        if (currentTarget != null && !(currentTarget instanceof AfterImageEntity)) {
            // Find after images belonging to the current target
            List<AfterImageEntity> ownerAfterImages = mob.level().getEntitiesOfClass(AfterImageEntity.class,
                new AABB(mob.position(), mob.position()).inflate(30.0),
                afterImage -> afterImage.isAlive()
                    && afterImage.getOpacity() > 0.1f
                    && afterImage.getOwnerUUID() != null
                    && afterImage.getOwnerUUID().equals(currentTarget.getUUID()));

            if (!ownerAfterImages.isEmpty()) {
                // Pick the closest after image
                AfterImageEntity closestAfterImage = null;
                double closestDistance = Double.MAX_VALUE;

                for (AfterImageEntity afterImage : ownerAfterImages) {
                    double dist = mob.distanceToSqr(afterImage);
                    if (dist < closestDistance) {
                        closestDistance = dist;
                        closestAfterImage = afterImage;
                    }
                }

                if (closestAfterImage != null) {
                    mob.setTarget(closestAfterImage);
                }
            }
        }
    }
}
