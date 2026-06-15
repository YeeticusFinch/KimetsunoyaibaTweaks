package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.api.StyleMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.config.CustomProgressionConfig;
import com.lerdorf.kimetsunoyaibamultiplayer.items.ModItems;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinOreItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public final class ScarletOreReplacementHandler {
    private static final float RARE_VARIANT_CHANCE = 0.05F;
    private static final Set<ResourceLocation> BASE_SCARLET_ORE_IDS = Set.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "scarlet_ore"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "ore_scarlet")
    );
    private static final Set<ResourceLocation> BASE_SCARLET_RARE_ORE_IDS = Set.of(
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "scarlet_ore_rare"),
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "ore_scarlet_rare")
    );

    private ScarletOreReplacementHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !isReplacementEnabled()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity itemEntity) || !(itemEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        boolean forceRare = isBaseScarletRareOre(stack);
        if (!isBaseScarletOre(stack) && !forceRare) {
            return;
        }

        ItemStack replacement = createReplacement(serverLevel, stack, forceRare);
        if (replacement.isEmpty()) {
            return;
        }

        itemEntity.setItem(replacement);
        logReplacement("Replaced base scarlet ore item at {} with nichirin ore", itemEntity.blockPosition());
    }

    private static boolean isReplacementEnabled() {
        return CustomProgressionConfig.replaceScarletOre != null
            && CustomProgressionConfig.replaceScarletOre.get();
    }

    private static boolean isBaseScarletOre(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId != null && BASE_SCARLET_ORE_IDS.contains(itemId);
    }

    private static boolean isBaseScarletRareOre(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId != null && BASE_SCARLET_RARE_ORE_IDS.contains(itemId);
    }

    private static ItemStack createReplacement(ServerLevel serverLevel, ItemStack original, boolean forceRare) {
        String styleId = pickRandomStyleId(serverLevel);
        if (styleId == null || styleId.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack replacement = forceRare || serverLevel.getRandom().nextFloat() < RARE_VARIANT_CHANCE
            ? NichirinOreItem.createRareForStyle(ModItems.NICHIRIN_ORE.get(), styleId)
            : NichirinOreItem.createForStyle(ModItems.NICHIRIN_ORE.get(), styleId);
        if (original != null && original.getCount() > 0) {
            replacement.setCount(original.getCount());
        }
        return replacement;
    }

    private static String pickRandomStyleId(ServerLevel serverLevel) {
        List<String> styleIds = new ArrayList<>();
        for (StyleMetadataRegistry.StyleMetadata style : StyleMetadataRegistry.getAllStyles()) {
            if (style == null) {
                continue;
            }
            String styleId = style.getStyleId();
            if (styleId == null || styleId.isEmpty() || "black".equals(styleId)) {
                continue;
            }
            styleIds.add(styleId);
        }

        if (styleIds.isEmpty()) {
            return null;
        }

        return styleIds.get(serverLevel.getRandom().nextInt(styleIds.size()));
    }

    private static void logReplacement(String message, Object arg) {
        if (CustomProgressionConfig.enableDebugLogging != null
            && CustomProgressionConfig.enableDebugLogging.get()) {
            Log.debug(message, arg);
        }
    }
}
