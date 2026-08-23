package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonEyesSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SunlightImmunityHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AlchemyMedicineHandler {
    public static final String SUNLIGHT_IMMUNITY_KEY = "KnYMpSolarAscension";
    public static final String DEMONIC_SATURATION_KEY = "KnYMpDemonicSaturation";
    private static final int LONG_EFFECT_TICKS = 20 * 60 * 60 * 24;
    private static final int DEMON_RESTORATION_TICKS = 20 * 60;
    private static final TagKey<Item> HUMAN_FLESH_SOURCES = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "human_flesh_sources")
    );

    private AlchemyMedicineHandler() {
    }

    public static boolean isLegendaryMedicine(ItemStack stack) {
        String id = BloodDemonArtAlchemyCatalog.id(stack);
        return "kimetsunoyaibamultiplayer:demon_genesis_medicine".equals(id)
            || "kimetsunoyaibamultiplayer:solar_ascension_cure".equals(id)
            || "kimetsunoyaibamultiplayer:demonic_hunger_cure".equals(id)
            || "kimetsunoyaibamultiplayer:demonic_restoration_cure".equals(id)
            || "kimetsunoyaibamultiplayer:solar_smite_serum".equals(id);
    }

    public static boolean applyDrunkMedicine(ItemStack stack, LivingEntity entity) {
        return applyMedicine(stack, entity);
    }

    public static InteractionResult applyTargetedMedicine(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!isLegendaryMedicine(stack)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!applyMedicine(stack, target)) {
            return InteractionResult.PASS;
        }
        consumeMedicineStack(stack, player);
        return InteractionResult.CONSUME;
    }

    private static boolean applyMedicine(ItemStack stack, LivingEntity entity) {
        String id = BloodDemonArtAlchemyCatalog.id(stack);
        if ("kimetsunoyaibamultiplayer:demon_genesis_medicine".equals(id)) {
            return applyDemonGenesis(entity);
        }
        if ("kimetsunoyaibamultiplayer:solar_ascension_cure".equals(id)) {
            return grantSunlightImmunity(entity);
        }
        if ("kimetsunoyaibamultiplayer:demonic_hunger_cure".equals(id)) {
            return grantDemonicSaturation(entity);
        }
        if ("kimetsunoyaibamultiplayer:demonic_restoration_cure".equals(id)) {
            return startHumanRestoration(entity);
        }
        if ("kimetsunoyaibamultiplayer:solar_smite_serum".equals(id)) {
            return revokeSunlightImmunity(entity);
        }
        return false;
    }

    private static boolean applyDemonGenesis(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        int count = 5 + player.getRandom().nextInt(6);
        DemonTransformationHandler.clearHumanRestorationMarker(player);
        DemonTransformationHandler.applyMuzanBloodEquivalent(player, count);
        return true;
    }

    public static boolean hasSunlightImmunity(LivingEntity entity) {
        return SunlightImmunityHelper.hasSunlightImmunity(entity);
    }

    public static boolean grantSunlightImmunity(LivingEntity entity) {
        if (entity == null || !Damager.isDemon(entity)) {
            return false;
        }
        boolean alreadyHadImmunityTag = entity.getPersistentData().getBoolean(SUNLIGHT_IMMUNITY_KEY);
        entity.getPersistentData().putBoolean(SUNLIGHT_IMMUNITY_KEY, true);
        entity.clearFire();
        if (!alreadyHadImmunityTag) {
            SunlightImmunityHelper.playSunlightImmunityGrantedEffects(entity);
        }
        return true;
    }

    private static boolean revokeSunlightImmunity(LivingEntity entity) {
        if (entity == null || !Damager.isDemon(entity)) {
            return false;
        }
        entity.getPersistentData().remove(SUNLIGHT_IMMUNITY_KEY);
        if (entity instanceof ServerPlayer player) {
            SunlightImmunityHelper.revokeBaseOvercomeSunlightAdvancement(player);
            SunlightImmunityHelper.revokeSunlightImmunityAdvancement(player);
        }
        return true;
    }

    public static boolean hasDemonicSaturation(LivingEntity entity) {
        return entity != null && (entity.getPersistentData().getBoolean(DEMONIC_SATURATION_KEY)
            || entity.hasEffect(ModEffects.DEMONIC_SATURATION.get()));
    }

    private static boolean grantDemonicSaturation(LivingEntity entity) {
        if (entity == null || !Damager.isDemon(entity)) {
            return false;
        }
        entity.getPersistentData().putBoolean(DEMONIC_SATURATION_KEY, true);
        entity.addEffect(new MobEffectInstance(ModEffects.DEMONIC_SATURATION.get(), LONG_EFFECT_TICKS, 0, false, true, true));
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.getPersistentData().putDouble("cnt_target", 0.0D);
            mob.getNavigation().stop();
        }
        return true;
    }

    private static boolean startHumanRestoration(LivingEntity entity) {
        if (entity == null || !Damager.isDemon(entity)) {
            return false;
        }
        entity.addEffect(new MobEffectInstance(ModEffects.DEMON_RESTORATION.get(), DEMON_RESTORATION_TICKS, 0, false, true, true));
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.getNavigation().stop();
        }
        return true;
    }

    public static void completeDemonRestoration(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || !Damager.isDemon(entity)) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            DemonTransformationHandler.restoreHumanity(player);
        } else {
            entity.getPersistentData().putBoolean("oni", false);
            entity.getPersistentData().remove(SUNLIGHT_IMMUNITY_KEY);
            entity.getPersistentData().remove(DEMONIC_SATURATION_KEY);
            entity.getPersistentData().remove("KnYMpHumanRestoration");
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
                mob.getNavigation().stop();
            }
        }
        removeAllEffectsExcept(entity, ModEffects.DEMON_RESTORATION.get());
    }

    private static void removeAllEffectsExcept(LivingEntity entity, MobEffect excludedEffect) {
        for (MobEffectInstance effect : java.util.List.copyOf(entity.getActiveEffects())) {
            if (effect.getEffect() != excludedEffect) {
                entity.removeEffect(effect.getEffect());
            }
        }
    }

    private static void consumeMedicineStack(ItemStack stack, Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack snapshot = stack.copy();
        stack.shrink(1);
        ItemStack returnStack = BloodDemonArtAlchemyCatalog.containerReturn(snapshot);
        if (!returnStack.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, returnStack);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (hasDemonicSaturation(player) && isHumanFlesh(stack)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (hasDemonicSaturation(entity) && event.getNewTarget() != null && !Damager.isDemon(event.getNewTarget())) {
            event.setCanceled(true);
        }
    }

    private static boolean isHumanFlesh(ItemStack stack) {
        return stack.is(HUMAN_FLESH_SOURCES) || BloodDemonArtAlchemyCatalog.isHumanFlesh(stack);
    }
}
