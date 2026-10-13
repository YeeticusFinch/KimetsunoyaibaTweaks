package com.lerdorf.kimetsunoyaibamultiplayer.blooddemonarts;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtForm;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.BloodDemonArtTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.api.KnYAPI;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.GuardStateHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.combat.BloodDemonArtM1AttackHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AbstractDemonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiDecapitatedHeadEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.RuiWebEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.SilkRibbonEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BloodDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.RuiHeadCameraPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/** Initial implementation of Rui's thread-based blood demon art. */
@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class RuiBloodDemonArt {
    public static final String ART_ID = "rui_blood_demon_art";

    public static final int FORM_BASIC_THREAD = 3800;
    public static final int FORM_SILK_STRIKE = 3801;
    public static final int FORM_SILK_BARRAGE = 3802;
    public static final int FORM_WEB_SNATCH = 3803;
    public static final int FORM_DICING_WEB_WALL = 3804;
    public static final int FORM_CUTTING_THREAD_CAGE = 3805;
    public static final int FORM_CONTORTING_THREADS = 3806;
    public static final int FORM_MURDEROUS_EYE_BASKET = 3807;
    public static final int FORM_CUTTING_THREAD_ROTATION = 3808;
    public static final int FORM_DECAPITATE_SELF = 3809;

    private static final double RANGE = 28.0D;
    private static final double M1_RANGE = RANGE * 0.70D;
    private static final float THREAD_DAMAGE = 7.0F;
    private static final float BASIC_THREAD_DAMAGE = 5.0F;
    private static final float BASIC_RED_THREAD_MULTIPLIER = 1.50F;
    private static final int GUARD_STATE_TICKS = 10;
    private static final String AXIS_TAG = "KnYRuiThreadAxis";
    private static final String ANIMATION_SIDE_TAG = "KnYRuiThreadAnimationSide";
    private static final String LAST_LEFT_CLICK_TICK_TAG = "KnYRuiLastLeftClickTick";
    private static final String RUI_FORM_ANIMATION_LOCK_UNTIL_TAG = "KnYRuiFormAnimationLockUntil";
    private static final String DECAP_ACTIVE_TAG = "KnYRuiDecapitateSelfActive";
    private static final String DECAP_PHASE_TAG = "KnYRuiDecapitateSelfPhase";
    private static final String DECAP_START_TICK_TAG = "KnYRuiDecapitateSelfStartTick";
    private static final String DECAP_HEAD_UUID_TAG = "KnYRuiDecapitateSelfHead";
    private static final ThreadLocal<UUID> FORWARDING_HEAD_DAMAGE = new ThreadLocal<>();
    public static final String WEB_SNATCH_VARIANT_TAG = "KnYRuiWebSnatchSnare";

    private RuiBloodDemonArt() {
    }

    public static void register() {
        if (BloodDemonArtRegistry.isRegistered(ART_ID)) {
            return;
        }
        KnYAPI.registerBloodDemonArt(ART_ID, "Blood Demon Art: Rui's Thread Manipulation", createTechnique());
    }

    private static BloodDemonArtTechnique createTechnique() {
        return new BloodDemonArtTechnique(
            "Rui's Thread Manipulation",
            List.of(
                new BloodDemonArtForm(FORM_SILK_BARRAGE, "Silk Barrage",
                    "Fire crossing threads that form an X through the target area.", 6,
                    RuiBloodDemonArt::executeSilkBarrage),
                new BloodDemonArtForm(FORM_WEB_SNATCH, "Web Snatch",
                    "Surround a target with threads and cut it; the alternate pattern lifts and squeezes it.", 8,
                    RuiBloodDemonArt::executeWebSnatch),
                new BloodDemonArtForm(FORM_DICING_WEB_WALL, "Dicing Web Wall",
                    "Launch a moving wall of horizontal and vertical threads.", 12,
                    RuiBloodDemonArt::executeDicingWebWall),
                new BloodDemonArtForm(FORM_CUTTING_THREAD_CAGE, "Cutting Thread Cage",
                    "Close a cage of threads around a target and cut it repeatedly.", 14,
                    RuiBloodDemonArt::executeCuttingThreadCage),
                new BloodDemonArtForm(FORM_CONTORTING_THREADS, "Contorting Threads",
                    "Contract a web around a target until the threads meet.", 12,
                    RuiBloodDemonArt::executeContortingThreads),
                new BloodDemonArtForm(FORM_MURDEROUS_EYE_BASKET, "Murderous Eye Basket",
                    "Enclose a target in a dome of cutting threads.", 16,
                    RuiBloodDemonArt::executeMurderousEyeBasket),
                new BloodDemonArtForm(FORM_CUTTING_THREAD_ROTATION, "Cutting Thread Rotation",
                    "Spin a circular cutting web around a target while leaving its center empty.", 18,
                    RuiBloodDemonArt::executeCuttingThreadRotation),
                new BloodDemonArtForm(FORM_DECAPITATE_SELF, "Decapitate Self",
                    "Launch your head away, become invulnerable, and regenerate when it returns.", 30,
                    RuiBloodDemonArt::executeDecapitateSelf)
            ),
            0xF3F0FF
        );
    }

    public static boolean performBasicAttack(LivingEntity caster, LivingEntity target) {
        if (caster == null || target == null || !(caster.level() instanceof ServerLevel)
            || caster.distanceToSqr(target) > M1_RANGE * M1_RANGE) {
            return false;
        }
        armGuardState(caster, FORM_BASIC_THREAD);
        boolean vertical = caster.getPersistentData().contains(AXIS_TAG)
            && !caster.getPersistentData().getBoolean(AXIS_TAG);
        caster.getPersistentData().putBoolean(AXIS_TAG, vertical);
        playAttackAnimation(caster, whipAnimation(caster, vertical), 10);
        boolean spawned = spawnBasicThread(caster, target, 0.0D, vertical ? 1.0D : 0.0D, vertical);
        if (spawned) {
            BloodDemonArtM1AttackHandler.performRuiM1Attack(caster, target.getUUID());
        }
        return spawned;
    }

    public static boolean performSilkStrike(LivingEntity caster, LivingEntity target) {
        if (caster == null || !(caster.level() instanceof ServerLevel)
            || (target != null && caster.distanceToSqr(target) > M1_RANGE * M1_RANGE)) {
            return false;
        }
        armGuardState(caster, FORM_SILK_STRIKE);
        boolean vertical = caster.getPersistentData().contains(AXIS_TAG)
            && !caster.getPersistentData().getBoolean(AXIS_TAG);
        caster.getPersistentData().putBoolean(AXIS_TAG, vertical);
        playAttackAnimation(caster, whipAnimation(caster, vertical), 12);
        boolean spawned = false;
        for (int index = -1; index <= 1; index++) {
            spawned |= spawnM1Thread(caster, target, index * 0.18D, 0.0D, vertical);
        }
        if (spawned) {
            BloodDemonArtM1AttackHandler.performRuiM1Attack(caster,
                target == null ? null : target.getUUID());
        }
        return spawned;
    }

    /** Handles every player left-click source: entity, block, and air. */
    public static boolean performLeftClickAttack(LivingEntity caster, LivingEntity target) {
        if (caster == null || !(caster.level() instanceof ServerLevel)) {
            return false;
        }
        if (caster.level().random.nextFloat() < 0.66F) {
            if (target != null) {
                return performBasicAttack(caster, target);
            }
            executeBasicThread(caster, caster.level(), FORM_BASIC_THREAD);
            return true;
        }
        return performSilkStrike(caster, target);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide) {
            handlePlayerLeftClick(event.getEntity(), event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getEntity().level().isClientSide) {
            handlePlayerLeftClick(event.getEntity(), null);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        tickDecapitation(event.player);
        if (event.player.swinging && event.player.swingTime == 0) {
            handlePlayerLeftClick(event.player, null);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && isHeadless(entity)
            && !entity.getUUID().equals(FORWARDING_HEAD_DAMAGE.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && isDecapitationActive(event.getEntity())) {
            abortDecapitation(event.getEntity());
        }
    }

    private static void handlePlayerLeftClick(Player player, Entity clickedEntity) {
        if (!isRuiArtItem(player.getMainHandItem()) || isFormAnimationLocked(player)
            || !markLeftClickHandled(player)) {
            return;
        }
        performLeftClickAttack(player, clickedEntity instanceof LivingEntity living ? living : null);
    }

    public static void markRightClickFormUse(Player player, int formId) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        long gameTime = serverLevel.getGameTime();
        player.getPersistentData().putLong(RUI_FORM_ANIMATION_LOCK_UNTIL_TAG,
            gameTime + formAnimationLockTicks(formId));
        player.getPersistentData().putLong(LAST_LEFT_CLICK_TICK_TAG, gameTime);
    }

    public static void cycleWebSnatchVariant(Player player) {
        if (!isRuiArtItem(player.getMainHandItem())) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean snare = !stack.getOrCreateTag().getBoolean(WEB_SNATCH_VARIANT_TAG);
        stack.getOrCreateTag().putBoolean(WEB_SNATCH_VARIANT_TAG, snare);
        player.getInventory().setChanged();
        player.sendSystemMessage(Component.literal(snare
            ? "Web Snatch variant: Silk Snatch Snare"
            : "Web Snatch variant: Web Snatch"));
    }

    private static boolean isFormAnimationLocked(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return serverLevel.getGameTime()
            < player.getPersistentData().getLong(RUI_FORM_ANIMATION_LOCK_UNTIL_TAG);
    }

    private static int formAnimationLockTicks(int formId) {
        return switch (formId) {
            case FORM_SILK_BARRAGE -> 20;
            case FORM_CUTTING_THREAD_ROTATION -> 24;
            default -> 20;
        };
    }

    private static boolean isRuiArtItem(ItemStack stack) {
        return stack.getItem() instanceof BloodDemonArtItem bloodDemonArtItem
            && ART_ID.equals(bloodDemonArtItem.getArtId());
    }

    public static boolean isSilkSnatchSnare(ItemStack stack) {
        return isRuiArtItem(stack) && stack.getOrCreateTag().getBoolean(WEB_SNATCH_VARIANT_TAG);
    }

    public static boolean isWebSnatchFormSelected(ItemStack stack) {
        if (!isRuiArtItem(stack)) {
            return false;
        }
        BloodDemonArtRegistry.RegisteredBloodDemonArt art = BloodDemonArtRegistry.getArt(ART_ID);
        if (art == null || art.getTechnique() == null) {
            return false;
        }
        BloodDemonArtForm form = art.getTechnique().getForm(
            ((BloodDemonArtItem) stack.getItem()).getSelectedFormIndex(stack));
        return form != null && form.getFormId() == FORM_WEB_SNATCH;
    }

    private static boolean markLeftClickHandled(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        long gameTime = serverLevel.getGameTime();
        long lastHandled = player.getPersistentData().getLong(LAST_LEFT_CLICK_TICK_TAG);
        if (lastHandled == gameTime) {
            return false;
        }
        player.getPersistentData().putLong(LAST_LEFT_CLICK_TICK_TAG, gameTime);
        return true;
    }

    private static void executeBasicThread(LivingEntity caster, Level level, int formId) {
        LivingEntity target = resolveTarget(caster, M1_RANGE);
        if (target != null) {
            performBasicAttack(caster, target);
        } else {
            armGuardState(caster, formId);
            boolean vertical = caster.getPersistentData().contains(AXIS_TAG)
                && !caster.getPersistentData().getBoolean(AXIS_TAG);
            caster.getPersistentData().putBoolean(AXIS_TAG, vertical);
            playAttackAnimation(caster, whipAnimation(caster, vertical), 10);
            if (spawnBasicThread(caster, null, 0.0D, vertical ? 1.0D : 0.0D, vertical)) {
                BloodDemonArtM1AttackHandler.performRuiM1Attack(caster, null);
            }
        }
    }

    private static void executeSilkStrike(LivingEntity caster, Level level, int formId) {
        LivingEntity target = resolveTarget(caster, M1_RANGE);
        performSilkStrike(caster, target);
    }

    private static void executeSilkBarrage(LivingEntity caster, Level level, int formId) {
        armGuardState(caster, formId);
        LivingEntity target = resolveTarget(caster, RANGE);
        playAttackAnimation(caster, "sword_to_left", 14);
        AbilityScheduler.scheduleOnce(caster, () -> {
            if (caster.isAlive()) {
                playAttackAnimation(caster, "sword_to_right", 14);
            }
        }, 5);
        for (int index = -1; index <= 1; index += 2) {
            double angle = index * Math.PI * 0.25D;
            spawnThread(caster, target, 0.0D, 0.0D, false, true, false, angle,
                false, false, Vec3.ZERO, 3.0F);
        }
    }

    private static void executeWebSnatch(LivingEntity caster, Level level, int formId) {
        armGuardState(caster, formId);
        LivingEntity target = resolveTarget(caster, RANGE);
        playAttackAnimation(caster, "punch_right", 14);
        boolean snareVariant = isSilkSnatchSnare(caster.getMainHandItem());
        for (int index = 0; index < 4; index++) {
            double angle = index * Math.PI * 2.0D / 4.0D;
            Vec3 offset = new Vec3(
                Math.cos(angle) * (0.25D + level.random.nextDouble() * 0.35D),
                0.2D + level.random.nextDouble() * 0.45D,
                Math.sin(angle) * (0.25D + level.random.nextDouble() * 0.35D));
            spawnThread(caster, target, 0.0D, 0.0D, false, false, false, 0.0D,
                true, snareVariant, offset);
        }
    }

    private static void executeDicingWebWall(LivingEntity caster, Level level, int formId) {
        armGuardState(caster, formId);
        playAttackAnimation(caster, "beast2", 10);
        AbilityScheduler.scheduleOnce(caster, () -> playAttackAnimation(caster, "cancel", 1), 10);
        spawnWeb(caster, null, RuiWebEntity.Pattern.DICING_WALL, 50);
    }

    private static void executeCuttingThreadCage(LivingEntity caster, Level level, int formId) {
        armGuardState(caster, formId);
        Vec3 targetPoint = resolveTargetPoint(caster, 50.0D);
        playAttackAnimation(caster, "summon", 20);
        RuiWebEntity cage = RuiWebEntity.createCage(caster.level(), caster, targetPoint,
            isRedThreads(caster), damageFor(caster));
        caster.level().addFreshEntity(cage);
    }

    private static void executeContortingThreads(LivingEntity caster, Level level, int formId) {
        armGuardState(caster, formId);
        Vec3 targetPoint = resolveTargetPoint(caster, 40.0D);
        Vec3 forward = caster.getLookAngle().normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();

        playAttackAnimation(caster, "sword_rotate", 20);
        double[][] launchAngles = {
            {15.0D, 0.0D},
            {-15.0D, 0.0D},
            {-10.0D, 10.0D},
            {10.0D, 10.0D},
            {0.0D, 10.0D}
        };
        for (int index = 0; index < launchAngles.length; index++) {
            double yaw = Math.toRadians(launchAngles[index][0]);
            double pitch = Math.toRadians(launchAngles[index][1]);
            Vec3 horizontal = forward.scale(Math.cos(yaw)).add(right.scale(Math.sin(yaw)));
            Vec3 launchDirection = horizontal.scale(Math.cos(pitch))
                .add(up.scale(Math.sin(pitch))).normalize();
            Vec3 start = getThreadAnchor(caster);
            Vec3 launchEnd = start.add(launchDirection.scale(40.0D));
            SilkRibbonEntity thread = SilkRibbonEntity.spawn(caster.level(), caster, start, launchEnd,
                SilkRibbonEntity.RibbonKind.SILK, damageFor(caster), false, null);
            thread.setContortingMode(launchDirection,
                com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(targetPoint), index == 0);
            thread.setRedThread(isRedThreads(caster));
            thread.setRuiThread(true);
            caster.level().addFreshEntity(thread);
        }
    }

    private static void executeMurderousEyeBasket(LivingEntity caster, Level level, int formId) {
        armGuardState(caster, formId);
        Vec3 targetPoint = resolveTargetPoint(caster, 60.0D);
        playAttackAnimation(caster, "summon", 20);
        RuiWebEntity basket = RuiWebEntity.createEyeBasket(caster.level(), caster, targetPoint,
            isRedThreads(caster), damageFor(caster));
        caster.level().addFreshEntity(basket);
    }

    private static void executeCuttingThreadRotation(LivingEntity caster, Level level, int formId) {
        armGuardState(caster, formId);
        playAttackAnimation(caster, "double_sword_overhead", 24);
        spawnWeb(caster, null, RuiWebEntity.Pattern.ROTATION, 40);
    }

    private static void executeDecapitateSelf(LivingEntity caster, Level level, int formId) {
        armGuardState(caster, formId);
        tryStartDecapitateSelf(caster);
    }

    /** Gives every Rui technique and M1 thread attack the same brief clash window. */
    private static void armGuardState(LivingEntity caster, int formId) {
        float damage = damageFor(caster);
        GuardStateHelper.setGuardState(caster, damage, formId);
        GuardStateHelper.setAttackState(caster, damage);
        AbilityScheduler.scheduleOnce(caster,
            () -> GuardStateHelper.clearGuardState(caster), GUARD_STATE_TICKS);
    }

    public static boolean tryStartDecapitateSelf(LivingEntity caster) {
        if (!(caster.level() instanceof ServerLevel serverLevel) || !caster.isAlive()
            || caster.getMaxHealth() <= 0.0F
            || caster instanceof RuiEntity && caster.getHealth() > caster.getMaxHealth() * 0.10F
            || isDecapitationActive(caster)) {
            return false;
        }

        long gameTime = serverLevel.getGameTime();
        caster.getPersistentData().putBoolean(DECAP_ACTIVE_TAG, true);
        caster.getPersistentData().putInt(DECAP_PHASE_TAG, 1);
        caster.getPersistentData().putLong(DECAP_START_TICK_TAG, gameTime);
        caster.getPersistentData().remove(DECAP_HEAD_UUID_TAG);
        RuiWebEntity circle = RuiWebEntity.createDecapitationCircle(serverLevel, caster);
        serverLevel.addFreshEntity(circle);
        return true;
    }

    public static boolean isDecapitationActive(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(DECAP_ACTIVE_TAG);
    }

    public static boolean isHeadless(LivingEntity entity) {
        return isDecapitationActive(entity)
            && entity.getPersistentData().getInt(DECAP_PHASE_TAG) == 2;
    }

    public static void tickDecapitation(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !isDecapitationActive(owner)) {
            return;
        }
        if (!owner.isAlive()) {
            abortDecapitation(owner);
            return;
        }

        int phase = owner.getPersistentData().getInt(DECAP_PHASE_TAG);
        if (phase == 1) {
            long startTick = owner.getPersistentData().getLong(DECAP_START_TICK_TAG);
            if (serverLevel.getGameTime() - startTick >= 5L) {
                becomeHeadless(owner);
            }
        } else if (phase == 2 && !hasLiveDecapitatedHead(owner, serverLevel)) {
            abortDecapitation(owner);
        }
    }

    private static void becomeHeadless(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel serverLevel) || !isDecapitationActive(owner)) {
            return;
        }
        RuiDecapitatedHeadEntity head = RuiDecapitatedHeadEntity.create(serverLevel, owner);
        if (!serverLevel.addFreshEntity(head)) {
            abortDecapitation(owner);
            return;
        }

        owner.getPersistentData().putInt(DECAP_PHASE_TAG, 2);
        owner.getPersistentData().putUUID(DECAP_HEAD_UUID_TAG, head.getUUID());
        if (owner instanceof AbstractDemonEntity demon) {
            demon.setHeadless(true);
        }
        playHeadlessAnimation(owner);
        if (owner instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(new RuiHeadCameraPacket(head.getId(), true), serverPlayer);
        }
    }

    private static boolean hasLiveDecapitatedHead(LivingEntity owner, ServerLevel serverLevel) {
        if (!owner.getPersistentData().hasUUID(DECAP_HEAD_UUID_TAG)) {
            return false;
        }
        Entity head = serverLevel.getEntity(owner.getPersistentData().getUUID(DECAP_HEAD_UUID_TAG));
        return head instanceof RuiDecapitatedHeadEntity && head.isAlive();
    }

    public static boolean damageThroughDecapitatedHead(LivingEntity owner,
                                                        RuiDecapitatedHeadEntity head,
                                                        DamageSource source, float amount) {
        if (!isHeadless(owner) || !owner.getPersistentData().hasUUID(DECAP_HEAD_UUID_TAG)
            || !owner.getPersistentData().getUUID(DECAP_HEAD_UUID_TAG).equals(head.getUUID())) {
            return false;
        }

        FORWARDING_HEAD_DAMAGE.set(owner.getUUID());
        try {
            return owner.hurt(source, amount);
        } finally {
            FORWARDING_HEAD_DAMAGE.remove();
        }
    }

    public static void completeDecapitation(LivingEntity owner, RuiDecapitatedHeadEntity head) {
        if (!isDecapitationActive(owner)) {
            head.discard();
            return;
        }

        head.discard();
        if (!owner.isAlive()) {
            abortDecapitation(owner);
            return;
        }

        owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + owner.getMaxHealth() * 0.5F));
        stopHeadlessAnimation(owner);
        clearDecapitationState(owner);
    }

    public static void abortDecapitation(LivingEntity owner) {
        if (!isDecapitationActive(owner)) {
            return;
        }
        if (owner.level() instanceof ServerLevel serverLevel
            && owner.getPersistentData().hasUUID(DECAP_HEAD_UUID_TAG)) {
            Entity head = serverLevel.getEntity(owner.getPersistentData().getUUID(DECAP_HEAD_UUID_TAG));
            if (head != null) {
                head.discard();
            }
        }
        stopHeadlessAnimation(owner);
        clearDecapitationState(owner);
    }

    private static void clearDecapitationState(LivingEntity owner) {
        if (owner instanceof AbstractDemonEntity demon) {
            demon.setHeadless(false);
        }
        owner.getPersistentData().remove(DECAP_ACTIVE_TAG);
        owner.getPersistentData().remove(DECAP_PHASE_TAG);
        owner.getPersistentData().remove(DECAP_START_TICK_TAG);
        owner.getPersistentData().remove(DECAP_HEAD_UUID_TAG);
    }

    private static void playHeadlessAnimation(LivingEntity owner) {
        if (owner instanceof AbstractDemonEntity demon) {
            demon.playGeckoAnimation("headless", 60);
        } else if (owner instanceof Player player) {
            AnimationHelper.playAnimation(player, "headless");
        }
    }

    private static void stopHeadlessAnimation(LivingEntity owner) {
        if (owner instanceof AbstractDemonEntity demon) {
            demon.playGeckoAnimation("idle", 1);
        } else if (owner instanceof Player player && player.level() instanceof ServerLevel) {
            ModNetworking.sendToAllClients(AnimationSyncPacket.createStopPacket(player.getUUID()));
        }
        if (owner instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(new RuiHeadCameraPacket(-1, false), serverPlayer);
        }
    }

    /** The exact head position used for the circle, tether, launch, and return. */
    public static Vec3 getHeadAnchor(LivingEntity owner) {
        return owner.getEyePosition().add(0.0D, -0.06D, 0.0D);
    }

    private static boolean spawnThread(LivingEntity caster, LivingEntity target,
                                       double offsetX, double offsetY, boolean liftOnHit,
                                       boolean whipMode, boolean whipVertical) {
        return spawnThread(caster, target, offsetX, offsetY, liftOnHit,
            whipMode, whipVertical, whipVertical ? Math.PI * 0.5D : 0.0D,
            false, false, Vec3.ZERO);
    }

    private static boolean spawnThread(LivingEntity caster, LivingEntity target,
                                       double offsetX, double offsetY, boolean liftOnHit,
                                       boolean whipMode, boolean whipVertical, double whipAngle) {
        return spawnThread(caster, target, offsetX, offsetY, liftOnHit,
            whipMode, whipVertical, whipAngle, false, false, Vec3.ZERO);
    }

    private static boolean spawnThread(LivingEntity caster, LivingEntity target,
                                       double offsetX, double offsetY, boolean liftOnHit,
                                       boolean whipMode, boolean whipVertical, double whipAngle,
                                       boolean snatchMode, boolean snareVariant, Vec3 snatchOffset) {
        return spawnThread(caster, target, offsetX, offsetY, liftOnHit,
            whipMode, whipVertical, whipAngle, snatchMode, snareVariant, snatchOffset,
            1.0F, RANGE, damageFor(caster));
    }

    private static boolean spawnM1Thread(LivingEntity caster, LivingEntity target,
                                          double offsetX, double offsetY, boolean whipVertical) {
        return spawnThread(caster, target, offsetX, offsetY, false,
            true, whipVertical, whipVertical ? Math.PI * 0.5D : 0.0D,
            false, false, Vec3.ZERO, 1.0F, M1_RANGE, damageFor(caster));
    }

    private static boolean spawnBasicThread(LivingEntity caster, LivingEntity target,
                                             double offsetX, double offsetY, boolean whipVertical) {
        return spawnThread(caster, target, offsetX, offsetY, false,
            true, whipVertical, whipVertical ? Math.PI * 0.5D : 0.0D,
            false, false, Vec3.ZERO, 1.0F, M1_RANGE, basicThreadDamage(caster));
    }

    private static boolean spawnThread(LivingEntity caster, LivingEntity target,
                                       double offsetX, double offsetY, boolean liftOnHit,
                                       boolean whipMode, boolean whipVertical, double whipAngle,
                                       boolean snatchMode, boolean snareVariant, Vec3 snatchOffset,
                                       float whipLengthScale) {
        return spawnThread(caster, target, offsetX, offsetY, liftOnHit,
            whipMode, whipVertical, whipAngle, snatchMode, snareVariant, snatchOffset,
            whipLengthScale, RANGE, damageFor(caster));
    }

    private static boolean spawnThread(LivingEntity caster, LivingEntity target,
                                       double offsetX, double offsetY, boolean liftOnHit,
                                       boolean whipMode, boolean whipVertical, double whipAngle,
                                       boolean snatchMode, boolean snareVariant, Vec3 snatchOffset,
                                       float whipLengthScale, double threadRange, float threadDamage) {
        Vec3 look = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(caster).normalize();
        Vec3 start = whipMode || snatchMode
            ? getThreadAnchor(caster)
            : caster.position().add(0.0D, caster.getBbHeight() * 0.45D, 0.0D);
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 end = whipMode || snatchMode
            ? start.add(look.scale(threadRange))
            : target == null
                ? start.add(look.scale(threadRange))
                : target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                    .add(side.scale(offsetX)).add(0.0D, offsetY, 0.0D);
        SilkRibbonEntity thread = SilkRibbonEntity.spawn(caster.level(), caster, start, end,
            SilkRibbonEntity.RibbonKind.SILK, threadDamage, false, target);
        thread.setLiftOnHit(liftOnHit);
        thread.setRedThread(isRedThreads(caster));
        thread.setRuiThread(true);
        thread.setWhipMode(whipMode, whipVertical, (float) offsetX, (float) whipAngle);
        thread.setWhipLengthScale(whipLengthScale);
        if (snatchMode) {
            thread.setSnatchMode(snareVariant, snatchOffset);
        }
        return caster.level().addFreshEntity(thread);
    }

    private static void spawnWeb(LivingEntity caster, LivingEntity target,
                                 RuiWebEntity.Pattern pattern, int lifetime) {
        RuiWebEntity web = RuiWebEntity.create(caster.level(), caster, target, pattern,
            isRedThreads(caster), damageFor(caster), lifetime);
        caster.level().addFreshEntity(web);
    }

    public static boolean isRedThreads(LivingEntity caster) {
        return caster.getMaxHealth() > 0.0F && caster.getHealth() <= caster.getMaxHealth() * 0.5F;
    }

    public static float damageFor(LivingEntity caster) {
        return THREAD_DAMAGE * (isRedThreads(caster) ? 1.30F : 1.0F);
    }

    private static float basicThreadDamage(LivingEntity caster) {
        return BASIC_THREAD_DAMAGE * (isRedThreads(caster) ? BASIC_RED_THREAD_MULTIPLIER : 1.0F);
    }

    /** The shared head-side anchor used by Rui's threads that return to the caster. */
    public static Vec3 getThreadAnchor(LivingEntity caster) {
        Vec3 forward = caster.getLookAngle().normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        return caster.getEyePosition()
            .add(0.0D, -0.06D, 0.0D)
            .add(right.scale(-0.05D))
            .add(forward.scale(0.05D));
    }

    private static String whipAnimation(LivingEntity caster, boolean vertical) {
        if (!vertical) {
            boolean useLeft = !caster.getPersistentData().getBoolean(ANIMATION_SIDE_TAG);
            caster.getPersistentData().putBoolean(ANIMATION_SIDE_TAG, useLeft);
            return useLeft ? "sword_to_left" : "sword_to_right";
        }
        return switch (caster.level().random.nextInt(3)) {
            case 0 -> "punch_left";
            case 1 -> "punch_right";
            default -> "punch_right";
        };
    }

    private static LivingEntity resolveTarget(LivingEntity caster, double range) {
        if (caster instanceof Mob mob && isValidTarget(mob.getTarget(), caster, range)) {
            return mob.getTarget();
        }

        Vec3 localStart = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(caster);
        Vec3 localEnd = localStart.add(
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(caster).normalize()
                .scale(range));
        Vec3 worldStart = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(localStart);
        Vec3 worldEnd = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(localEnd);
        BlockHitResult blockHit = caster.level().clip(new ClipContext(
            worldStart, worldEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 rayEnd = blockHit.getType() == HitResult.Type.MISS ? worldEnd : blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            caster.level(), caster, worldStart, rayEnd,
            new AABB(worldStart, rayEnd).inflate(1.0D),
            entity -> entity != caster && entity instanceof LivingEntity living
                && isValidTarget(living, caster, range));
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            return target;
        }
        return null;
    }

    private static Vec3 resolveTargetPoint(LivingEntity caster, double range) {
        Vec3 start = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        BlockHitResult blockHit = caster.level().clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 rayEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            caster.level(), caster, start, rayEnd,
            new AABB(start, rayEnd).inflate(1.0D),
            entity -> entity != caster && entity instanceof LivingEntity living
                && isValidTarget(living, caster, range));
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
            return living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
        }
        return blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
    }

    private static boolean isValidTarget(LivingEntity target, LivingEntity caster, double range) {
        return target != null && target != caster && target.isAlive()
            && caster.distanceToSqr(target) <= range * range;
    }

    private static void playAttackAnimation(LivingEntity caster, String animation, int duration) {
        if (caster instanceof AbstractDemonEntity demon) {
            demon.playGeckoAnimation(animation, duration);
        } else {
            AnimationHelper.playAnimation(caster, animation, duration);
        }
    }
}
