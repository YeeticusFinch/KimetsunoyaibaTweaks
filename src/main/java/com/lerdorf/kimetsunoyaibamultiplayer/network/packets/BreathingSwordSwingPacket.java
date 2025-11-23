package com.lerdorf.kimetsunoyaibamultiplayer.network.packets;

// import net.minecraft.client.player.LocalPlayer; // REMOVED: Client-only, unused
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.function.Supplier;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKanrojiAnimated;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedLoveForms;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.client.particles.BonePositionTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.ModParticles;

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

    private static final float DEFAULT_BOX_SIZE = 5f;
    private static final float KANROJI_BOX_SIZE = 10f; // Increased range for Kanroji's whip sword
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(heldItem.getItem() instanceof BreathingSwordItem)) return;

            // Check if player has cool_time effect from KnY mod (prevents attacks)
            if (com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.hasCoolTime(player)) {
                if (Config.logDebug) {
                    Log.debug("Sword swing blocked by cool_time effect for player {}", player.getName().getString());
                }
                return;
            }

            // Set weak defensive power for basic sword swing (lasts 10 ticks)
            // This allows basic swings to clash with and mitigate enemy attacks
            double weakDefense = 3.0; // Weak defensive power (3 damage reduction)
            GuardStateHelper.setWeakAttackState(player, weakDefense);

            // Schedule clearing the weak attack state after 10 ticks (0.5 seconds)
            com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler.scheduleOnce(
                player,
                () -> GuardStateHelper.clearGuardState(player),
                10
            );

            // Determine box size based on sword type
            boolean isKanrojiSword = heldItem.getItem() instanceof NichirinSwordKanrojiAnimated;
            float boxSize = isKanrojiSword ? KANROJI_BOX_SIZE : DEFAULT_BOX_SIZE;

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

            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (LivingEntity target : targets) {
                // Use smart targeting system to prevent friendly fire
                if (!EnhancedLoveForms.isTargetable(player, target)) {
                    continue; // Skip non-targetable entities
                }

                Damager.hurt(player, target, damage);

                // Spawn love_slash particle on hit for Kanroji sword
                if (isKanrojiSword && player.level() instanceof ServerLevel serverLevel) {
                    double particleX = target.getX() + target.getBbHeight() * (Math.random() - 0.5);
                    double particleY = target.getY() + target.getBbHeight() * Math.random();
                    double particleZ = target.getZ() + target.getBbHeight() * (Math.random() - 0.5);
                    serverLevel.sendParticles(
                        ModParticles.LOVE_SLASH.get(),
                        particleX, particleY, particleZ,
                        1, 0, 0, 0, 0
                    );
                }
            }

            if (Config.logDebug && !targets.isEmpty()) {
                Log.debug("AOE attack hit {} entities (boxSize={}, isKanroji={})",
                    targets.size(), boxSize, isKanrojiSword);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
