package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.DemonRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.config.RaidConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.items.HumanFleshItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.RaidTriggerHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Log.startupProbeOnce("ModEvents.onPlayerLogin.start");
        Player player = event.getEntity();
        // Load player's breathing form data from NBT
        PlayerBreathingData.loadFromNBT(player);
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
        if (event.isWasDeath()) {
            DemonTransformationHandler.resetTrackedMuzanBlood(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!event.player.getPersistentData().getBoolean("oni")
            && DemonTransformationHandler.getTrackedMuzanBlood(event.player) > 0) {
            DemonTransformationHandler.resetTrackedMuzanBlood(event.player);
        }
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
}
