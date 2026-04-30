package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class BloodOfMuzanReplacer {
    private static final Set<ResourceLocation> BASE_MUZAN_BLOOD_IDS = Set.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "blood_of_muzan"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "muzan_blood"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "bloodmuzan")
    );
    private static final Map<UUID, Integer> PLAYER_TICK_COUNTERS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!DemonTransformationHandler.isCustomDemonInitiationEnabled()) {
            return;
        }

        Player player = event.player;
        UUID playerId = player.getUUID();
        int currentCount = PLAYER_TICK_COUNTERS.getOrDefault(playerId, 0) + 1;
        if (currentCount < 20) {
            PLAYER_TICK_COUNTERS.put(playerId, currentCount);
            return;
        }
        PLAYER_TICK_COUNTERS.put(playerId, 0);

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            ItemStack replacement = createReplacement(stack);
            if (!replacement.isEmpty()) {
                player.getInventory().setItem(i, replacement);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !DemonTransformationHandler.isCustomDemonInitiationEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (!isBaseMuzanBlood(stack)) {
            return;
        }

        ItemStack replacement = createReplacement(stack);
        player.setItemInHand(event.getHand(), replacement);
        if (DemonTransformationHandler.consumeCustomBlood(player, replacement)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static ItemStack createReplacement(ItemStack original) {
        if (original == null || original.isEmpty() || !isBaseMuzanBlood(original) || !CustomProgressionConfig.customDemonInitiation.get()) {
            return ItemStack.EMPTY;
        }

        ItemStack replacement = new ItemStack(ModItems.BLOOD_OF_MUZAN.get(), original.getCount());
        if (original.hasTag()) {
            replacement.setTag(original.getTag().copy());
        }
        if (original.isDamaged()) {
            replacement.setDamageValue(original.getDamageValue());
        }
        return replacement;
    }

    private static boolean isBaseMuzanBlood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId != null && BASE_MUZAN_BLOOD_IDS.contains(itemId);
    }
}
