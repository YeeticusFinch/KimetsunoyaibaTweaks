package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = KimetsunoyaibaMultiplayer.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AlchemyInteractionHandler {
    private AlchemyInteractionHandler() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            MicroscopeStationSavedData.get(serverLevel).tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
            && BloodDemonArtAlchemyCatalog.isMicroscopeBlock(serverLevel.getBlockState(event.getPos()))) {
            MicroscopeStationSavedData.dropAndClear(serverLevel, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return;
        }

        if (BloodDemonArtAlchemyCatalog.isMicroscopeBlock(event.getLevel().getBlockState(event.getPos()))) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) -> new MicroscopeMenu(containerId, inventory, event.getPos()),
                        Component.translatable("menu.kimetsunoyaibamultiplayer.microscope")),
                    event.getPos());
                event.getLevel().playSound(null, event.getPos(), SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 0.8F, 0.65F);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
                return;
            }
        }

        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.GRINDSTONE)) {
            return;
        }

        ItemStack held = event.getItemStack();
        ItemStack output = ItemStack.EMPTY;
        if (held.is(Items.BONE)) {
            output = new ItemStack(ModAlchemyItems.BONE_DUST.get(), 1);
        } else if (held.is(Items.CALCITE)) {
            output = new ItemStack(ModAlchemyItems.CALCITE_POWDER.get(), 1);
        }

        if (output.isEmpty()) {
            return;
        }

        held.shrink(1);
        ItemHandlerHelper.giveItemToPlayer(event.getEntity(), output);
        event.getLevel().playSound(null, event.getPos(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }
}
