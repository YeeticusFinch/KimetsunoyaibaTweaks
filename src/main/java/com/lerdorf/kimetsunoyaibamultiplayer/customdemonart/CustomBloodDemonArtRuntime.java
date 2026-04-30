package com.lerdorf.kimetsunoyaibamultiplayer.customdemonart;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AnimationHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.MovementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.items.CustomDemonArtItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class CustomBloodDemonArtRuntime {
    private CustomBloodDemonArtRuntime() {
    }

    public static Component selectedFormName(ServerPlayer player, ItemStack stack) {
        CustomBloodDemonArtSavedData.PlayerArtData data = CustomBloodDemonArtSavedData.get((ServerLevel) player.level()).getOrCreate(player);
        int selectedSlot = CustomDemonArtItem.getSelectedSlot(stack, data.selectedSlot());
        if (selectedSlot < 0 || selectedSlot >= data.slots().size()) {
            return Component.literal("No Form").withStyle(ChatFormatting.GRAY);
        }
        CustomBloodDemonArtSavedData.CustomFormSlot slot = data.slots().get(selectedSlot);
        if (!slot.filled()) {
            return Component.literal("Empty Form").withStyle(ChatFormatting.GRAY);
        }
        return Component.literal(slot.name()).withStyle(ChatFormatting.DARK_GREEN);
    }

    public static boolean cycleForm(ServerPlayer player, ItemStack stack, int direction) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        CustomBloodDemonArtSavedData savedData = CustomBloodDemonArtSavedData.get(serverLevel);
        CustomBloodDemonArtSavedData.PlayerArtData data = savedData.getOrCreate(player);

        List<Integer> filledSlots = new ArrayList<>();
        int unlockedSlots = savedData.getUnlockedSlots(player);
        for (int i = 0; i < Math.min(unlockedSlots, data.slots().size()); i++) {
            if (data.slots().get(i).filled()) {
                filledSlots.add(i);
            }
        }
        if (filledSlots.isEmpty()) {
            player.displayClientMessage(Component.literal("No custom blood demon art forms are configured yet.")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        int current = CustomDemonArtItem.getSelectedSlot(stack, data.selectedSlot());
        int currentIndex = Math.max(0, filledSlots.indexOf(current));
        int nextIndex = Math.floorMod(currentIndex + (direction < 0 ? -1 : 1), filledSlots.size());
        int slotIndex = filledSlots.get(nextIndex);

        CustomDemonArtItem.setSelectedSlot(stack, slotIndex);
        savedData.setSelectedSlot(player, slotIndex);
        player.sendSystemMessage(Component.literal("Selected Form: " + data.slots().get(slotIndex).name())
            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        return true;
    }

    public static boolean use(ServerPlayer player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!Damager.isDemon(player)) {
            player.displayClientMessage(Component.literal("You must be a demon to use this ability")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        CustomBloodDemonArtSavedData.PlayerArtData data = CustomBloodDemonArtSavedData.get(serverLevel).getOrCreate(player);
        int slotIndex = CustomDemonArtItem.getSelectedSlot(stack, data.selectedSlot());
        if (slotIndex < 0 || slotIndex >= data.slots().size()) {
            return false;
        }
        CustomBloodDemonArtSavedData.CustomFormSlot slot = data.slots().get(slotIndex);
        if (!slot.filled() || slot.moves().isEmpty()) {
            player.displayClientMessage(Component.literal("That custom form has no moves configured yet.")
                .withStyle(ChatFormatting.RED), true);
            return false;
        }

        BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind = slot.dominantAmplifierKind();
        int tickOffset = 0;
        for (CustomBloodDemonArtSavedData.MoveType move : slot.moves()) {
            final int startAt = tickOffset;
            AbilityScheduler.scheduleOnce(player, () -> executeMove(player, data.coreSettings(), move, amplifierKind), startAt);
            tickOffset += move.durationTicks();
        }

        if (amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.DEFENSE) {
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, Math.max(60, tickOffset + 20), 0));
        } else if (amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED) {
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, Math.max(60, tickOffset + 20), 0));
        }

        int cooldownTicks = Math.max(20, slot.cooldownSeconds() * 20);
        player.getCooldowns().addCooldown(stack.getItem(), cooldownTicks);
        player.displayClientMessage(Component.literal(slot.name()).withStyle(ChatFormatting.DARK_GREEN), true);
        return true;
    }

    public static boolean grantItem(ServerPlayer player) {
        if (player.experienceLevel < 5) {
            return false;
        }

        ItemStack existingMatch = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ModItems.CUSTOM_DEMON_ART.get()) {
                existingMatch = stack;
                break;
            }
        }
        if (!existingMatch.isEmpty() || player.getOffhandItem().getItem() == ModItems.CUSTOM_DEMON_ART.get()) {
            return false;
        }

        ItemStack stack = new ItemStack(ModItems.CUSTOM_DEMON_ART.get());
        CustomDemonArtItem.setPlayerSkin(stack, player.getUUID(), player.getGameProfile().getName());
        player.giveExperienceLevels(-5);
        player.getInventory().add(stack);
        return true;
    }

    private static void executeMove(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                    CustomBloodDemonArtSavedData.MoveType move, BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind) {
        switch (move) {
            case PUNCH_RIGHT -> executePunch(player, core, "punch_right", amplifierKind);
            case PUNCH_LEFT -> executePunch(player, core, "punch_left", amplifierKind);
            case FRONT_FLIP -> executeFrontFlip(player, core, amplifierKind);
            case MELEE_COMBO -> executeMeleeCombo(player, core, amplifierKind);
        }
    }

    private static void executePunch(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core, String animationName,
                                     BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind) {
        double rangeScale = amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE ? 1.1D : 1.0D;
        float damageScale = amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE ? 1.1F : 1.0F;
        AnimationHelper.playAnimation(player, animationName, 10);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 center = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D).add(look.scale(1.0D * rangeScale));
        spawnRing(player.serverLevel(), core.primaryParticle(), center, 5.0D * rangeScale, 22);

        AABB hitBox = new AABB(center, center).inflate(5.0D * rangeScale, 1.75D, 5.0D * rangeScale);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, hitBox,
            entity -> entity != player && entity.isAlive())) {
            double forwardDot = target.position().subtract(player.position()).normalize().dot(look);
            if (forwardDot < 0.15D) {
                continue;
            }
            Damager.hurt(player, target, 5.0F * damageScale);
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(0.7D).add(0.0D, 0.2D, 0.0D);
            MovementHelper.addVelocity(target, knockback);
            applyTargetPotion(core.primaryPotion(), player, target, amplifierKind);
        }

        applySelfPotion(core.primaryPotion(), player, amplifierKind);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void executeFrontFlip(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                         BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind) {
        double rangeScale = amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE ? 1.1D : 1.0D;
        double speedScale = amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.SPEED ? 1.1D : 1.0D;
        AnimationHelper.playAnimation(player, "kimetsunoyaibamultiplayer:front_flip", 15);
        Vec3 look = player.getLookAngle().normalize();
        MovementHelper.setVelocity(player, look.x * 1.0D * speedScale * rangeScale, 0.65D * speedScale, look.z * 1.0D * speedScale * rangeScale);
        for (int i = 0; i < 5; i++) {
            final int offset = i * 3;
            AbilityScheduler.scheduleOnce(player, () -> {
                Vec3 pos = player.position().add(0.0D, 0.35D, 0.0D);
                spawnBurst(player.serverLevel(), core.secondaryParticle(), pos, 7);
            }, offset);
        }
        applySelfPotion(core.secondaryPotion(), player, amplifierKind);
    }

    private static void executeMeleeCombo(ServerPlayer player, CustomBloodDemonArtSavedData.CoreSettings core,
                                          BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind) {
        double rangeScale = amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.RANGE ? 1.1D : 1.0D;
        float damageScale = amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.DAMAGE ? 1.1F : 1.0F;
        String[] animations = {"punch_right", "punch_left", "kick_right", "kick_left"};
        for (int i = 0; i < animations.length; i++) {
            final String animation = animations[i];
            AbilityScheduler.scheduleOnce(player, () -> {
                AnimationHelper.playAnimation(player, animation, 10);
                Vec3 center = player.position().add(player.getLookAngle().normalize().scale(1.3D * rangeScale));
                spawnBurst(player.serverLevel(), core.primaryParticle(), center, 10);
                AABB hitBox = new AABB(center, center).inflate(2.25D * rangeScale, 1.35D, 2.25D * rangeScale);
                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                    entity -> entity != player && entity.isAlive())) {
                    Damager.hurt(player, target, 4.0F * damageScale, true);
                    applyTargetPotion(core.primaryPotion(), player, target, amplifierKind);
                }
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.75F);
            }, i * 3);
        }
        applySelfPotion(core.primaryPotion(), player, amplifierKind);
    }

    private static void applySelfPotion(CustomBloodDemonArtSavedData.PotionSetting potionSetting, ServerPlayer player,
                                        BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind) {
        if (!potionSetting.selfEffect()) {
            return;
        }
        MobEffect effect = potionSetting.resolveEffect();
        if (effect != null) {
            player.addEffect(new MobEffectInstance(effect,
                amplifiedDurationTicks(potionSetting, amplifierKind),
                amplifiedEffectAmplifier(effect, potionSetting, amplifierKind)));
        }
    }

    private static void applyTargetPotion(CustomBloodDemonArtSavedData.PotionSetting potionSetting, ServerPlayer player, LivingEntity target,
                                          BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind) {
        if (potionSetting.selfEffect()) {
            return;
        }
        MobEffect effect = potionSetting.resolveEffect();
        if (effect != null) {
            target.addEffect(new MobEffectInstance(effect,
                amplifiedDurationTicks(potionSetting, amplifierKind),
                amplifiedEffectAmplifier(effect, potionSetting, amplifierKind)));
        }
    }

    private static int amplifiedDurationTicks(CustomBloodDemonArtSavedData.PotionSetting potionSetting,
                                              BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind) {
        double scale = amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.DURATION ? 1.1D : 1.0D;
        return Math.max(40, (int) Math.round(potionSetting.durationSeconds() * 20 * scale));
    }

    private static int amplifiedEffectAmplifier(MobEffect effect, CustomBloodDemonArtSavedData.PotionSetting potionSetting,
                                                BloodDemonArtAlchemyCatalog.AmplifierKind amplifierKind) {
        int amplifier = Math.max(0, potionSetting.amplifier() - 1);
        boolean harmful = BloodDemonArtAlchemyCatalog.isHarmfulEffect(effect);
        if (harmful && amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.HARMFUL_EFFECT) {
            return amplifier + 1;
        }
        if (!harmful && amplifierKind == BloodDemonArtAlchemyCatalog.AmplifierKind.BENEFICIAL_EFFECT) {
            return amplifier + 1;
        }
        return amplifier;
    }

    private static void spawnRing(ServerLevel level, CustomBloodDemonArtSavedData.ParticleStyle particleStyle, Vec3 center, double radius, int steps) {
        ParticleOptions particle = resolveParticle(particleStyle);
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2.0D * i) / steps;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 3, 0.05D, 0.05D, 0.05D, 0.01D);
        }
    }

    private static void spawnBurst(ServerLevel level, CustomBloodDemonArtSavedData.ParticleStyle particleStyle, Vec3 center, int count) {
        ParticleOptions particle = resolveParticle(particleStyle);
        level.sendParticles(particle, center.x, center.y, center.z, count, 0.3D, 0.25D, 0.3D, 0.01D);
    }

    private static ParticleOptions resolveParticle(CustomBloodDemonArtSavedData.ParticleStyle style) {
        ResourceLocation id = ResourceLocation.tryParse(style.particleId());
        ParticleType<?> particleType = id == null ? null : ForgeRegistries.PARTICLE_TYPES.getValue(id);
        if (particleType == null) {
            return net.minecraft.core.particles.ParticleTypes.SMOKE;
        }
        if (particleType == net.minecraft.core.particles.ParticleTypes.DUST || "minecraft:dust".equals(style.particleId())) {
            Vector3f color = new Vector3f(
                ((style.color() >> 16) & 0xFF) / 255.0F,
                ((style.color() >> 8) & 0xFF) / 255.0F,
                (style.color() & 0xFF) / 255.0F
            );
            return new DustParticleOptions(color, Mth.clamp(style.size(), 0.2F, 4.0F));
        }
        if (particleType == net.minecraft.core.particles.ParticleTypes.SMOKE) {
            return net.minecraft.core.particles.ParticleTypes.SMOKE;
        }
        return (ParticleOptions) particleType;
    }
}
