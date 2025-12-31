package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.ModEffects;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.EntitySpawnHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles favor potion protection mechanics.
 *
 * Favor of Ubuyashiki: Spawns demon slayers when attacked by demons or players
 * Favor of Muzan: Spawns demons when attacked by non-demons (slayers or players)
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FavorProtectionHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(victim.level() instanceof ServerLevel level)) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return; // Environmental damage doesn't trigger favor

        // Check for Favor of Ubuyashiki (spawns slayers when attacked by demons/players)
        MobEffectInstance ubuyashikiFavor = victim.getEffect(ModEffects.FAVOR_OF_UBUYASHIKI.get());
        if (ubuyashikiFavor != null) {
            // Triggers when attacked by demons or players
            if (isDemon(attacker) || attacker instanceof Player) {
                int favorLevel = ubuyashikiFavor.getAmplifier() + 1; // 1-3
                spawnProtectors(level, victim, attacker, favorLevel, true);
                victim.removeEffect(ModEffects.FAVOR_OF_UBUYASHIKI.get());
                return;
            }
        }

        // Check for Favor of Muzan (spawns demons when attacked by non-demons)
        MobEffectInstance muzanFavor = victim.getEffect(ModEffects.FAVOR_OF_MUZAN.get());
        if (muzanFavor != null) {
            // Triggers when attacked by non-demons (slayers or players)
            if (!isDemon(attacker) || attacker instanceof Player) {
                int favorLevel = muzanFavor.getAmplifier() + 1; // 1-3
                spawnProtectors(level, victim, attacker, favorLevel, false);
                victim.removeEffect(ModEffects.FAVOR_OF_MUZAN.get());
                return;
            }
        }
    }

    /**
     * Spawn protective entities around the victim.
     * @param level Server level
     * @param victim Player being protected
     * @param attacker Entity that attacked the player
     * @param favorLevel Favor potion level (1-3)
     * @param spawnSlayers True to spawn slayers, false to spawn demons
     */
    private static void spawnProtectors(ServerLevel level, Player victim, Entity attacker, int favorLevel, boolean spawnSlayers) {
        int spawnCount = switch (favorLevel) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            default -> 2;
        };

        BlockPos victimPos = victim.blockPosition();

        for (int i = 0; i < spawnCount; i++) {
            // Spawn in a ring around the victim (3-8 blocks away)
            double angle = (Math.PI * 2 * i) / spawnCount;
            int distance = 3 + level.random.nextInt(6);
            int x = victimPos.getX() + (int)(Math.cos(angle) * distance);
            int z = victimPos.getZ() + (int)(Math.sin(angle) * distance);

            BlockPos spawnPos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, victimPos.getY(), z)
            );

            // Spawn the protector
            Mob protector = spawnSlayers
                ? spawnRandomSlayer(level, spawnPos, favorLevel)
                : spawnRandomDemon(level, spawnPos, favorLevel);

            if (protector != null) {
                // CRITICAL: Mark the victim as protected - protector should NEVER attack them
                markAsProtected(protector, victim);

                // Make protector target the attacker
                if (attacker instanceof Mob mob) {
                    protector.setTarget(mob);
                } else if (attacker instanceof Player player) {
                    protector.setTarget(player);
                    protector.setLastHurtByMob(player);
                }

                Log.debug("Spawned " + (spawnSlayers ? "slayer" : "demon") + " protector at " + spawnPos);
            }
        }

        // Notify victim
        String protectorType = spawnSlayers ? "demon slayers" : "demons";
        victim.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§e" + spawnCount + " " + protectorType + " come to your aid!"),
            false
        );
    }

    /**
     * Spawn a random demon slayer based on favor level.
     */
    private static Mob spawnRandomSlayer(ServerLevel level, BlockPos pos, int favorLevel) {
        // Determine which slayer type to spawn based on favor level
        ResourceLocation slayerId;

        if (favorLevel >= 3) {
            // Level 3: Spawn hashira (with enhanced breathing replacement)
            String[] hashiraOptions = {"kocho", "kanroji", "muichirou", "himejima", "tomioka", "rengoku"};
            String selectedHashira = hashiraOptions[level.random.nextInt(hashiraOptions.length)];
            slayerId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", selectedHashira);

            // Apply enhanced breathing replacement
            slayerId = EntitySpawnHelper.filterForProtectiveSpawning(slayerId);
        } else if (favorLevel >= 2) {
            // Level 2: Spawn named slayer
            String[] namedSlayers = {"tanjiro", "zennitsu", "inosuke", "kanawo"};
            String selectedSlayer = namedSlayers[level.random.nextInt(namedSlayers.length)];
            slayerId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", selectedSlayer);
        } else {
            // Level 1: Spawn generic demon slayer
            slayerId = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "demon_slayer");
        }

        if (slayerId == null) {
            return null;
        }

        // Try to spawn the entity
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(slayerId);
        if (entityType == null) {
            return null;
        }

        try {
            Entity entity = entityType.create(level);
            if (entity instanceof Mob mob) {
                mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
                return mob;
            }
        } catch (Exception e) {
            System.err.println("[Favor Protection] Failed to spawn slayer: " + e.getMessage());
        }

        return null;
    }

    /**
     * Spawn a random demon based on favor level.
     */
    private static Mob spawnRandomDemon(ServerLevel level, BlockPos pos, int favorLevel) {
        // Try to get a demon entity type from the base mod
        String[] demonTypes = {
            "kimetsunoyaiba:easy_demon",
            "kimetsunoyaiba:medium_demon",
            "kimetsunoyaiba:hard_demon",
            "minecraft:zombie" // Fallback
        };

        // Choose demon type based on favor level
        String[] typesToTry = favorLevel >= 3
            ? new String[]{"kimetsunoyaiba:hard_demon", "kimetsunoyaiba:medium_demon"}
            : favorLevel >= 2
            ? new String[]{"kimetsunoyaiba:medium_demon", "kimetsunoyaiba:easy_demon"}
            : new String[]{"kimetsunoyaiba:easy_demon"};

        for (String typeId : typesToTry) {
            try {
                net.minecraft.resources.ResourceLocation entityId =
                    net.minecraft.resources.ResourceLocation.tryParse(typeId);
                if (entityId == null) continue;

                EntityType<?> entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(entityId);
                if (entityType != null) {
                    Entity entity = entityType.create(level);
                    if (entity instanceof Mob mob) {
                        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                        mob.setPersistenceRequired();
                        level.addFreshEntity(mob);
                        return mob;
                    }
                }
            } catch (Exception e) {
                // Try next type
            }
        }

        return null;
    }

    /**
     * Check if an entity is a demon (MONSTER category).
     */
    private static boolean isDemon(Entity entity) {
        if (!(entity instanceof Mob mob)) return false;
        return mob.getType().getCategory() == MobCategory.MONSTER;
    }

    /**
     * Mark a protector entity to NEVER attack the protected player.
     * This is critical - protectors should never harm the player who summoned them!
     */
    private static void markAsProtected(Mob protector, Player protectedPlayer) {
        // Store protected player UUID in entity NBT
        net.minecraft.nbt.CompoundTag entityData = protector.getPersistentData();
        entityData.putUUID("ProtectedPlayerUUID", protectedPlayer.getUUID());

        // If protector is currently targeting the protected player, clear it immediately
        if (protector.getTarget() == protectedPlayer) {
            protector.setTarget(null);
        }

        // Add a custom AI goal to prevent targeting the protected player
        // This runs every tick to ensure the protector never attacks the victim
        protector.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.Goal() {
            @Override
            public boolean canUse() {
                // Check every tick if we're targeting the protected player
                net.minecraft.world.entity.LivingEntity target = protector.getTarget();
                if (target instanceof Player player) {
                    net.minecraft.nbt.CompoundTag data = protector.getPersistentData();
                    if (data.hasUUID("ProtectedPlayerUUID")) {
                        java.util.UUID protectedUUID = data.getUUID("ProtectedPlayerUUID");
                        if (player.getUUID().equals(protectedUUID)) {
                            // We're targeting the protected player - STOP!
                            protector.setTarget(null);
                            return true; // Interrupt other goals
                        }
                    }
                }
                return false;
            }

            @Override
            public void start() {
                // Clear target if it's the protected player
                protector.setTarget(null);
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return true; // Check every single tick
            }
        });

        Log.debug("Marked protector to never attack " + protectedPlayer.getName().getString());
    }
}
