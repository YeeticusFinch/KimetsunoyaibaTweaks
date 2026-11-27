package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ArachnidMandiblesItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Server-side attack handling for the arachnid mandibles helmet. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArachnidMandiblesHandler {
    private static final int COOLDOWN_TICKS = 40;
    private static final float BITE_DAMAGE = 4.0F;
    private static final int POISON_TICKS = 100;
    private static final int POISON_AMPLIFIER = 3;

    private ArachnidMandiblesHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            performAttack(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            performAttack(player);
        }
    }

    public static boolean performAttack(Player player) {
        if (player.level().isClientSide() || !ArachnidMandiblesItem.isEquipped(player)) {
            return false;
        }

        ItemStack mandibles = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (player.getCooldowns().isOnCooldown(ModItems.ARACHNID_MANDIBLES.get())) {
            return false;
        }

        player.getCooldowns().addCooldown(ModItems.ARACHNID_MANDIBLES.get(), COOLDOWN_TICKS);
        ArachnidMandiblesItem.triggerBite(mandibles, player.level().getGameTime());
        AnimationHelper.playAnimation(player, KimetsunoyaibaMultiplayer.MODID + ":bite2", 15);

        Vec3 head = player.getEyePosition();
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 effectCenter = head.add(forward.scale(1.5D));

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, effectCenter.x, effectCenter.y, effectCenter.z,
                16, 0.5D, 0.5D, 0.5D, 0.08D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK,
                SoundSource.PLAYERS, 1.0F, 1.25F);
        }

        AABB attackBox = new AABB(effectCenter.x - 2.0D, effectCenter.y - 2.0D, effectCenter.z - 2.0D,
            effectCenter.x + 2.0D, effectCenter.y + 2.0D, effectCenter.z + 2.0D);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, attackBox,
            target -> target != player && target.isAlive() && !target.isSpectator());
        for (LivingEntity target : targets) {
            if (Damager.hurt(player, target, BITE_DAMAGE, true, true)) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, POISON_AMPLIFIER));
            }
        }
        return true;
    }
}
