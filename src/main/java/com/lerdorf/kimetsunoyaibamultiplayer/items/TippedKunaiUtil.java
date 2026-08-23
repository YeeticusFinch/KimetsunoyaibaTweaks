package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;

public final class TippedKunaiUtil {
    public static final ResourceLocation BASE_KUNAI_ID = ResourceLocation.fromNamespaceAndPath("kimetsunoyaiba", "kunai");
    private static final String EFFECTS_KEY = "TippedKunaiEffects";
    private static final String ID_KEY = "Id";
    private static final String DURATION_KEY = "Duration";
    private static final String AMPLIFIER_KEY = "Amplifier";
    private static final int FIRE_PARTICLE_COLOR = 0xF47A22;
    private static final int FROZEN_PARTICLE_COLOR = 0x8EEBFF;

    private TippedKunaiUtil() {
    }

    public static boolean isBaseKunai(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return BASE_KUNAI_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static Item baseKunaiItem() {
        return ForgeRegistries.ITEMS.getValue(BASE_KUNAI_ID);
    }

    public static boolean isEffectCarrier(ItemStack stack) {
        return !effectEntriesFromCarrier(stack).isEmpty();
    }

    public static ItemStack createTippedKunai(ItemStack effectCarrier, int count) {
        List<EffectEntry> entries = effectEntriesFromCarrier(effectCarrier);
        if (entries.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.TIPPED_KUNAI.get(), count);
        ListTag list = new ListTag();
        for (EffectEntry entry : entries) {
            list.add(entry.save());
        }
        result.getOrCreateTag().put(EFFECTS_KEY, list);
        return result;
    }

    public static List<EffectEntry> effectEntries(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(EFFECTS_KEY)) {
            return List.of();
        }

        ListTag list = stack.getTag().getList(EFFECTS_KEY, CompoundTag.TAG_COMPOUND);
        List<EffectEntry> entries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            EffectEntry entry = EffectEntry.load(list.getCompound(i));
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public static void applyStoredEffects(ItemStack stack, LivingEntity target) {
        for (EffectEntry entry : effectEntries(stack)) {
            if (BloodDemonArtAlchemyCatalog.isFireInfusionEffectId(entry.id())) {
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), entry.duration()));
                continue;
            }
            if (BloodDemonArtAlchemyCatalog.isFrozenInfusionEffectId(entry.id())) {
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(), Math.max(target.getTicksFrozen(), entry.duration())));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, entry.duration(), Math.max(0, entry.amplifier() - 1), false, true));
                continue;
            }

            ResourceLocation effectKey = ResourceLocation.tryParse(entry.id());
            MobEffect effect = effectKey == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(effectKey);
            if (effect != null) {
                target.addEffect(new MobEffectInstance(effect, entry.duration(), entry.amplifier(), false, true));
            }
        }
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        for (EffectEntry entry : effectEntries(stack)) {
            tooltip.add(effectName(entry).copy()
                .append(Component.literal(effectLevel(entry.amplifier())))
                .append(Component.literal(" (" + formatDuration(entry.duration()) + ")"))
                .withStyle(ChatFormatting.GRAY));
        }
    }

    public static int particleColor(ItemStack stack) {
        List<EffectEntry> entries = effectEntries(stack);
        if (entries.isEmpty()) {
            return 0x385DC6;
        }

        List<MobEffectInstance> vanillaEffects = new ArrayList<>();
        for (EffectEntry entry : entries) {
            if (BloodDemonArtAlchemyCatalog.isFireInfusionEffectId(entry.id())) {
                return FIRE_PARTICLE_COLOR;
            }
            if (BloodDemonArtAlchemyCatalog.isFrozenInfusionEffectId(entry.id())) {
                return FROZEN_PARTICLE_COLOR;
            }

            ResourceLocation effectKey = ResourceLocation.tryParse(entry.id());
            MobEffect effect = effectKey == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(effectKey);
            if (effect != null) {
                vanillaEffects.add(new MobEffectInstance(effect, entry.duration(), entry.amplifier(), false, true));
            }
        }
        return vanillaEffects.isEmpty() ? 0x385DC6 : PotionUtils.getColor(vanillaEffects);
    }

    private static List<EffectEntry> effectEntriesFromCarrier(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        if (BloodDemonArtAlchemyCatalog.isInfusion(stack)) {
            String id = BloodDemonArtAlchemyCatalog.infusionEffectId(stack);
            if (id.isBlank()) {
                return List.of();
            }
            return List.of(new EffectEntry(id, BloodDemonArtAlchemyCatalog.infusionDurationSeconds(stack) * 20,
                BloodDemonArtAlchemyCatalog.infusionAmplifier(stack)));
        }
        if (stack.is(Items.POTION)) {
            List<EffectEntry> entries = new ArrayList<>();
            for (MobEffectInstance effect : PotionUtils.getMobEffects(stack)) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                if (id != null) {
                    entries.add(new EffectEntry(id.toString(), effect.getDuration(), effect.getAmplifier()));
                }
            }
            return entries;
        }
        return List.of();
    }

    private static Component effectName(EffectEntry entry) {
        if (BloodDemonArtAlchemyCatalog.isFireInfusionEffectId(entry.id())) {
            return Component.translatable("item.kimetsunoyaibamultiplayer.fire_infusion");
        }
        if (BloodDemonArtAlchemyCatalog.isFrozenInfusionEffectId(entry.id())) {
            return Component.translatable("item.kimetsunoyaibamultiplayer.frozen_infusion");
        }
        ResourceLocation effectKey = ResourceLocation.tryParse(entry.id());
        MobEffect effect = effectKey == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(effectKey);
        return effect == null ? Component.literal(entry.id()) : Component.translatable(effect.getDescriptionId());
    }

    private static String effectLevel(int amplifier) {
        return switch (amplifier) {
            case 0 -> "";
            case 1 -> " II";
            case 2 -> " III";
            case 3 -> " IV";
            case 4 -> " V";
            default -> " " + (amplifier + 1);
        };
    }

    private static String formatDuration(int durationTicks) {
        int seconds = Math.max(0, durationTicks / 20);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    public record EffectEntry(String id, int duration, int amplifier) {
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString(ID_KEY, id);
            tag.putInt(DURATION_KEY, Math.max(1, duration));
            tag.putInt(AMPLIFIER_KEY, Math.max(0, amplifier));
            return tag;
        }

        private static EffectEntry load(CompoundTag tag) {
            String id = tag.getString(ID_KEY);
            if (id.isBlank()) {
                return null;
            }
            return new EffectEntry(id, Math.max(1, tag.getInt(DURATION_KEY)), Math.max(0, tag.getInt(AMPLIFIER_KEY)));
        }
    }
}
