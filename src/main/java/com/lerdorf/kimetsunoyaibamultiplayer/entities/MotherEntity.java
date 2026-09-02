package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.DemonwebPuppetry;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.BloodDemonArtM1AttackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

/** Mother, the stationary spider-family puppet master. */
public class MotherEntity extends AbstractDemonEntity {
    public static final String BLOOD_DEMON_ART_ID = DemonwebPuppetry.ART_ID;

    private static final String SITTING_STONE_X_KEY = "KnYMotherSittingStoneX";
    private static final String SITTING_STONE_Y_KEY = "KnYMotherSittingStoneY";
    private static final String SITTING_STONE_Z_KEY = "KnYMotherSittingStoneZ";
    private static final String SITTING_STONE_PLACED_KEY = "KnYMotherSittingStonePlaced";
    private static final int ABILITY_COOLDOWN_TICKS = 100;
    private static final double WEB_MELEE_RANGE = 5.0D;
    private static final int WEB_MELEE_COOLDOWN_TICKS = 10;
    private static final double SAFE_DISTANCE = 20.0D;
    private static final double ESCAPE_SPEED = 0.45D;

    private int webMeleeCooldownTicks;

    public MotherEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, 70.0D)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.ARMOR, 4.0D)
            .add(Attributes.FOLLOW_RANGE, 200.0D);
    }

    public static void registerBloodDemonArt() {
        DemonwebPuppetry.register();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected boolean canTargetNonDemonVictim(LivingEntity target) {
        // Mother chooses targets herself so the base demon player retargeter
        // cannot make her abandon the throne unexpectedly.
        return false;
    }

    @Override
    protected void tickBloodDemonArt() {
        if (com.lerdorf.kimetsunoyaibamultiplayer.effects.PuppetryHandler.isAbilityUseBlocked(this)
            || getExternalBloodDemonArtCooldownTicks() > 0
            || DemonwebPuppetry.hasActiveManifestation(this)
            || isUsingLockedAnimation()
            || !(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = getTarget();
        if (!DemonwebPuppetry.isValidManifestationTarget(this, target)) {
            target = DemonwebPuppetry.findNearestEnemy(this, serverLevel, 200.0D, false);
        }
        if (target == null || distanceToSqr(target) <= WEB_MELEE_RANGE * WEB_MELEE_RANGE) {
            return;
        }

        faceTarget(target);
        if (DemonwebPuppetry.spawnManifestation(this, target, serverLevel)) {
            setTarget(target);
            setExternalBloodDemonArtCooldownTicks(ABILITY_COOLDOWN_TICKS);
        }
    }

    @Override
    protected String resolveWalkAnimation() {
        return "walk_female";
    }

    @Override
    protected String resolveSprintAnimation() {
        // Mother never uses the sprint animation, even if a movement speed
        // calculation crosses the base class sprint threshold.
        return "walk_female";
    }

    @Override
    protected boolean isSprintAnimation(String animation) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) {
            return;
        }

        if (webMeleeCooldownTicks > 0) {
            webMeleeCooldownTicks--;
        }
        setSprinting(false);
        if (!DemonwebPuppetry.hasWebTraversal(this)) {
            setNoGravity(false);
        }

        LivingEntity closeEnemy = DemonwebPuppetry.findNearestEnemy(this,
            (net.minecraft.server.level.ServerLevel) level(), (int) WEB_MELEE_RANGE, false);
        LivingEntity currentTarget = getTarget();
        if (closeEnemy == null && DemonwebPuppetry.isValidManifestationTarget(this, currentTarget)
            && distanceToSqr(currentTarget) <= WEB_MELEE_RANGE * WEB_MELEE_RANGE) {
            closeEnemy = currentTarget;
        }
        if (closeEnemy != null) {
            setTarget(closeEnemy);
            if (webMeleeCooldownTicks <= 0
                && BloodDemonArtM1AttackHandler.performWebSlashAttack(this, closeEnemy.getUUID())) {
                webMeleeCooldownTicks = WEB_MELEE_COOLDOWN_TICKS;
            }
        }

        if (DemonwebPuppetry.hasWebTraversal(this)) {
            tickWebTraversalEscape();
            return;
        }

        if (closeEnemy != null) {
            DemonwebPuppetry.activateWebTraversal(this);
            stopSitting();
            tickWebTraversalEscape();
            return;
        }

        LivingEntity visibleEnemy = DemonwebPuppetry.findVisibleEnemy(this);
        if (visibleEnemy != null) {
            setTarget(visibleEnemy);
            sitOnStone();
            return;
        }

        sitOnStone();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker
            && DemonwebPuppetry.isEnemyTarget(attacker)) {
            setTarget(attacker);
            DemonwebPuppetry.activateWebTraversal(this);
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return super.causeFallDamage(fallDistance, damageMultiplier * 0.1F, source);
    }

    private void tickWebTraversalEscape() {
        setSprinting(false);
        setInSittingPose(false);
        getNavigation().stop();
        setNoGravity(true);

        List<LivingEntity> enemies = level().getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(200.0D), target -> DemonwebPuppetry.isEnemyTarget(target)
                && distanceToSqr(target) <= 200.0D * 200.0D);
        Vec3 away = Vec3.ZERO;
        boolean needsDistance = false;
        for (LivingEntity enemy : enemies) {
            double distance = distanceTo(enemy);
            if (distance <= SAFE_DISTANCE) {
                needsDistance = true;
                Vec3 fromEnemy = position().subtract(enemy.position());
                if (fromEnemy.lengthSqr() < 1.0E-4D) {
                    fromEnemy = new Vec3(1.0D, 0.0D, 0.0D);
                }
                away = away.add(fromEnemy.normalize().scale(Math.max(1.0D, SAFE_DISTANCE - distance)));
            }
        }

        if (!needsDistance) {
            DemonwebPuppetry.deactivateWebTraversal(this);
            setNoGravity(false);
            sitOnStone();
            return;
        }

        if (away.lengthSqr() < 1.0E-4D) {
            away = getLookAngle();
        }
        Vec3 movement = away.normalize().scale(ESCAPE_SPEED);
        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
        hurtMarked = true;
    }

    private void sitOnStone() {
        if (!ensureSittingStone()) {
            setInSittingPose(false);
            setNoGravity(false);
            return;
        }

        setSprinting(false);
        setInSittingPose(true);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
    }

    private void stopSitting() {
        setInSittingPose(false);
        clearSittingStone();
    }

    private boolean ensureSittingStone() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        var data = getPersistentData();
        BlockPos stonePos;
        if (data.contains(SITTING_STONE_X_KEY) && data.contains(SITTING_STONE_Y_KEY)
            && data.contains(SITTING_STONE_Z_KEY)) {
            stonePos = new BlockPos(data.getInt(SITTING_STONE_X_KEY), data.getInt(SITTING_STONE_Y_KEY),
                data.getInt(SITTING_STONE_Z_KEY));
        } else {
            stonePos = blockPosition();
            data.putInt(SITTING_STONE_X_KEY, stonePos.getX());
            data.putInt(SITTING_STONE_Y_KEY, stonePos.getY());
            data.putInt(SITTING_STONE_Z_KEY, stonePos.getZ());
        }

        if (data.getBoolean(SITTING_STONE_PLACED_KEY)) {
            if (!serverLevel.getBlockState(stonePos).is(Blocks.STONE)
                || !hasSolidSupport(serverLevel, stonePos)) {
                if (serverLevel.getBlockState(stonePos).is(Blocks.STONE)) {
                    serverLevel.removeBlock(stonePos, false);
                }
                clearSittingStoneData(data);
                return false;
            }
            setPos(stonePos.getX() + 0.5D, stonePos.getY() + 1.0D, stonePos.getZ() + 0.5D);
            return true;
        }

        // A naturally generated stone directly below her is also a valid seat.
        if (serverLevel.getBlockState(blockPosition().below()).is(Blocks.STONE)) {
            clearSittingStoneData(data);
            return true;
        } else if (serverLevel.getBlockState(stonePos).isAir()
            && hasSolidSupport(serverLevel, stonePos)) {
            serverLevel.setBlock(stonePos, Blocks.STONE.defaultBlockState(), 3);
            data.putBoolean(SITTING_STONE_PLACED_KEY, true);
            setPos(stonePos.getX() + 0.5D, stonePos.getY() + 1.0D, stonePos.getZ() + 0.5D);
            return true;
        }

        clearSittingStoneData(data);
        return false;
    }

    private boolean hasSolidSupport(net.minecraft.server.level.ServerLevel level, BlockPos stonePos) {
        for (Direction direction : Direction.values()) {
            BlockPos supportPos = stonePos.relative(direction);
            var supportState = level.getBlockState(supportPos);
            if (supportState.isAir() || !supportState.getFluidState().isEmpty()) {
                continue;
            }
            if (!supportState.getCollisionShape(level, supportPos).isEmpty()
                || supportState.isSolid()
                || supportState.isSolidRender(level, supportPos)
                || supportState.is(BlockTags.LEAVES)) {
                return true;
            }
        }
        return false;
    }

    private void clearSittingStoneData(net.minecraft.nbt.CompoundTag data) {
        data.remove(SITTING_STONE_X_KEY);
        data.remove(SITTING_STONE_Y_KEY);
        data.remove(SITTING_STONE_Z_KEY);
        data.remove(SITTING_STONE_PLACED_KEY);
    }

    private void faceTarget(LivingEntity target) {
        getLookControl().setLookAt(target, 180.0F, 180.0F);
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getEyeY() - getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontal) * (180.0D / Math.PI)));
        setYRot(yaw);
        setYHeadRot(yaw);
        setXRot(pitch);
    }

    private void clearSittingStone() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        var data = getPersistentData();
        if (data.getBoolean(SITTING_STONE_PLACED_KEY)
            && data.contains(SITTING_STONE_X_KEY) && data.contains(SITTING_STONE_Y_KEY)
            && data.contains(SITTING_STONE_Z_KEY)) {
            BlockPos stonePos = new BlockPos(data.getInt(SITTING_STONE_X_KEY), data.getInt(SITTING_STONE_Y_KEY),
                data.getInt(SITTING_STONE_Z_KEY));
            if (serverLevel.getBlockState(stonePos).is(Blocks.STONE)) {
                serverLevel.removeBlock(stonePos, false);
                setPos(getX(), getY() - 1.0D, getZ());
            }
        }
        data.remove(SITTING_STONE_X_KEY);
        data.remove(SITTING_STONE_Y_KEY);
        data.remove(SITTING_STONE_Z_KEY);
        data.remove(SITTING_STONE_PLACED_KEY);
    }

    @Override
    public void die(DamageSource source) {
        try {
            stopSitting();
            DemonwebPuppetry.deactivateWebTraversal(this);
        } catch (Exception ex) {
            System.err.println("[MotherEntity] Failed to clean up Mother state on death: " + ex);
        }
        super.die(source);
    }
}
