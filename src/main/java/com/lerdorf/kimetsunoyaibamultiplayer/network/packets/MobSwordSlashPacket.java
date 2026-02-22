package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientPacketHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> Client: Instructs clients to spawn a standard sword slash (model/particles)
 * for a mob entity using the existing BonePositionTracker logic.
 */
public class MobSwordSlashPacket {
    private final UUID entityUUID;
    private final String animationName;
    private final int animationTick;

    public MobSwordSlashPacket(UUID entityUUID, String animationName, int animationTick) {
        this.entityUUID = entityUUID;
        this.animationName = animationName;
        this.animationTick = animationTick;
    }

    public MobSwordSlashPacket(FriendlyByteBuf buf) {
        this.entityUUID = buf.readUUID();
        this.animationName = buf.readUtf(256);
        this.animationTick = buf.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(entityUUID);
        buf.writeUtf(animationName, 256);
        buf.writeVarInt(animationTick);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(entityUUID, animationName, animationTick)));
        ctx.setPacketHandled(true);
        return true;
    }

    private static void handleClient(UUID entityUUID, String animationName, int animationTick) {
        var level = ClientPacketHandler.getClientLevel();
        if (!(level instanceof ClientLevel clientLevel)) return;

        // Resolve entity by UUID
        Entity found = clientLevel.getEntity(entityUUID.hashCode());
        LivingEntity living = null;
        if (found instanceof LivingEntity) {
            living = (LivingEntity) found;
        } else {
            for (Entity e : clientLevel.entitiesForRendering()) {
                if (e.getUUID().equals(entityUUID) && e instanceof LivingEntity) {
                    living = (LivingEntity) e;
                    break;
                }
            }
        }
        if (living == null) return;

        // Use the entity's main-hand item to determine particles/model
        ItemStack swordItem = living.getMainHandItem();
        ParticleOptions particleType = SwordParticleMapping.getParticleForSword(swordItem);
        if (particleType == null) {
            if (!SwordParticleMapping.isKimetsunoyaibaSword(swordItem)) {
                return;
            }
            // Keep slash rendering active even if a sword has no explicit mapping.
            particleType = ParticleTypes.CLOUD;
        }

        // Delegate to BonePositionTracker which will select model vs particles
        BonePositionTracker.spawnRadialRibbonParticles(living, swordItem, animationName, animationTick, particleType);
    }
}
