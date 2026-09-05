package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RestrictSunGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.util.RandomSource;

/** Shared biped demon implementation for the named player-skin demons. */
public class NamedDemonEntity extends AbstractDemonEntity {
    private static final ResourceLocation MT_YOKO =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mt_yoko");
    private static final ResourceLocation MUGEN_CASTLE =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "mugen_castle_dimension");

    public NamedDemonEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(40.0D, 0.30D, 5.0D, 0.3D, 0.3D);
    }

    public static AttributeSupplier.Builder createMakenaAttributes() {
        return createAttributes(60.0D, 0.28D, 12.0D, 1.0D, 1.2D);
    }

    private static AttributeSupplier.Builder createAttributes(double health, double movementSpeed,
                                                               double armor, double armorToughness,
                                                               double knockbackResistance) {
        return AbstractDemonEntity.createDemonAttributes()
            .add(Attributes.MAX_HEALTH, health)
            .add(Attributes.ATTACK_DAMAGE, 3.0D)
            .add(Attributes.MOVEMENT_SPEED, movementSpeed)
            .add(Attributes.ARMOR, armor)
            .add(Attributes.FOLLOW_RANGE, 16.0D)
            .add(Attributes.ARMOR_TOUGHNESS, armorToughness)
            .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RestrictSunGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, getAttackSpeed(), true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.2D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
            10, true, false, this::canTargetNonDemonVictim));
    }

    @Override
    protected String resolveWalkAnimation() {
        return isFemaleDemon() ? "walk_female" : "walk";
    }

    @Override
    protected String resolveSprintAnimation() {
        return resolveWalkAnimation();
    }

    private boolean isFemaleDemon() {
        return switch (getDemonName()) {
            case "ari", "efe", "makena" -> true;
            default -> false;
        };
    }

    private double getAttackSpeed() {
        return switch (getDemonName()) {
            case "kai", "zuri" -> 1.4D; // Demon3-style attack speed
            case "makena" -> 1.0D; // Demon4-style attack speed
            default -> 1.2D;
        };
    }

    public String getDemonName() {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(getType());
        return id == null ? "efe" : id.getPath().replaceFirst("^demon_", "");
    }

    public ResourceLocation getSkinTexture() {
        return texture("textures/entity/demon_" + getDemonName() + ".png");
    }

    public ResourceLocation getEyesTexture() {
        return texture("textures/entity/demon_eyes_" + getDemonName() + ".png");
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", path);
    }

    /** Uses the same overworld/night restrictions as the base generic demons. */
    public static boolean canSpawn(EntityType<NamedDemonEntity> type, ServerLevelAccessor level,
                                    MobSpawnType reason, BlockPos pos, RandomSource random) {
        ServerLevel serverLevel = level.getLevel();
        ResourceLocation dimension = serverLevel.dimension().location();
        if (!dimension.equals(Level.OVERWORLD.location()) && !dimension.equals(MUGEN_CASTLE)) {
            return false;
        }
        if (serverLevel.isDay() && !serverLevel.isRaining() && !serverLevel.isThundering()) {
            return false;
        }

        ResourceLocation biome = serverLevel.registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
            .getKey(serverLevel.getBiome(pos).value());
        return !MT_YOKO.equals(biome)
            && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    public static void registerSpawnPlacements() {
        registerSpawnPlacement(ModEntities.DEMON_EFE.get());
        registerSpawnPlacement(ModEntities.DEMON_ARI.get());
        registerSpawnPlacement(ModEntities.DEMON_KAI.get());
        registerSpawnPlacement(ModEntities.DEMON_MAKENA.get());
        registerSpawnPlacement(ModEntities.DEMON_NOOR.get());
        registerSpawnPlacement(ModEntities.DEMON_SUNNY.get());
        registerSpawnPlacement(ModEntities.DEMON_ZURI.get());
    }

    private static void registerSpawnPlacement(EntityType<NamedDemonEntity> type) {
        net.minecraft.world.entity.SpawnPlacements.register(type,
            net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            NamedDemonEntity::canSpawn);
    }
}
