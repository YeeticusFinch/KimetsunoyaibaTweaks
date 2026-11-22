package com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.AfterImageEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ThunderDragonEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.BaseFireBlock;
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
    private static final double SECOND_FORM_RANGE = 20.0D;
    private static final double THIRD_FORM_RANGE = 20.0D;
    private static final int SIXFOLD_FORM_ID = 30002;
    private static final int EIGHTFOLD_FORM_ID = 30003;
    private static final int GODSPEED_FORM_ID = 30004;
    private static final int IAI1_ANIMATION_TICKS = 15;
    private static final double GODSPEED_SPARK_BLOCK_RANGE = 3.0D;
    private static final float THUNDER_FORM_DAMAGE = 12.0F;
    private static final float THUNDER_SWARM_DAMAGE = 4.0F;
    private static final Vec3 HEAT_LIGHTNING_ORANGE = new Vec3(1.0D, 0.35D, 0.03D);
    private static final Vec3 HEAT_LIGHTNING_RED = new Vec3(0.95D, 0.03D, 0.01D);
    private static final DustParticleOptions BLACK_DUST =
        new DustParticleOptions(new org.joml.Vector3f(0.0F, 0.0F, 0.0F), 4.0F);
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
        if (formId == 303) {
            return thirdForm(baseForm, lightningVariant);
        }
        if (formId == 304) {
            return fourthForm(baseForm, lightningVariant);
        }
        if (formId == 305) {
            return fifthForm(baseForm, lightningVariant);
        }
        if (formId == 306) {
            return sixthForm(baseForm, lightningVariant);
        }
        if (formId == 307) {
            return seventhForm(baseForm, lightningVariant);
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

    private static BreathingForm thirdForm(BaseKnYForms.BaseForm baseForm,
                                           LightningVariant lightningVariant) {
        return new BreathingForm(
            303,
            baseForm.name,
            "Lock onto a target and attack from every direction with lightning afterimages",
            5,
            (entity, level, formId) -> executeThirdForm(entity, level, formId, lightningVariant)
        );
    }

    private static BreathingForm fourthForm(BaseKnYForms.BaseForm baseForm,
                                            LightningVariant lightningVariant) {
        return new BreathingForm(
            304,
            baseForm.name,
            "Blind enemies with four distant lightning slashes before dashing through them",
            5,
            (entity, level, formId) -> executeFourthForm(entity, level, formId, lightningVariant)
        );
    }

    private static BreathingForm fifthForm(BaseKnYForms.BaseForm baseForm,
                                           LightningVariant lightningVariant) {
        return new BreathingForm(
            305,
            baseForm.name,
            "Charge red lightning, then dash with a paralyzing upward slash",
            5,
            (entity, level, formId) -> executeFifthForm(entity, level, formId, lightningVariant)
        );
    }

    private static BreathingForm sixthForm(BaseKnYForms.BaseForm baseForm,
                                           LightningVariant lightningVariant) {
        return new BreathingForm(
            306,
            baseForm.name,
            "Envelop yourself in dark smoke and unleash an explosive lightning slash",
            5,
            (entity, level, formId) -> executeSixthForm(entity, level, formId, lightningVariant)
        );
    }

    private static BreathingForm seventhForm(BaseKnYForms.BaseForm baseForm,
                                             LightningVariant lightningVariant) {
        return new BreathingForm(
            307,
            baseForm.name,
            "Charge a legendary thunder dragon, then cross the battlefield in one devastating slash",
            30,
            (entity, level, formId) -> executeSeventhForm(entity, level, formId, lightningVariant)
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
        final Vec3 targetPosition = raycastTarget(serverLevel, entity, SECOND_FORM_RANGE);
        final LivingEntity target = findTrajectoryTarget(serverLevel, entity, start, targetPosition, 10.0D);
        final boolean originalNoGravity = entity.isNoGravity();
        final float originalStepHeight = entity.getStepHeight();
        final int[] elapsed = {0};

        playThunderSound(serverLevel, entity, ModSounds.RICE_SPIRIT_DASH.get(), 30.0D);
        GuardStateHelper.setGuardState(entity, THUNDER_FORM_DAMAGE, formId);
        setCancelAttackSwing(entity, true);
        MovementHelper.setStepHeight(entity, 3.0F);
        entity.setNoGravity(true);
        entity.setSprinting(true);
        playEntityAnimationOnLayer(entity, "sprinting", 6, 1.0F, 4000);
        if (target != null) {
            addImmovable(target, 30);
        }

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = elapsed[0]++;
            if (tick < 5) {
                entity.setSprinting(true);
                MovementHelper.setVelocity(entity, targetPosition.subtract(start).scale(0.2D));
                return;
            }

            entity.setSprinting(false);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            if (tick == 5) {
                entity.teleportTo(targetPosition.x, targetPosition.y, targetPosition.z);
                syncLightning(serverLevel, entity, true, true,
                    new Vec3[] {start, targetPosition}, lightningVariant);
            }

            if (target != null && tick >= 6 && tick <= 30) {
                playEntityAnimationOnLayer(entity, "iai1", 25, 1.0F, 4000);
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                spawnThunderParticles(serverLevel, target);
            }

            if (target != null && tick == 20) {
                GuardStateHelper.setAttackState(entity, THUNDER_FORM_DAMAGE);
                Damager.hurt(entity, target, THUNDER_FORM_DAMAGE);
                GuardStateHelper.clearAttackFlag(entity);
                playThunderSound(serverLevel, target, ModSounds.RICE_SPIRIT_ELECTRIC_DAMAGE.get(), 20.0D);
                Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                spawnZenitsuImpact(serverLevel, center);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            }

            if (target != null && tick == 30) {
                removeImmovable(target);
                spawnZenitsuImpact(serverLevel, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
                final UUID knockbackSparkId = UUID.randomUUID();
                final Vec3 knockbackSparkStart = target.position();
                syncLightningSpark(serverLevel, knockbackSparkId, true, true, knockbackSparkStart,
                    target.position(), lightningSecondary(lightningVariant), 0.2F, 15, false, Vec3.ZERO);
                AbilityScheduler.scheduleRepeating(entity, new Runnable() {
                    private int sparkTicks;

                    @Override
                    public void run() {
                        if (sparkTicks++ >= 15 || !target.isAlive()) {
                            syncLightningSpark(serverLevel, knockbackSparkId, false, false,
                                knockbackSparkStart, target.position(), lightningSecondary(lightningVariant),
                                0.2F, 1, false, Vec3.ZERO);
                            return;
                        }
                        syncLightningSpark(serverLevel, knockbackSparkId, true, false, knockbackSparkStart,
                            target.position(), lightningSecondary(lightningVariant), 0.2F, 15, false, Vec3.ZERO);
                    }
                }, 1, 16);
                Vec3 knockback = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                    com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity))
                    .normalize().scale(1.5D);
                target.setDeltaMovement(knockback.x, 0.75D, knockback.z);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 4, false, false, false));
                playThunderSound(serverLevel, target, ModSounds.RICE_SPIRIT_KNOCKBACK.get(), 20.0D);
            }
        }, 1, 32);

        AbilityScheduler.scheduleOnce(entity, () -> {
            syncLightning(serverLevel, entity, false, false,
                new Vec3[] {start, targetPosition}, lightningVariant);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setSprinting(false);
            entity.setNoGravity(originalNoGravity);
            MovementHelper.setStepHeight(entity, originalStepHeight);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "sprinting");
            stopEntityAnimation(entity, "iai1");
        }, 31);
    }

    private static void executeThirdForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                         int formId, LightningVariant lightningVariant) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final boolean kaigaku = lightningVariant == LightningVariant.KAIGAKU;
        final boolean originalNoGravity = entity.isNoGravity();
        final float originalStepHeight = entity.getStepHeight();
        final int[] elapsed = {0};
        final LivingEntity[] target = {null};
        final List<AfterImageEntity> afterImages = new ArrayList<>();
        final List<UUID> circleSparks = new ArrayList<>();
        final boolean[] cancelled = {false};

        GuardStateHelper.setGuardState(entity, THUNDER_SWARM_DAMAGE, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", 10, 1.0F, 4000);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            if (cancelled[0]) {
                return;
            }

            int tick = elapsed[0]++;
            if (tick < 10) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                MovementHelper.lookAtTarget(entity);
                repeatIai1DuringCharge(entity, tick, 10);
                return;
            }

            if (tick == 10) {
                target[0] = findCrosshairTarget(serverLevel, entity, THIRD_FORM_RANGE);
                if (target[0] == null) {
                    cancelled[0] = true;
                    if (entity instanceof ServerPlayer player) {
                        player.displayClientMessage(Component.literal(
                            "You must be looking at a target entity to use Thunder Swarm."), true);
                    }
                    finishThirdForm(entity, originalNoGravity, originalStepHeight, afterImages, circleSparks,
                        serverLevel);
                    return;
                }
                Vec3 midpoint = entity.position().lerp(target[0].position(), 0.5D);
                playThunderSoundAt(serverLevel, midpoint, ModSounds.THUNDER_SWARM_DASH.get(), 20.0D);
                return;
            }

            if (target[0] == null || !target[0].isAlive()) {
                finishThirdForm(entity, originalNoGravity, originalStepHeight, afterImages, circleSparks,
                    serverLevel);
                cancelled[0] = true;
                return;
            }

            Vec3 targetPosition = target[0].position();
            Vec3 behind = thunderSwarmBehind(target[0]);
            if (tick >= 11 && tick <= 20) {
                MovementHelper.setVelocity(entity,
                    com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(behind)
                        .subtract(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity))
                        .scale(0.1D));
                playEntityAnimationOnLayer(entity, "speed_attack_sword", 10, 1.0F, 4000);
                if (tick == 20) {
                    MovementHelper.setVelocity(entity, Vec3.ZERO);
                    entity.teleportTo(behind.x, behind.y, behind.z);
                }
                return;
            }

            if (tick == 21) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                entity.teleportTo(behind.x, behind.y, behind.z);
                spawnThunderSwarmAfterImages(serverLevel, entity, target[0], kaigaku, afterImages);
                playEntityAnimation(entity, "speed_attack_sword");
                for (int i = 0; i < 4; i++) {
                    circleSparks.add(UUID.randomUUID());
                }
                updateThunderSwarmSparks(serverLevel, target[0], circleSparks, lightningVariant, true);
                playThunderSoundAt(serverLevel, targetPosition,
                    ModSounds.THUNDER_SWARM_AFTER_IMAGE_SWARM.get(), 20.0D);
            }

            if (tick >= 21 && tick <= 81) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                MovementHelper.lookAt(entity, target[0].position().add(0.0D, target[0].getBbHeight() * 0.5D, 0.0D));
                updateThunderSwarmAfterImages(afterImages, target[0]);
                updateThunderSwarmSparks(serverLevel, target[0], circleSparks, lightningVariant, false);
                spawnThunderSwarmParticles(serverLevel, target[0], tick);

                if (tick > 21 && (tick - 21) % 8 == 0 && !afterImages.isEmpty()) {
                    AfterImageEntity selected = afterImages.get(kaigaku
                        ? serverLevel.random.nextInt(afterImages.size()) : 0);
                    Vec3 userPosition = entity.position();
                    Vec3 imagePosition = selected.position();
                    entity.teleportTo(imagePosition.x, imagePosition.y, imagePosition.z);
                    selected.teleportTo(userPosition.x, userPosition.y, userPosition.z);
                }

                if ((tick - 21) % 3 == 0) {
                    LivingEntity visualAttacker = entity;
                    if (kaigaku && !afterImages.isEmpty()) {
                        visualAttacker = afterImages.get(serverLevel.random.nextInt(afterImages.size()));
                    } else if (!kaigaku && !afterImages.isEmpty() && serverLevel.random.nextBoolean()) {
                        visualAttacker = afterImages.get(0);
                    }
                    String[] attacks = {"sword_to_left", "sword_to_right", "sword_to_upper"};
                    playEntityAnimation(visualAttacker, attacks[serverLevel.random.nextInt(attacks.length)]);
                    GuardStateHelper.setAttackState(entity, THUNDER_SWARM_DAMAGE);
                    if (Damager.hurt(entity, target[0], THUNDER_SWARM_DAMAGE)) {
                        Vec3 push = target[0].position().subtract(entity.position());
                        if (push.lengthSqr() < 1.0E-6D) {
                            push = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                                com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity));
                        }
                        push = push.normalize().scale(0.3D);
                        target[0].setDeltaMovement(push.x, 0.15D, push.z);
                        target[0].hurtMarked = true;
                        spawnZenitsuImpact(serverLevel,
                            target[0].position().add(0.0D, target[0].getBbHeight() * 0.5D, 0.0D));
                    }
                    GuardStateHelper.clearAttackFlag(entity);
                }
            }

            if (tick == 82) {
                finishThirdForm(entity, originalNoGravity, originalStepHeight, afterImages, circleSparks,
                    serverLevel);
            }
        }, 1, 84);
    }

    private static void executeFourthForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                          int formId, LightningVariant lightningVariant) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final boolean originalNoGravity = entity.isNoGravity();
        final float originalStepHeight = entity.getStepHeight();
        final int[] elapsed = {0};
        final List<ThunderSparkState> sparks = new ArrayList<>();
        final Set<UUID> damaged = new HashSet<>();
        final Vec3[] dashDirection = {null};
        final String[] slashAnimations = {"sword_to_left", "sword_to_right", "sword_overhead", "sword_to_upper"};

        playThunderSound(serverLevel, entity, ModSounds.DISTANT_THUNDER_BLINDING_SLASHES.get(), 25.0D);
        GuardStateHelper.setGuardState(entity, THUNDER_FORM_DAMAGE, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = elapsed[0]++;
            if (tick <= 8) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                if (tick < 8 && (tick & 1) == 0) {
                    playEntityAnimationOnLayer(entity, slashAnimations[tick / 2], 2, 3.0F, 4000);
                }
                if (tick % 2 == 0 && sparks.size() < 4) {
                    ThunderSparkState spark = createDistantThunderSpark(entity, sparks.size(), lightningVariant);
                    sparks.add(spark);
                    syncSpark(serverLevel, spark, true);
                }
                updateDistantThunderAttachedSparks(serverLevel, entity, sparks, lightningVariant);
                if (tick < 8) {
                    return;
                }
                Vec3 projectileCenter = Vec3.ZERO;
                int endpointCount = 0;
                for (ThunderSparkState spark : sparks) {
                    projectileCenter = projectileCenter.add(spark.start).add(spark.end);
                    endpointCount += 2;
                }
                projectileCenter = projectileCenter.scale(1.0D / endpointCount);
                Vec3 projectileDirection = frameVector(thunderFrame(entity).forward).normalize();
                for (ThunderSparkState spark : sparks) {
                    spark.projectileOrigin = projectileCenter;
                    spark.projectileDirection = projectileDirection;
                    spark.initialStart = spark.start;
                    spark.initialEnd = spark.end;
                }
            }

            if (tick >= 8 && tick <= 18) {
                int projectileTick = tick - 8;
                updateDistantThunderProjectileSparks(serverLevel, sparks, projectileTick);
                damageDistantThunderDebuffs(serverLevel, entity, sparks);
            }

            if (tick == 13) {
                dashDirection[0] = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                    .look(entity).normalize();
                MovementHelper.setStepHeight(entity, 3.0F);
                playThunderSound(serverLevel, entity, ModSounds.DISTANT_THUNDER_DASH.get(), 25.0D);
                playEntityAnimationOnLayer(entity, "sword_rotate", 12, 1.0F, 4000);
            }

            if (tick >= 13 && tick < 23 && dashDirection[0] != null) {
                MovementHelper.setVelocity(entity, dashDirection[0].scale(1.5D));
                for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(5.0D),
                    candidate -> candidate != entity && candidate.isAlive()
                        && !CrowEnhancementHandler.isKasugaiCrow(candidate))) {
                    if (damaged.add(target.getUUID())) {
                        GuardStateHelper.setAttackState(entity, THUNDER_FORM_DAMAGE);
                        Damager.hurt(entity, target, THUNDER_FORM_DAMAGE);
                        GuardStateHelper.clearAttackFlag(entity);
                        MovementHelper.addVelocity(target,
                            frameVector(dashDirection[0]).scale(0.8D).add(0.0D, 0.35D, 0.0D));
                    }
                }
            } else if (tick == 23) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
            }

            if (tick == 19) {
                playThunderSound(serverLevel, entity, ModSounds.DISTANT_THUNDER_SLASH.get(), 25.0D);
            }
        }, 1, 26);

        AbilityScheduler.scheduleOnce(entity, () -> {
            clearThunderSparks(serverLevel, sparks);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            MovementHelper.setStepHeight(entity, originalStepHeight);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "sword_rotate");
        }, 25);
    }

    private static void executeFifthForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                         int formId, LightningVariant lightningVariant) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final boolean fireTargets = lightningVariant == LightningVariant.KAIGAKU || Damager.isDemon(entity);
        final boolean originalNoGravity = entity.isNoGravity();
        final float originalStepHeight = entity.getStepHeight();
        final int[] elapsed = {0};
        final List<ThunderSparkState> chargeSparks = new ArrayList<>();
        final List<ThunderSparkState> targetSparks = new ArrayList<>();
        final Set<UUID> markedTargets = new HashSet<>();
        final Vec3[] dashDirection = {null};

        playThunderSound(serverLevel, entity, ModSounds.HEAT_LIGHTNING_CHARGING.get(), 20.0D);
        GuardStateHelper.setGuardState(entity, THUNDER_FORM_DAMAGE, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", 16, 1.0F, 4000);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = elapsed[0]++;
            if (tick < 15) {
                freezeZenitsuEntity(entity);
                updateHeatLightningChargeSparks(serverLevel, entity, chargeSparks, tick);
                return;
            }

            if (tick == 15) {
                dashDirection[0] = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                    .look(entity).normalize();
                MovementHelper.setStepHeight(entity, 3.0F);
                playThunderSound(serverLevel, entity, ModSounds.HEAT_LIGHTNING_TAKEOFF.get(), 20.0D);
                playEntityAnimation(entity, "speed_attack_sword");
            }

            if (tick >= 15 && tick <= 25 && dashDirection[0] != null) {
                int b = tick - 15;
                if (tick < 25) {
                    MovementHelper.setVelocity(entity, dashDirection[0].scale(2.0D));
                } else {
                    MovementHelper.setVelocity(entity, Vec3.ZERO);
                }
                Vec3 localPosition = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
                Vec3 localForward = dashDirection[0];
                Vec3 localStart = localPosition.subtract(localForward.scale(5.0D - b * 0.5D));
                Vec3 localEnd = localPosition.add(localForward.scale(6.0D - b / 2.0D))
                    .add(new Vec3(0.0D, 15.0D * b, 0.0D));
                syncLightningBolt(serverLevel, UUID.randomUUID(), true, true,
                    new Vec3[] {
                        com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(localStart),
                        com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(localEnd)
                    }, HEAT_LIGHTNING_ORANGE, HEAT_LIGHTNING_RED, false, 20);

                for (LivingEntity target : damageTrajectoryTargets(serverLevel, entity,
                    com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(localStart),
                    com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(localEnd), 1.5D)) {
                    if (markedTargets.add(target.getUUID())) {
                        GuardStateHelper.setAttackState(entity, THUNDER_FORM_DAMAGE);
                        Damager.hurt(entity, target, THUNDER_FORM_DAMAGE);
                        GuardStateHelper.clearAttackFlag(entity);
                    }
                }
            }

            if (tick == 21) {
                playEntityAnimation(entity, "sword_to_upper");
                playThunderSound(serverLevel, entity, ModSounds.HEAT_LIGHTNING_HEAT_SLASH.get(), 20.0D);
            }

            if (tick == 25) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
            }

            if (tick == 30) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                playEntityAnimationOnLayer(entity, "iai1", 10, 1.0F, 4000);
                for (UUID targetId : markedTargets) {
                    if (!(serverLevel.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive()) {
                        continue;
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, false, false));
                    addImmovable(target, 100);
                    spawnZenitsuImpact(serverLevel,
                        target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
                    playThunderSound(serverLevel, target, ModSounds.HEAT_LIGHTNING_PARALYZING_DAMAGE.get(), 20.0D);
                    if (fireTargets) {
                        target.setSecondsOnFire(5);
                        igniteNearbyBlocks(serverLevel, target);
                    }
                    ThunderSparkState spark = newThunderSpark(HEAT_LIGHTNING_RED, 0.08F, 20, false);
                    spark.targetId = targetId;
                    targetSparks.add(spark);
                    updateHeatLightningTargetSpark(serverLevel, target, spark, true);
                }
            }

            if (tick >= 30 && tick < 50) {
                for (ThunderSparkState spark : targetSparks) {
                    if (serverLevel.getEntity(spark.targetId) instanceof LivingEntity target && target.isAlive()) {
                        updateHeatLightningTargetSpark(serverLevel, target, spark, false);
                    }
                }
            }

            if (tick >= 30 && tick < 35) {
                freezeZenitsuEntity(entity);
            }
        }, 1, 52);

        AbilityScheduler.scheduleOnce(entity, () -> {
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            MovementHelper.setStepHeight(entity, originalStepHeight);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "iai1");
            stopEntityAnimation(entity, "speed_attack_sword");
            stopEntityAnimation(entity, "sword_to_upper");
        }, 35);
        AbilityScheduler.scheduleOnce(entity, () -> clearThunderSparks(serverLevel, targetSparks), 50);
    }

    private static void executeSixthForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                         int formId, LightningVariant lightningVariant) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final boolean originalNoGravity = entity.isNoGravity();
        final float originalStepHeight = entity.getStepHeight();
        final int[] elapsed = {0};
        final List<ThunderSparkState> sparks = new ArrayList<>();
        final Set<UUID> damagedTargets = new HashSet<>();
        final Vec3[] dashDirection = {null};
        final Vec3[] tubeOrigin = {null};

        playThunderSound(serverLevel, entity, ModSounds.RUMBLE_AND_FLASH_CHARGING.get(), 20.0D);
        GuardStateHelper.setGuardState(entity, THUNDER_FORM_DAMAGE, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", 21, 1.0F, 4000);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = elapsed[0]++;
            if (tick < 20) {
                freezeZenitsuEntity(entity);
                spawnRumbleChargeSmoke(serverLevel, entity);
                return;
            }

            if (tick == 20) {
                dashDirection[0] = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                    .look(entity).normalize();
                tubeOrigin[0] = entity.position();
                MovementHelper.setStepHeight(entity, 3.0F);
                playThunderSound(serverLevel, entity, ModSounds.RUMBLE_AND_FLASH_TAKEOFF.get(), 20.0D);
                playEntityAnimation(entity, "side_flip");
            }

            if (tick >= 20 && tick < 30 && dashDirection[0] != null) {
                MovementHelper.setVelocity(entity, dashDirection[0].scale(1.5D));
                spawnRumbleSmokeRing(serverLevel, entity, dashDirection[0]);
                if (sparks.size() < 10) {
                    ThunderSparkState spark = newThunderSpark(
                        (sparks.size() & 1) == 0 ? lightningPrimary(lightningVariant)
                            : lightningSecondary(lightningVariant),
                        0.05F + serverLevel.random.nextFloat() * 0.04F, 40 + serverLevel.random.nextInt(11), true);
                    sparks.add(spark);
                    updateRumbleSparks(serverLevel, entity, List.of(spark), tubeOrigin[0], dashDirection[0], tick);
                }
                updateRumbleSparks(serverLevel, entity, sparks, tubeOrigin[0], dashDirection[0], tick);
                damageRumblePath(serverLevel, entity, tubeOrigin[0], entity.position(), damagedTargets);
            }

            if (tick == 30) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                playThunderSound(serverLevel, entity, ModSounds.RUMBLE_AND_FLASH_STRONG_SLASH.get(), 25.0D);
                playEntityAnimation(entity, "sword_rotate");
                spawnZenitsuImpact(serverLevel, entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));
                for (ThunderSparkState spark : sparks) {
                    spark.width = spark.baseWidth * 1.6F;
                }
            }

            if (tick >= 30 && tick < 40) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                spawnRumbleSmokeRing(serverLevel, entity, dashDirection[0]);
                updateRumbleSparks(serverLevel, entity, sparks, tubeOrigin[0], dashDirection[0], tick);
            }

            if (tick == 40) {
                playThunderSound(serverLevel, entity, ModSounds.RUMBLE_AND_FLASH_LEFTOVER_LIGHTNING.get(), 20.0D);
                for (ThunderSparkState spark : sparks) {
                    spark.width *= 0.5F;
                }
            }

            if (tick >= 40 && tick < 50) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                updateRumbleSparks(serverLevel, entity, sparks, tubeOrigin[0], dashDirection[0], tick);
            }
        }, 1, 52);

        AbilityScheduler.scheduleOnce(entity, () -> {
            clearThunderSparks(serverLevel, sparks);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            MovementHelper.setStepHeight(entity, originalStepHeight);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "iai1");
            stopEntityAnimation(entity, "side_flip");
            stopEntityAnimation(entity, "sword_rotate");
        }, 50);
    }

    private static void executeSeventhForm(LivingEntity entity, net.minecraft.world.level.Level level,
                                           int formId, LightningVariant lightningVariant) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final boolean gravityEnabled = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity.isEnabled();
        final Direction originalGravity = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity
            .getBaseGravityDirection(entity);
        final SeventhGravitySelection gravitySelection = findSeventhGravitySelection(serverLevel, entity, gravityEnabled);
        final Direction forcedGravity = gravitySelection == null ? null : gravitySelection.direction();
        final boolean originalNoGravity = entity.isNoGravity();
        final int[] elapsed = {0};
        final List<ThunderSparkState> chargeSparks = new ArrayList<>();
        final List<ThunderSparkState> travelingSparks = new ArrayList<>();
        final List<ThunderSparkState> launchSparks = new ArrayList<>();
        final Set<BlockPos> chargeBlocks = new HashSet<>();
        final Set<UUID> travelingTargets = new HashSet<>();
        final Vec3 startPosition = entity.position();
        final Vec3[] launchDirection = {null};
        final SeventhFormTarget[] target = {null};
        final ThunderDragonEntity[] dragon = {null};

        if (gravityEnabled && forcedGravity != null) {
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity.setBaseGravityDirection(entity, forcedGravity);
        }
        playThunderSound(serverLevel, entity, ModSounds.THUNDER_GOD_CHARGING.get(), 20.0D);
        GuardStateHelper.setGuardState(entity, 40.0D, formId);
        setCancelAttackSwing(entity, true);
        entity.setNoGravity(true);
        playEntityAnimationOnLayer(entity, "iai1", 70, 1.0F, 4000);

        AbilityScheduler.scheduleRepeating(entity, () -> {
            int tick = elapsed[0]++;
            if (gravityEnabled && forcedGravity != null) {
                com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity
                    .setBaseGravityDirection(entity, forcedGravity);
            }

            if (tick <= 70) {
                freezeZenitsuEntity(entity);
                if (gravitySelection != null) {
                    moveSeventhFormToSurface(entity, gravitySelection);
                }
                if (tick == 0) {
                    for (int i = 0; i < 6; i++) {
                        chargeSparks.add(newThunderSpark(lightningSecondary(lightningVariant), 0.15F, 100, i < 2));
                    }
                }
                if (tick == 30) {
                    for (int i = 0; i < 6; i++) {
                        chargeSparks.add(newThunderSpark(lightningPrimary(lightningVariant), 0.075F, 100, false));
                    }
                }
                updateSeventhChargeSparks(serverLevel, entity, chargeSparks, tick, chargeBlocks);
                spawnSeventhChargeSpiral(serverLevel, entity, tick);
                if (tick == 40) {
                    createSeventhTravelingSparks(serverLevel, entity, lightningVariant, travelingSparks);
                }
                if (tick >= 40) {
                    updateSeventhTravelingSparks(serverLevel, entity, travelingSparks, travelingTargets, tick);
                }
                if (tick == 70) {
                    target[0] = findSeventhFormTarget(serverLevel, entity);
                    launchDirection[0] = target[0].position().subtract(startPosition).normalize();
                    if (launchDirection[0].lengthSqr() < 1.0E-8D) {
                        launchDirection[0] = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                            .world(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity))
                            .normalize();
                    }
                }
                return;
            }

            if (tick >= 71 && tick <= 76) {
                if (tick == 71) {
                    Vec3 behind = startPosition.subtract(launchDirection[0].scale(10.0D));
                    playThunderSound(serverLevel, entity, ModSounds.THUNDER_GOD_ULTIMATE_SLASH.get(), 50.0D);
                    syncLightningCustom(serverLevel, entity, true, true,
                        new Vec3[] {behind, entity.position()},
                        lightningSecondary(lightningVariant), lightningPrimary(lightningVariant),
                        lightningVariant == LightningVariant.NORMAL, 80);
                    for (int i = 0; i < 5; i++) {
                        ThunderSparkState spark = newThunderSpark(
                            i < 3 ? lightningPrimary(lightningVariant) : lightningSecondary(lightningVariant),
                            0.35F, 80, false);
                        spark.start = behind;
                        spark.end = entity.position();
                        spark.arc = randomVector(serverLevel, 2.5D);
                        launchSparks.add(spark);
                        syncSpark(serverLevel, spark, true);
                    }
                }

                if (tick <= 74) {
                    double progress = (tick - 70) / 4.0D * 0.6D;
                    Vec3 currentLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                        .local(entity.position());
                    Vec3 desiredLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                        .local(startPosition).lerp(
                            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(target[0].position()),
                            progress);
                    if (tick == 74) {
                        Vec3 desiredWorld = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame
                            .world(desiredLocal);
                        entity.teleportTo(desiredWorld.x, desiredWorld.y, desiredWorld.z);
                        MovementHelper.setVelocity(entity, Vec3.ZERO);
                    } else {
                        MovementHelper.setVelocity(entity, desiredLocal.subtract(currentLocal));
                    }
                } else {
                    MovementHelper.setVelocity(entity, Vec3.ZERO);
                }

                if (dragon[0] != null && dragon[0].isAlive()) {
                    dragon[0].teleportTo(entity.getX(), entity.getY(), entity.getZ());
                    dragon[0].setYRot(entity.getYRot());
                    dragon[0].setXRot(entity.getXRot());
                }
                if (tick >= 74) {
                    spawnZenitsuImpact(serverLevel, entity.position());
                }
                updateSeventhLaunchSparks(serverLevel, entity, launchSparks, startPosition, launchDirection[0]);
                syncLightningCustom(serverLevel, entity, true, false,
                    new Vec3[] {startPosition.subtract(launchDirection[0].scale(10.0D)), entity.position()},
                    lightningSecondary(lightningVariant), lightningPrimary(lightningVariant),
                    lightningVariant == LightningVariant.NORMAL, 80);

                if (tick == 74) {
                    ThunderDragonEntity createdDragon = ThunderDragonEntity.create(
                        serverLevel, entity, entity.position(), 80);
                    if (serverLevel.addFreshEntity(createdDragon)) {
                        dragon[0] = createdDragon;
                    }
                }
                return;
            }

            if (tick == 77) {
                Vec3 targetPosition = target[0].position();
                entity.teleportTo(targetPosition.x, targetPosition.y, targetPosition.z);
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                playEntityAnimation(entity, "sword_to_right");
                GuardStateHelper.setAttackState(entity, 40.0D);
                for (LivingEntity victim : nearbyLivingEntities(serverLevel, entity, targetPosition, 8.0D)) {
                    if (Damager.hurt(entity, victim, 40.0F)) {
                        spawnZenitsuImpact(serverLevel,
                            victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D));
                        if (Damager.isDemon(entity)) {
                            victim.setSecondsOnFire(60);
                        }
                    }
                }
                GuardStateHelper.clearAttackFlag(entity);
                if (dragon[0] != null && dragon[0].isAlive()) {
                    dragon[0].teleportTo(targetPosition.x, targetPosition.y, targetPosition.z);
                }
                return;
            }

            if (tick >= 78 && tick < 89) {
                if (tick == 78) {
                    MovementHelper.setVelocity(entity,
                        com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(launchDirection[0])
                            .scale(1.8D));
                }
                if (dragon[0] != null && dragon[0].isAlive()) {
                    Vec3 dragonPosition = dragon[0].position().add(launchDirection[0].scale(0.8D));
                    dragon[0].teleportTo(dragonPosition.x, dragonPosition.y, dragonPosition.z);
                }
                spawnSeventhFlames(serverLevel, entity);
                return;
            }

            if (tick == 88) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 9, false, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 9, false, false, false));
                syncLightningCustom(serverLevel, entity, false, false,
                    new Vec3[] {startPosition, entity.position()},
                    lightningSecondary(lightningVariant), lightningPrimary(lightningVariant),
                    lightningVariant == LightningVariant.NORMAL, 1);
                clearThunderSparks(serverLevel, chargeSparks);
                clearThunderSparks(serverLevel, travelingSparks);
                clearThunderSparks(serverLevel, launchSparks);
            }

            if (tick >= 89 && tick < 150) {
                freezeZenitsuEntity(entity);
                repeatIai1DuringCharge(entity, tick - 89, 61);
                spawnSeventhSmoke(serverLevel, entity);
            }

            if (tick == 120 && dragon[0] != null && dragon[0].isAlive()) {
                spawnSeventhSmokeBurst(serverLevel, dragon[0].position());
                dragon[0].discard();
            }
        }, 1, 151);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (dragon[0] != null && dragon[0].isAlive()) {
                spawnSeventhSmokeBurst(serverLevel, dragon[0].position());
                dragon[0].discard();
            }
            clearThunderSparks(serverLevel, chargeSparks);
            clearThunderSparks(serverLevel, travelingSparks);
            clearThunderSparks(serverLevel, launchSparks);
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            if (gravityEnabled) {
                com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.KNYGravity
                    .setBaseGravityDirection(entity, originalGravity);
            }
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "iai1");
            stopEntityAnimation(entity, "sword_to_right");
        }, 150);
    }

    public static void executeGodspeed(LivingEntity entity, net.minecraft.world.level.Level level, int ignoredFormId) {
        executeGodspeed(entity, level);
    }

    public static void executeGodspeed(LivingEntity entity, net.minecraft.world.level.Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final Vec3 startWorld = firstFormStartPoint(entity);
        final boolean originalNoGravity = entity.isNoGravity();
        final int[] currentTick = {0};
        final List<Vec3> path = new ArrayList<>();
        final List<UUID> launchSparks = new ArrayList<>();
        final Vec3[] launchStart = {null};
        final Vec3[] launchTarget = {null};
        final Vec3[] launchVelocity = {Vec3.ZERO};

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
                if (tick == 0) {
                    spawnLightningSparks(serverLevel, entity, 5, NORMAL_PRIMARY,
                        0.0875F, 50, false, GODSPEED_SPARK_BLOCK_RANGE);
                }
                freezeZenitsuEntity(entity);
                return;
            }

            if (tick < 80) {
                int launchTick = tick - 50;
                int phase = launchTick % 3;
                if (phase == 0) {
                    launchStart[0] = entity.position();
                    launchTarget[0] = raycastTarget(serverLevel, entity, 10.0D);
                    Vec3 startLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
                    Vec3 targetLocal = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(launchTarget[0]);
                    launchVelocity[0] = targetLocal.subtract(startLocal).scale(1.0D / 3.0D);
                    playEntityAnimation(entity, "speed_attack_sword");
                    if (launchTick == 0) {
                        path.add(startWorld);
                        path.add(entity.position());
                        playThunderSound(serverLevel, entity, ModSounds.THUNDER_GODSPEED.get(), 100.0D);
                        syncLightning(serverLevel, entity, true, true, path.toArray(new Vec3[0]),
                            LightningVariant.NORMAL, 60);
                    }
                    UUID sparkId = UUID.randomUUID();
                    launchSparks.add(sparkId);
                    syncLightningSpark(serverLevel, sparkId, true, true, launchStart[0], entity.position(),
                        NORMAL_PRIMARY, 0.5F, 6, false, Vec3.ZERO);
                }

                MovementHelper.setVelocity(entity, launchVelocity[0]);
                if (!path.isEmpty()) {
                    path.set(path.size() - 1, entity.position());
                    syncLightning(serverLevel, entity, true, false, path.toArray(new Vec3[0]),
                        LightningVariant.NORMAL, 60);
                }
                if (!launchSparks.isEmpty()) {
                    UUID sparkId = launchSparks.get(launchSparks.size() - 1);
                    syncLightningSpark(serverLevel, sparkId, true, false, launchStart[0], entity.position(),
                        NORMAL_PRIMARY, 0.5F, 6, false, Vec3.ZERO);
                }

                if (phase == 2) {
                    MovementHelper.setVelocity(entity, Vec3.ZERO);
                    entity.teleportTo(launchTarget[0].x, launchTarget[0].y, launchTarget[0].z);
                    GuardStateHelper.setAttackState(entity, THUNDER_FORM_DAMAGE);
                    damageTrajectory(serverLevel, entity, launchStart[0], launchTarget[0], 6.0F);
                    GuardStateHelper.clearAttackFlag(entity);
                    if (!path.isEmpty()) {
                        path.set(path.size() - 1, launchTarget[0]);
                        path.add(entity.position());
                        syncLightning(serverLevel, entity, true, false, path.toArray(new Vec3[0]),
                            LightningVariant.NORMAL, 60);
                    }
                }
                return;
            }

            if (tick == 80) {
                MovementHelper.setVelocity(entity, Vec3.ZERO);
                entity.setSprinting(false);
                Vec3 finalTarget = entity.position();
                spawnZenitsuImpact(serverLevel, finalTarget);
                for (LivingEntity victim : nearbyLivingEntities(serverLevel, entity, finalTarget, 12.0D)) {
                    GuardStateHelper.setAttackState(entity, THUNDER_FORM_DAMAGE);
                    Damager.hurt(entity, victim, THUNDER_FORM_DAMAGE);
                    GuardStateHelper.clearAttackFlag(entity);
                    spawnZenitsuImpact(serverLevel, victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D));
                    Vec3 victimCenter = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                        com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(victim)
                            .lerp(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(victim), 0.5D));
                    Vec3 targetAbove = finalTarget.add(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                        new Vec3(0.0D, 1.0D, 0.0D)));
                    spawnLightningSpark(serverLevel, victimCenter, targetAbove,
                        NORMAL_PRIMARY, 0.0875F, 8, false, Vec3.ZERO);
                }
                entity.hurt(entity.damageSources().generic(), 6.0F);
                Vec3 targetAbove = finalTarget.add(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                    new Vec3(0.0D, 1.0D, 0.0D)));
                for (int i = 0; i < 5; i++) {
                    spawnLightningSpark(serverLevel, targetAbove,
                        randomSparkEndpoint(targetAbove, 12.0D), NORMAL_PRIMARY,
                        0.0875F, 8, false, Vec3.ZERO);
                }
                playEntityAnimationOnLayer(entity, "iai1", 25, 1.0F, 4000);
                return;
            }

            if (tick > 80 && tick <= 105) {
                freezeZenitsuEntity(entity);
            }
        }, 1, 107);

        AbilityScheduler.scheduleOnce(entity, () -> {
            if (!path.isEmpty()) {
                syncLightning(serverLevel, entity, false, false, path.toArray(new Vec3[0]), LightningVariant.NORMAL, 60);
            }
            for (UUID sparkId : launchSparks) {
                syncLightningSpark(serverLevel, sparkId, false, false, Vec3.ZERO, Vec3.ZERO,
                    NORMAL_PRIMARY, 0.5F, 1, false, Vec3.ZERO);
            }
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            entity.setNoGravity(originalNoGravity);
            GuardStateHelper.clearGuardState(entity);
            setCancelAttackSwing(entity, false);
            stopEntityAnimation(entity, "iai1");
            stopEntityAnimation(entity, "speed_attack_sword");
        }, 106);
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

    private static SeventhGravitySelection findSeventhGravitySelection(ServerLevel level,
                                                                       LivingEntity entity,
                                                                       boolean gravityEnabled) {
        Vec3 origin = entity.position();
        List<Vec3> directions = new ArrayList<>();
        Vec3 down = gravityEnabled
            ? com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.GravityDirectionHelper.localToWorld(
                entity, new Vec3(0.0D, -1.0D, 0.0D)).normalize()
            : new Vec3(0.0D, -1.0D, 0.0D);
        directions.add(down);
        if (gravityEnabled) {
            directions.add(new Vec3(1.0D, 0.0D, 0.0D));
            directions.add(new Vec3(-1.0D, 0.0D, 0.0D));
            directions.add(new Vec3(0.0D, 0.0D, 1.0D));
            directions.add(new Vec3(0.0D, 0.0D, -1.0D));
            directions.add(new Vec3(0.0D, 1.0D, 0.0D));
        }

        for (Vec3 direction : directions) {
            Vec3 end = origin.add(direction.scale(5.0D));
            BlockHitResult hit = level.clip(new ClipContext(
                origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                && level.getBlockState(hit.getBlockPos()).isSolidRender(level, hit.getBlockPos())) {
                Direction gravityDirection = Direction.getNearest(direction.x, direction.y, direction.z);
                return new SeventhGravitySelection(gravityDirection, hit.getLocation(), direction.normalize());
            }
        }
        return null;
    }

    private static void moveSeventhFormToSurface(LivingEntity entity, SeventhGravitySelection selection) {
        Vec3 desired = selection.hitLocation().subtract(selection.worldDown().scale(0.05D));
        Vec3 delta = desired.subtract(entity.position());
        if (delta.lengthSqr() < 0.01D) {
            MovementHelper.setVelocity(entity, Vec3.ZERO);
            return;
        }
        double speed = Math.min(0.35D, delta.length());
        MovementHelper.setVelocity(entity,
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(delta.normalize())
                .scale(speed));
    }

    private static void updateSeventhChargeSparks(ServerLevel level, LivingEntity entity,
                                                  List<ThunderSparkState> sparks, int tick,
                                                  Set<BlockPos> usedBlocks) {
        Vec3 localFeet = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity);
        Vec3 localHead = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity);
        for (ThunderSparkState spark : sparks) {
            boolean spawn = spark.start == null;
            spark.start = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                localFeet.lerp(localHead, level.random.nextDouble()));
            boolean halfRange = spark.baseWidth < 0.08F;
            double range = (1.0D + Math.min(tick, 60) * 5.0D / 60.0D) * (halfRange ? 0.5D : 1.0D);
            spark.width = (float) (range * 0.05D);
            if (spark.end == null || (tick % 2 == 0 && level.random.nextFloat() < 0.60F)) {
                if (spark.endpointBlock != null) {
                    usedBlocks.remove(spark.endpointBlock);
                }
                SeventhSparkEndpoint endpoint = findSeventhChargeEndpoint(level, spark.start, range, usedBlocks);
                spark.end = endpoint.position();
                spark.endpointBlock = endpoint.block();
                if (endpoint.block() != null) {
                    usedBlocks.add(endpoint.block());
                    double arcMagnitude = range * (0.2D + level.random.nextDouble() * 0.3D);
                    spark.arc = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                        new Vec3(0.0D, arcMagnitude, 0.0D));
                } else {
                    spark.arc = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                        randomVector(level, range * 0.5D));
                }
            }
            syncSpark(level, spark, spawn);
        }
    }

    private static SeventhSparkEndpoint findSeventhChargeEndpoint(ServerLevel level, Vec3 start, double radius,
                                                                  Set<BlockPos> usedBlocks) {
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos origin = BlockPos.containing(start);
        int searchRadius = (int) Math.ceil(radius);
        double radiusSquared = radius * radius;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    if (usedBlocks.contains(candidate)
                        || Vec3.atCenterOf(candidate).distanceToSqr(start) > radiusSquared
                        || !level.getBlockState(candidate).isSolidRender(level, candidate)) {
                        continue;
                    }
                    candidates.add(candidate);
                }
            }
        }
        if (candidates.isEmpty()) {
            return new SeventhSparkEndpoint(start.add(
                com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
                    randomVector(level, radius))), null);
        }
        BlockPos block = candidates.get(level.random.nextInt(candidates.size()));
        Vec3 point = Vec3.atCenterOf(block).add(randomVector(level, 0.35D));
        return new SeventhSparkEndpoint(point, block);
    }

    private static void spawnSeventhChargeSpiral(ServerLevel level, LivingEntity entity, int tick) {
        ThunderFrame frame = thunderFrame(entity);
        Vec3 up = frameVector(frame.up).normalize();
        Vec3 axis = frame.forward.normalize();
        Vec3 side = up.cross(axis);
        if (side.lengthSqr() < 1.0E-8D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 otherSide = axis.cross(side).normalize();
        Vec3 center = framePoint(frame, 0.0D, 0.0D, 0.0D);
        for (int i = 0; i < 8; i++) {
            double progress = (tick * 0.18D + i / 8.0D) % 1.0D;
            double angle = progress * Math.PI * 2.0D;
            Vec3 point = center.add(side.scale(Math.cos(angle) * 2.0D))
                .add(otherSide.scale(Math.sin(angle) * 2.0D))
                .add(up.scale((progress - 0.5D) * 2.0D));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
                1, 0.08D, 0.08D, 0.08D, 0.03D);
        }
    }

    private static void createSeventhTravelingSparks(ServerLevel level, LivingEntity entity,
                                                     LightningVariant lightningVariant,
                                                     List<ThunderSparkState> sparks) {
        ThunderFrame frame = thunderFrame(entity);
        Vec3 up = frameVector(frame.up).normalize();
        Vec3 forward = frameVector(frame.forward).normalize();
        Vec3 side = up.cross(forward);
        if (side.lengthSqr() < 1.0E-8D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        Vec3 otherSide = forward.cross(side).normalize();
        Vec3 center = framePoint(frame, 0.0D, 0.0D, 0.0D);
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2.0D * i / 10.0D;
            Vec3 radial = side.scale(Math.cos(angle)).add(otherSide.scale(Math.sin(angle))).normalize();
            ThunderSparkState spark = newThunderSpark(lightningSecondary(lightningVariant), 0.1F, 50, false);
            spark.start = center.add(radial.scale(4.0D));
            spark.end = spark.start.add(radial.scale(3.0D));
            spark.arc = up;
            sparks.add(spark);
            syncSpark(level, spark, true);
        }
    }

    private static void updateSeventhTravelingSparks(ServerLevel level, LivingEntity entity,
                                                     List<ThunderSparkState> sparks,
                                                     Set<UUID> hitTargets, int tick) {
        if (tick <= 40 || (tick & 1) != 0) {
            return;
        }
        for (ThunderSparkState spark : sparks) {
            SeventhSparkTarget target = findSeventhTravelTarget(level, entity, spark, hitTargets);
            spark.start = spark.end;
            spark.end = target.position();
            spark.arc = target.arc();
            if (target.entity() != null) {
                hitTargets.add(target.entity().getUUID());
                target.entity().addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, false, false));
            }
            syncSpark(level, spark, false);
        }
    }

    private static SeventhSparkTarget findSeventhTravelTarget(ServerLevel level, LivingEntity owner,
                                                              ThunderSparkState spark,
                                                              Set<UUID> hitTargets) {
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(spark.end, spark.end).inflate(4.0D),
            other -> other != owner && other.isAlive() && !hitTargets.contains(other.getUUID())
                && !CrowEnhancementHandler.isKasugaiCrow(other))) {
            double distance = candidate.position().distanceToSqr(spark.end);
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        double magnitude = 0.8D + level.random.nextDouble() * 0.6D;
        if (closest != null) {
            Vec3 up = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.GravityDirectionHelper
                .localToWorld(closest, new Vec3(0.0D, 1.0D, 0.0D)).normalize();
            return new SeventhSparkTarget(closest.position().add(0.0D, closest.getBbHeight() * 0.5D, 0.0D),
                up.scale(magnitude), closest);
        }

        List<BlockPos> blocks = new ArrayList<>();
        BlockPos origin = BlockPos.containing(spark.end);
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    if (candidate.equals(BlockPos.containing(spark.start))
                        || candidate.equals(BlockPos.containing(spark.end))
                        || !level.getBlockState(candidate).isSolidRender(level, candidate)) {
                        continue;
                    }
                    blocks.add(candidate);
                }
            }
        }
        if (blocks.isEmpty()) {
            return new SeventhSparkTarget(spark.end.add(randomVector(level, 2.0D)),
                randomVector(level, magnitude), null);
        }
        BlockPos selected = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (BlockPos block : blocks) {
            double score = Vec3.atCenterOf(block).distanceToSqr(owner.position()) * level.random.nextDouble();
            if (score > bestScore) {
                bestScore = score;
                selected = block;
            }
        }
        Direction face = Direction.UP;
        for (Direction candidate : Direction.values()) {
            if (level.isEmptyBlock(selected.relative(candidate))) {
                face = candidate;
                break;
            }
        }
        return new SeventhSparkTarget(Vec3.atCenterOf(selected),
            Vec3.atLowerCornerOf(face.getNormal()).scale(magnitude), null);
    }

    private static SeventhFormTarget findSeventhFormTarget(ServerLevel level, LivingEntity entity) {
        Vec3 eye = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity));
        Vec3 look = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity)).normalize();
        Vec3 rayEnd = eye.add(look.scale(80.0D));
        BlockHitResult blockHit = level.clip(new ClipContext(
            eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        double blockDistance = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? 80.0D : eye.distanceTo(blockHit.getLocation());
        LivingEntity closest = null;
        double entityDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(eye, rayEnd).inflate(1.0D),
            other -> other != entity && other.isAlive() && !CrowEnhancementHandler.isKasugaiCrow(other))) {
            double distanceToRay = distanceToSegmentSquared(candidate.getBoundingBox().getCenter(), eye, rayEnd);
            double reach = 0.8D + candidate.getBbWidth() * 0.5D;
            double alongRay = candidate.position().subtract(eye).dot(look);
            if (distanceToRay <= reach * reach && alongRay >= 0.0D && alongRay < entityDistance) {
                closest = candidate;
                entityDistance = alongRay;
            }
        }
        if (closest != null && entityDistance <= blockDistance) {
            return new SeventhFormTarget(closest.position(), closest);
        }
        Vec3 blockTarget = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? rayEnd : findNearestSafePosition(level, entity, blockHit.getLocation());
        return new SeventhFormTarget(blockTarget, null);
    }

    private static void updateSeventhLaunchSparks(ServerLevel level, LivingEntity entity,
                                                  List<ThunderSparkState> sparks, Vec3 start,
                                                  Vec3 direction) {
        for (ThunderSparkState spark : sparks) {
            spark.start = start;
            spark.end = entity.position().add(randomVector(level, 2.0D));
            spark.arc = randomVector(level, 2.5D);
            syncSpark(level, spark, false);
        }
    }

    private static void spawnSeventhFlames(ServerLevel level, LivingEntity entity) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
            12, 1.0D, 1.0D, 1.0D, 0.04D);
    }

    private static void spawnSeventhSmoke(ServerLevel level, LivingEntity entity) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < 6; i++) {
            double x = (level.random.nextDouble() * 2.0D - 1.0D) * 1.5D;
            double y = (level.random.nextDouble() * 2.0D - 1.0D) * 1.5D;
            double z = (level.random.nextDouble() * 2.0D - 1.0D) * 1.5D;
            level.sendParticles(ParticleTypes.SMOKE, center.x + x, center.y + y, center.z + z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spawnSeventhSmokeBurst(ServerLevel level, Vec3 center) {
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z,
            35, 1.5D, 1.5D, 1.5D, 0.08D);
    }

    private static LivingEntity findCrosshairTarget(ServerLevel level, LivingEntity attacker, double distance) {
        Vec3 eye = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(attacker));
        Vec3 look = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(attacker)).normalize();
        Vec3 rayEnd = eye.add(look.scale(distance));
        BlockHitResult blockHit = level.clip(new ClipContext(
            eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker));
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            rayEnd = blockHit.getLocation();
        }

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(eye, rayEnd).inflate(1.0D),
            other -> other != attacker && other.isAlive()
                && !CrowEnhancementHandler.isKasugaiCrow(other))) {
            double reach = 0.75D + candidate.getBbWidth() * 0.5D;
            double distanceToRay = distanceToSegmentSquared(candidate.getBoundingBox().getCenter(), eye, rayEnd);
            if (distanceToRay <= reach * reach) {
                double alongRay = candidate.position().subtract(eye).dot(look);
                if (alongRay >= 0.0D && alongRay < closestDistance) {
                    closest = candidate;
                    closestDistance = alongRay;
                }
            }
        }
        return closest;
    }

    private static Vec3 thunderSwarmBehind(LivingEntity target) {
        Vec3 forward = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(target)).normalize();
        return target.position().subtract(forward.scale(2.0D));
    }

    private static Vec3 thunderSwarmForward(LivingEntity target) {
        Vec3 forward = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(target)).normalize();
        return forward.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : forward;
    }

    private static void spawnThunderSwarmAfterImages(ServerLevel level, LivingEntity user,
                                                     LivingEntity target, boolean kaigaku,
                                                     List<AfterImageEntity> afterImages) {
        Vec3 forward = thunderSwarmForward(target);
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).normalize();
        List<Vec3> positions = new ArrayList<>();
        positions.add(target.position().add(forward.scale(2.0D)));
        if (kaigaku) {
            positions.add(target.position().add(side.scale(2.0D)));
            positions.add(target.position().subtract(side.scale(2.0D)));
        }

        for (Vec3 worldPosition : positions) {
            AfterImageEntity afterImage = new AfterImageEntity(level, user, 90,
                com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.local(worldPosition),
                target.getYRot());
            afterImage.setOwnerUUID(user.getUUID());
            afterImage.startVisibleFor(65);
            level.addFreshEntity(afterImage);
            afterImages.add(afterImage);
        }
    }

    private static void updateThunderSwarmAfterImages(List<AfterImageEntity> afterImages, LivingEntity target) {
        if (afterImages.isEmpty()) {
            return;
        }
        Vec3 forward = thunderSwarmForward(target);
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x).normalize();
        List<Vec3> positions = new ArrayList<>();
        positions.add(target.position().add(forward.scale(2.0D)));
        if (afterImages.size() > 1) {
            positions.add(target.position().add(side.scale(2.0D)));
            positions.add(target.position().subtract(side.scale(2.0D)));
        }
        for (int i = 0; i < afterImages.size(); i++) {
            AfterImageEntity afterImage = afterImages.get(i);
            if (afterImage.isAlive()) {
                Vec3 position = positions.get(i % positions.size());
                afterImage.teleportTo(position.x, position.y, position.z);
                afterImage.setFacingYaw(target.getYRot());
            }
        }
    }

    private static void updateThunderSwarmSparks(ServerLevel level, LivingEntity target,
                                                 List<UUID> sparkIds, LightningVariant variant,
                                                 boolean spawn) {
        if (sparkIds.isEmpty()) {
            return;
        }
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 outerColor = lightningPrimary(variant);
        for (int i = 0; i < sparkIds.size(); i++) {
            double startAngle = (level.getGameTime() * 0.12D) + (i * Math.PI * 0.5D);
            Vec3 start = center.add(Math.cos(startAngle) * 2.0D, 0.0D, Math.sin(startAngle) * 2.0D);
            Vec3 end = center.add(Math.cos(startAngle + Math.PI * 0.5D) * 2.0D,
                0.0D, Math.sin(startAngle + Math.PI * 0.5D) * 2.0D);
            syncLightningSpark(level, sparkIds.get(i), true, spawn, start, end,
                i % 2 == 0 ? outerColor : lightningSecondary(variant), 0.07F, 60, false, Vec3.ZERO);
        }
    }

    private static void spawnThunderSwarmParticles(ServerLevel level, LivingEntity target, int tick) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        double angle = tick * 0.35D;
        Vec3 position = center.add(Math.cos(angle) * 1.4D, Math.sin(angle * 0.5D) * 0.8D,
            Math.sin(angle) * 1.4D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z,
            4, 0.15D, 0.15D, 0.15D, 0.04D);
    }

    private static void spawnThunderParticles(ServerLevel level, LivingEntity target) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        net.minecraft.core.particles.ParticleOptions particle = ParticleTypes.ELECTRIC_SPARK;
        net.minecraft.core.particles.ParticleType<?> registered = ForgeRegistries.PARTICLE_TYPES.getValue(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "particle_thunder"));
        if (registered instanceof net.minecraft.core.particles.SimpleParticleType simpleParticle) {
            particle = simpleParticle;
        }
        level.sendParticles(particle, center.x, center.y, center.z,
            8, 0.8D, 0.8D, 0.8D, 0.06D);
    }

    private static void finishThirdForm(LivingEntity entity, boolean originalNoGravity, float originalStepHeight,
                                        List<AfterImageEntity> afterImages, List<UUID> circleSparks,
                                        ServerLevel level) {
        for (AfterImageEntity afterImage : afterImages) {
            if (afterImage != null && afterImage.isAlive()) {
                afterImage.discard();
            }
        }
        for (UUID sparkId : circleSparks) {
            syncLightningSpark(level, sparkId, false, false, Vec3.ZERO, Vec3.ZERO,
                NORMAL_PRIMARY, 0.07F, 1, false, Vec3.ZERO);
        }
        MovementHelper.setVelocity(entity, Vec3.ZERO);
        entity.setNoGravity(originalNoGravity);
        MovementHelper.setStepHeight(entity, originalStepHeight);
        GuardStateHelper.clearGuardState(entity);
        setCancelAttackSwing(entity, false);
        stopEntityAnimation(entity, "iai1");
        stopEntityAnimation(entity, "speed_attack_sword");
    }

    private static void addImmovable(LivingEntity entity, int duration) {
        net.minecraft.world.effect.MobEffect immovable = ForgeRegistries.MOB_EFFECTS.getValue(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "immovable"));
        if (immovable != null) {
            entity.addEffect(new MobEffectInstance(immovable, duration, 0, false, false, false));
        }
    }

    private static void removeImmovable(LivingEntity entity) {
        net.minecraft.world.effect.MobEffect immovable = ForgeRegistries.MOB_EFFECTS.getValue(
            ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "immovable"));
        if (immovable != null) {
            entity.removeEffect(immovable);
        }
    }

    private static Vec3 lightningPrimary(LightningVariant variant) {
        return switch (variant) {
            case KAIGAKU, BLUE -> BLUE_PRIMARY;
            case NORMAL -> NORMAL_PRIMARY;
        };
    }

    private static Vec3 lightningSecondary(LightningVariant variant) {
        return switch (variant) {
            case KAIGAKU -> KAIGAKU_SECONDARY;
            case BLUE -> BLUE_SECONDARY;
            case NORMAL -> NORMAL_SECONDARY;
        };
    }

    private static ThunderSparkState createDistantThunderSpark(LivingEntity entity, int index,
                                                               LightningVariant lightningVariant) {
        ThunderFrame frame = thunderFrame(entity);
        Vec3 start;
        Vec3 end;
        switch (index) {
            case 0 -> {
                start = framePoint(frame, 2.0D, 1.0D, 0.4D);
                end = framePoint(frame, 2.0D, -1.0D, 0.4D);
            }
            case 1 -> {
                start = framePoint(frame, 2.0D, -1.0D, -0.4D);
                end = framePoint(frame, 2.0D, 1.0D, -0.4D);
            }
            case 2 -> {
                start = framePoint(frame, 2.0D, -0.4D, 1.0D);
                end = framePoint(frame, 2.0D, -0.4D, -1.0D);
            }
            default -> {
                start = framePoint(frame, 2.0D, 0.4D, -1.0D);
                end = framePoint(frame, 2.0D, 0.4D, 1.0D);
            }
        }
            ThunderSparkState spark = newThunderSpark(lightningPrimary(lightningVariant), 0.08F, 20, false);
        spark.start = start;
        spark.end = end;
        spark.arc = frameVector(frame.forward.scale(0.25D));
        return spark;
    }

    private static void updateDistantThunderAttachedSparks(ServerLevel level, LivingEntity entity,
                                                           List<ThunderSparkState> sparks,
                                                           LightningVariant lightningVariant) {
        for (int i = 0; i < sparks.size(); i++) {
            ThunderSparkState spark = sparks.get(i);
            ThunderSparkState current = createDistantThunderSpark(entity, i, lightningVariant);
            spark.start = current.start;
            spark.end = current.end;
            spark.arc = current.arc;
            syncSpark(level, spark, false);
        }
    }

    private static void updateDistantThunderProjectileSparks(ServerLevel level,
                                                             List<ThunderSparkState> sparks, int tick) {
        double scale = 1.0D + tick * 0.08D;
        for (ThunderSparkState spark : sparks) {
            Vec3 center = spark.projectileOrigin.add(spark.projectileDirection.scale(tick * 3.0D));
            spark.start = center.add(spark.initialStart.subtract(spark.projectileOrigin).scale(scale));
            spark.end = center.add(spark.initialEnd.subtract(spark.projectileOrigin).scale(scale));
            spark.width = (float) (spark.baseWidth + 0.32D * tick / 10.0D);
            spark.arc = spark.projectileDirection.scale(0.25D);
            syncSpark(level, spark, false);
        }
    }

    private static void damageDistantThunderDebuffs(ServerLevel level, LivingEntity owner,
                                                    List<ThunderSparkState> sparks) {
        for (ThunderSparkState spark : sparks) {
            if (spark.start == null || spark.end == null) {
                continue;
            }
            Vec3 flashPosition = spark.start.lerp(spark.end, level.random.nextDouble());
            level.sendParticles(ParticleTypes.FLASH, flashPosition.x, flashPosition.y, flashPosition.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(spark.start, spark.end).inflate(1.5D),
                candidate -> candidate != owner && candidate.isAlive()
                    && !CrowEnhancementHandler.isKasugaiCrow(candidate))) {
                if (distanceToSegmentSquared(target.getBoundingBox().getCenter(), spark.start, spark.end)
                    <= 2.25D) {
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0, false, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2, false, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, false, false));
                    level.sendParticles(ParticleTypes.FLASH, target.getX(),
                        target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 1,
                        0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private static void updateHeatLightningChargeSparks(ServerLevel level, LivingEntity entity,
                                                        List<ThunderSparkState> sparks, int tick) {
        ThunderFrame frame = thunderFrame(entity);
        while (sparks.size() < 4) {
            sparks.add(newThunderSpark(HEAT_LIGHTNING_RED, 0.09F, 8, false));
        }
        if (tick % 3 == 0) {
            Vec3 start = framePoint(frame, 0.0D, 0.0D, 0.0D);
            for (ThunderSparkState spark : sparks) {
                boolean resetLifetime = spark.start == null || tick % 3 == 0;
                SparkEndpoint endpoint = findSparkEndpoint(level, start, 4.0D);
                spark.start = start;
                spark.end = endpoint.position();
                spark.arc = endpoint.solidBlock()
                    ? frameVector(new Vec3(0.0D, 0.7D, 0.0D))
                    : randomVector(level, 0.7D);
                syncSpark(level, spark, resetLifetime);
            }
        }
        for (ThunderSparkState spark : sparks) {
            if (spark.start != null) {
                syncSpark(level, spark, false);
            }
        }
    }

    private static void updateHeatLightningTargetSpark(ServerLevel level, LivingEntity target,
                                                       ThunderSparkState spark, boolean spawn) {
        Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 head = target.position().add(0.0D, target.getBbHeight(), 0.0D);
        spark.start = center.add(randomVector(level, 0.2D));
        spark.end = head.add(randomVector(level, 0.2D));
        spark.arc = randomVector(level, 0.6D);
        syncSpark(level, spark, spawn);
    }

    private static void igniteNearbyBlocks(ServerLevel level, LivingEntity target) {
        BlockPos origin = target.blockPosition();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos firePos = origin.offset(x, 0, z);
                if (!level.getBlockState(firePos).isAir()) {
                    firePos = firePos.above();
                }
                if (level.getBlockState(firePos).isAir()
                    && BaseFireBlock.canBePlacedAt(level, firePos, net.minecraft.core.Direction.UP)) {
                    level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
                }
            }
        }
    }

    private static void spawnRumbleChargeSmoke(ServerLevel level, LivingEntity entity) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < 8; i++) {
            double x = (level.random.nextDouble() * 2.0D - 1.0D) * 3.0D;
            double y = (level.random.nextDouble() * 2.0D - 1.0D) * 3.0D;
            double z = (level.random.nextDouble() * 2.0D - 1.0D) * 3.0D;
            level.sendParticles(ParticleTypes.SMOKE, center.x + x, center.y + y, center.z + z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(BLACK_DUST, center.x + x, center.y + y, center.z + z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spawnRumbleSmokeRing(ServerLevel level, LivingEntity entity, Vec3 localDirection) {
        Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 axis = frameVector(localDirection).normalize();
        Vec3 reference = Math.abs(axis.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side = axis.cross(reference).normalize();
        Vec3 otherSide = axis.cross(side).normalize();
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0D * i / 16.0D;
            Vec3 point = center.add(side.scale(Math.cos(angle) * 2.5D))
                .add(otherSide.scale(Math.sin(angle) * 2.5D));
            level.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z,
                1, 0.04D, 0.04D, 0.04D, 0.0D);
            level.sendParticles(BLACK_DUST, point.x, point.y, point.z,
                1, 0.04D, 0.04D, 0.04D, 0.0D);
        }
    }

    private static void updateRumbleSparks(ServerLevel level, LivingEntity entity,
                                           List<ThunderSparkState> sparks, Vec3 origin,
                                           Vec3 localDirection, int tick) {
        Vec3 direction = frameVector(localDirection).normalize();
        for (ThunderSparkState spark : sparks) {
            if (tick < 40) {
                spark.start = randomTubePoint(level, origin, direction, 15.0D, 2.5D);
                if (tick < 30) {
                    spark.end = randomTubePoint(level, origin, direction, 15.0D, 2.5D);
                    spark.arc = randomVector(level, 1.0D);
                } else {
                    Vec3 userPoint = entity.position().add(direction.scale(level.random.nextDouble() * 5.0D))
                        .add(randomVector(level, 5.0D));
                    spark.end = userPoint;
                    spark.arc = randomVector(level, 2.0D);
                }
            } else {
                spark.start = randomTubePoint(level, origin, direction, 15.0D, 2.5D);
                spark.end = randomTubePoint(level, origin, direction, 15.0D, 2.5D);
                spark.arc = randomVector(level, 1.0D);
            }
            syncSpark(level, spark, false);
        }
    }

    private static void damageRumblePath(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 end,
                                         Set<UUID> damaged) {
        for (LivingEntity target : damageTrajectoryTargets(level, owner, start, end, 3.0D)) {
            if (damaged.add(target.getUUID())) {
                GuardStateHelper.setAttackState(owner, THUNDER_FORM_DAMAGE);
                Damager.hurt(owner, target, THUNDER_FORM_DAMAGE * 1.5F);
                GuardStateHelper.clearAttackFlag(owner);
                MovementHelper.addVelocity(target,
                    target.position().subtract(owner.position()).normalize().scale(1.0D)
                        .add(0.0D, 0.45D, 0.0D));
            }
        }
    }

    private static Vec3 randomTubePoint(ServerLevel level, Vec3 origin, Vec3 direction,
                                        double length, double radius) {
        Vec3 axis = direction.normalize();
        Vec3 reference = Math.abs(axis.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side = axis.cross(reference).normalize();
        Vec3 otherSide = axis.cross(side).normalize();
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double distance = level.random.nextDouble() * length;
        double radial = level.random.nextDouble() * radius;
        return origin.add(axis.scale(distance))
            .add(side.scale(Math.cos(angle) * radial))
            .add(otherSide.scale(Math.sin(angle) * radial));
    }

    private static Vec3 randomVector(ServerLevel level, double magnitude) {
        double theta = level.random.nextDouble() * Math.PI * 2.0D;
        double vertical = level.random.nextDouble() * 2.0D - 1.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
        return new Vec3(Math.cos(theta) * horizontal, vertical, Math.sin(theta) * horizontal)
            .scale(magnitude * level.random.nextDouble());
    }

    private static ThunderSparkState newThunderSpark(Vec3 color, float width, int lifetime,
                                                     boolean flickering) {
        ThunderSparkState spark = new ThunderSparkState(UUID.randomUUID(), color, width, lifetime, flickering);
        spark.baseWidth = width;
        return spark;
    }

    private static void syncSpark(ServerLevel level, ThunderSparkState spark, boolean spawn) {
        if (spark.start == null || spark.end == null) {
            return;
        }
        syncLightningSpark(level, spark.id, true, spawn, spark.start, spark.end,
            spark.color, spark.width, spark.lifetime, spark.flickering,
            spark.arc == null ? Vec3.ZERO : spark.arc);
    }

    private static void clearThunderSparks(ServerLevel level, List<ThunderSparkState> sparks) {
        for (ThunderSparkState spark : sparks) {
            Vec3 endpoint = spark.end == null ? Vec3.ZERO : spark.end;
            syncLightningSpark(level, spark.id, false, false,
                endpoint, endpoint, spark.color, Math.max(0.01F, spark.width), 1,
                spark.flickering, Vec3.ZERO);
        }
    }

    private static ThunderFrame thunderFrame(LivingEntity entity) {
        Vec3 forward = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.look(entity).normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = up.cross(forward);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        up = forward.cross(right).normalize();
        Vec3 center = com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.position(entity)
            .lerp(com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.eye(entity), 0.5D);
        return new ThunderFrame(center, forward, right, up);
    }

    private static Vec3 framePoint(ThunderFrame frame, double forward, double right, double up) {
        return com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(
            frame.center.add(frame.forward.scale(forward))
                .add(frame.right.scale(right)).add(frame.up.scale(up)));
    }

    private static Vec3 frameVector(Vec3 localVector) {
        return com.lerdorf.kimetsunoyaibamultiplayer.gravity.api.CombatGravityFrame.world(localVector);
    }

    private static void syncLightningCustom(ServerLevel level, LivingEntity entity, boolean active, boolean spawn,
                                            Vec3[] positions, Vec3 primary, Vec3 secondary,
                                            boolean whiteMiddle, int lifetime) {
        syncLightningBolt(level, entity.getUUID(), active, spawn, positions, primary, secondary,
            whiteMiddle, lifetime);
    }

    private static void syncLightningBolt(ServerLevel level, UUID boltId, boolean active, boolean spawn,
                                          Vec3[] positions, Vec3 primary, Vec3 secondary,
                                          boolean whiteMiddle, int lifetime) {
        Vec3[] elevatedPositions = new Vec3[positions.length];
        for (int i = 0; i < positions.length; i++) {
            elevatedPositions[i] = positions[i].add(0.0D, 1.0D, 0.0D);
        }
        ThunderLightningSyncPacket packet = new ThunderLightningSyncPacket(
            boltId, active, spawn, elevatedPositions, primary, secondary, whiteMiddle, lifetime);
        Vec3 end = elevatedPositions.length == 0 ? Vec3.ZERO : elevatedPositions[elevatedPositions.length - 1];
        ModNetworking.sendToNearby(packet, level, end.x, end.y, end.z, 128.0D);
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
        syncLightningSpark(level, UUID.randomUUID(), true, true, start, end, color,
            width, lifetime, flickering, arcing);
    }

    private static void syncLightningSpark(ServerLevel level, UUID sparkId, boolean active, boolean spawn,
                                           Vec3 start, Vec3 end, Vec3 color, float width, int lifetime,
                                           boolean flickering, Vec3 arcing) {
        ThunderLightningSparkSyncPacket packet = new ThunderLightningSparkSyncPacket(
            sparkId, active, spawn, start, end, Vec3.ZERO, arcing,
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
        playThunderSoundAt(level, source.position(), sound, radius);
    }

    private static void playThunderSoundAt(ServerLevel level, Vec3 position, SoundEvent sound, double radius) {
        var holder = ForgeRegistries.SOUND_EVENTS.getHolder(sound).orElse(null);
        if (holder == null) {
            return;
        }
        double radiusSquared = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(position) <= radiusSquared) {
                player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS,
                    position.x, position.y, position.z, 1.0F, 1.0F, level.random.nextLong()));
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

    private static final class ThunderSparkState {
        private final UUID id;
        private final Vec3 color;
        private final int lifetime;
        private final boolean flickering;
        private float width;
        private float baseWidth;
        private Vec3 start;
        private Vec3 end;
        private Vec3 arc;
        private Vec3 projectileOrigin;
        private Vec3 projectileDirection;
        private Vec3 initialStart;
        private Vec3 initialEnd;
        private UUID targetId;
        private BlockPos endpointBlock;

        private ThunderSparkState(UUID id, Vec3 color, float width, int lifetime, boolean flickering) {
            this.id = id;
            this.color = color;
            this.width = width;
            this.lifetime = lifetime;
            this.flickering = flickering;
        }
    }

    private record ThunderFrame(Vec3 center, Vec3 forward, Vec3 right, Vec3 up) {
    }

    private enum LightningVariant {
        NORMAL,
        BLUE,
        KAIGAKU
    }

    private record SparkEndpoint(Vec3 position, boolean solidBlock) {
    }

    private record SeventhGravitySelection(Direction direction, Vec3 hitLocation, Vec3 worldDown) {
    }

    private record SeventhSparkEndpoint(Vec3 position, BlockPos block) {
    }

    private record SeventhSparkTarget(Vec3 position, Vec3 arc, LivingEntity entity) {
    }

    private record SeventhFormTarget(Vec3 position, LivingEntity entity) {
    }
}
