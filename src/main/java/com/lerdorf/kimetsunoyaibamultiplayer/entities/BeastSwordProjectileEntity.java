package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.EnhancedBeastForms;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Thrown Beast sword that returns once, then restores its complete ItemStack. */
public class BeastSwordProjectileEntity extends ThrowableItemProjectile {
    private static final float DAMAGE = 10.0F;
    private boolean bouncedBack;
    private boolean restored;
    private boolean damageApplied;
    private boolean dualWield;
    private CompoundTag storedSwordTag = new CompoundTag();

    public BeastSwordProjectileEntity(EntityType<? extends BeastSwordProjectileEntity> type, Level level) {
        super(type, level);
    }

    public BeastSwordProjectileEntity(EntityType<? extends BeastSwordProjectileEntity> type,
                                      LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    public void setSwordStack(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        this.storedSwordTag = copy.save(new CompoundTag());
        this.setItem(copy);
    }

    public void setDualWield(boolean dualWield) {
        this.dualWield = dualWield;
    }

    public boolean hasBouncedBack() {
        return this.bouncedBack;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD;
    }

    @Override
    public void tick() {
        if (this.bouncedBack && !this.level().isClientSide) {
            Entity owner = this.getOwner();
            if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) {
                dropSword();
                return;
            }

            Vec3 toOwner = livingOwner.getEyePosition().subtract(this.position());
            if (toOwner.lengthSqr() <= 2.25D) {
                restoreToOwner(livingOwner);
                return;
            }

            Vec3 velocity = toOwner.normalize().scale(1.8D);
            this.setDeltaMovement(velocity);
            this.setYRot((float) (Math.atan2(velocity.z, velocity.x) * 180.0D / Math.PI) - 90.0F);
            this.setXRot((float) (-(Math.atan2(velocity.y,
                Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)) * 180.0D / Math.PI)));
        }

        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide || this.restored) {
            return;
        }

        Entity hitEntity = result.getEntity();
        Entity owner = this.getOwner();
        if (this.bouncedBack) {
            if (hitEntity == owner && owner instanceof LivingEntity livingOwner) {
                restoreToOwner(livingOwner);
            }
            return;
        }

        if (!this.damageApplied && hitEntity instanceof LivingEntity target
                && hitEntity != owner && owner instanceof LivingEntity attacker) {
            this.damageApplied = true;
            EnhancedBeastForms.hurtWithBeastBleeding(attacker, target, DAMAGE, true, this.dualWield);
        }
        beginReturn();
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide || this.restored) {
            return;
        }

        if (result instanceof EntityHitResult entityHit) {
            onHitEntity(entityHit);
            return;
        }
        if (this.bouncedBack) {
            dropSword();
        } else {
            beginReturn();
        }
    }

    private void beginReturn() {
        if (this.bouncedBack || this.restored) {
            return;
        }
        this.bouncedBack = true;
        this.setNoGravity(true);
        Entity owner = this.getOwner();
        Vec3 returnDirection = owner == null
            ? this.getDeltaMovement().reverse().normalize()
            : owner.position().add(0, owner.getBbHeight() * 0.65D, 0).subtract(this.position()).normalize();
        if (returnDirection.lengthSqr() < 1.0E-6D) {
            returnDirection = new Vec3(0, 0, 1);
        }
        this.setDeltaMovement(returnDirection.scale(1.8D));
        this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK,
            SoundSource.PLAYERS, 0.9F, 1.2F);
    }

    private void restoreToOwner(LivingEntity owner) {
        if (this.restored) {
            return;
        }
        this.restored = true;
        ItemStack sword = getStoredSword();
        boolean placed = false;

        if (owner instanceof Player player) {
            if (player.getMainHandItem().isEmpty()) {
                player.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
                placed = true;
            } else if (player.getOffhandItem().isEmpty()) {
                player.setItemSlot(EquipmentSlot.OFFHAND, sword.copy());
                placed = true;
            } else {
                placed = player.getInventory().add(sword);
            }
        } else if (owner.getMainHandItem().isEmpty()) {
            owner.setItemSlot(EquipmentSlot.MAINHAND, sword.copy());
            placed = true;
        } else if (owner.getOffhandItem().isEmpty()) {
            owner.setItemSlot(EquipmentSlot.OFFHAND, sword.copy());
            placed = true;
        }

        if (!placed) {
            dropStackAt(sword, owner.getX(), owner.getY(), owner.getZ());
        }
        owner.level().playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
            SoundSource.PLAYERS, 0.7F, 1.5F);
        this.discard();
    }

    private void dropSword() {
        if (this.restored) {
            return;
        }
        this.restored = true;
        dropStack(getStoredSword());
        this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP,
            SoundSource.NEUTRAL, 0.8F, 0.7F);
        this.discard();
    }

    private void dropStack(ItemStack stack) {
        dropStackAt(stack, this.getX(), this.getY(), this.getZ());
    }

    private void dropStackAt(ItemStack stack, double x, double y, double z) {
        if (stack.isEmpty()) {
            this.discard();
            return;
        }
        ItemEntity itemEntity = new ItemEntity(this.level(), x, y, z, stack);
        itemEntity.setDefaultPickUpDelay();
        this.level().addFreshEntity(itemEntity);
    }

    private ItemStack getStoredSword() {
        if (!this.storedSwordTag.isEmpty()) {
            return ItemStack.of(this.storedSwordTag.copy());
        }
        return this.getItem().copy();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("SwordItem", this.getStoredSword().save(new CompoundTag()));
        tag.putBoolean("BouncedBack", this.bouncedBack);
        tag.putBoolean("Restored", this.restored);
        tag.putBoolean("DamageApplied", this.damageApplied);
        tag.putBoolean("DualWield", this.dualWield);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SwordItem")) {
            this.storedSwordTag = tag.getCompound("SwordItem").copy();
            this.setItem(ItemStack.of(this.storedSwordTag.copy()));
        }
        this.bouncedBack = tag.getBoolean("BouncedBack");
        this.restored = tag.getBoolean("Restored");
        this.damageApplied = tag.getBoolean("DamageApplied");
        this.dualWield = tag.getBoolean("DualWield");
        if (this.bouncedBack) {
            this.setNoGravity(true);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
