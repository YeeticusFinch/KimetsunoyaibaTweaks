package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.items.TippedKunaiUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public class KunaiEntity extends ThrowableItemProjectile {
    private static final float DAMAGE = 4.0F;
    private static final int TRAIL_PARTICLE_COUNT = 2;

    public KunaiEntity(EntityType<? extends KunaiEntity> type, Level level) {
        super(type, level);
    }

    public KunaiEntity(EntityType<? extends KunaiEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    public static void throwFrom(Player player, ItemStack thrownStack) {
        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
            0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            KunaiEntity kunai = new KunaiEntity(ModEntities.KUNAI.get(), player, level);
            ItemStack renderStack = thrownStack.copy();
            renderStack.setCount(1);
            kunai.setItem(renderStack);
            kunai.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(kunai);
        }

        player.awardStat(Stats.ITEM_USED.get(thrownStack.getItem()));
        player.getCooldowns().addCooldown(thrownStack.getItem(), 5);
        if (!player.getAbilities().instabuild) {
            thrownStack.shrink(1);
        }
    }

    @Override
    protected Item getDefaultItem() {
        Item baseKunai = TippedKunaiUtil.baseKunaiItem();
        return baseKunai == null ? ModItems.TIPPED_KUNAI.get() : baseKunai;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.isRemoved()) {
            spawnTrailParticles();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }

        Entity owner = this.getOwner();
        Entity hitEntity = result.getEntity();

        if (hitEntity instanceof LivingEntity target && owner instanceof ServerPlayer serverPlayer) {
            Damager.hurt(serverPlayer, target, DAMAGE, true);
        } else if (hitEntity instanceof LivingEntity target && owner instanceof LivingEntity livingOwner) {
            target.hurt(target.damageSources().mobProjectile(this, livingOwner), DAMAGE);
        } else if (hitEntity instanceof LivingEntity target) {
            target.hurt(target.damageSources().generic(), DAMAGE);
        } else if (owner instanceof LivingEntity livingOwner) {
            hitEntity.hurt(hitEntity.damageSources().mobProjectile(this, livingOwner), DAMAGE);
        } else {
            hitEntity.hurt(hitEntity.damageSources().generic(), DAMAGE);
        }

        if (hitEntity instanceof LivingEntity target && this.getItem().is(ModItems.TIPPED_KUNAI.get())) {
            TippedKunaiUtil.applyStoredEffects(this.getItem(), target);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void spawnTrailParticles() {
        ItemStack item = this.getItem();
        double stepX = (this.xo - this.getX()) / TRAIL_PARTICLE_COUNT;
        double stepY = (this.yo - this.getY()) / TRAIL_PARTICLE_COUNT;
        double stepZ = (this.zo - this.getZ()) / TRAIL_PARTICLE_COUNT;

        if (item.is(ModItems.TIPPED_KUNAI.get()) && !TippedKunaiUtil.effectEntries(item).isEmpty()) {
            int color = TippedKunaiUtil.particleColor(item);
            double red = ((color >> 16) & 0xFF) / 255.0D;
            double green = ((color >> 8) & 0xFF) / 255.0D;
            double blue = (color & 0xFF) / 255.0D;
            for (int i = 0; i < TRAIL_PARTICLE_COUNT; i++) {
                this.level().addParticle(ParticleTypes.ENTITY_EFFECT,
                    this.getX() + stepX * i,
                    this.getY() + stepY * i,
                    this.getZ() + stepZ * i,
                    red, green, blue);
            }
            return;
        }

        for (int i = 0; i < TRAIL_PARTICLE_COUNT; i++) {
            this.level().addParticle(ParticleTypes.CRIT,
                this.getX() + stepX * i,
                this.getY() + stepY * i,
                this.getZ() + stepZ * i,
                0.0D, 0.0D, 0.0D);
        }
    }
}
