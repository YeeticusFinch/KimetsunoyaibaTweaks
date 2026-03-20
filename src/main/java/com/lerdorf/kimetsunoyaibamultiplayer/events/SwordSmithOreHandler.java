package com.lerdorf.kimetsunoyaibamultiplayer.events;

import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinOreItem;
import com.lerdorf.kimetsunoyaibamultiplayer.util.EntityTagHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = "kimetsunoyaibamultiplayer")
public final class SwordSmithOreHandler {
    private static final double DROP_INTERACTION_RADIUS = 1.5D;
    private static final ResourceLocation SUN_BLADE_ADVANCEMENT =
        ResourceLocation.fromNamespaceAndPath("kimetsunoyaibamultiplayer", "sun_blade");

    private SwordSmithOreHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Entity target = event.getTarget();
        if (!EntityTagHelper.isSwordSmith(target)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack heldStack = event.getItemStack();
        if (!canUseOreStack(heldStack)) {
            return;
        }

        if (craftSwordFromOre(target, heldStack, player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (Entity entity : serverLevel.getAllEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) {
                continue;
            }

            ItemStack stack = itemEntity.getItem();
            if (!canUseOreStack(stack)) {
                continue;
            }

            AABB smithRange = itemEntity.getBoundingBox().inflate(DROP_INTERACTION_RADIUS);
            List<Entity> swordSmiths = serverLevel.getEntities(itemEntity, smithRange, EntityTagHelper::isSwordSmith);
            if (swordSmiths.isEmpty()) {
                continue;
            }

            Player nearestPlayer = serverLevel.getNearestPlayer(itemEntity, 8.0D);
            if (craftSwordFromOre(swordSmiths.get(0), stack, nearestPlayer)) {
                // Consumed by craftSwordFromOre.
            }
        }
    }

    private static boolean canUseOreStack(ItemStack stack) {
        return !stack.isEmpty()
            && stack.getItem() instanceof NichirinOreItem
            && NichirinOreItem.getStyleId(stack) != null;
    }

    private static boolean craftSwordFromOre(Entity swordSmith, ItemStack oreStack, Player player) {
        String styleId = NichirinOreItem.getStyleId(oreStack);
        if (styleId == null || styleId.isEmpty()) {
            return false;
        }

        Item swordItem = resolveLevelZeroSword(styleId);
        if (swordItem == null) {
            return false;
        }

        boolean consumed = player != null
            ? consumePlayerOre(player, styleId, 2)
            : consumeDroppedOre(swordSmith, styleId, 2);
        if (!consumed) {
            return false;
        }

        swordSmith.spawnAtLocation(new ItemStack(swordItem));
        awardSunBladeAdvancement(player);
        return true;
    }

    private static boolean consumePlayerOre(Player player, String styleId, int amount) {
        int available = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isMatchingOre(stack, styleId)) {
                available += stack.getCount();
            }
        }
        if (available < amount) {
            return false;
        }

        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isMatchingOre(stack, styleId)) {
                continue;
            }
            int toShrink = Math.min(remaining, stack.getCount());
            stack.shrink(toShrink);
            remaining -= toShrink;
        }
        return remaining == 0;
    }

    private static boolean consumeDroppedOre(Entity swordSmith, String styleId, int amount) {
        if (!(swordSmith.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        AABB search = swordSmith.getBoundingBox().inflate(DROP_INTERACTION_RADIUS);
        List<ItemEntity> matchingItems = serverLevel.getEntitiesOfClass(
            ItemEntity.class,
            search,
            itemEntity -> isMatchingOre(itemEntity.getItem(), styleId)
        );

        int available = matchingItems.stream().mapToInt(item -> item.getItem().getCount()).sum();
        if (available < amount) {
            return false;
        }

        int remaining = amount;
        for (ItemEntity itemEntity : matchingItems) {
            ItemStack stack = itemEntity.getItem();
            int toShrink = Math.min(remaining, stack.getCount());
            stack.shrink(toShrink);
            remaining -= toShrink;

            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }

            if (remaining == 0) {
                break;
            }
        }

        return remaining == 0;
    }

    private static boolean isMatchingOre(ItemStack stack, String styleId) {
        return canUseOreStack(stack) && styleId.equals(NichirinOreItem.getStyleId(stack));
    }

    private static void awardSunBladeAdvancement(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }

        Advancement advancement = serverPlayer.server.getAdvancements().getAdvancement(SUN_BLADE_ADVANCEMENT);
        if (advancement == null) {
            return;
        }

        var progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }

        List<String> remainingCriteria = new ArrayList<>();
        for (String criterion : progress.getRemainingCriteria()) {
            remainingCriteria.add(criterion);
        }

        for (String criterion : remainingCriteria) {
            serverPlayer.getAdvancements().award(advancement, criterion);
        }
    }

    private static Item resolveLevelZeroSword(String styleId) {
        List<Item> swords = new ArrayList<>();

        boolean preferEnhanced = shouldPreferEnhancedSwords(styleId);
        for (SwordRegistry.RegisteredSword registered : SwordRegistry.getSwordsByStyleAndLevel(styleId, 0)) {
            swords.add(registered.getSwordItem());
        }

        if (!swords.isEmpty() && preferEnhanced) {
            return chooseDeterministicSword(swords);
        }

        for (SwordMetadataRegistry.SwordMetadata meta : SwordMetadataRegistry.getSwordsByStyleAndLevel(styleId, 0)) {
            Item item = meta.getSwordItem();
            if (item != null) {
                swords.add(item);
            }
        }

        return chooseDeterministicSword(swords);
    }

    private static Item chooseDeterministicSword(List<Item> swords) {
        return swords.stream()
            .filter(item -> item != null)
            .sorted(Comparator.comparing(item -> {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                return id != null ? id.toString() : "";
            }))
            .findFirst()
            .orElse(null);
    }

    private static boolean shouldPreferEnhancedSwords(String styleId) {
        return switch (styleId) {
            case "mist_breathing" -> com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.enhancedMistBreathing;
            case "beast_breathing" -> com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.enhancedBeastBreathing;
            case "love_breathing" -> com.lerdorf.kimetsunoyaibamultiplayer.config.EnhancedBreathingConfig.enhancedLoveBreathing;
            default -> false;
        };
    }
}
