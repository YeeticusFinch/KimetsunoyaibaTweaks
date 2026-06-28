package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyBlocks;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.ModAlchemyItems;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.VialRackBlock;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.VialRackBlockItem;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.VialRackContents;
import com.lerdorf.kimetsunoyaibamultiplayer.blocks.entity.VialRackBlockEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBlocksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID)
public final class EnhancedVialRackHandler {
    private static final ResourceLocation BASE_MEDICINE_HOLDER_ID =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "medicine_holder");
    private static final int BLOCK_SCAN_INTERVAL_TICKS = 200;
    private static final int BLOCK_SCAN_RADIUS = 5;
    private static final int EMPTY_VIAL_WEIGHT = 5;

    private static final List<RegistryObject<Item>> RANDOM_VIALS = List.of(
        ModAlchemyItems.EMPTY_VIAL,
        ModAlchemyItems.BLOOD_SAMPLE,
        ModAlchemyItems.ROTTEN_BLOOD_SAMPLE,
        ModAlchemyItems.CRUDE_VIAL,
        ModAlchemyItems.REFINED_VIAL,
        ModAlchemyItems.CRUEL_VIAL
    );

    private EnhancedVialRackHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;
        if (player.level().isClientSide() || !EnhancedBlocksConfig.enhancedVialRack) {
            return;
        }

        replaceInventoryMedicineHolders(player);
        if (player.tickCount % BLOCK_SCAN_INTERVAL_TICKS == 0 && player.level() instanceof ServerLevel serverLevel) {
            replaceNearbyMedicineHolders(serverLevel, player);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        Level level = event.getLevel();
        if (level.isClientSide() || !EnhancedBlocksConfig.enhancedVialRack) {
            return;
        }

        if (replaceMedicineHolderBlock(level, event.getPos(), level.random)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void replaceInventoryMedicineHolders(Player player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> overflow = new ArrayList<>();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isMedicineHolder(stack)) {
                continue;
            }

            int count = stack.getCount();
            inventory.setItem(slot, new ItemStack(ModAlchemyBlocks.VIAL_RACK.get().asItem()));
            for (int i = 1; i < count; i++) {
                overflow.add(new ItemStack(ModAlchemyBlocks.VIAL_RACK.get().asItem()));
            }
        }

        for (ItemStack stack : overflow) {
            if (!inventory.add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static void replaceNearbyMedicineHolders(ServerLevel level, Player player) {
        BlockPos center = player.blockPosition();
        Vec3 playerPosition = player.position();
        BlockPos.betweenClosedStream(
                center.offset(-BLOCK_SCAN_RADIUS, -BLOCK_SCAN_RADIUS, -BLOCK_SCAN_RADIUS),
                center.offset(BLOCK_SCAN_RADIUS, BLOCK_SCAN_RADIUS, BLOCK_SCAN_RADIUS))
            .filter(pos -> pos.getCenter().distanceToSqr(playerPosition) <= BLOCK_SCAN_RADIUS * BLOCK_SCAN_RADIUS)
            .forEach(pos -> replaceMedicineHolderBlock(level, pos, level.random));
    }

    private static boolean replaceMedicineHolderBlock(Level level, BlockPos pos, RandomSource random) {
        BlockState currentState = level.getBlockState(pos);
        Block currentBlock = currentState.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(currentBlock);
        if (!BASE_MEDICINE_HOLDER_ID.equals(blockId)) {
            return false;
        }

        int rackCount = randomRackCount(random);
        BlockState replacementState = ModAlchemyBlocks.VIAL_RACK.get()
            .defaultBlockState()
            .setValue(VialRackBlock.ROTATION, random.nextInt(8));
        if (!level.setBlock(pos, replacementState, 3)) {
            return false;
        }

        if (level.getBlockEntity(pos) instanceof VialRackBlockEntity rack) {
            rack.loadFromItem(randomRackStack(random));
            for (int i = 1; i < rackCount; i++) {
                rack.addRack(randomRackStack(random));
            }
        }
        return true;
    }

    private static int randomRackCount(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.6F) {
            return 1;
        }
        return roll < 0.9F ? 2 : 3;
    }

    private static ItemStack randomRackStack(RandomSource random) {
        ItemStack stack = new ItemStack(ModAlchemyBlocks.VIAL_RACK.get().asItem());
        ItemStack[] contents = new ItemStack[VialRackContents.SLOT_COUNT];
        for (int slot = 0; slot < contents.length; slot++) {
            contents[slot] = random.nextBoolean() ? randomVial(random) : ItemStack.EMPTY;
        }
        VialRackBlockItem.setStoredItems(stack, contents);
        return stack;
    }

    private static ItemStack randomVial(RandomSource random) {
        int totalWeight = EMPTY_VIAL_WEIGHT + RANDOM_VIALS.size() - 1;
        int roll = random.nextInt(totalWeight);
        if (roll < EMPTY_VIAL_WEIGHT) {
            return new ItemStack(ModAlchemyItems.EMPTY_VIAL.get());
        }
        return new ItemStack(RANDOM_VIALS.get(roll - EMPTY_VIAL_WEIGHT + 1).get());
    }

    private static boolean isMedicineHolder(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return BASE_MEDICINE_HOLDER_ID.equals(itemId);
    }
}
