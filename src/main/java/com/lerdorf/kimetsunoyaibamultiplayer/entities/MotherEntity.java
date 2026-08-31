package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts.DemonwebPuppetry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
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
    private static final double ENEMY_ALERT_RANGE = 10.0D;
    private static final double SAFE_DISTANCE = 20.0D;
    private static final double ESCAPE_SPEED = 0.45D;

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
        this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
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
        // Mother uses the forms according to her own behavior state.
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

        setSprinting(false);
        if (DemonwebPuppetry.hasWebTraversal(this)) {
            tickWebTraversalEscape();
            return;
        }

        LivingEntity closeEnemy = DemonwebPuppetry.findNearestEnemy(this, (net.minecraft.server.level.ServerLevel) level(),
            ENEMY_ALERT_RANGE, false);
        if (closeEnemy != null) {
            setTarget(closeEnemy);
            DemonwebPuppetry.activateWebTraversal(this);
            stopSitting();
            tickWebTraversalEscape();
            return;
        }

        LivingEntity visibleEnemy = DemonwebPuppetry.findVisibleEnemy(this);
        if (visibleEnemy != null) {
            setTarget(visibleEnemy);
            if (getExternalBloodDemonArtCooldownTicks() <= 0) {
                DemonwebPuppetry.applyPuppetryToTarget(this, visibleEnemy);
                setExternalBloodDemonArtCooldownTicks(ABILITY_COOLDOWN_TICKS);
            }
            sitOnStone();
            return;
        }

        if (DemonwebPuppetry.countPuppets(this) == 0
            && !DemonwebPuppetry.hasActiveManifestation(this)
            && getExternalBloodDemonArtCooldownTicks() <= 0) {
            LivingEntity distantEnemy = DemonwebPuppetry.findNearestEnemy(this,
                (net.minecraft.server.level.ServerLevel) level(), 200.0D, false);
            if (distantEnemy != null) {
                DemonwebPuppetry.spawnManifestation(this, distantEnemy,
                    (net.minecraft.server.level.ServerLevel) level());
                setExternalBloodDemonArtCooldownTicks(ABILITY_COOLDOWN_TICKS);
            }
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
        setSprinting(false);
        setInSittingPose(true);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        ensureSittingStone();
    }

    private void stopSitting() {
        setInSittingPose(false);
        clearSittingStone();
    }

    private void ensureSittingStone() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
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

        if (data.getBoolean(SITTING_STONE_PLACED_KEY)
            && !serverLevel.getBlockState(stonePos).is(Blocks.STONE)) {
            serverLevel.setBlock(stonePos, Blocks.STONE.defaultBlockState(), 3);
        } else if (!data.getBoolean(SITTING_STONE_PLACED_KEY)
            && serverLevel.getBlockState(stonePos).isAir()) {
            serverLevel.setBlock(stonePos, Blocks.STONE.defaultBlockState(), 3);
            data.putBoolean(SITTING_STONE_PLACED_KEY, true);
        }

        if (data.getBoolean(SITTING_STONE_PLACED_KEY)) {
            setPos(stonePos.getX() + 0.5D, stonePos.getY() + 1.0D, stonePos.getZ() + 0.5D);
        }
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
