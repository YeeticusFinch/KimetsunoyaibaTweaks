package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DamageTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

/**
 * Handles demon slayer entity aggro towards demons.
 *
 * This handler ensures that all demon slayer entities (including hashira)
 * automatically target demons at any time, not just when they first spawn.
 *
 * Targeting priorities:
 * 1. Entities with the kimetsunoyaiba:demon tag
 * 2. Entities with the kimetsunoyaiba:twelve_kizuki tag
 * 3. Players with the "oni" NBT tag (demon players)
 * 4. Entities identified as demons by EntityCategorization
 */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DemonSlayerAggroHandler {
    private static final String DEMON_AGGRO_ADDED_TAG = "knymp_added_demon_aggro_goal";
    private static final String DEMON_TARGET_SLAYER_GOAL_TAG = "knymp_added_demon_target_slayer_goal";
    private static final String HOSTILE_TARGET_SLAYER_GOAL_TAG = "knymp_added_hostile_target_slayer_goal";
    private static final TagKey<EntityType<?>> FORGE_WOMAN_TAG = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("forge", "woman")
    );

    // How often to scan for demons (in ticks). 20 ticks = 1 second
    private static final int SCAN_INTERVAL = 20;

    // Maximum range to scan for demons
    private static final double SCAN_RANGE = 32.0;
    // Hostile non-demon mobs should use a tighter slayer aggro radius.
    private static final double HOSTILE_SLAYER_SCAN_RANGE = 24.0;

    /**
     * Add targeting goals when a demon slayer entity joins the level.
     * This provides the base AI targeting capability.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        long startNanos = System.nanoTime();
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        Log.startupProbeOnce("DemonSlayerAggroHandler.onEntityJoin");

        // Keep entity-join work minimal. Chunk/entity loading during world entry can invoke this
        // for many mobs at once, so defer range scans for demon/hostile retargeting to periodic ticks.
        if (isDemonSlayerOrHashira(mob)) {
            addSlayerAggroGoals(mob);
        }
        Log.debugVisibleIfSlow(
            "demon-slayer-aggro-join",
            startNanos,
            25L,
            "DemonSlayerAggroHandler.onEntityJoin entity={} pos={}, {}, {}",
            EntityTagHelper.getEntityTypeId(mob),
            mob.getBlockX(),
            mob.getBlockY(),
            mob.getBlockZ()
        );
    }

    private static void addSlayerAggroGoals(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(DEMON_AGGRO_ADDED_TAG)) {
            return;
        }

        // Add targeting goals for demons
        // Priority 2: Target demons and kizuki by tag
        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
            mob,
            LivingEntity.class,
            10,    // randomInterval - check every 10 ticks
            true,  // mustSee
            false, // mustReach
            DemonSlayerAggroHandler::isDemonTarget
        ));

        // Priority 3: Target demon players (players with "oni" NBT tag)
        mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
            mob,
            Player.class,
            10,
            true,
            false,
            player -> player.getPersistentData().getBoolean("oni")
                && !DemonTransformationHandler.hasTransformationTag(player)
        ));

        data.putBoolean(DEMON_AGGRO_ADDED_TAG, true);
        Log.debug("[DemonSlayerAggro] Added demon targeting goals to: " + mob.getType().getDescriptionId());
    }

    private static void addDemonVsSlayerGoal(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(DEMON_TARGET_SLAYER_GOAL_TAG)) {
            return;
        }

        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
            mob,
            DemonSlayerEntity.class,
            10,
            true,
            false,
            target -> canDemonTargetSlayer(mob, target) && mob.distanceToSqr(target) <= (SCAN_RANGE * SCAN_RANGE)
        ));

        data.putBoolean(DEMON_TARGET_SLAYER_GOAL_TAG, true);
        Log.debug("[DemonSlayerAggro] Added demon->slayer targeting goal to: {}", mob.getType().getDescriptionId());
    }

    private static void addHostileVsSlayerGoal(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(HOSTILE_TARGET_SLAYER_GOAL_TAG)) {
            return;
        }

        mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
            mob,
            DemonSlayerEntity.class,
            10,
            true,
            false,
            target -> target != null && target.isAlive()
                && mob.distanceToSqr(target) <= (HOSTILE_SLAYER_SCAN_RANGE * HOSTILE_SLAYER_SCAN_RANGE)
        ));

        data.putBoolean(HOSTILE_TARGET_SLAYER_GOAL_TAG, true);
        Log.debug("[DemonSlayerAggro] Added hostile->slayer targeting goal to: {}", mob.getType().getDescriptionId());
    }

    /**
     * Periodically scan for nearby demons and set as target if needed.
     * This ensures targeting works continuously, not just relying on AI goals.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (isDemonSlayerOrHashira(mob)) {
            tickSlayerAggro(mob);
            return;
        }

        // Keep demon-tagged mobs focused on nearby DemonSlayerEntity targets.
        if (EntityTagHelper.isDemon(mob)) {
            tickDemonVsSlayerAggro(mob);
            return;
        }

        if (isHostileMobForSlayer(mob)) {
            tickHostileVsSlayerAggro(mob);
        }
    }

    private static void tickSlayerAggro(Mob mob) {
        LivingEntity currentTarget = mob.getTarget();
        if (CrowEnhancementHandler.isKasugaiCrow(currentTarget)) {
            mob.setTarget(null);
            currentTarget = null;
        }

        if (isFriendlySlayerTarget(currentTarget) && !shouldKeepFriendlyFireTarget(mob, currentTarget)) {
            mob.setTarget(null);
            currentTarget = null;
        }

        // Only scan periodically to reduce performance impact
        if (mob.tickCount % SCAN_INTERVAL != 0) {
            return;
        }

        // If the mob already has a valid demon target, don't change it
        if (currentTarget != null && currentTarget.isAlive() && isDemonTarget(currentTarget)) {
            return;
        }

        // Scan for nearby demons
        LivingEntity nearestDemon = findNearestDemon(mob);
        if (nearestDemon != null) {
            mob.setTarget(nearestDemon);
            Log.debug("[DemonSlayerAggro] " + mob.getType().getDescriptionId() +
                " targeting demon: " + nearestDemon.getType().getDescriptionId());
        }
    }

    private static void tickDemonVsSlayerAggro(Mob mob) {
        if (mob.tickCount % SCAN_INTERVAL != 0) {
            return;
        }

        // Only add this targeting goal when a slayer is actually nearby.
        if (!mob.getPersistentData().getBoolean(DEMON_TARGET_SLAYER_GOAL_TAG)
            && findNearestTargetableDemonSlayer(mob, SCAN_RANGE) != null) {
            addDemonVsSlayerGoal(mob);
        }

        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()
            && canDemonTargetSlayer(mob, currentTarget)
            && mob.distanceToSqr(currentTarget) <= (SCAN_RANGE * SCAN_RANGE)) {
            return;
        }

        LivingEntity nearestSlayer = findNearestTargetableDemonSlayer(mob, SCAN_RANGE);
        if (nearestSlayer != null) {
            mob.setTarget(nearestSlayer);
            Log.debug("[DemonSlayerAggro] {} targeting demon slayer: {}",
                mob.getType().getDescriptionId(), nearestSlayer.getType().getDescriptionId());
        } else if (currentTarget instanceof DemonSlayerEntity) {
            mob.setTarget(null);
        }
    }

    private static void tickHostileVsSlayerAggro(Mob mob) {
        if (mob.tickCount % SCAN_INTERVAL != 0) {
            return;
        }

        // Only add this targeting goal when a slayer is nearby.
        if (!mob.getPersistentData().getBoolean(HOSTILE_TARGET_SLAYER_GOAL_TAG)
            && findNearestTargetableDemonSlayer(mob, HOSTILE_SLAYER_SCAN_RANGE) != null) {
            addHostileVsSlayerGoal(mob);
        }

        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget instanceof DemonSlayerEntity slayer
            && slayer.isAlive()
            && mob.distanceToSqr(slayer) <= (HOSTILE_SLAYER_SCAN_RANGE * HOSTILE_SLAYER_SCAN_RANGE)) {
            return;
        }

        LivingEntity nearestSlayer = findNearestTargetableDemonSlayer(mob, HOSTILE_SLAYER_SCAN_RANGE);
        if (nearestSlayer != null) {
            mob.setTarget(nearestSlayer);
        } else if (currentTarget instanceof DemonSlayerEntity) {
            mob.setTarget(null);
        }
    }

    /**
     * Find the nearest demon within scan range.
     */
    private static LivingEntity findNearestDemon(Mob slayer) {
        AABB searchBox = slayer.getBoundingBox().inflate(SCAN_RANGE);

        List<LivingEntity> nearbyEntities = slayer.level().getEntitiesOfClass(
            LivingEntity.class,
            searchBox,
            entity -> entity != slayer && entity.isAlive() && isDemonTarget(entity)
        );

        if (nearbyEntities.isEmpty()) {
            return null;
        }

        // Return the closest demon
        return nearbyEntities.stream()
            .min(Comparator.comparingDouble(e -> slayer.distanceToSqr(e)))
            .orElse(null);
    }

    private static LivingEntity findNearestTargetableDemonSlayer(Mob demon, double range) {
        AABB searchBox = demon.getBoundingBox().inflate(range);

        List<DemonSlayerEntity> nearbySlayers = demon.level().getEntitiesOfClass(
            DemonSlayerEntity.class,
            searchBox,
            slayer -> slayer != demon
                && slayer.isAlive()
                && canDemonTargetSlayer(demon, slayer)
                && demon.distanceToSqr(slayer) <= (range * range)
        );

        if (nearbySlayers.isEmpty()) {
            return null;
        }

        return nearbySlayers.stream()
            .min(Comparator.comparingDouble(e -> demon.distanceToSqr(e)))
            .orElse(null);
    }

    /**
     * Check if an entity is a demon slayer or hashira.
     * Includes both tag-based checks and class-based checks.
     */
    private static boolean isDemonSlayerOrHashira(Mob mob) {
        // Ubuyashiki children intentionally share the demon_slayer tag for systems,
        // but should remain passive and never run slayer aggro logic.
        if (mob instanceof UbuyashikiKidEntity) {
            return false;
        }

        // Check our custom tags
        if (EntityTagHelper.isDemonSlayer(mob) || EntityTagHelper.isHashira(mob)) {
            return true;
        }

        // Check for our custom entities
        if (mob instanceof BreathingSlayerEntity) {
            return true;
        }

        // Check entity type ID for base mod slayers
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(mob);
        if (entityId != null) {
            String path = entityId.getPath();
            String namespace = entityId.getNamespace();

            // Base mod demon slayers
            if ("kimetsunoyaiba".equals(namespace)) {
                // Check for known demon slayer entity types
                if (path.equals("demon_slayer") || path.equals("mob_slayer") ||
                    path.equals("tomioka") || path.equals("rengoku") ||
                    path.equals("uzui") || path.equals("muichirou") ||
                    path.equals("kanroji") || path.equals("iguro") ||
                    path.equals("shinazugawa") || path.equals("himejima") ||
                    path.equals("kocho") || path.equals("kanae") ||
                    path.equals("tanjiro") || path.equals("zennitsu") ||
                    path.equals("inosuke") || path.equals("genya") ||
                    path.equals("kanawo") || path.equals("nezuko") ||
                    path.equals("sabito") || path.equals("makomo") ||
                    path.equals("murata") || path.equals("urokodaki") ||
                    path.equals("kuwajima") || path.equals("pandan")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Immediately retarget nearby demon slayers to attack an aggressor.
     */
    public static void alertNearbySlayersToAttacker(LivingEntity protectedEntity, LivingEntity attacker, double range) {
        if (protectedEntity == null || attacker == null || !attacker.isAlive() || protectedEntity.level().isClientSide()) {
            return;
        }

        AABB searchBox = protectedEntity.getBoundingBox().inflate(range);
        List<Mob> nearby = protectedEntity.level().getEntitiesOfClass(
            Mob.class,
            searchBox,
            mob -> mob.isAlive() && mob != protectedEntity && mob != attacker && isDemonSlayerOrHashira(mob)
        );

        for (Mob mob : nearby) {
            mob.setTarget(attacker);
            mob.setLastHurtByMob(attacker);
        }
    }

    private static boolean canDemonTargetSlayer(Mob demon, LivingEntity target) {
        if (!(target instanceof DemonSlayerEntity) || !target.isAlive()) {
            return false;
        }

        ResourceLocation demonId = EntityTagHelper.getEntityTypeId(demon);
        String path = demonId != null ? demonId.getPath().toLowerCase() : "";

        // Akaza variants never target forge:woman entities.
        if (path.contains("akaza")) {
            return !isWoman(target);
        }

        // Doma/Douma variants only target forge:woman unless attacked first.
        if (path.contains("doma") || path.contains("douma")) {
            if (isWoman(target)) {
                return true;
            }
            return wasRecentlyAttackedBy(demon, target);
        }

        return true;
    }

    private static boolean isHostileMobForSlayer(Mob mob) {
        return mob instanceof Monster
            && !EntityTagHelper.isDemon(mob)
            && !EntityTagHelper.isDemonSlayer(mob)
            && !EntityTagHelper.isHashira(mob)
            && !EntityTagHelper.isKamaboko(mob);
    }

    private static boolean isFriendlySlayerTarget(LivingEntity entity) {
        return entity != null
            && entity.isAlive()
            && (EntityTagHelper.isDemonSlayer(entity)
                || EntityTagHelper.isHashira(entity)
                || EntityTagHelper.isKamaboko(entity));
    }

    private static boolean shouldKeepFriendlyFireTarget(Mob attacker, LivingEntity target) {
        if (attacker == null || target == null || !target.isAlive()) {
            return false;
        }

        // Let external systems like Mob Battle keep the fight when they have explicitly set aggro.
        if (target instanceof Mob targetMob && targetMob.getTarget() == attacker) {
            return true;
        }

        if (attacker.getLastHurtByMob() == target || target.getLastHurtByMob() == attacker) {
            return true;
        }

        return DamageTracker.hasDamageHistory(attacker, target);
    }

    private static boolean isWoman(LivingEntity entity) {
        return entity.getType().is(FORGE_WOMAN_TAG);
    }

    private static boolean wasRecentlyAttackedBy(Mob demon, LivingEntity attacker) {
        LivingEntity lastHurtBy = demon.getLastHurtByMob();
        if (lastHurtBy != attacker) {
            return false;
        }
        int ticksSinceAttack = demon.tickCount - demon.getLastHurtByMobTimestamp();
        return ticksSinceAttack >= 0 && ticksSinceAttack <= 200;
    }

    /**
     * Check if an entity is a valid demon target.
     */
    public static boolean isDemonTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (CrowEnhancementHandler.isKasugaiCrow(target)) {
            return false;
        }

        if (target instanceof Player player && DemonTransformationHandler.hasTransformationTag(player)) {
            return false;
        }

        // Check entity tags first (most reliable)
        if (EntityTagHelper.isDemon(target) || EntityTagHelper.isTwelveKizuki(target)) {
            return true;
        }

        // Check if it's a demon player
        if (target instanceof Player player) {
            return isDemonPlayer(player);
        }

        // Fallback: Check by entity ID using EntityCategorization
        ResourceLocation entityId = EntityTagHelper.getEntityTypeId(target);
        if (entityId != null && EntityCategorization.isDemon(entityId)) {
            return true;
        }

        // Additional check: Look for common demon entity name patterns
        if (entityId != null) {
            String path = entityId.getPath().toLowerCase();
            String namespace = entityId.getNamespace();

            // Base mod demons
            if ("kimetsunoyaiba".equals(namespace)) {
                if (path.startsWith("demon") || path.equals("muzan") || path.equals("muzan_2") ||
                    path.equals("akaza") || path.equals("doma") || path.equals("kokushibo") ||
                    path.equals("gyokko") || path.equals("gyokko_2") || path.equals("gyutaro") ||
                    path.equals("daki") || path.equals("kaigaku") || path.equals("zohakuten") ||
                    path.equals("hantengu") || path.startsWith("hantengu_") ||
                    path.equals("nakime") || path.equals("enmu") || path.equals("rui") ||
                    path.startsWith("rui_") || path.equals("rokuro") || path.equals("wakuraba") ||
                    path.equals("hairo") || path.equals("mukago") || path.equals("kamanue") ||
                    path.equals("kyogai") || path.equals("susamaru") || path.equals("yahaba") ||
                    path.equals("hand_demon") || path.equals("spider_demon") ||
                    path.equals("temple_demon") || path.equals("swamp_demon") ||
                    path.equals("goldfishbig") || path.equals("dice_steak_senior_demon") ||
                    path.equals("tanjiro_demon") || path.equals("shadow_wolf") ||
                    path.equals("yushiro") || path.equals("tamayo")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if a player is a demon (has "oni" NBT tag).
     */
    private static boolean isDemonPlayer(Player player) {
        if (player == null) {
            return false;
        }
        return player.getPersistentData().getBoolean("oni");
    }
}
