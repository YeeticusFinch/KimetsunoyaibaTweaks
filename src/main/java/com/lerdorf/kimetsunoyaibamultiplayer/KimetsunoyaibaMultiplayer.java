package com.lerdorf.kimetsunoyaibamultiplayer;

import com.mojang.logging.LogUtils;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.ISwordWielderData;
import com.lerdorf.kimetsunoyaibamultiplayer.capability.SwordWielderData;
import com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking;
import com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathingSwordSwingPacket;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.DamageCalculator;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestParticlesCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.DebugParticlesCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestAnimCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.commands.TestCrowQuestCommand;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.particles.SwordParticleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowEnhancementHandler;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities;
import com.lerdorf.kimetsunoyaibamultiplayer.sounds.ModSounds;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.proxy.IClientProxy;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

// The value here should match an entry in the META-INF/mods.toml file
@Mod(KimetsunoyaibaMultiplayer.MODID)
public class KimetsunoyaibaMultiplayer
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "kimetsunoyaibamultiplayer";

    // Client proxy - loads ClientProxy on client, ServerProxy on server
    public static final IClientProxy CLIENT_PROXY = net.minecraftforge.fml.DistExecutor.safeRunForDist(
        () -> () -> new com.lerdorf.kimetsunoyaibamultiplayer.client.ClientProxy(),
        () -> () -> new com.lerdorf.kimetsunoyaibamultiplayer.proxy.ServerProxy()
    );

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
        Log.info("Initializing Kimetsunoyaiba Multiplayer animation sync...");

        // Register network messages
        ModNetworking.register();
        Log.info("Network messages registered");
        
        
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
        Log.info("Kimetsunoyaiba Multiplayer server starting");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        Log.info("Registering test commands");
        // Only register client-safe commands on the server
        // TestAnimationCommand and DebugCrowCommand have client-only imports
        // and should only be registered client-side via ClientCommandHandler
        TestParticlesCommand.register(event.getDispatcher());
        DebugParticlesCommand.register(event.getDispatcher());
        TestAnimCommand.register(event.getDispatcher());
        TestCrowQuestCommand.register(event.getDispatcher());
    }

    // Prevent infinite recursion when AOE attacks trigger more events
    private static final ThreadLocal<Boolean> IS_PROCESSING_AOE = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event)
    {
        try {
            // Prevent recursion from AOE attacks triggering more events
            if (IS_PROCESSING_AOE.get()) {
                return;
            }

            LivingEntity target = event.getEntity();

            // NOTE: AOE damage is now handled client-side via BreathingSwordSwingPacket
            // sent from onLeftClickEmpty and onClientLivingAttack events.
            // This server-side event is only for logging/debugging purposes.

            // Handle attack-based logging (for debugging)
            if (event.getSource().getEntity() instanceof Player attacker) {
                ItemStack weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);

                if (Config.logDebug && SwordParticleMapping.isKimetsunoyaibaSword(weapon)) {
                    System.err.println("Server: Attack detected with kimetsunoyaiba sword: " +
                        attacker.getName().getString() + " -> " + target.getName().getString());
                }
            }
        } catch (Exception e) {
            // Silently catch exceptions to prevent crashes
            if (Config.logDebug) {
                System.err.println("Error in onLivingAttack: " + e.getMessage());
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
            Log.info("Animation sync system initialized for client");
        }

        @SubscribeEvent
        public static void onKeyRegister(net.minecraftforge.client.event.RegisterKeyMappingsEvent event)
        {
            event.register(com.lerdorf.kimetsunoyaibamultiplayer.client.ModKeyBindings.CYCLE_BREATHING_FORM);
            if (Config.logDebug)
            Log.info("Registered breathing technique key binding");
        }

        @SubscribeEvent
        public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event)
        {
            // Register GeckoLib entity renderers
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.ICE_SLAYER.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.entities.client.IceSlayerRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.FROST_SLAYER.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.entities.client.FrostSlayerRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.KOMOREBI.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.entities.client.KomorebiRenderer::new);
            event.registerEntityRenderer(com.lerdorf.kimetsunoyaibamultiplayer.entities.ModEntities.SHIMIZU.get(),
                com.lerdorf.kimetsunoyaibamultiplayer.entities.client.ShimizuRenderer::new);
            if (Config.logDebug)
            Log.info("Registered breathing slayer entity renderers");
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        // Client-only imports (safe inside Dist.CLIENT annotated class)
        // These are imported here to avoid loading client classes on dedicated server

        private static int debugTickCounter = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase == TickEvent.Phase.END) {
                debugTickCounter++;
                if (Config.logDebug && debugTickCounter % 100 == 0) { // Log every 5 seconds
                    Log.info("Client tick event handler is working, tick: {}", debugTickCounter);
                }
                com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.tick();
                com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowQuestMarkerHandlerClient.clientTick();
                com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker.tick();

                // Update gun animations for local player
                if (net.minecraft.client.Minecraft.getInstance().player != null) {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.updatePlayerGunAnimation(
                            net.minecraft.client.Minecraft.getInstance().player);
                }

                // Update gun animations for all nearby entities (including mobs)
                com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.updateAllEntityGunAnimations();
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(TickEvent.ClientTickEvent event)
        {
            if (net.minecraft.client.Minecraft.getInstance().level == null && event.phase == TickEvent.Phase.END) {
                com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.clearTrackedAnimations();
                com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationSyncHandler.clearAllAnimations();
                com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowQuestMarkerHandlerClient.clearAllMarkers();
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
            com.lerdorf.kimetsunoyaibamultiplayer.client.ClientCommandHandler.onRegisterClientCommands(event);
        }

        @SubscribeEvent
        public static void onClientLivingAttack(LivingAttackEvent event)
        {
            try {
                // IMPORTANT: This event fires on BOTH client and server threads!
                // We MUST check that we're on the logical client side
                if (!event.getEntity().level().isClientSide()) {
                    return; // Skip server-side events
                }

                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
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
                            Log.debug("Triggered gun shoot animation for local player: " + gunType);
                        }
                        return;
                    }

                    // Check for breathing sword attacks (entity hit)
                    if (attacker instanceof net.minecraft.client.player.AbstractClientPlayer clientAttacker) {
                        // Play attack animation for breathing swords (both our mod and kimetsunoyaiba)
                        String animationName = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingSwordAnimationHandler.onAttack(clientAttacker);

                        // Set the left-click attack flag (sticky bit) for ATTACK_ONLY mode
                        if (animationName != null && SwordParticleMapping.isKimetsunoyaibaSword(weapon)) {
                            com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.markLeftClickAttack(clientAttacker.getUUID());

                            if (Config.logDebug) {
                                Log.debug("Set left-click attack flag for entity attack: " + animationName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Silently catch exceptions to prevent crash
                if (Config.logError) {
                    Log.error("Error in onClientLivingAttack: " + e.getMessage());
                }
            }
        }

        @SubscribeEvent
        public static void onChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event)
        {
            // Monitor chat for crow quest messages
            String message = event.getMessage().getString();
            com.lerdorf.kimetsunoyaibamultiplayer.entities.CrowQuestMarkerHandlerClient.onChatMessage(message);
        }

        /**
         * Handle left-click attacks (works for both air and entity clicks)
         * This is called BEFORE LivingAttackEvent, so we can handle air clicks here
         */
        @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
        public static void onLeftClickEmpty(net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event)
        {
            try {
                // Only handle attack key
                if (!event.isAttack()) {
                    return;
                }

                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player == null || mc.level == null) {
                    return;
                }

                ItemStack heldItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

                if (SwordParticleMapping.isKimetsunoyaibaSword(heldItem)) {
                	// Set the left-click attack flag (sticky bit) so AnimationTracker will spawn particles
                    com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.markLeftClickAttack(mc.player.getUUID());

                    if (Config.logDebug) {
                        Log.debug("Set left-click attack flag for breathing sword left-click for kimetsynoyaibaSword");
                    }
                }
                
                // Check if holding breathing sword from our mod - play attack animation and handle AOE
                if (heldItem.getItem() instanceof com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem) {
                    String animationName = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingSwordAnimationHandler.onAttack(mc.player);

                    // Only send packet and set sticky bit if animation was actually played (cooldown check passed)
                    if (animationName != null) {
                        ModNetworking.sendToServer(new BreathingSwordSwingPacket()); // For AOE damage (only our mod's swords)

                        // Set the left-click attack flag (sticky bit) so AnimationTracker will spawn particles
                        com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.markLeftClickAttack(mc.player.getUUID());

                        if (Config.logDebug) {
                            Log.debug("Set left-click attack flag for breathing sword left-click: " + animationName);
                        }
                    }
                }
                // Check if holding nichirinsword from kimetsunoyaiba mod - only handle particles, no AOE
                else if (SwordParticleMapping.isKimetsunoyaibaSword(heldItem) &&
                         heldItem.getItem().toString().contains("nichirinsword")) {

                    // Play similar attack animation for kimetsunoyaiba swords
                    String animationName = com.lerdorf.kimetsunoyaibamultiplayer.client.BreathingSwordAnimationHandler.onAttack(mc.player);

                    // Set the left-click attack flag (sticky bit) so AnimationTracker will spawn particles
                    if (animationName != null) {
                        com.lerdorf.kimetsunoyaibamultiplayer.client.AnimationTracker.markLeftClickAttack(mc.player.getUUID());

                        if (Config.logDebug) {
                            Log.debug("Set left-click attack flag for nichirinsword left-click: " + animationName);
                        }
                    }
                }

                // Check if holding rifle
                if (heldItem.getItem().toString().contains("rifle")) {
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.GunType gunType =
                            com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.GunType.RIFLE;

                    // Play shoot animation and effects
                    com.lerdorf.kimetsunoyaibamultiplayer.client.GunAnimationHandler.playShootAnimation(
                            mc.player, gunType);

                    if (Config.logDebug) {
                        Log.debug("Triggered rifle shoot animation (air click)");
                    }
                }
            } catch (Exception e) {
                // Silently catch exceptions to prevent crash
                // The exception likely occurs due to threading or mod conflicts
                if (Config.logDebug) {
                    Log.error("Error in onLeftClickEmpty: " + e.getMessage());
                }
            }
        }
    }
}
