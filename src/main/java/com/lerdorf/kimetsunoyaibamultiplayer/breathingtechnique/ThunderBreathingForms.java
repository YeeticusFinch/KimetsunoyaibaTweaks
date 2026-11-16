package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordZenitsu;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordZenitsuFullPotential;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.AnimationSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.SwordWielderSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ThunderLightningSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.ThunderLightningSparkSyncPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Enhanced Thunder Breathing forms and their shared lightning presentation. */
public final class ThunderBreathingForms {
    private static final double FIRST_FORM_RANGE = 20.0D;
    private static final double SIXFOLD_RANGE = 15.0D;
    private static final double EIGHTFOLD_RANGE = 10.0D;
    private static final double FIRST_FORM_START_OFFSET = 10.0D;
    private static final int SIXFOLD_FORM_ID = 30002;
    private static final int EIGHTFOLD_FORM_ID = 30003;
    private static final int GODSPEED_FORM_ID = 30004;
    private static final int IAI1_ANIMATION_TICKS = 15;
    private static final double GODSPEED_SPARK_BLOCK_RANGE = 3.0D;
    private static final float THUNDER_FORM_DAMAGE = 12.0F;
    private static final Vec3 NORMAL_PRIMARY = new Vec3(1.0D, 0.9D, 0.12D);
    private static final Vec3 NORMAL_SECONDARY = new Vec3(0.42D, 0.86D, 1.0D);
    private static final Vec3 BLUE_PRIMARY = new Vec3(0.05D, 0.16D, 0.72D);
    private static final Vec3 BLUE_SECONDARY = new Vec3(0.38D, 0.86D, 1.0D);
    private static final Vec3 KAIGAKU_SECONDARY = Vec3.ZERO;
    private static final Map<UUID, ZenitsuState> ZENITSU_STATES = new HashMap<>();

    private ThunderBreathingForms() {
    }

    public static BreathingTechnique createThunderBreathing(boolean blueVariant) {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm(blueVariant ? LightningVariant.BLUE : LightningVariant.NORMAL));
        forms.addAll(baseThunderForms(302, 306, blueVariant ? LightningVariant.BLUE : LightningVariant.NORMAL));
        return thunderTechnique(forms);
    }

    public static BreathingTechnique createKaigakuBreathing() {
        return thunderTechnique(baseThunderForms(302, 306, LightningVariant.KAIGAKU));
    }

    public static BreathingTechnique createKaigakuHumanBreathing() {
        return thunderTechnique(baseThunderForms(302, 306, LightningVariant.BLUE));
    }

    public static BreathingTechnique createHashiraBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm(LightningVariant.NORMAL));
        forms.addAll(baseThunderForms(302, 307, LightningVariant.NORMAL));
        return thunderTechnique(forms);
    }

    public static BreathingTechnique createZenitsuBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm(LightningVariant.NORMAL));
        forms.add(zenitsuChainForm(SIXFOLD_FORM_ID, false));
        forms.add(zenitsuChainForm(EIGHTFOLD_FORM_ID, true));
        forms.add(godspeedForm());
        forms.add(baseThunderForm(307, LightningVariant.NORMAL));
        return thunderTechnique(forms);
    }

    public static BreathingTechnique createZenitsuFullPotentialBreathing() {
        List<BreathingForm> forms = new ArrayList<>();
        forms.add(firstForm(LightningVariant.NORMAL));
        forms.add(zenitsuChainForm(SIXFOLD_FORM_ID, false));
        forms.add(zenitsuChainForm(EIGHTFOLD_FORM_ID, true));
        forms.add(godspeedForm());
        forms.addAll(baseThunderForms(302, 307, LightningVariant.NORMAL));
        return thunderTechnique(forms);
    }

    public static void handleKaigakuAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
            || !(attacker.getMainHandItem().getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordKaigaku)
            || !isKaigakuThunderFormActive(attacker)) {
            return;
        }

        LivingEntity target = event.getEntity();

        target.addEffect(new MobEffectInstance(
            com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects.ELECTROCUTED.get(),
            100, 0, false, true, true));
    }

    public static boolean isZenitsuSword(ItemStack stack) {
        return stack.getItem() instanceof NichirinSwordZenitsu
            || stack.getItem() instanceof NichirinSwordZenitsuFullPotential;
    }

    private static boolean isKaigakuThunderFormActive(LivingEntity entity) {
        if (GuardStateHelper.isCustomBreathingActive(entity)) {
            return true;
        }
        double breathes = entity.getPersistentData().getDouble("breathes");
        return breathes >= 302.0D && breathes <= 306.0D
            && entity.getPersistentData().getBoolean("skill");
    }

    private static BreathingTechnique thunderTechnique(List<BreathingForm> forms) {
        return new BreathingTechnique("Thunder Breathing", forms, "§e", "§e");
    }

    private static List<BreathingForm> baseThunderForms(int firstFormId, int lastFormId,
                                                       LightningVariant lightningVariant) {
        List<BreathingForm> forms = new ArrayList<>();
        for (int formId = firstFormId; formId <= lastFormId; formId++) {
            BreathingForm form = baseThunderForm(formId, lightningVariant);
            if (form != null) {
                forms.add(form);
            }
        }
        return forms;
    }

    private static BreathingForm baseThunderForm(int formId, LightningVariant lightningVariant) {
        BaseKnYForms.BaseForm baseForm = BaseKnYForms.forms.get(formId);
        if (baseForm == null) {
            return null;
        }
        if (formId == 302) {
            return secondForm(baseForm, lightningVariant);
        }
        return new BreathingForm(
            formId,
            baseForm.name,
            "Use the base Thunder Breathing form",
            5,
            (entity, level, ignoredFormId) -> BaseModFormExecutionHelper.executeBaseModForm(entity, level, formId)
        );
    }

    private static BreathingForm secondForm(BaseKnYForms.BaseForm baseForm,
                                            LightningVariant lightningVariant) {
        return new BreathingForm(
            302,
            baseForm.name,
            "Deliver five rapid cuts and finish with an electrifying knockback",
            5,
            (entity, level, formId) -> executeSecondForm(entity, level, formId, lightningVariant)
        );
    }

    /** First Form: Thunderclap and Flash. */
    private static BreathingForm firstForm(LightningVariant lightningVariant) {
        return new BreathingForm(
            30001,
            "First Form: Thunderclap and Flash",
            "Hold an unsheathing stance, then dash through opponents in a lightning-fast slash",
            2,
            (entity, level, formId) -> executeFirstForm(entity, level, formId, lightningVariant)
        );
    }

    private static BreathingForm zenitsuChainForm(int formId, boolean eightfold) {
        String name = eightfold
            ? "First Form: Thunderclap and Flash, Eightfold"
            : "First Form: Thunderclap and Flash, Sixfold";
        String description = eightfold
            ? "Chain up to eight lightning dashes, prompting each dash after the first"
            : "Chain up to six lightning dashes, prompting each dash after the first";
        return new BreathingForm(
            formId,
            name,
            description,
            eightfold ? 3 : 2,
            (entity, level, ignoredFormId) -> startZenitsuForm(entity, level, formId, eightfold)
        );
    }

    private static BreathingForm godspeedForm() {
        return new BreathingForm(
            GODSPEED_FORM_ID,
            "First Form: Thunderclap and Flash, Godspeed",
            "Teleport forward at extreme speed with a wide lightning strike",
            5,
            ThunderBreathingForms::executeGodspeed
        );
    }

    private static void executeFirstForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                         int formId, LightningVariant lightningVariant) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final Vec3 startLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        final Vec3 startWorld = firstFormStartPoint(entity);
        final Vec3[] targetLocal = {null};
        final Vec3[] targetWorld = {null};
        final boolean originalNoGravity = entity.isNoGravity();
        final int[] currentTick = {0};

        playThunderSound(serverLevel, entity, ModSounds.THUNDER_CHARGEUP.get(), 16.0D);
        GuardStateHelper.setGuardState(entity, 12.0D, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", 20, 1.0F, 4000);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = currentTick[0]++;

            if (tick < 20) {
                repeatIai1DuringCharge(entity, tick, 20);
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                entity.setSprinting(false);
                if (!(entity instanceof Player)) {
                    MovementHelper.lookAtTarget(entity);
                }
                return;
            }

            if (tick == 20) {
                targetWorld[0] = raycastTarget(serverLevel, entity, FIRST_FORM_RANGE);
                targetLocal[0] = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(targetWorld[0]);
                return;
            }

            if (tick >= 21 && tick <= 25 && targetLocal[0] != null) {
                if (tick == 21) {
                    playEntityAnimation(entity, "sword_to_right");
                    playThunderSound(serverLevel, entity, ModSounds.THUNDERCLAP.get(), 30.0D);
                    syncLightning(serverLevel, entity, true, true,
                        new Vec3[] {startWorld, entity.position()}, lightningVariant);
                }

                if (tick < 25) {
                    MovementHelper.setVelocity(entity, targetLocal[0].subtract(startLocal).scale(0.2D));
                } else {
                    MovementHelper.setVelocity(entity, Vec3.ZERO);
                    Vec3 finalWorldPosition = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(targetLocal[0]);
                    entity.teleportTo(finalWorldPosition.x, finalWorldPosition.y, finalWorldPosition.z);
                    targetWorld[0] = entity.position();
                    syncLightning(serverLevel, entity, true, false,
                        new Vec3[] {startWorld, targetWorld[0]}, lightningVariant);

                    GuardStateHelper.setAttackState(entity, 12.0D);
                    damageTrajectory(serverLevel, entity, startWorld, targetWorld[0], THUNDER_FORM_DAMAGE);
                    spawnZenitsuImpact(serverLevel, targetWorld[0]);
                    spawnLightningSparks(serverLevel, entity, lightningVariant);
                    GuardStateHelper.clearAttackFlag(entity);
                }

                if (tick > 21 && tick < 25) {
                    syncLightning(serverLevel, entity, true, false,
                        new Vec3[] {startWorld, entity.position()}, lightningVariant);
                }
            }
        }, 1, 37);

        AbilityScheduler.scheduleOnce(entity, () -> {
            syncLightning(serverLevel, entity, false, false,
                new Vec3[] {startWorld, entity.position()}, lightningVariant);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "iai1");
        }, 37);
    }

    private static void executeSecondForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                          int formId, LightningVariant lightningVariant) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final Vec3 start = entity.position();
        final Vec3 targetPosition = raycastTarget(serverLevel, entity, 12.0D);
        final LivingEntity target = findTrajectoryTarget(serverLevel, entity, start, targetPosition, 2.0D);
        final boolean originalNoGravity = entity.isNoGravity();
        final int[] elapsed = {0};
        final int[] hits = {0};

        playThunderSound(serverLevel, entity, ModSounds.RICE_SPIRIT_DASH.get(), 30.0D);
        GuardStateHelper.setGuardState(entity, THUNDER_FORM_DAMAGE, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimation(entity, "sword_to_right");
        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 9, false, false, false));
        }

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = elapsed[0]++;
            if (tick < 5) {
                MovementHelper.setVelocity(entity, targetPosition.subtract(start).scale(0.2D));
                return;
            }

            MovementHelper.setVelocity(entity, Vec3.ZERO);
            if (tick == 5) {
                entity.teleportTo(targetPosition.x, targetPosition.y, targetPosition.z);
                syncLightning(serverLevel, entity, true, true,
                    new Vec3[] {start, targetPosition}, lightningVariant);
            }

            if (target != null && hits[0] < 5 && tick <= 9) {
                GuardStateHelper.setAttackState(entity, THUNDER_FORM_DAMAGE);
                Damager.hurt(entity, target, THUNDER_FORM_DAMAGE);
                GuardStateHelper.clearAttackFlag(entity);
                spawnZenitsuImpact(serverLevel,
                    target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
                hits[0]++;
                if (hits[0] == 1) {
                    playThunderSound(serverLevel, entity, ModSounds.RICE_SPIRIT_ELECTRIC_DAMAGE.get(), 30.0D);
                }
                if (hits[0] == 5) {
                    Vec3 knockback = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                        .world(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity))
                        .normalize().scale(1.5D);
                    target.setDeltaMovement(knockback.x, 0.55D, knockback.z);
                    playThunderSound(serverLevel, entity, ModSounds.RICE_SPIRIT_KNOCKBACK.get(), 30.0D);
                }
            }
        }, 1, 13);

        AbilityScheduler.scheduleOnce(entity, () -> {
            syncLightning(serverLevel, entity, false, false,
                new Vec3[] {start, targetPosition}, lightningVariant);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
        }, 13);
    }

    public static void executeGodspeed(LivingEntity entity, net.minecraft.world.level.Level level, int ignoredFormId) {
        executeGodspeed(entity, level);
    }

    public static void executeGodspeed(LivingEntity entity, net.minecraft.world.level.Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final Vec3 startWorld = firstFormStartPoint(entity);
        final Vec3[] targetWorld = {null};
        final boolean originalNoGravity = entity.isNoGravity();
        final int[] currentTick = {0};

        playThunderSound(serverLevel, entity, ModSounds.THUNDER_CHARGEUP_GODSPEED.get(), 16.0D);
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE,
            SoundSource.PLAYERS, 1.0F, 1.0F);
        GuardStateHelper.setGuardState(entity, THUNDER_FORM_DAMAGE, GODSPEED_FORM_ID);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", 50, 1.0F, 4000);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = currentTick[0]++;

            if (tick < 50) {
                repeatIai1DuringCharge(entity, tick, 50);
                if ((tick & 1) == 0) {
                    spawnLightningSparks(serverLevel, entity, 5, NORMAL_PRIMARY,
                        0.04375F, 8, false, GODSPEED_SPARK_BLOCK_RANGE);
                }
                freezeZenitsuEntity(entity);
                return;
            }

            if (tick == 50) {
                targetWorld[0] = raycastTarget(serverLevel, entity, 60.0D);
                return;
            }

            if (tick == 51 && targetWorld[0] != null) {
                Vec3 target = targetWorld[0];
                syncLightning(serverLevel, entity, true, true,
                    new Vec3[] {startWorld, target}, LightningVariant.NORMAL);
                spawnLightningSpark(serverLevel, startWorld, target,
                    NORMAL_PRIMARY, 0.5F, 15, false, Vec3.ZERO);
                spawnLightningSpark(serverLevel, startWorld, target,
                    NORMAL_PRIMARY, 0.5F, 15, false, Vec3.ZERO);
                spawnLightningSpark(serverLevel, startWorld, target,
                    NORMAL_PRIMARY, 0.5F, 15, false, Vec3.ZERO);
                playEntityAnimation(entity, "speed_attack_sword");
                playThunderSound(serverLevel, entity, ModSounds.THUNDER_GODSPEED.get(), 100.0D);
                entity.teleportTo(target.x, target.y, target.z);

                GuardStateHelper.setAttackState(entity, THUNDER_FORM_DAMAGE);
                Set<LivingEntity> damaged = damageTrajectoryTargets(serverLevel, entity, startWorld, target, 6.0D);
                for (LivingEntity victim : nearbyLivingEntities(serverLevel, entity, target, 12.0D)) {
                    damaged.add(victim);
                }
                for (LivingEntity victim : damaged) {
                    Damager.hurt(entity, victim, THUNDER_FORM_DAMAGE);
                    spawnZenitsuImpact(serverLevel, victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D));
                    Vec3 victimCenter = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                        com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(victim)
                            .lerp(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(victim), 0.5D));
                    Vec3 targetAbove = target.add(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                        new Vec3(0.0D, 1.0D, 0.0D)));
                    spawnLightningSpark(serverLevel, victimCenter, targetAbove,
                        NORMAL_PRIMARY, 0.0875F, 8, false, Vec3.ZERO);
                }
                GuardStateHelper.clearAttackFlag(entity);
                entity.hurt(entity.damageSources().generic(), 6.0F);

                Vec3 targetAbove = target.add(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                    new Vec3(0.0D, 1.0D, 0.0D)));
                for (int i = 0; i < 5; i++) {
                    spawnLightningSpark(serverLevel, targetAbove,
                        randomSparkEndpoint(targetAbove, 12.0D), NORMAL_PRIMARY,
                        0.0875F, 8, false, Vec3.ZERO);
                }
                syncLightning(serverLevel, entity, true, false,
                    new Vec3[] {startWorld, target}, LightningVariant.NORMAL);
                return;
            }

            if (tick <= 77) {
                freezeZenitsuEntity(entity);
            }
        }, 1, 79);

        AbilityScheduler.scheduleOnce(entity, () -> {
            syncLightning(serverLevel, entity, false, false,
                new Vec3[] {startWorld, entity.position()}, LightningVariant.NORMAL);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "iai1");
        }, 78);
    }

    private static void startZenitsuForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                         int formId, boolean eightfold) {
        if (!(level instanceof ServerLevel serverLevel) || ZENITSU_STATES.containsKey(entity.getUUID())) {
            return;
        }

        int chargeTicks = eightfold ? 40 : 30;
        int launchWindowTicks = eightfold ? 60 : 40;
        int launchTicks = eightfold ? 3 : 5;
        int endingTicks = eightfold ? 15 : 20;
        ZenitsuState state = new ZenitsuState(
            entity.getUUID(),
            eightfold,
            eightfold ? 8 : 6,
            chargeTicks,
            launchWindowTicks,
            launchTicks,
            endingTicks,
            firstFormStartPoint(entity),
            entity.isNoGravity()
        );
        ZENITSU_STATES.put(entity.getUUID(), state);

        playThunderSound(serverLevel, entity,
            eightfold ? ModSounds.THUNDER_CHARGEUP_EIGHTFOLD.get() : ModSounds.THUNDER_CHARGEUP_SIXFOLD.get(),
            16.0D);
        GuardStateHelper.setGuardState(entity, 12.0D, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", chargeTicks, 1.0F, 4000);

        int totalTicks = chargeTicks + launchWindowTicks + endingTicks + 2;
        AbilityScheduler.scheduleRepeating(entity, () -> tickZenitsuForm(serverLevel, entity, state), 1, totalTicks);
    }

    private static void tickZenitsuForm(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (ZENITSU_STATES.get(entity.getUUID()) != state || state.cleaned) {
            return;
        }

        int tick = state.elapsedTicks++;
        if (!entity.isAlive()) {
            finishZenitsuForm(level, entity, state);
            return;
        }

        if (state.ending) {
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            state.endingTicksElapsed++;
            if (state.endingTicksElapsed >= state.endingTicks) {
                finishZenitsuForm(level, entity, state);
            }
            return;
        }

        if (tick < state.chargeTicks) {
            repeatIai1DuringCharge(entity, tick, state.chargeTicks);
            freezeZenitsuEntity(entity);
            return;
        }

        if (tick == state.chargeTicks) {
            state.windowOpen = true;
            state.path.clear();
            state.path.add(state.startWorld);
            state.path.add(entity.position());
            syncLightning(level, entity, true, true, state.pathArray(), LightningVariant.NORMAL, 40);
            beginZenitsuLaunch(level, entity, state);
            return;
        }

        if (tick < state.chargeTicks + state.launchWindowTicks) {
            if (state.launching) {
                advanceZenitsuLaunch(level, entity, state);
            } else {
                freezeZenitsuEntity(entity);
            }
            return;
        }

         beginZenitsuEnding(level, entity, state);
    }

    /**
     * Handles either mouse button during a Zenitsu chain. Returning true means the
     * click belongs to the ability and should not become a normal sword swing.
     */
    public static boolean handleZenitsuClick(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }

        ZenitsuState state = ZENITSU_STATES.get(entity.getUUID());
        if (state == null || state.ending) {
            return false;
        }

        if (state.launching) {
            if (state.launchesCompleted + state.queuedLaunches < state.maxLaunches) {
                state.queuedLaunches++;
            }
            return true;
        }

        if (state.windowOpen && !state.launching && state.launchesCompleted < state.maxLaunches) {
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.run(entity,
                () -> beginZenitsuLaunch(level, entity, state));
        }
        return true;
    }

    private static void beginZenitsuLaunch(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (state.launching || state.ending) {
            return;
        }

        double range = state.eightfold ? EIGHTFOLD_RANGE : SIXFOLD_RANGE;
        Vec3 targetWorld = raycastTarget(level, entity, range);
        Vec3 startLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        Vec3 targetLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(targetWorld);
        state.launchStartWorld = entity.position();
        state.targetWorld = targetWorld;
        state.targetLocal = targetLocal;
        state.dashVelocityLocal = targetLocal.subtract(startLocal).scale(1.0D / state.launchTicks);
        state.launchTick = 0;
        state.launching = true;

        playEntityAnimation(entity, "sword_to_right");
        playThunderSound(level, entity, ModSounds.THUNDERCLAP.get(), 30.0D);
    }

    private static Vec3 firstFormStartPoint(LivingEntity entity) {
        Vec3 startLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        Vec3 lookLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity).normalize();
        return com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            startLocal.subtract(lookLocal.scale(FIRST_FORM_START_OFFSET)));
    }

    private static void advanceZenitsuLaunch(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        MovementHelper.setVelocity(entity, state.dashVelocityLocal);
        state.launchTick++;
        updateZenitsuPath(level, entity, state);

        if (state.launchTick >= state.launchTicks) {
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.teleportTo(state.targetWorld.x, state.targetWorld.y, state.targetWorld.z);
            state.path.set(state.path.size() - 1, state.targetWorld);
            syncLightning(level, entity, true, false, state.pathArray(), LightningVariant.NORMAL, 40);

            GuardStateHelper.setAttackState(entity, 12.0D);
                damageTrajectory(level, entity, state.launchStartWorld, state.targetWorld, THUNDER_FORM_DAMAGE);
            GuardStateHelper.clearAttackFlag(entity);
            spawnZenitsuImpact(level, state.targetWorld);

            state.launchesCompleted++;
            state.launching = false;
            if (state.launchesCompleted >= state.maxLaunches) {
                beginZenitsuEnding(level, entity, state);
            } else {
                state.path.add(state.targetWorld);
                state.path.add(entity.position());
                syncLightning(level, entity, true, false, state.pathArray(), LightningVariant.NORMAL, 40);
                if (state.queuedLaunches > 0) {
                    state.queuedLaunches--;
                    beginZenitsuLaunch(level, entity, state);
                }
            }
        }
    }

    private static void updateZenitsuPath(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (!state.path.isEmpty()) {
            state.path.set(state.path.size() - 1, entity.position());
            syncLightning(level, entity, true, false, state.pathArray(), LightningVariant.NORMAL, 40);
        }
    }

    private static void beginZenitsuEnding(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (state.ending) {
            return;
        }
        state.ending = true;
        state.windowOpen = false;
        state.endingTicksElapsed = 0;
        MovementHelper.setVelocity(entity, Vec3.ZERO);
        playEntityAnimationOnLayer(entity, "iai1", state.endingTicks, 1.0F, 4000);
        spawnLightningSparks(level, entity, 4, NORMAL_SECONDARY, 0.0875F, 8, true);
    }

    private static void finishZenitsuForm(ServerLevel level, LivingEntity entity, ZenitsuState state) {
        if (state.cleaned) {
            return;
        }
        state.cleaned = true;
        ZENITSU_STATES.remove(entity.getUUID(), state);
        syncLightning(level, entity, false, false, state.pathArray(), LightningVariant.NORMAL, 40);
        MovementHelper.setVelocity(entity, Vec3.ZERO);
        entity.setNoGravity(state.originalNoGravity);
        GuardStateHelper.clearGuardState(entity);
        setCancelAttackSwing(entity, false);
        stopEntityAnimation(entity, "iai1");
    }

    private static void freezeZenitsuEntity(LivingEntity entity) {
        MovementHelper.setVelocity(entity, Vec3.ZERO);
        entity.setSprinting(false);
        if (!(entity instanceof Player)) {
            MovementHelper.lookAtTarget(entity);
        }
    }

    private static Vec3 raycastTarget(ServerLevel level, LivingEntity entity, double distance) {
        Vec3 eyeLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity);
        Vec3 lookLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity).normalize();
        Vec3 eyeWorld = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(eyeLocal);
        Vec3 lookWorld = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(lookLocal).normalize();
        Vec3 rayEnd = eyeWorld.add(lookWorld.scale(distance));
        BlockHitResult hit = level.clip(new ClipContext(
            eyeWorld, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        Vec3 hitWorld = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? rayEnd : hit.getLocation();
        Vec3 eyeHeightWorld = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            new Vec3(0.0D, entity.getEyeHeight(), 0.0D));
        Vec3 desiredFeetPosition = hitWorld.subtract(eyeHeightWorld);
        return findNearestSafePosition(level, entity, desiredFeetPosition);
    }

    /**
     * Raycast landing points can be inside the block whose face was hit, especially
     * when looking down. Search the nearest position around that point where the
     * entity's complete collision box is clear before allowing a teleport.
     */
    private static Vec3 findNearestSafePosition(ServerLevel level, LivingEntity entity, Vec3 desiredPosition) {
        final double step = 0.25D;
        final int radiusInSteps = 8; // Two blocks in every direction.
        Vec3 bestPosition = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -radiusInSteps; x <= radiusInSteps; x++) {
            for (int y = -radiusInSteps; y <= radiusInSteps; y++) {
                for (int z = -radiusInSteps; z <= radiusInSteps; z++) {
                    double offsetX = x * step;
                    double offsetY = y * step;
                    double offsetZ = z * step;
                    double distanceSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
                    if (distanceSquared > 4.0D || distanceSquared >= bestDistance) {
                        continue;
                    }

                    Vec3 candidate = desiredPosition.add(offsetX, offsetY, offsetZ);
                    Vec3 movement = candidate.subtract(entity.position());
                    if (level.noCollision(entity, entity.getBoundingBox().move(movement))) {
                        bestPosition = candidate;
                        bestDistance = distanceSquared;
                    }
                }
            }
        }

        return bestPosition != null ? bestPosition : entity.position();
    }

    private static void damageTrajectory(ServerLevel level, LivingEntity attacker, Vec3 start, Vec3 end,
                                         float damage) {
        for (LivingEntity target : damageTrajectoryTargets(level, attacker, start, end, 2.0D)) {
            Damager.hurt(attacker, target, damage);
            spawnZenitsuImpact(level, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
        }
    }

    private static LivingEntity findTrajectoryTarget(ServerLevel level, LivingEntity attacker,
                                                     Vec3 start, Vec3 end, double radius) {
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(start, end).inflate(radius),
            candidate -> candidate != attacker && candidate.isAlive()
                && !CrowEnhancementHandler.isKasugaiCrow(candidate))) {
            double reach = radius + target.getBbWidth() * 0.5D;
            if (distanceToSegmentSquared(target.getBoundingBox().getCenter(), start, end) <= reach * reach) {
                double distance = target.distanceToSqr(start);
                if (distance < closestDistance) {
                    closest = target;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    private static Set<LivingEntity> damageTrajectoryTargets(ServerLevel level, LivingEntity attacker,
                                                             Vec3 start, Vec3 end, double radius) {
        AABB trajectory = new AABB(start, end).inflate(radius);
        Set<LivingEntity> targets = new HashSet<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, trajectory,
            candidate -> candidate != attacker && candidate.isAlive()
                && !CrowEnhancementHandler.isKasugaiCrow(candidate))) {
            double reach = radius + target.getBbWidth() * 0.5D;
            if (distanceToSegmentSquared(target.getBoundingBox().getCenter(), start, end) <= reach * reach) {
                targets.add(target);
            }
        }
        return targets;
    }

    private static Set<LivingEntity> nearbyLivingEntities(ServerLevel level, LivingEntity attacker,
                                                          Vec3 center, double radius) {
        double radiusSquared = radius * radius;
        Set<LivingEntity> targets = new HashSet<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(center, center).inflate(radius),
            candidate -> candidate != attacker && candidate.isAlive()
                && !CrowEnhancementHandler.isKasugaiCrow(candidate))) {
            if (target.position().distanceToSqr(center) <= radiusSquared) {
                targets.add(target);
            }
        }
        return targets;
    }

    private static void spawnLightningSparks(ServerLevel level, LivingEntity entity, int count,
                                             Vec3 color, float width, int lifetime, boolean flickering) {
        spawnLightningSparks(level, entity, count, color, width, lifetime, flickering, 2.0D);
    }

    private static void spawnLightningSparks(ServerLevel level, LivingEntity entity, int count,
                                             Vec3 color, float width, int lifetime, boolean flickering,
                                             double blockRange) {
        Vec3 localFeet = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        Vec3 localHead = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity);
        Vec3 start = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            localFeet.lerp(localHead, 0.5D));
        for (int i = 0; i < count; i++) {
            spawnLightningSpark(level, start, findSparkEndpoint(level, start, blockRange).position(),
                color, width, lifetime, flickering, new Vec3(0.0D, 1.0D, 0.0D));
        }
    }

    private static void spawnLightningSparks(ServerLevel level, LivingEntity entity,
                                             LightningVariant lightningVariant) {
        Vec3 outerColor = switch (lightningVariant) {
            case KAIGAKU -> BLUE_PRIMARY;
            case BLUE -> BLUE_SECONDARY;
            case NORMAL -> NORMAL_SECONDARY;
        };
        spawnLightningSparks(level, entity, 4, outerColor, 0.0875F, 8, true);
    }

    private static void spawnLightningSpark(ServerLevel level, Vec3 start, Vec3 end, Vec3 color,
                                            float width, int lifetime, boolean flickering, Vec3 arcing) {
        ThunderLightningSparkSyncPacket packet = new ThunderLightningSparkSyncPacket(
            UUID.randomUUID(), true, true, start, end, Vec3.ZERO, arcing,
            color, new Vec3(1.0D, 1.0D, 1.0D), width, lifetime, flickering);
        ModNetworking.sendToNearby(packet, level, end.x, end.y, end.z, 128.0D);
    }

    private static Vec3 randomSparkEndpoint(Vec3 start, double distance) {
        double theta = Math.random() * Math.PI * 2.0D;
        double vertical = Math.random() * 2.0D - 1.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        return start.add(Math.cos(theta) * horizontal * distance,
            vertical * distance, Math.sin(theta) * horizontal * distance);
    }

    private static void spawnZenitsuImpact(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.FLASH, position.x, position.y, position.z,
            1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z,
            12, 0.35D, 0.35D, 0.35D, 0.04D);
    }

    private static SparkEndpoint findSparkEndpoint(ServerLevel level, Vec3 start) {
        return findSparkEndpoint(level, start, 2.0D);
    }

    private static SparkEndpoint findSparkEndpoint(ServerLevel level, Vec3 start, double blockRange) {
        List<Vec3> solidBlocks = new ArrayList<>();
        BlockPos origin = BlockPos.containing(start);
        int searchRadius = (int) Math.ceil(blockRange);
        double rangeSquared = blockRange * blockRange;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    Vec3 center = Vec3.atCenterOf(candidate);
                    if (center.distanceToSqr(start) <= rangeSquared
                        && level.getBlockState(candidate).isSolidRender(level, candidate)) {
                        solidBlocks.add(center);
                    }
                }
            }
        }

        if (!solidBlocks.isEmpty()) {
            return new SparkEndpoint(solidBlocks.get(level.random.nextInt(solidBlocks.size())), true);
        }

        double theta = level.random.nextDouble() * Math.PI * 2.0D;
        double vertical = level.random.nextDouble() * 2.0D - 1.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        Vec3 direction = new Vec3(Math.cos(theta) * horizontal, vertical, Math.sin(theta) * horizontal);
        return new SparkEndpoint(start.add(direction.scale(blockRange)), false);
    }

    private static void repeatIai1DuringCharge(LivingEntity entity, int tick, int chargeTicks) {
        if (tick > 0 && tick < chargeTicks && tick % IAI1_ANIMATION_TICKS == 0) {
            playEntityAnimationOnLayer(entity, "iai1", chargeTicks - tick, 1.0F, 4000);
        }
    }

    private static void syncLightning(ServerLevel level, LivingEntity entity, boolean active, boolean spawn,
                                      Vec3[] positions, LightningVariant lightningVariant) {
        syncLightning(level, entity, active, spawn, positions, lightningVariant, 15);
    }

    private static void syncLightning(ServerLevel level, LivingEntity entity, boolean active, boolean spawn,
                                      Vec3[] positions, LightningVariant lightningVariant, int lifetime) {
        Vec3 primary = lightningVariant == LightningVariant.NORMAL ? NORMAL_PRIMARY : BLUE_PRIMARY;
        Vec3 secondary = switch (lightningVariant) {
            case NORMAL -> NORMAL_SECONDARY;
            case BLUE -> BLUE_SECONDARY;
            case KAIGAKU -> KAIGAKU_SECONDARY;
        };
        Vec3[] elevatedPositions = new Vec3[positions.length];
        for (int i = 0; i < positions.length; i++) {
            Vec3 position = positions[i];
            elevatedPositions[i] = position.add(0.0D, 1.0D, 0.0D);
        }
        ThunderLightningSyncPacket packet = new ThunderLightningSyncPacket(
            entity.getUUID(), active, spawn, elevatedPositions, primary, secondary,
            lightningVariant == LightningVariant.NORMAL, lifetime);
        Vec3 end = elevatedPositions.length == 0
            ? entity.position().add(0.0D, 1.0D, 0.0D)
            : elevatedPositions[elevatedPositions.length - 1];
        ModNetworking.sendToNearby(packet, level, end.x, end.y, end.z, 64.0D);
    }

    private static double distanceToSegmentSquared(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 1.0E-8D) {
            return point.distanceToSqr(start);
        }
        double progress = point.subtract(start).dot(segment) / lengthSquared;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return point.distanceToSqr(start.add(segment.scale(progress)));
    }

    private static void playThunderSound(ServerLevel level, LivingEntity source, SoundEvent sound, double radius) {
        var holder = ForgeRegistries.SOUND_EVENTS.getHolder(sound).orElse(null);
        if (holder == null) {
            return;
        }
        double radiusSquared = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(source) <= radiusSquared) {
                player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS,
                    source.getX(), source.getY(), source.getZ(), 1.0F, 1.0F, level.random.nextLong()));
            }
        }
    }

    private static void playEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimation(player, animationName);
        } else {
            AnimationHelper.playAnimation(entity, animationName);
        }
    }

    private static void playEntityAnimationOnLayer(LivingEntity entity, String animationName,
                                                   int maxTicks, float speed, int layer) {
        if (entity instanceof Player player) {
            AnimationHelper.playAnimationOnLayer(player, animationName, maxTicks, speed, layer);
        } else {
            AnimationHelper.playAnimationOnLayer(entity, animationName, maxTicks, speed, layer);
        }
    }

    private static void setCancelAttackSwing(LivingEntity entity, boolean value) {
        if (!(entity instanceof Player player)) {
            return;
        }
        player.getCapability(KimetsunoyaibaMultiplayer.SWORD_WIELDER_DATA)
            .ifPresent(data -> data.setCancelAttackSwing(value));
        if (player instanceof ServerPlayer serverPlayer) {
            ModNetworking.sendToPlayer(new SwordWielderSyncPacket(player.getUUID(), value), serverPlayer);
        }
    }

    private static void stopEntityAnimation(LivingEntity entity, String animationName) {
        if (entity instanceof Player player) {
            ResourceLocation animation = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", animationName);
            ModNetworking.sendToAllClients(new AnimationSyncPacket(
                player.getUUID(), animation, 0, 0, false, true));
        } else if (!entity.level().isClientSide) {
            ModNetworking.sendToAllClients(new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.MobAnimationSyncPacket(
                entity.getId(), "__stop__"));
        }
    }

    private static final class ZenitsuState {
        private final UUID entityId;
        private final boolean eightfold;
        private final int maxLaunches;
        private final int chargeTicks;
        private final int launchWindowTicks;
        private final int launchTicks;
        private final int endingTicks;
        private final Vec3 startWorld;
        private final boolean originalNoGravity;
        private final List<Vec3> path = new ArrayList<>();
        private int elapsedTicks;
        private int launchesCompleted;
        private int queuedLaunches;
        private int launchTick;
        private int endingTicksElapsed;
        private boolean windowOpen;
        private boolean launching;
        private boolean ending;
        private boolean cleaned;
        private Vec3 launchStartWorld;
        private Vec3 targetWorld;
        private Vec3 targetLocal;
        private Vec3 dashVelocityLocal;

        private ZenitsuState(UUID entityId, boolean eightfold, int maxLaunches, int chargeTicks,
                             int launchWindowTicks, int launchTicks, int endingTicks,
                             Vec3 startWorld, boolean originalNoGravity) {
            this.entityId = entityId;
            this.eightfold = eightfold;
            this.maxLaunches = maxLaunches;
            this.chargeTicks = chargeTicks;
            this.launchWindowTicks = launchWindowTicks;
            this.launchTicks = launchTicks;
            this.endingTicks = endingTicks;
            this.startWorld = startWorld;
            this.originalNoGravity = originalNoGravity;
        }

        private Vec3[] pathArray() {
            return path.toArray(new Vec3[0]);
        }
    }

    private enum LightningVariant {
        NORMAL,
        BLUE,
        KAIGAKU
    }

    private record SparkEndpoint(Vec3 position, boolean solidBlock) {
    }
}
