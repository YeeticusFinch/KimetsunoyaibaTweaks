package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces base mod Nezuko Blood Demon Art with enhanced Combustible Blood.
 */
@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public class CombustibleBloodReplacer {
    private static final ResourceLocation BASE_NEZUKO_BLOOD_ART =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "blooddemonart_nezuko");
    private static final Map<UUID, Integer> PLAYER_TICK_COUNTERS = new ConcurrentHashMap<>();

    private CombustibleBloodReplacer() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (!EnhancedBreathingConfig.enhancedCombustibleBlood) {
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
            ItemStack replacement = createReplacement(player.getInventory().getItem(i));
            if (!replacement.isEmpty()) {
                player.getInventory().setItem(i, replacement);
                Log.debug("[Combustible Blood Replacer] Replaced base Nezuko Blood Demon Art in slot {} for {}",
                    i, player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !EnhancedBreathingConfig.enhancedCombustibleBlood) {
            return;
        }

        Player player = event.getEntity();
        ItemStack replacement = createReplacement(player.getItemInHand(event.getHand()));
        if (replacement.isEmpty()) {
            return;
        }

        player.setItemInHand(event.getHand(), replacement);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Log.debug("[Combustible Blood Replacer] Replaced base Nezuko Blood Demon Art in hand for {}",
            player.getName().getString());
    }

    private static ItemStack createReplacement(ItemStack original) {
        if (original == null || original.isEmpty() || !isBaseNezukoBloodArt(original)) {
            return ItemStack.EMPTY;
        }

        ItemStack replacement = new ItemStack(ModItems.COMBUSTIBLE_BLOOD.get(), original.getCount());
        if (original.hasTag()) {
            replacement.setTag(original.getTag().copy());
        }
        if (original.isDamaged()) {
            replacement.setDamageValue(original.getDamageValue());
        }
        return replacement;
    }

    private static boolean isBaseNezukoBloodArt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return BASE_NEZUKO_BLOOD_ART.equals(itemId);
    }
}
