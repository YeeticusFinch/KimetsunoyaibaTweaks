package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.raids.StructureLocationCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;

public final class QuestScenarioActions {
    public static final String QUEST_NPC_ID_TAG = "KnYQuestNpcId";
    public static final String QUEST_TARGET_ID_TAG = "KnYQuestTargetId";
    public static final String CURRENT_STRUCTURE_X = "KnYQuestStructureX";
    public static final String CURRENT_STRUCTURE_Y = "KnYQuestStructureY";
    public static final String CURRENT_STRUCTURE_Z = "KnYQuestStructureZ";
    public static final String KIDNAPPERS_BOG_ACTIVE_TAG = "KnYKidnappersBogActive";
    public static final String SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG = "KnYSwampDomainEncounterStarted";

    private static final ResourceLocation VILLAGE_SWAMP = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "village_swamp");
    private static final ResourceLocation CIVILIAN_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "civilian");
    private static final ResourceLocation SWAMP_DEMON_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "swamp_demon");
    private static final ResourceLocation SATOKOS_BOW = ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "satokos_bow");

    private QuestScenarioActions() {
    }

    public static void storeCurrentStructureCenter(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        StructureLocationCache.getStructureAt(serverLevel, player.blockPosition()).ifPresent(cached -> {
            player.getPersistentData().putInt(CURRENT_STRUCTURE_X, cached.center.getX());
            player.getPersistentData().putInt(CURRENT_STRUCTURE_Y, cached.center.getY());
            player.getPersistentData().putInt(CURRENT_STRUCTURE_Z, cached.center.getZ());
        });
    }

    public static BlockPos getStoredStructureCenter(ServerPlayer player) {
        if (!player.getPersistentData().contains(CURRENT_STRUCTURE_X)) {
            return null;
        }
        return new BlockPos(
            player.getPersistentData().getInt(CURRENT_STRUCTURE_X),
            player.getPersistentData().getInt(CURRENT_STRUCTURE_Y),
            player.getPersistentData().getInt(CURRENT_STRUCTURE_Z)
        );
    }

    public static void ensureKazumiSpawned(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = getStoredStructureCenter(player);
        if (center == null) {
            center = findNearestStructure(serverLevel, player.blockPosition(), VILLAGE_SWAMP);
        }
        if (center == null) {
            return;
        }

        List<Entity> existing = serverLevel.getEntities((Entity) null,
            new net.minecraft.world.phys.AABB(center).inflate(400.0D),
            entity -> "kazumi".equals(entity.getPersistentData().getString(QUEST_NPC_ID_TAG)));
        if (!existing.isEmpty()) {
            return;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(CIVILIAN_ID);
        Entity entity = type != null ? type.create(serverLevel) : EntityType.VILLAGER.create(serverLevel);
        if (entity == null) {
            return;
        }

        BlockPos spawnPos = findRandomSurfacePosition(serverLevel, center, 24, 12);
        entity.getPersistentData().putString(QUEST_NPC_ID_TAG, "kazumi");
        entity.setCustomName(Component.literal("Kazumi"));
        entity.setCustomNameVisible(true);
        entity.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setNoAi(true);
        }
        if (entity instanceof Villager villager) {
            villager.setNoAi(true);
        }
        serverLevel.addFreshEntity(entity);
    }

    public static void ensureSwampDemonSpawned(ServerPlayer player, QuestRuntimeContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel) || !serverLevel.isNight()) {
            return;
        }

        BlockPos center = getStoredStructureCenter(player);
        if (center == null) {
            center = findNearestStructure(serverLevel, player.blockPosition(), VILLAGE_SWAMP);
        }
        if (center == null) {
            return;
        }

        List<Entity> existing = serverLevel.getEntities((Entity) null,
            new net.minecraft.world.phys.AABB(center).inflate(128.0D),
            entity -> "swamp_demon_kidnappers_bog".equals(entity.getPersistentData().getString(QUEST_TARGET_ID_TAG)));
        if (!existing.isEmpty()) {
            return;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(SWAMP_DEMON_ID);
        Entity entity = type != null ? type.create(serverLevel) : EntityType.ZOMBIE.create(serverLevel);
        if (entity == null) {
            return;
        }

        BlockPos spawnPos = findRandomSurfacePosition(serverLevel, center, 28, 16);
        entity.getPersistentData().putString(QUEST_TARGET_ID_TAG, "swamp_demon_kidnappers_bog");
        entity.setCustomName(Component.literal("Swamp Demon"));
        entity.setCustomNameVisible(true);
        entity.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setTarget(player);
        }
        if (entity instanceof Zombie zombie) {
            zombie.setTarget(player);
        }
        serverLevel.addFreshEntity(entity);
    }

    public static BlockPos findNearestQuestEntity(ServerPlayer player, String targetKey, double radius) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        return serverLevel.getEntities((Entity) null,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(radius),
                entity -> targetKey.equals(entity.getPersistentData().getString(QUEST_NPC_ID_TAG)) ||
                    targetKey.equals(entity.getPersistentData().getString(QUEST_TARGET_ID_TAG)))
            .stream()
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .map(Entity::blockPosition)
            .orElse(null);
    }

    public static BlockPos findNearestStructure(ServerLevel level, BlockPos origin, ResourceLocation structureId) {
        BlockPos nearest = level.findNearestMapStructure(QuestStructureTags.tagFor(structureId), origin, 100, false);
        if (nearest == null) {
            return null;
        }
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, nearest.getX(), nearest.getZ());
        return new BlockPos(nearest.getX(), y, nearest.getZ());
    }

    public static void sendKazumiDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6[Kazumi] §fSatoko disappeared last night... please help me find her."));
        player.sendSystemMessage(Component.literal("§6[Kazumi] §fStay close. I will show you where she vanished."));
    }

    public static void sendSwampDemonEncounterDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§c[Numa] §fHeh... another one has come to be devoured..."));
        player.sendSystemMessage(Component.literal("§c[Numa] §fYou Demon Slayers are always so predictable..."));
    }

    public static void sendSwampDomainDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6[Player] §fHey! You're wearing Satoko's bow in your hair!"));
        player.sendSystemMessage(Component.literal("§c[Numa] §fA trophy from my last meal!"));
        player.sendSystemMessage(Component.literal("§6[Player] §fWhere is Satoko?"));
        player.sendSystemMessage(Component.literal("§c[Numa] §fYou're too late! I've already eaten her!"));
        player.sendSystemMessage(Component.literal("§c[Numa] §fI keep artifacts from all my victims! I think I'll keep your sword!"));
        player.sendSystemMessage(Component.literal("§6[Player] §fNot if I kill you first!"));
        player.sendSystemMessage(Component.literal("§c[Numa] §fYou think you can defeat me in MY domain?!"));
        player.sendSystemMessage(Component.literal("§c[Numa] §fI'll drag you into the swamp just like the others!"));
    }

    public static void sendSwampDemonLowHealthDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§c[Numa] §fImpossible... how can a human... be this strong...?!"));
    }

    public static void sendKazumiReturnDialogue(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6[Kazumi] §fYou're back...! Did you find her?!"));
    }

    public static void markKidnappersBogActive(ServerPlayer player, QuestRuntimeContext context) {
        player.getPersistentData().putBoolean(KIDNAPPERS_BOG_ACTIVE_TAG, true);
        player.getPersistentData().putBoolean(SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG, false);
    }

    public static void markSwampDomainEncounterStarted(ServerPlayer player, QuestRuntimeContext context) {
        if (!player.getPersistentData().getBoolean(SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG)) {
            player.getPersistentData().putBoolean(SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG, true);
            sendSwampDomainDialogue(player);
        }
    }

    public static void clearKidnappersBogFlags(ServerPlayer player, QuestRuntimeContext context) {
        player.getPersistentData().remove(KIDNAPPERS_BOG_ACTIVE_TAG);
        player.getPersistentData().remove(SWAMP_DOMAIN_ENCOUNTER_STARTED_TAG);
    }

    public static boolean hasSatokosBow(ServerPlayer player, QuestRuntimeContext context) {
        net.minecraft.world.item.Item bowItem = ForgeRegistries.ITEMS.getValue(SATOKOS_BOW);
        if (bowItem == null) {
            return false;
        }
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.is(bowItem)) {
                return true;
            }
        }
        return false;
    }

    public static void giveSatokosBowToKazumi(ServerPlayer player, QuestRuntimeContext context) {
        net.minecraft.world.item.Item bowItem = ForgeRegistries.ITEMS.getValue(SATOKOS_BOW);
        if (bowItem == null) {
            return;
        }
        // Remove one Satoko's Bow from player inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(bowItem)) {
                stack.shrink(1);
                player.getInventory().setChanged();
                break;
            }
        }
        player.sendSystemMessage(Component.literal("§6[Kazumi] §f...That's... Satoko's..."));
        player.sendSystemMessage(Component.literal("§6[Kazumi] §f...She's gone... isn't she...?"));
        player.sendSystemMessage(Component.literal("§6[Kazumi] §fThank you... for finding the truth..."));
    }

    private static BlockPos findRandomSurfacePosition(ServerLevel level, BlockPos center, int radius, int attempts) {
        BlockPos fallback = center;
        for (int i = 0; i < attempts; i++) {
            int x = center.getX() + level.random.nextInt(radius * 2 + 1) - radius;
            int z = center.getZ() + level.random.nextInt(radius * 2 + 1) - radius;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).isSolid() && level.getBlockState(pos).isAir()) {
                return pos;
            }
            fallback = pos;
        }
        return fallback;
    }
}
