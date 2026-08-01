package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.BridgerBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeMovement;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgePlacementResolver;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeType;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.BridgeTypes;
import com.lerdorf.kimetsunoyaibamultiplayer.bridges.EndcapPreviewMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class BridgerPreviewManager {
    private static final String PREVIEW_ENTITY_TAG = "KimetsuBridgerPreview";
    private static final Map<BlockPos, List<Integer>> PREVIEWS = new HashMap<>();
    private static final Map<ResourceLocation, BridgePlacementResolver.SourceStructure> STRUCTURE_CACHE = new HashMap<>();
    private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(-2_000_000_000);
    private static Constructor<FallingBlockEntity> fallingBlockConstructor;
    private static Field fallingBlockTimeField;

    private BridgerPreviewManager() {
    }

    public static void refresh(BridgerBlockEntity bridger) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        BlockPos origin = bridger.getBlockPos();
        clear(origin);
        if (!bridger.isPreviewEnabled()) {
            return;
        }

        BridgeType type = BridgeTypes.getOrDefault(bridger.getBridgeType());
        BridgePlacementResolver.SourceStructure structure = load(BridgeTypes.segmentStructure(type, bridger.getMovement()));
        if (structure == null || structure.blocks().isEmpty()) {
            return;
        }

        List<Integer> entityIds = new ArrayList<>();
        for (BridgePlacementResolver.PlacedBlock block : BridgePlacementResolver.resolveRepeats(
            origin, bridger.getFacing(), bridger.getMovement(), type, structure, bridger.getPreviewLength())) {
            spawn(minecraft.level, block.pos(), block.state(), entityIds);
        }

        BridgePlacementResolver.SourceStructure endcap = loadPreviewEndcap(type, bridger.getMovement(), bridger.getEndcapPreviewMode());
        if (endcap != null) {
            boolean shortEndcap = bridger.getEndcapPreviewMode() == EndcapPreviewMode.SHORT;
            for (BridgePlacementResolver.PlacedBlock block : BridgePlacementResolver.resolveEndcap(
                origin, bridger.getFacing(), bridger.getMovement(), type, structure, endcap, bridger.getPreviewLength(), shortEndcap)) {
                spawn(minecraft.level, block.pos(), block.state(), entityIds);
            }
        }

        if (!entityIds.isEmpty()) {
            PREVIEWS.put(origin.immutable(), entityIds);
        }
    }

    public static void clear(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        List<Integer> entityIds = PREVIEWS.remove(pos.immutable());
        if (level == null || entityIds == null) {
            return;
        }
        for (Integer entityId : entityIds) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static BridgePlacementResolver.SourceStructure loadPreviewEndcap(BridgeType type, BridgeMovement movement,
                                                                             EndcapPreviewMode mode) {
        if (mode == EndcapPreviewMode.NONE) {
            return null;
        }

        return mode == EndcapPreviewMode.SHORT
            ? loadShortEndcap(type, movement)
            : load(BridgeTypes.endStructure(type, movement));
    }

    private static void spawn(ClientLevel level, BlockPos pos, BlockState state, List<Integer> entityIds) {
        if (state.isAir()) {
            return;
        }
        FallingBlockEntity entity = createFallingBlock(level, pos, state);
        if (entity == null) {
            return;
        }
        int id = NEXT_ENTITY_ID.getAndIncrement();
        entity.setId(id);
        entity.noPhysics = true;
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        markPreviewEntity(entity);
        setPreviewLifetime(entity);
        level.putNonPlayerEntity(id, entity);
        entityIds.add(id);
    }

    public static boolean isPreviewEntity(Entity entity) {
        return entity.getPersistentData().getBoolean(PREVIEW_ENTITY_TAG);
    }

    private static void markPreviewEntity(Entity entity) {
        entity.getPersistentData().putBoolean(PREVIEW_ENTITY_TAG, true);
    }

    private static FallingBlockEntity createFallingBlock(ClientLevel level, BlockPos pos, BlockState state) {
        try {
            if (fallingBlockConstructor == null) {
                fallingBlockConstructor = FallingBlockEntity.class.getDeclaredConstructor(Level.class, double.class, double.class, double.class, BlockState.class);
                fallingBlockConstructor.setAccessible(true);
            }
            return fallingBlockConstructor.newInstance(level, pos.getX() + 0.5D, (double) pos.getY(), pos.getZ() + 0.5D, state);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static void setPreviewLifetime(FallingBlockEntity entity) {
        try {
            if (fallingBlockTimeField == null) {
                fallingBlockTimeField = findFallingBlockTimeField();
            }
            fallingBlockTimeField.setInt(entity, -1_000_000);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static Field findFallingBlockTimeField() {
        try {
            return ObfuscationReflectionHelper.findField(FallingBlockEntity.class, "f_31942_");
        } catch (RuntimeException ignored) {
            try {
                Field field = FallingBlockEntity.class.getDeclaredField("time");
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }

    private static BridgePlacementResolver.SourceStructure loadShortEndcap(BridgeType type, BridgeMovement movement) {
        BridgePlacementResolver.SourceStructure structure = load(BridgeTypes.shortEndStructure(type, movement));
        if (structure != null) {
            return structure;
        }
        ResourceLocation alternate = BridgeTypes.alternateShortEndStructure(type, movement);
        if (!alternate.equals(BridgeTypes.shortEndStructure(type, movement))) {
            return load(alternate);
        }
        return null;
    }

    private static BridgePlacementResolver.SourceStructure load(ResourceLocation structureId) {
        if (STRUCTURE_CACHE.containsKey(structureId)) {
            return STRUCTURE_CACHE.get(structureId);
        }

        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(structureId.getNamespace(), "structures/" + structureId.getPath() + ".nbt");
        try {
            Optional<Resource> resource = minecraft.getResourceManager().getResource(file);
            if (resource.isEmpty()) {
                STRUCTURE_CACHE.put(structureId, null);
                return null;
            }
            try (InputStream stream = resource.get().open()) {
                CompoundTag tag = NbtIo.readCompressed(stream);
                BridgePlacementResolver.SourceStructure structure = parse(tag);
                STRUCTURE_CACHE.put(structureId, structure);
                return structure;
            }
        } catch (Exception ignored) {
            STRUCTURE_CACHE.put(structureId, null);
            return null;
        }
    }

    private static BridgePlacementResolver.SourceStructure parse(CompoundTag tag) {
        ListTag sizeTag = tag.getList("size", 3);
        BlockPos size = new BlockPos(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2));
        List<BlockState> palette = parsePalette(tag.getList("palette", 10));
        List<BridgePlacementResolver.SourceBlock> blocks = new ArrayList<>();
        ListTag blockList = tag.getList("blocks", 10);
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag blockTag = blockList.getCompound(i);
            ListTag posTag = blockTag.getList("pos", 3);
            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.size()) {
                continue;
            }
            BlockState state = palette.get(stateIndex);
            if (!state.isAir()) {
                blocks.add(new BridgePlacementResolver.SourceBlock(
                    new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2)),
                    state,
                    null
                ));
            }
        }
        return new BridgePlacementResolver.SourceStructure(size, blocks);
    }

    private static List<BlockState> parsePalette(ListTag paletteTag) {
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            ResourceLocation blockId = ResourceLocation.tryParse(stateTag.getString("Name"));
            Block block = blockId == null ? Blocks.AIR : ForgeRegistries.BLOCKS.getValue(blockId);
            BlockState state = block == null ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
            if (stateTag.contains("Properties", 10)) {
                state = applyProperties(state, stateTag.getCompound("Properties"));
            }
            palette.add(state);
        }
        return palette;
    }

    private static BlockState applyProperties(BlockState state, CompoundTag propertiesTag) {
        for (String key : propertiesTag.getAllKeys()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
            if (property != null) {
                state = setProperty(state, property, propertiesTag.getString(key));
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(t -> state.setValue(property, t)).orElse(state);
    }
}
