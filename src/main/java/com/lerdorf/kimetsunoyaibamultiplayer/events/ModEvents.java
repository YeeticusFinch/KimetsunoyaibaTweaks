package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.EyeFamiliarEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.OrochiEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.items.HumanFleshItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.PassiveSkillManager;
import com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationStatsTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.quest.QuestProgressionManager;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.RaidTriggerHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.FamiliarEntityHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

@Mod.EventBusSubscriber
public class ModEvents {
    private static final Set<ResourceLocation> MUZAN_BLOOD_ITEM_IDS = Set.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "blood_of_muzan"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "muzan_blood"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "bloodmuzan"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "blood_of_muzan")
    );

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Log.startupProbeOnce("ModEvents.onPlayerLogin.start");
        Player player = event.getEntity();
        // Load player's breathing form data from NBT
        PlayerBreathingData.loadFromNBT(player);
        if (player instanceof ServerPlayer serverPlayer) {
            DemonTransformationHandler.restorePersistentDemonhood(serverPlayer);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService.enforceTransformationRolePrecedence(serverPlayer);
            com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService.enforceDemonRolePrecedence(serverPlayer);
        }
        Log.startupProbeOnce("ModEvents.onPlayerLogin.end");
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        // Save player's breathing form data to NBT
        PlayerBreathingData.saveToNBT(player);
        // Clean up in-memory cache
        PlayerBreathingData.clear(player.getUUID());
        // Clean up demon slayer initiation tracking
        DemonSlayerInitiationHandler.onPlayerLogout(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        QuestProgressionManager.copyQuestProgressOnClone(event.getOriginal(), event.getEntity(), event.isWasDeath());
        MeditationStatsTracker.copyOnClone(event.getOriginal(), event.getEntity());
        if (event.isWasDeath()) {
            DemonTransformationHandler.capturePersistentDemonhood(event.getOriginal(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (event.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            DemonTransformationHandler.restorePersistentDemonhood(serverPlayer);
            com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService.enforceTransformationRolePrecedence(serverPlayer);
            com.lerdorf.kimetsunoyaibamultiplayer.meditation.MeditationMenuService.enforceDemonRolePrecedence(serverPlayer);
            PassiveSkillManager.tick(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (FamiliarEntityHelper.isDamageImmuneQuestFamiliar(event.getEntity())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (event.getSource().is(DamageTypes.DROWN) && isDrowningImmuneQuestEntity(event.getEntity())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (isMountedQuestEntityImmune(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
            event.setAmount(0.0F);
            return;
        }
        if (event.getEntity() instanceof Player player) {
            PassiveSkillManager.recordDamage(player);
        }
        if (!event.getEntity().level().isClientSide()
            && event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity target = event.getEntity();
            float projectedHealth = Math.max(0.0F, target.getHealth() - event.getAmount());
            QuestProgressionManager.handleKamanueHurt(player, target, projectedHealth);
            QuestProgressionManager.handleSlayersBloodSlayerHurt(player, target, projectedHealth, event);
        }
    }

    private static boolean isDrowningImmuneQuestEntity(LivingEntity entity) {
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeId == null) {
            return false;
        }
        String path = typeId.getPath();
        return path.contains("kasugai_crow")
            || path.contains("princess")
            || path.contains("orochi")
            || path.contains("eye_familiar");
    }

    private static boolean isMountedQuestEntityImmune(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof OrochiEntity orochi) && !(entity instanceof EyeFamiliarEntity)) {
            return false;
        }

        if (entity.getVehicle() instanceof Player player && entity instanceof net.minecraft.world.entity.OwnableEntity ownable
            && player.getUUID().equals(ownable.getOwnerUUID())) {
            return true;
        }

        return isDemonPlayerSource(source);
    }

    private static boolean isDemonPlayerSource(DamageSource source) {
        if (source == null) {
            return false;
        }

        if (source.getEntity() instanceof Player player && Damager.isDemon(player)) {
            return true;
        }

        return source.getDirectEntity() instanceof Player player && Damager.isDemon(player);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        LivingEntity source = null;

        if (event.getSource().getEntity() instanceof LivingEntity livingEntity) {
            source = livingEntity;
        }

        if (!target.level().isClientSide() && source != null && Damager.isDemon(source)) {
            handleCustomHumanFleshDrops(target);
        }

        if (!target.level().isClientSide() && target instanceof DemonSlayerEntity slayer) {
            QuestProgressionManager.handleSlayersBloodCaptiveDeath(slayer);
            QuestProgressionManager.handleSlayersBloodFinalSlayerDeath(slayer);
        }

        // Skip omen effect application if raids are disabled
        if (!RaidConfig.enableRaids.get()) {
            return;
        }

        // Don't apply omen effects if target is a raid entity or source is near a raid
        if (RaidTriggerHandler.isRaidEntity(target)) {
            return;
        }
        if (source instanceof Player && RaidTriggerHandler.isPlayerNearRaid((Player) source)) {
            return;
        }

        if (source instanceof net.minecraft.server.level.ServerPlayer serverPlayer &&
            CustomProgressionConfig.disableBaseModDemonSlayerInitiation.get()) {
            ResourceLocation targetId = net.minecraft.world.entity.EntityType.getKey(target.getType());
            if (targetId != null && DemonRegistry.isRegistered(targetId)) {
                DemonSlayerInitiationHandler.triggerCustomInitiation(serverPlayer, "killed addon demon " + targetId);
            }
        }

        if (source instanceof net.minecraft.server.level.ServerPlayer serverPlayer
            && QuestProgressionManager.shouldSuppressOmenForQuestKill(serverPlayer, target)) {
            return;
        }

        // Check if the target was a demon slayer and the source is a demon
        if (source != null && Damager.isDemon(source) && Damager.isDemonSlayer(target)) {
            // Apply omen_of_ubuyashiki effect to the demon who killed the demon slayer
            // ambient=false, visible=true, showIcon=true - no sound effect
            source.addEffect(new MobEffectInstance(ModEffects.OMEN_OF_UBUYASHIKI.get(), 6000, 0, false, true, true));
        }
        // Check if the target was a demon and the source is not a demon (a human)
        else if (source != null && !Damager.isDemon(source) && Damager.isDemon(target)) {
            // Apply omen_of_muzan effect to the human who killed the demon (20% chance)
            if (Math.random() < 0.2) {
                // ambient=false, visible=true, showIcon=true - no sound effect
                source.addEffect(new MobEffectInstance(ModEffects.OMEN_OF_MUZAN.get(), 6000, 0, false, true, true));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || !DemonTransformationHandler.isCustomDemonInitiationEnabled()) {
            return;
        }

        if (!Damager.isDemon(target) || EntityTagHelper.isTwelveKizuki(target)) {
            return;
        }

        event.getDrops().removeIf(drop -> isMuzanBlood(drop));
    }

    private static void handleCustomHumanFleshDrops(LivingEntity target) {
        if (target instanceof Player player && !player.getPersistentData().getBoolean("oni")) {
            ItemStack stack = new ItemStack(ModItems.HUMAN_FLESH_5.get());
            HumanFleshItem.setPlayerSkin(stack, player.getUUID(), player.getGameProfile().getName());
            target.spawnAtLocation(stack);
            return;
        }

        if (target instanceof DemonSlayerEntity slayer) {
            if (slayer.isFemale()) {
                ItemStack stack = new ItemStack(ModItems.HUMAN_FLESH_4.get());
                HumanFleshItem.setTexture(stack, getFemaleSlayerTexture(slayer));
                target.spawnAtLocation(stack);
            } else {
                Item flesh2 = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "human_flesh_2"));
                if (flesh2 != null) {
                    target.spawnAtLocation(new ItemStack(flesh2));
                }
            }
            return;
        }

        ResourceLocation entityId = EntityType.getKey(target.getType());
        if (entityId != null && EntityTagHelper.isCivilian(target)
            && com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID.equals(entityId.getNamespace())) {
            ItemStack stack = new ItemStack(ModItems.HUMAN_FLESH_3.get());
            HumanFleshItem.setTexture(stack, getCivilianTexture(entityId));
            target.spawnAtLocation(stack);
        }
    }

    private static ResourceLocation getCivilianTexture(ResourceLocation entityId) {
        return ResourceLocation.fromNamespaceAndPath(entityId.getNamespace(), "textures/entity/" + entityId.getPath() + ".png");
    }

    private static ResourceLocation getFemaleSlayerTexture(DemonSlayerEntity slayer) {
        int textureIndex = Math.max(0, Math.min(8, slayer.getTextureIndex()));
        return ResourceLocation.fromNamespaceAndPath(
            com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/slayer_female_" + (textureIndex + 1) + ".png");
    }

    private static boolean isMuzanBlood(ItemEntity itemEntity) {
        return itemEntity != null && isMuzanBlood(itemEntity.getItem());
    }

    private static boolean isMuzanBlood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId != null && MUZAN_BLOOD_ITEM_IDS.contains(itemId);
    }
}
