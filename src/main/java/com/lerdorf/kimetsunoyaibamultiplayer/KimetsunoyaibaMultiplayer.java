package com.lerdorf.kimetsunoyaibamultiplayer;

import com.mojang.logging.LogUtils;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ISwordWielderData;
import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordWielderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker;
import com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.client.ClientCommandHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestAnimationCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestParticlesCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.DebugParticlesCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestAnimCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestCrowQuestCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.DebugCrowCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowQuestMarkerHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(KimetsunoyaibaMultiplayer.MODID)
public class KimetsunoyaibaMultiplayer
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "kimetsunoyaibamultiplayer";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public KimetsunoyaibaMultiplayer(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register entities
        ModEntities.register(modEventBus);

        // Register sounds
        ModSounds.register(modEventBus);

        // Register items
        ModItems.register(modEventBus);

        // Register config event handlers on the mod event bus
        modEventBus.register(Config.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig.class);
        modEventBus.register(com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig.class);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "kimetsunoyaibamultiplayer/common.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.SPEC, "kimetsunoyaibamultiplayer/particles.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.EntityConfig.SPEC, "kimetsunoyaibamultiplayer/entities.toml");
        context.registerConfig(ModConfig.Type.COMMON, com.lerdorf.kimetsunoyaibamultiplayer.config.SwordDisplayConfig.SPEC, "kimetsunoyaibamultiplayer/sword_display.toml");
    }

    public static final Capability<ISwordWielderData> SWORD_WIELDER_DATA = CapabilityManager.get(new CapabilityToken<>() {});
    
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Initializing Kimetsunoyaiba Multiplayer animation sync...");

        // Register network messages
        ModNetworking.register();
        LOGGER.info("Network messages registered");
        
        
    }
    
 // 4. Attach it to entities
    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player || event.getObject() instanceof LivingEntity) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(MODID, "sword_wielder_data"),
                    new ICapabilityProvider() {
                        final SwordWielderData backend = new SwordWielderData();
                        @Override
                        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                            return cap == SWORD_WIELDER_DATA ? LazyOptional.of(() -> backend).cast() : LazyOptional.empty();
                        }
                    });
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Kimetsunoyaiba Multiplayer server starting");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        LOGGER.info("Registering test commands");
        TestAnimationCommand.register(event.getDispatcher());
        TestParticlesCommand.register(event.getDispatcher());
        DebugParticlesCommand.register(event.getDispatcher());
        TestAnimCommand.register(event.getDispatcher());
        TestCrowQuestCommand.register(event.getDispatcher());
        DebugCrowCommand.register(event.getDispatcher());
        //com.lerdorf.kimetsunoyaibamultiplayer.commands.TestCrowRenderCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event)
    {
        LivingEntity target = event.getEntity();

        // Handle AOE attacks for breathing swords
        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);

            // Check if using a breathing sword (our custom swords)
            if (weapon.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) {
                // Perform AOE attack in a 3x3 cube in front of player
                Vec3 attackerPos = attacker.position().add(0, attacker.getEyeHeight(), 0);
                Vec3 lookVec = attacker.getLookAngle();
                Vec3 frontPos = attackerPos.add(lookVec.scale(2.0)); // 2 blocks in front

                // Create 3x3x3 cube
                AABB attackBox = new AABB(frontPos.add(-1.5, -1.5, -1.5), frontPos.add(1.5, 1.5, 1.5));

                List<LivingEntity> nearbyEntities = attacker.level().getEntitiesOfClass(
                    LivingEntity.class, attackBox,
                    e -> e != attacker && e != target && e.isAlive()
                );

                // Damage all nearby entities (excluding the primary target which is already being damaged)
                float damage = 6.0F; // Base AOE damage
                for (LivingEntity entity : nearbyEntities) {
                    entity.hurt(attacker.level().damageSources().playerAttack(attacker), damage);

                    // Play sweep attack particles
                    if (attacker.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                            entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                            1, 0, 0, 0, 0
                        );
                    }
                }

                if (Config.logDebug && !nearbyEntities.isEmpty()) {
                    LOGGER.debug("AOE attack hit {} additional entities", nearbyEntities.size());
                }
            }

            // Handle attack-based particle triggering (for server-side events)
            if (com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.particleTriggerMode ==
                com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.ParticleTriggerMode.ATTACK_ONLY) {
                if (SwordParticleMapping.isKimetsunoyaibaSword(weapon)) {
                    if (Config.logDebug)
                    LOGGER.debug("Attack detected with kimetsunoyaiba sword: {} -> {}",
                        attacker.getName().getString(),
                        target.getName().getString());
                    // Note: Particle spawning will be handled client-side via animation tracking
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) {
            if (event.getServer() != null) {
                ServerLevel overworld = event.getServer().overworld();

                // Update flying crows ONCE per tick (not per dimension)
                CrowEnhancementHandler.tick(overworld);

                // Tick breathing technique ability scheduler
                com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.AbilityScheduler.tick(overworld);

                // Scan for unmirrored crows every second (20 ticks)
                if (overworld.getGameTime() % 20 == 0) {
                    com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowMirrorHandler.scanForUnmirroredCrows(overworld);
                }
            }
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        	if (Config.logDebug)
            LOGGER.info("Animation sync system initialized for client");
        }

        @SubscribeEvent
        public static void onKeyRegister(net.minecraftforge.client.event.RegisterKeyMappingsEvent event)
        {
            event.register(com.lerdorf.kimetsunoyaibamultiplayer.client.ModKeyBindings.CYCLE_BREATHING_FORM);
            if (Config.logDebug)
            LOGGER.info("Registered breathing technique key binding");
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        private static int debugTickCounter = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase == TickEvent.Phase.END) {
                debugTickCounter++;
                if (Config.logDebug && debugTickCounter % 100 == 0) { // Log every 5 seconds
                    LOGGER.info("Client tick event handler is working, tick: {}", debugTickCounter);
                }
                AnimationTracker.tick();
                CrowQuestMarkerHandler.clientTick();
                com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.tick();

                // Update gun animations for local player
                if (Minecraft.getInstance().player != null) {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.updatePlayerGunAnimation(
                            Minecraft.getInstance().player);
                }

                // Update gun animations for all nearby entities (including mobs)
                com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.updateAllEntityGunAnimations();
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(TickEvent.ClientTickEvent event)
        {
            if (Minecraft.getInstance().level == null && event.phase == TickEvent.Phase.END) {
                AnimationTracker.clearTrackedAnimations();
                AnimationSyncHandler.clearAllAnimations();
                CrowQuestMarkerHandler.clearAllMarkers();
                CrowEnhancementHandler.clearFlyingCrows();
                com.lerdorf.kimetsunoyaibamultiplayer.client.CrowAnimatableWrapper.clearAll();
                com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.clearAll();
                com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.clearAll();
                // Don't clear mirrors from client side - they are server-side entities
                // They will be cleared when the server shuts down or dimension unloads
            }
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(net.minecraftforge.client.event.RegisterClientCommandsEvent event)
        {
            ClientCommandHandler.onRegisterClientCommands(event);
        }

        @SubscribeEvent
        public static void onClientLivingAttack(LivingAttackEvent event)
        {
            // IMPORTANT: This event fires on BOTH client and server threads!
            // We MUST check that we're on the logical client side
            if (!event.getEntity().level().isClientSide()) {
                return; // Skip server-side events
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }

            // ONLY handle attacks by the LOCAL PLAYER
            // Other players' and mobs' gun attacks should NOT trigger effects here
            // (they would be handled via network sync or other mechanisms)
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                // Only process if it's the local player attacking
                if (!attacker.getUUID().equals(mc.player.getUUID())) {
                    return; // Not the local player - ignore
                }

                ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);

                // Check for gun attacks by LOCAL PLAYER ONLY
                if (com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.isGun(weapon)) {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.GunType gunType =
                            com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.getGunType(attacker);

                    // Play shoot animation for local player
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.playShootAnimation(
                            mc.player, gunType);

                    if (Config.logDebug) {
                        LOGGER.debug("Triggered gun shoot animation for local player: {}", gunType);
                    }
                    return;
                }

                // Check for breathing sword attacks
                if (attacker instanceof AbstractClientPlayer clientAttacker) {
                    // Play attack animation for breathing swords
                    com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingSwordAnimationHandler.onAttack(clientAttacker);

                    // Check for sword particle effects (existing code, only for local player)
                    if (com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.swordParticlesEnabled &&
                        com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.particleTriggerMode ==
                        com.lerdorf.kimetsunoyaibamultiplayer.config.ParticleConfig.ParticleTriggerMode.ATTACK_ONLY) {

                        if (SwordParticleMapping.isKimetsunoyaibaSword(weapon)) {
                            // Force spawn particles on attack
                            SwordParticleHandler.forceSpawnParticles(clientAttacker, weapon, "attack");

                            if (Config.logDebug) {
                                LOGGER.debug("Triggered attack-based particles for local player");
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event)
        {
            // Monitor chat for crow quest messages
            String message = event.getMessage().getString();
            CrowQuestMarkerHandler.onChatMessage(message);
        }

        /**
         * Handle left-click attacks (works for both air and entity clicks)
         * This is called BEFORE LivingAttackEvent, so we can handle air clicks here
         */
        @SubscribeEvent
        public static void onLeftClickEmpty(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event)
        {
            // Only handle attack key
            if (!event.isAttack()) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }

            ItemStack heldItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

            // Check if holding breathing sword - play attack animation
            if (heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) {
                com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingSwordAnimationHandler.onAttack(mc.player);
            }

            // Check if holding rifle
            if (heldItem.getItem().toString().contains("rifle")) {
                com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.GunType gunType =
                        com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.GunType.RIFLE;

                // Play shoot animation and effects
                com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.playShootAnimation(
                        mc.player, gunType);

                if (Config.logDebug) {
                    LOGGER.debug("Triggered rifle shoot animation (air click)");
                }
            }
        }
    }
}
