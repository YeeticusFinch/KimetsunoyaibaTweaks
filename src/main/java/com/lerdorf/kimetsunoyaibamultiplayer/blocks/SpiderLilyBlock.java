package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyItems;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.WisteriaIncenseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SpiderLilyBlock extends FlowerBlock {
    public static final int BLOOM_SEASON_LENGTH_DAYS = 32;
    public static final int BLOOM_SEASON_DAYS = 2;
    public static final int BLOOM_START_TICK = 1200;
    public static final int BLOOM_END_TICK = 5000;
    private static final int BLOOM_CHECK_INTERVAL_TICKS = 1200;
    public static final int TICKS_PER_DAY = 24000;
    private static final int WISTERIA_SEARCH_RADIUS = 6;
    private static final int WISTERIA_INCENSE_SEARCH_RADIUS = 6;
    private static final int MAX_INCENSE_BLOOM_BONUS_COUNT = 4;

    private static final ResourceLocation WISTERIA_FOREST =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "wisteria_forest");
    private static final ResourceLocation WISTERIA_FOREST_CYAN =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "wisteria_forest_cyan");
    private static final ResourceLocation WISTERIA_FOREST_CREAM =
        ResourceLocation.fromNamespaceAndPath(KimetsunoyaibaMultiplayer.MODID, "wisteria_forest_cream");

    private final boolean blooms;
    private final boolean unblooms;
    private final boolean waxed;

    public SpiderLilyBlock(boolean blooms, Supplier<MobEffect> suspiciousEffect, int effectDuration, Properties properties) {
        this(blooms, false, suspiciousEffect, effectDuration, properties);
    }

    public SpiderLilyBlock(boolean blooms, boolean waxed, Supplier<MobEffect> suspiciousEffect, int effectDuration, Properties properties) {
        super(suspiciousEffect, effectDuration, properties);
        this.waxed = waxed;
        this.blooms = !waxed && blooms;
        this.unblooms = !waxed && !blooms;
    }

    public SpiderLilyBlock(boolean blooms, Properties properties) {
        this(blooms, () -> MobEffects.CONFUSION, 5, properties);
    }

    public SpiderLilyBlock(boolean blooms, boolean waxed, Properties properties) {
        this(blooms, waxed, () -> MobEffects.CONFUSION, 5, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if ((this.blooms || this.unblooms) && !level.isClientSide && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, BLOOM_CHECK_INTERVAL_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        if (tryUnbloom(state, level, pos)) {
            return;
        }
        if (tryBloom(state, level, pos, random)) {
            return;
        }
        level.scheduleTick(pos, this, BLOOM_CHECK_INTERVAL_TICKS);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (tryUnbloom(state, level, pos)) {
            return;
        }
        tryBloom(state, level, pos, random);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!waxed && stack.is(Items.HONEYCOMB)) {
            BlockState waxedState = waxedStateFor(state.getBlock());
            if (waxedState == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, waxedState, Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D, 8, 0.25D, 0.25D, 0.25D, 0.0D);
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!waxed && stack.is(Items.SHEARS) && state.is(ModBlocks.BLUE_SPIDER_LILY.get())) {
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                int petalCount = 1 + serverLevel.random.nextInt(3);
                popResource(serverLevel, pos, new ItemStack(ModAlchemyItems.BLUE_SPIDER_LILY_PETALS.get(), petalCount));
                recordUnbloomLock(serverLevel, pos);
                level.setBlock(pos, ModBlocks.SPIDER_LILY.get().defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D, 8, 0.25D, 0.25D, 0.25D, 0.0D);
                stack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (waxed && stack.is(Items.SHEARS)) {
            BlockState unwaxedState = unwaxedStateFor(state.getBlock());
            if (unwaxedState == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, unwaxedState, Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D, 8, 0.25D, 0.25D, 0.25D, 0.0D);
                }
                stack.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private boolean tryBloom(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!this.blooms || isUnbloomLocked(level, pos) || !canBloom(level, pos) || random.nextFloat() >= 0.05F) {
            return false;
        }

        level.setBlock(pos, selectBloomedState(level, pos, random), Block.UPDATE_ALL);
        return true;
    }

    private boolean tryUnbloom(BlockState state, ServerLevel level, BlockPos pos) {
        if (!this.unblooms || canBloom(level, pos)) {
            return false;
        }

        recordUnbloomLock(level, pos);
        level.setBlock(pos, ModBlocks.SPIDER_LILY.get().defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    private static boolean canBloom(ServerLevel level, BlockPos pos) {
        if (!isBloomSeason(level)) {
            return false;
        }

        return isBloomWindow(level)
            && level.canSeeSky(pos.above())
            && level.getBrightness(LightLayer.SKY, pos.above()) >= 15;
    }

    public static boolean isBloomSeason(Level level) {
        return getBloomSeasonDay(level) < BLOOM_SEASON_DAYS;
    }

    public static boolean isBloomWindow(Level level) {
        long timeOfDay = Math.floorMod(level.getDayTime(), TICKS_PER_DAY);
        return timeOfDay >= BLOOM_START_TICK && timeOfDay <= BLOOM_END_TICK;
    }

    public static long daysUntilNextBloomSeason(Level level) {
        long dayInSeason = getBloomSeasonDay(level);
        return dayInSeason < BLOOM_SEASON_DAYS ? 0L : BLOOM_SEASON_LENGTH_DAYS - dayInSeason;
    }

    public static long getBloomSeasonDay(Level level) {
        long day = level.getDayTime() / TICKS_PER_DAY;
        return Math.floorMod(day, BLOOM_SEASON_LENGTH_DAYS);
    }

    private static boolean isUnbloomLocked(ServerLevel level, BlockPos pos) {
        return UnbloomLockData.get(level).isLocked(level, pos, getCurrentDay(level));
    }

    private static void recordUnbloomLock(ServerLevel level, BlockPos pos) {
        UnbloomLockData.get(level).record(level, pos, getCurrentDay(level));
    }

    private static long getCurrentDay(Level level) {
        return level.getDayTime() / TICKS_PER_DAY;
    }

    public static Block waxedBlockFor(Block block) {
        if (block == ModBlocks.SPIDER_LILY.get()) {
            return ModBlocks.WAXED_SPIDER_LILY.get();
        }
        if (block == ModBlocks.WHITE_SPIDER_LILY.get()) {
            return ModBlocks.WAXED_WHITE_SPIDER_LILY.get();
        }
        if (block == ModBlocks.RED_SPIDER_LILY.get()) {
            return ModBlocks.WAXED_RED_SPIDER_LILY.get();
        }
        if (block == ModBlocks.PURPLE_SPIDER_LILY.get()) {
            return ModBlocks.WAXED_PURPLE_SPIDER_LILY.get();
        }
        if (block == ModBlocks.YELLOW_SPIDER_LILY.get()) {
            return ModBlocks.WAXED_YELLOW_SPIDER_LILY.get();
        }
        if (block == ModBlocks.BLUE_SPIDER_LILY.get()) {
            return ModBlocks.WAXED_BLUE_SPIDER_LILY.get();
        }
        if (block == ModBlocks.LIME_SPIDER_LILY.get()) {
            return ModBlocks.WAXED_LIME_SPIDER_LILY.get();
        }
        if (block == ModBlocks.PINK_SPIDER_LILY.get()) {
            return ModBlocks.WAXED_PINK_SPIDER_LILY.get();
        }
        if (block == ModBlocks.ORANGE_SPIDER_LILY.get()) {
            return ModBlocks.WAXED_ORANGE_SPIDER_LILY.get();
        }
        return null;
    }

    public static Block unwaxedBlockFor(Block block) {
        if (block == ModBlocks.WAXED_SPIDER_LILY.get()) {
            return ModBlocks.SPIDER_LILY.get();
        }
        if (block == ModBlocks.WAXED_WHITE_SPIDER_LILY.get()) {
            return ModBlocks.WHITE_SPIDER_LILY.get();
        }
        if (block == ModBlocks.WAXED_RED_SPIDER_LILY.get()) {
            return ModBlocks.RED_SPIDER_LILY.get();
        }
        if (block == ModBlocks.WAXED_PURPLE_SPIDER_LILY.get()) {
            return ModBlocks.PURPLE_SPIDER_LILY.get();
        }
        if (block == ModBlocks.WAXED_YELLOW_SPIDER_LILY.get()) {
            return ModBlocks.YELLOW_SPIDER_LILY.get();
        }
        if (block == ModBlocks.WAXED_BLUE_SPIDER_LILY.get()) {
            return ModBlocks.BLUE_SPIDER_LILY.get();
        }
        if (block == ModBlocks.WAXED_LIME_SPIDER_LILY.get()) {
            return ModBlocks.LIME_SPIDER_LILY.get();
        }
        if (block == ModBlocks.WAXED_PINK_SPIDER_LILY.get()) {
            return ModBlocks.PINK_SPIDER_LILY.get();
        }
        if (block == ModBlocks.WAXED_ORANGE_SPIDER_LILY.get()) {
            return ModBlocks.ORANGE_SPIDER_LILY.get();
        }
        return null;
    }

    private static BlockState waxedStateFor(Block block) {
        Block waxedBlock = waxedBlockFor(block);
        return waxedBlock == null ? null : waxedBlock.defaultBlockState();
    }

    private static BlockState unwaxedStateFor(Block block) {
        Block unwaxedBlock = unwaxedBlockFor(block);
        return unwaxedBlock == null ? null : unwaxedBlock.defaultBlockState();
    }

    private static BlockState selectBloomedState(ServerLevel level, BlockPos pos, RandomSource random) {
        int blueChance = 2;
        if (pos.getY() > 120) {
            blueChance += 1;
        }
        if (isInWisteriaForest(level, pos) || isNearWisteria(level, pos)) {
            blueChance += 3;
        }
        int blueOrPurpleIncenseChance = countNearbyLitWisteriaIncense(level, pos) * 4;

        int roll = random.nextInt(100);

        if (roll < blueChance) {
            return ModBlocks.BLUE_SPIDER_LILY.get().defaultBlockState();
        }

        roll -= blueChance;
        if (roll < blueOrPurpleIncenseChance) {
            return random.nextBoolean()
                ? ModBlocks.BLUE_SPIDER_LILY.get().defaultBlockState()
                : ModBlocks.PURPLE_SPIDER_LILY.get().defaultBlockState();
        }

        roll -= blueOrPurpleIncenseChance;
        if (roll < 40) {
            return ModBlocks.RED_SPIDER_LILY.get().defaultBlockState();
        }

        roll -= 40;
        if (roll < 18) {
            return ModBlocks.WHITE_SPIDER_LILY.get().defaultBlockState();
        }

        roll -= 18;
        if (roll < 18) {
            return ModBlocks.YELLOW_SPIDER_LILY.get().defaultBlockState();
        }

        roll -= 18;
        int rareVariant = roll % 4;
        if (rareVariant == 0) {
            return ModBlocks.LIME_SPIDER_LILY.get().defaultBlockState();
        }
        if (rareVariant == 1) {
            return ModBlocks.PINK_SPIDER_LILY.get().defaultBlockState();
        }
        if (rareVariant == 2) {
            return ModBlocks.ORANGE_SPIDER_LILY.get().defaultBlockState();
        }
        return ModBlocks.PURPLE_SPIDER_LILY.get().defaultBlockState();
    }

    private static boolean isInWisteriaForest(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        ResourceKey<Biome> biomeKey = biome.unwrapKey().orElse(null);
        if (biomeKey == null) {
            return false;
        }

        ResourceLocation biomeLoc = biomeKey.location();
        return biomeLoc.equals(WISTERIA_FOREST)
            || biomeLoc.equals(WISTERIA_FOREST_CYAN)
            || biomeLoc.equals(WISTERIA_FOREST_CREAM);
    }

    private static boolean isNearWisteria(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int x = -WISTERIA_SEARCH_RADIUS; x <= WISTERIA_SEARCH_RADIUS; x++) {
            for (int y = -WISTERIA_SEARCH_RADIUS; y <= WISTERIA_SEARCH_RADIUS; y++) {
                for (int z = -WISTERIA_SEARCH_RADIUS; z <= WISTERIA_SEARCH_RADIUS; z++) {
                    checkPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (isWisteriaBlock(level.getBlockState(checkPos))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int countNearbyLitWisteriaIncense(ServerLevel level, BlockPos pos) {
        int incenseCount = 0;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int x = -WISTERIA_INCENSE_SEARCH_RADIUS; x <= WISTERIA_INCENSE_SEARCH_RADIUS; x++) {
            for (int y = -WISTERIA_INCENSE_SEARCH_RADIUS; y <= WISTERIA_INCENSE_SEARCH_RADIUS; y++) {
                for (int z = -WISTERIA_INCENSE_SEARCH_RADIUS; z <= WISTERIA_INCENSE_SEARCH_RADIUS; z++) {
                    checkPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    BlockState state = level.getBlockState(checkPos);
                    if (!(state.getBlock() instanceof WisteriaIncenseBlock) || !state.getValue(WisteriaIncenseBlock.LIT)) {
                        continue;
                    }

                    int count = state.getValue(WisteriaIncenseBlock.COUNT);
                    double radius = 2.0D + count;
                    if (isWithinIncenseRadius(pos, checkPos, radius)) {
                        incenseCount += count;
                        if (incenseCount >= MAX_INCENSE_BLOOM_BONUS_COUNT) {
                            return MAX_INCENSE_BLOOM_BONUS_COUNT;
                        }
                    }
                }
            }
        }
        return incenseCount;
    }

    private static boolean isWithinIncenseRadius(BlockPos pos, BlockPos incensePos, double radius) {
        return Math.abs(pos.getX() - incensePos.getX()) <= radius
            && Math.abs(pos.getY() - incensePos.getY()) <= radius
            && Math.abs(pos.getZ() - incensePos.getZ()) <= radius;
    }

    private static boolean isWisteriaBlock(BlockState state) {
        return state.is(ModBlocks.WISTERIA_LEAVES.get())
            || state.is(ModBlocks.WISTERIA_LEAVES_PINK.get())
            || state.is(ModBlocks.WISTERIA_LEAVES_CYAN.get())
            || state.is(ModBlocks.WISTERIA_LEAVES_LAVENDER.get())
            || state.is(ModBlocks.WISTERIA_LEAVES_CREAM.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_PINK.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CYAN.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_LAVENDER.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_LEAVES_CREAM.get())
            || state.is(ModBlocks.WISTERIA_PETALS.get())
            || state.is(ModBlocks.WISTERIA_PETALS_PINK.get())
            || state.is(ModBlocks.WISTERIA_PETALS_CYAN.get())
            || state.is(ModBlocks.WISTERIA_PETALS_LAVENDER.get())
            || state.is(ModBlocks.WISTERIA_PETALS_CREAM.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_PETALS_PINK.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_PETALS_CYAN.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_PETALS_LAVENDER.get())
            || state.is(ModBlocks.GLOWING_WISTERIA_PETALS_CREAM.get());
    }

    private static class UnbloomLockData extends SavedData {
        private static final String DATA_NAME = "kny_spider_lily_unbloom_locks";
        private static final String LOCKS_KEY = "locks";
        private static final String DIMENSION_KEY = "dimension";
        private static final String POS_KEY = "pos";
        private static final String DAY_KEY = "day";

        private final Map<String, Map<Long, Long>> locksByDimension = new HashMap<>();

        static UnbloomLockData get(ServerLevel level) {
            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            ServerLevel storageLevel = overworld == null ? level : overworld;
            return storageLevel.getDataStorage().computeIfAbsent(
                UnbloomLockData::load,
                UnbloomLockData::new,
                DATA_NAME
            );
        }

        static UnbloomLockData load(CompoundTag tag) {
            UnbloomLockData data = new UnbloomLockData();
            if (tag.contains(LOCKS_KEY, Tag.TAG_LIST)) {
                ListTag locks = tag.getList(LOCKS_KEY, Tag.TAG_COMPOUND);
                for (int i = 0; i < locks.size(); i++) {
                    CompoundTag entry = locks.getCompound(i);
                    String dimension = entry.getString(DIMENSION_KEY);
                    long pos = entry.getLong(POS_KEY);
                    long day = entry.getLong(DAY_KEY);
                    data.locksByDimension.computeIfAbsent(dimension, ignored -> new HashMap<>()).put(pos, day);
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag locks = new ListTag();
            for (Map.Entry<String, Map<Long, Long>> dimensionEntry : locksByDimension.entrySet()) {
                for (Map.Entry<Long, Long> lockEntry : dimensionEntry.getValue().entrySet()) {
                    CompoundTag entry = new CompoundTag();
                    entry.putString(DIMENSION_KEY, dimensionEntry.getKey());
                    entry.putLong(POS_KEY, lockEntry.getKey());
                    entry.putLong(DAY_KEY, lockEntry.getValue());
                    locks.add(entry);
                }
            }
            tag.put(LOCKS_KEY, locks);
            return tag;
        }

        boolean isLocked(ServerLevel level, BlockPos pos, long currentDay) {
            Map<Long, Long> locks = locksByDimension.get(dimensionName(level));
            if (locks == null) {
                return false;
            }

            long posKey = pos.asLong();
            Long unbloomDay = locks.get(posKey);
            if (unbloomDay == null) {
                return false;
            }
            if (unbloomDay == currentDay) {
                return true;
            }

            locks.remove(posKey);
            if (locks.isEmpty()) {
                locksByDimension.remove(dimensionName(level));
            }
            setDirty();
            return false;
        }

        void record(ServerLevel level, BlockPos pos, long currentDay) {
            locksByDimension
                .computeIfAbsent(dimensionName(level), ignored -> new HashMap<>())
                .put(pos.asLong(), currentDay);
            setDirty();
        }

        private static String dimensionName(ServerLevel level) {
            return level.dimension().location().toString();
        }
    }
}
