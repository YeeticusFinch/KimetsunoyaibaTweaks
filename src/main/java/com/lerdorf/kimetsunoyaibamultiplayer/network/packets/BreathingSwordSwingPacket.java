package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

// import net.minecraft.client.player.LocalPlayer; // REMOVED: Client-only, unused
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.function.Supplier;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
// import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler; // REMOVED: Client-only, unused
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordFrost;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordIce;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.NetworkEvent;

public class BreathingSwordSwingPacket {
	public BreathingSwordSwingPacket() {}
    public BreathingSwordSwingPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    float boxSize = 5f;
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(heldItem.getItem() instanceof BreathingSwordItem)) return;

            // Perform AOE
            Vec3 attackerPos = player.position().add(0, player.getEyeHeight(), 0);
            Vec3 lookVec = player.getLookAngle().normalize();
            Vec3 frontPos = attackerPos.add(lookVec.scale(boxSize/1.5f));

            AABB attackBox = new AABB(frontPos.add(-boxSize/2, -boxSize/2, -boxSize/2), frontPos.add(boxSize/2, boxSize/2, boxSize/2));

            List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class, attackBox,
                e -> e != player && e.isAlive()
            );
            
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            
            if (heldItem.getItem() instanceof NichirinSwordFrost) {
            	player.level().playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_BREAK,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            } 
            else if (heldItem.getItem() instanceof NichirinSwordIce) {
            	player.level().playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (LivingEntity target : targets) {
                //target.hurt(player.level().damageSources().playerAttack(player), damage);
                Damager.hurt(player, target, damage);
                /*if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        1, 0, 0, 0, 0);
                }*/
            }
            
            if (Config.logDebug && !targets.isEmpty()) {
                Log.debug("AOE attack hit {} additional entities", targets.size());
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
