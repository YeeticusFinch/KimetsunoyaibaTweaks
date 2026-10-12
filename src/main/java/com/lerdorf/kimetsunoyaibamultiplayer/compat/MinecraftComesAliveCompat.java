package com.lerdorf.kimetsunoyaibamultiplayer.compat;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.items.HumanFleshItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.McaDemonStatePacket;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SunlightImmunityHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

/**
 * Optional MCA integration. This class deliberately uses entity IDs and reflection so
 * the mod remains loadable when MCA is not installed.
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MinecraftComesAliveCompat {
    private static final String MCA_NAMESPACE = "mca";
    private static final String MCA_MOD_ID = "mca";
    public static final String MCA_DEMON_TAG = "KnYMpMcaDemonized";
    public static final String MCA_EYE_INDEX_TAG = "KnYMpMcaDemonEyesIndex";
    public static final String MCA_EYE_HUE_TAG = "KnYMpMcaDemonEyesHue";
    private static final String MCA_SUNLIGHT_BURN_TAG = "KnYMpMcaSunlightBurnTicks";
    private static final int EFFECT_REFRESH_TICKS = 100;
    private static final int SUNLIGHT_DEATH_TICKS = 40;

    private static Boolean installed;

    private MinecraftComesAliveCompat() {
    }

    public static boolean isInstalled() {
        if (installed == null) {
            installed = ModList.get().isLoaded(MCA_MOD_ID);
        }
        return installed;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        try {
            LivingEntity entity = event.getEntity();
            if (!isInstalled() || entity.level().isClientSide()) {
                return;
            }

            if (isMcaDemon(entity)) {
                maintainDemonState(entity);
            }

            if (!(entity instanceof Mob mob) || mob.tickCount % 10 != 0) {
                return;
            }

            if (Damager.isDemon(mob) && !isMcaDemon(mob)) {
                LivingEntity target = findNearest(mob, MinecraftComesAliveCompat::isMcaVillager);
                if (target != null && (mob.getTarget() == null
                    || !isMcaVillager(mob.getTarget())
                    || mob.distanceToSqr(target) < mob.distanceToSqr(mob.getTarget()))) {
                    mob.setTarget(target);
                }
            } else if (isMcaGuard(mob)) {
                LivingEntity target = findNearest(mob, candidate -> Damager.isDemon(candidate));
                if (target != null && (mob.getTarget() == null
                    || !Damager.isDemon(mob.getTarget())
                    || mob.distanceToSqr(target) < mob.distanceToSqr(mob.getTarget()))) {
                    mob.setTarget(target);
                }
            }
        } catch (RuntimeException exception) {
            System.err.println("[KnY-MP MCA] Failed to process an MCA tick: " + exception.getMessage());
        }
    }

    /**
     * Creates the normal villager flesh item and stores MCA's complete conversion data in it.
     * The existing generic flesh renderer is used for now; the data is retained for a future
     * MCA-aware item renderer or crafting recipe.
     */
    public static boolean addHumanFleshDrop(LivingEntity victim) {
        if (!isInstalled() || !isMcaVillager(victim)) {
            return false;
        }

        ItemStack stack = new ItemStack(ModItems.HUMAN_FLESH_3.get());
        CompoundTag itemTag = stack.getOrCreateTag();
        CompoundTag mcaData = captureMcaData(victim);
        if (mcaData != null) {
            itemTag.put(HumanFleshItem.MCA_VILLAGER_DATA_TAG, mcaData);
        }

        ResourceLocation entityId = EntityType.getKey(victim.getType());
        if (entityId != null) {
            itemTag.putString(HumanFleshItem.MCA_VILLAGER_TYPE_TAG, entityId.toString());
        }
        itemTag.putString(HumanFleshItem.MCA_VILLAGER_NAME_TAG, victim.getName().getString());
        victim.spawnAtLocation(stack);
        return true;
    }

    public static boolean isMcaVillagerTarget(Entity entity) {
        return isInstalled() && entity instanceof LivingEntity living && isMcaVillager(living);
    }

    public static boolean isMcaDemonized(Entity entity) {
        return isInstalled() && entity instanceof LivingEntity living && isMcaDemon(living);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher)
            || !isMcaDemonized(event.getTarget())
            || !(event.getTarget() instanceof LivingEntity demon)) {
            return;
        }

        ModNetworking.sendToPlayer(createDemonStatePacket(demon), watcher);
    }

    /**
     * Converts an MCA villager to MCA's matching zombie villager type and marks it as a demon.
     * This is public so a future Blood Demon Art or item can invoke the same conversion.
     */
    public static boolean demonize(LivingEntity villager) {
        if (!isInstalled() || !isMcaVillager(villager) || villager.isRemoved()) {
            return false;
        }

        ResourceLocation sourceId = EntityType.getKey(villager.getType());
        if (sourceId == null) {
            return false;
        }

        String zombiePath = sourceId.getPath().startsWith("female_")
            ? "female_zombie_villager"
            : "male_zombie_villager";
        EntityType<?> zombieType = ForgeRegistries.ENTITY_TYPES.getValue(
            ResourceLocation.fromNamespaceAndPath(MCA_NAMESPACE, zombiePath));
        if (zombieType == null) {
            return false;
        }

        Entity replacement = zombieType.create(villager.level());
        if (!(replacement instanceof LivingEntity)) {
            return false;
        }

        replacement.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), villager.getXRot());
        copyMcaData(villager, replacement, zombieType);
        replacement.setCustomName(villager.getCustomName());
        replacement.setCustomNameVisible(villager.isCustomNameVisible());
        replacement.getPersistentData().putBoolean(MCA_DEMON_TAG, true);
        replacement.getPersistentData().putBoolean("oni", true);
        replacement.getPersistentData().putInt(MCA_EYE_INDEX_TAG, villager.level().random.nextInt(8));
        replacement.getPersistentData().putInt(MCA_EYE_HUE_TAG, villager.level().random.nextInt(360));

        villager.level().addFreshEntity(replacement);
        villager.discard();
        ModNetworking.sendToTrackingAndSelf(createDemonStatePacket((LivingEntity) replacement), replacement);
        maintainDemonState((LivingEntity) replacement);
        return true;
    }

    private static McaDemonStatePacket createDemonStatePacket(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return new McaDemonStatePacket(
            entity.getId(),
            data.getInt(MCA_EYE_INDEX_TAG),
            data.getInt(MCA_EYE_HUE_TAG)
        );
    }

    private static void maintainDemonState(LivingEntity entity) {
        if (entity.tickCount % EFFECT_REFRESH_TICKS == 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_REFRESH_TICKS + 20, 1, true, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_REFRESH_TICKS + 20, 1, true, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_REFRESH_TICKS + 20, 1, true, false, false));
            entity.addEffect(new MobEffectInstance(ModEffects.DEMON_TRANSFORMATION.get(), EFFECT_REFRESH_TICKS + 20, 0, true, false, false));
        }

        if (!SunlightImmunityHelper.isInBurningSunlight(entity)) {
            entity.getPersistentData().putInt(MCA_SUNLIGHT_BURN_TAG, 0);
            return;
        }

        int burnTicks = entity.getPersistentData().getInt(MCA_SUNLIGHT_BURN_TAG) + 1;
        entity.getPersistentData().putInt(MCA_SUNLIGHT_BURN_TAG, burnTicks);
        entity.setSecondsOnFire(2);
        if (burnTicks % 10 == 0) {
            SunlightImmunityHelper.hurtSunlightBurn(entity, 10.0F);
        }
        if (burnTicks >= SUNLIGHT_DEATH_TICKS) {
            entity.discard();
        }
    }

    private static LivingEntity findNearest(Mob source, java.util.function.Predicate<LivingEntity> predicate) {
        double range = Math.max(32.0D, source.getAttributeValue(Attributes.FOLLOW_RANGE));
        AABB box = source.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = source.level().getEntitiesOfClass(
            LivingEntity.class,
            box,
            candidate -> candidate != source
                && candidate.isAlive()
                && source.distanceToSqr(candidate) <= range * range
                && predicate.test(candidate)
                && (!com.lerdorf.kimetsunoyaibamultiplayer.Config.requireLineOfSightAggro
                    || source.getSensing().hasLineOfSight(candidate))
        );
        return candidates.stream().min(Comparator.comparingDouble(source::distanceToSqr)).orElse(null);
    }

    private static boolean isMcaVillager(LivingEntity entity) {
        ResourceLocation id = EntityType.getKey(entity.getType());
        return id != null && MCA_NAMESPACE.equals(id.getNamespace())
            && ("male_villager".equals(id.getPath()) || "female_villager".equals(id.getPath()));
    }

    private static boolean isMcaZombieVillager(LivingEntity entity) {
        ResourceLocation id = EntityType.getKey(entity.getType());
        return id != null && MCA_NAMESPACE.equals(id.getNamespace())
            && ("male_zombie_villager".equals(id.getPath()) || "female_zombie_villager".equals(id.getPath()));
    }

    private static boolean isMcaDemon(LivingEntity entity) {
        return isMcaZombieVillager(entity) && entity.getPersistentData().getBoolean(MCA_DEMON_TAG);
    }

    private static boolean isMcaGuard(LivingEntity entity) {
        if (!isMcaVillager(entity)) {
            return false;
        }

        Object result = invokeNoArg(entity, "isGuard");
        if (result instanceof Boolean guard) {
            return guard;
        }

        result = invokeNoArg(entity, "getProfessionId");
        return result instanceof ResourceLocation profession
            && MCA_NAMESPACE.equals(profession.getNamespace())
            && "guard".equals(profession.getPath());
    }

    private static CompoundTag captureMcaData(LivingEntity source) {
        try {
            Method method = findMethod(source, "toNbtForConversion", 1);
            if (method == null) {
                return null;
            }
            Object value = method.invoke(source, source.getType());
            return value instanceof CompoundTag tag ? tag.copy() : null;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static void copyMcaData(LivingEntity source, Entity destination, EntityType<?> destinationType) {
        try {
            Method copyMethod = findMethod(destination, "copyVillagerAttributesFrom", 1);
            if (copyMethod != null) {
                copyMethod.invoke(destination, source);
                return;
            }

            Method writeMethod = findMethod(source, "toNbtForConversion", 1);
            Method readMethod = findMethod(destination, "readNbtForConversion", 2);
            if (writeMethod != null && readMethod != null) {
                Object data = writeMethod.invoke(source, destinationType);
                readMethod.invoke(destination, source.getType(), data);
            }
        } catch (ReflectiveOperationException exception) {
            System.err.println("[KnY-MP MCA] Failed to copy villager data during demonization: " + exception.getMessage());
        }
    }

    private static Object invokeNoArg(Object target, String name) {
        try {
            Method method = findMethod(target, name, 0);
            return method == null ? null : method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static Method findMethod(Object target, String name, int parameterCount) {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }
}
