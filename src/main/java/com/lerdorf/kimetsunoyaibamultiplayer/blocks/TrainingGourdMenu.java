package com.lerdorf.kimetsunoyaibamultiplayer.blocks;

import com.lerdorf.kimetsunoyaibamultiplayer.Damager;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.items.TrainingGourdItem;
import com.lerdorf.kimetsunoyaibamultiplayer.progression.DemonSlayerRank;
import com.lerdorf.kimetsunoyaibamultiplayer.progression.DemonSlayerRankManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public class TrainingGourdMenu extends AbstractContainerMenu {
    private static final int BREATHES_DATA = 0;
    private static final int MAX_BREATHES_DATA = 1;
    private static final int REMAINING_TICKS_DATA = 2;
    private static final int DATA_COUNT = 3;

    @Nullable
    private final BlockPos blockPos;
    @Nullable
    private final InteractionHand hand;
    private final TrainingGourdSize gourdSize;
    private final TrainingGourdDifficulty difficulty;
    private final ContainerData data;
    private int breaths;
    private int remainingTicks;
    private int timerTickCounter;

    private TrainingGourdMenu(int containerId, Inventory inventory, @Nullable BlockPos blockPos,
                              @Nullable InteractionHand hand, TrainingGourdSize gourdSize,
                              TrainingGourdDifficulty difficulty) {
        super(ModMenus.TRAINING_GOURD.get(), containerId);
        this.blockPos = blockPos;
        this.hand = hand;
        this.gourdSize = gourdSize;
        this.difficulty = difficulty;
        this.breaths = 0;
        this.remainingTicks = difficulty.getSeconds() * 20;
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case BREATHES_DATA -> breaths;
                    case MAX_BREATHES_DATA -> gourdSize.getRequiredBreaths();
                    case REMAINING_TICKS_DATA -> remainingTicks;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case BREATHES_DATA -> breaths = value;
                    case REMAINING_TICKS_DATA -> remainingTicks = value;
                    default -> {
                    }
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(data);
    }

    private TrainingGourdMenu(int containerId, Inventory inventory, BlockPos blockPos,
                              TrainingGourdSize gourdSize, TrainingGourdDifficulty difficulty) {
        this(containerId, inventory, blockPos, null, gourdSize, difficulty);
    }

    private TrainingGourdMenu(int containerId, Inventory inventory, InteractionHand hand,
                              TrainingGourdSize gourdSize, TrainingGourdDifficulty difficulty) {
        this(containerId, inventory, null, hand, gourdSize, difficulty);
    }

    public static TrainingGourdMenu clientMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        boolean held = buffer.readBoolean();
        BlockPos blockPos = held ? null : buffer.readBlockPos();
        InteractionHand hand = held ? InteractionHand.values()[buffer.readVarInt()] : null;
        TrainingGourdSize size = TrainingGourdSize.fromId(buffer.readVarInt());
        TrainingGourdDifficulty difficulty = TrainingGourdDifficulty.fromId(buffer.readVarInt());
        return new TrainingGourdMenu(containerId, inventory, blockPos, hand, size, difficulty);
    }

    public TrainingGourdSize getGourdSize() {
        return gourdSize;
    }

    public ItemStack getGourdStack() {
        return new ItemStack(ModItems.getTrainingGourdItem(gourdSize, difficulty));
    }

    public int getBreaths() {
        return data.get(BREATHES_DATA);
    }

    public int getMaxBreaths() {
        return data.get(MAX_BREATHES_DATA);
    }

    public int getRemainingTicks() {
        return data.get(REMAINING_TICKS_DATA);
    }

    public int getRemainingSeconds() {
        return (getRemainingTicks() + 19) / 20;
    }

    public TrainingGourdDifficulty getDifficulty() {
        return difficulty;
    }

    public void serverTick(ServerPlayer player) {
        if (!stillValid(player)) {
            player.closeContainer();
            return;
        }
        if (--remainingTicks <= 0) {
            player.closeContainer();
            return;
        }
        if (++timerTickCounter >= 20) {
            timerTickCounter = 0;
            breaths = Math.max(0, breaths - 1);
        }
    }

    public void addBreath(ServerPlayer player) {
        if (!stillValid(player) || remainingTicks <= 0 || breaths >= getMaxBreaths()) {
            return;
        }

        DemonSlayerRank rank = DemonSlayerRankManager.getRank(player);
        int breathsPerPress;
        if (rank == null || !Damager.isDemonSlayer(player)) {
            if (!player.getRandom().nextBoolean()) {
                return;
            }
            breathsPerPress = 1;
        } else {
            breathsPerPress = switch (rank) {
                case MIZUNOTO -> 1;
                case MIZUNOE -> 2;
                case KANOTO -> 3;
                case KANOE -> 4;
                case TSUCHINOTO -> 5;
                case TSUCHINOE -> 10;
                case HINOTO -> 15;
                case HINOE -> 20;
                case KINOTO -> 25;
                case KINOE -> 30;
                case HASHIRA, STRONGEST -> 50;
            };
        }

        breaths = Math.min(getMaxBreaths(), breaths + breathsPerPress);
        if (breaths >= getMaxBreaths()) {
            shatter(player);
        }
    }

    private void shatter(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !stillValid(player)) {
            player.closeContainer();
            return;
        }

        BlockPos effectPos;
        if (blockPos != null) {
            level.removeBlock(blockPos, false);
            effectPos = blockPos;
        } else {
            player.getItemInHand(hand).shrink(1);
            effectPos = player.blockPosition();
        }

        DemonSlayerRankManager.addSlayerExperience(player, getExperienceReward());
        double x = player.getX() + player.getLookAngle().x * 1.5D;
        double y = player.getEyeY() - 0.35D + player.getLookAngle().y * 1.5D;
        double z = player.getZ() + player.getLookAngle().z * 1.5D;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CLAY.defaultBlockState()),
            x, y, z, 18, 0.35D, 0.35D, 0.35D, 0.08D);
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 6, 0.25D, 0.25D, 0.25D, 0.0D);
        level.playSound(null, effectPos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (gourdSize == TrainingGourdSize.HUGE || gourdSize == TrainingGourdSize.GARGANTUAN) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, effectPos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        player.closeContainer();
    }

    private int getExperienceReward() {
        int baseReward = switch (gourdSize) {
            case SMALL -> 2;
            case MEDIUM -> 5;
            case LARGE -> 10;
            case HUGE -> 20;
            case GARGANTUAN -> 40;
        };
        return switch (difficulty) {
            case EASY -> baseReward;
            case HARD -> baseReward + baseReward / 2;
            case EXTREME -> baseReward * 2;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        if (hand != null) {
            ItemStack stack = player.getItemInHand(hand);
            return stack.getItem() instanceof TrainingGourdItem item
                && item.getGourdSize() == gourdSize
                && item.getDifficulty() == difficulty;
        }
        return blockPos != null
            && player.level().getBlockState(blockPos).getBlock() instanceof TrainingGourdBlock block
            && block.getGourdSize() == gourdSize
            && block.getDifficulty(player.level().getBlockState(blockPos)) == difficulty
            && player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public static final class Provider implements MenuProvider {
        @Nullable
        private final BlockPos blockPos;
        @Nullable
        private final InteractionHand hand;
        private final TrainingGourdSize gourdSize;
        private final TrainingGourdDifficulty difficulty;

        public Provider(BlockPos blockPos, TrainingGourdSize gourdSize,
                        TrainingGourdDifficulty difficulty) {
            this.blockPos = blockPos;
            this.hand = null;
            this.gourdSize = gourdSize;
            this.difficulty = difficulty;
        }

        public Provider(InteractionHand hand, TrainingGourdSize gourdSize,
                        TrainingGourdDifficulty difficulty) {
            this.blockPos = null;
            this.hand = hand;
            this.gourdSize = gourdSize;
            this.difficulty = difficulty;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.kimetsunoyaibamultiplayer.training_gourd.title");
        }

        @Override
        public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return blockPos != null
                ? new TrainingGourdMenu(containerId, inventory, blockPos, gourdSize, difficulty)
                : new TrainingGourdMenu(containerId, inventory, hand, gourdSize, difficulty);
        }
    }
}
