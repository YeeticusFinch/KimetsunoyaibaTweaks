package com.lerdorf.kimetsunoyaibamultiplayer.entities;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntityCategorization;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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

    // How often to scan for demons (in ticks). 20 ticks = 1 second
    private static final int SCAN_INTERVAL = 20;

    // Maximum range to scan for demons
    private static final double SCAN_RANGE = 32.0;

    /**
     * Add targeting goals when a demon slayer entity joins the level.
     * This provides the base AI targeting capability.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!isDemonSlayerOrHashira(mob)) {
            return;
        }

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
            (player) -> player.getPersistentData().getBoolean("oni")
        ));

        data.putBoolean(DEMON_AGGRO_ADDED_TAG, true);
        Log.debug("[DemonSlayerAggro] Added demon targeting goals to: " + mob.getType().getDescriptionId());
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

        // Only process demon slayers and hashira
        if (!isDemonSlayerOrHashira(mob)) {
            return;
        }

        // Only scan periodically to reduce performance impact
        if (mob.tickCount % SCAN_INTERVAL != 0) {
            return;
        }

        // If the mob already has a valid demon target, don't change it
        LivingEntity currentTarget = mob.getTarget();
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

    /**
     * Check if an entity is a demon slayer or hashira.
     * Includes both tag-based checks and class-based checks.
     */
    private static boolean isDemonSlayerOrHashira(Mob mob) {
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
     * Check if an entity is a valid demon target.
     */
    public static boolean isDemonTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
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
