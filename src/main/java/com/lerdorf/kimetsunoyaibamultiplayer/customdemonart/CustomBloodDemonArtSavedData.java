package com.lerdorf.kimetsunoyaibamultiplayer.customdemonart;

import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog;
import com.lerdorf.kimetsunoyaibamultiplayer.events.DemonTransformationHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomBloodDemonArtSavedData extends SavedData {
    private static final String DATA_NAME = "kny_custom_blood_demon_arts";
    private static final int MAX_SLOTS = 10;

    private final Map<UUID, PlayerArtData> players = new ConcurrentHashMap<>();

    public static CustomBloodDemonArtSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return new CustomBloodDemonArtSavedData();
        }
        return overworld.getDataStorage().computeIfAbsent(
            CustomBloodDemonArtSavedData::load,
            CustomBloodDemonArtSavedData::new,
            DATA_NAME
        );
    }

    private static CustomBloodDemonArtSavedData load(CompoundTag tag) {
        CustomBloodDemonArtSavedData data = new CustomBloodDemonArtSavedData();
        if (!tag.contains("players", Tag.TAG_LIST)) {
            return data;
        }

        ListTag playersTag = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playersTag.size(); i++) {
            CompoundTag playerTag = playersTag.getCompound(i);
            if (!playerTag.hasUUID("uuid")) {
                continue;
            }
            data.players.put(playerTag.getUUID("uuid"), PlayerArtData.fromTag(playerTag));
        }

        Log.debug("Loaded " + data.players.size() + " custom blood demon art profiles");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag playersTag = new ListTag();
        for (Map.Entry<UUID, PlayerArtData> entry : players.entrySet()) {
            CompoundTag playerTag = entry.getValue().toTag();
            playerTag.putUUID("uuid", entry.getKey());
            playersTag.add(playerTag);
        }
        tag.put("players", playersTag);
        return tag;
    }

    public PlayerArtData getOrCreate(ServerPlayer player) {
        return players.computeIfAbsent(player.getUUID(), unused -> {
            setDirty();
            return PlayerArtData.createDefault(player);
        });
    }

    public boolean hasCustomItem(ServerPlayer player, ItemStack predicateStack) {
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(stack, predicateStack) || stack.getItem() == predicateStack.getItem()) {
                return true;
            }
        }
        return player.getOffhandItem().getItem() == predicateStack.getItem();
    }

    public int getUnlockedSlots(ServerPlayer player) {
        return Math.max(0, Math.min(MAX_SLOTS, DemonTransformationHandler.getTrackedMuzanBlood(player) / 10));
    }

    public boolean createBlankForm(ServerPlayer player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS || slotIndex >= getUnlockedSlots(player)) {
            return false;
        }
        if (player.experienceLevel < 10) {
            return false;
        }

        PlayerArtData data = getOrCreate(player);
        CustomFormSlot slot = data.slots().get(slotIndex);
        if (slot.filled()) {
            return false;
        }

        player.giveExperienceLevels(-10);
        data.slots().set(slotIndex, CustomFormSlot.filled("Form " + (slotIndex + 1)));
        if (data.selectedSlot() < 0 || !data.hasFilledSlot(data.selectedSlot())) {
            data.setSelectedSlot(slotIndex);
        }
        setDirty();
        return true;
    }

    public void setSelectedSlot(ServerPlayer player, int slotIndex) {
        PlayerArtData data = getOrCreate(player);
        if (slotIndex < 0 || slotIndex >= data.slots().size()) {
            return;
        }
        data.setSelectedSlot(slotIndex);
        setDirty();
    }

    public boolean setFormName(ServerPlayer player, int slotIndex, String name) {
        PlayerArtData data = getOrCreate(player);
        if (!data.hasFilledSlot(slotIndex)) {
            return false;
        }
        data.slots().get(slotIndex).setName(name == null || name.isBlank() ? "Form " + (slotIndex + 1) : name.trim());
        setDirty();
        return true;
    }

    public boolean addMove(ServerPlayer player, int slotIndex, MoveType move) {
        PlayerArtData data = getOrCreate(player);
        if (!data.hasFilledSlot(slotIndex) || move == null) {
            return false;
        }
        CustomFormSlot slot = data.slots().get(slotIndex);
        if (slot.moves().contains(move) || !data.availableMoves().contains(move)) {
            return false;
        }
        if (slot.moves().size() >= maxMovesForSlot(slotIndex)) {
            return false;
        }
        int xpCost = slot.moves().isEmpty() ? 0 : move.xpCost();
        if (player.experienceLevel < xpCost) {
            return false;
        }
        if (xpCost > 0) {
            player.giveExperienceLevels(-xpCost);
        }
        slot.moves().add(move);
        setDirty();
        return true;
    }

    public boolean unlockCatalystFromInventory(ServerPlayer player, int inventorySlot) {
        PlayerArtData data = getOrCreate(player);
        ItemStack stack = inventoryStack(player, inventorySlot);
        if (stack.isEmpty() || !BloodDemonArtAlchemyCatalog.isCatalyst(stack)) {
            return false;
        }
        String itemId = BloodDemonArtAlchemyCatalog.id(stack);
        if (data.catalystIds().contains(itemId)) {
            return false;
        }
        consumeInventoryItem(player, inventorySlot, stack);
        data.catalystIds().add(itemId);
        setDirty();
        return true;
    }

    public boolean applyAmplifierFromInventory(ServerPlayer player, int slotIndex, int inventorySlot) {
        PlayerArtData data = getOrCreate(player);
        if (!data.hasFilledSlot(slotIndex)) {
            return false;
        }
        ItemStack stack = inventoryStack(player, inventorySlot);
        if (stack.isEmpty() || !BloodDemonArtAlchemyCatalog.isAmplifier(stack)) {
            return false;
        }
        CustomFormSlot slot = data.slots().get(slotIndex);
        consumeInventoryItem(player, inventorySlot, stack);
        slot.amplifierIds().add(BloodDemonArtAlchemyCatalog.id(stack));
        setDirty();
        return true;
    }

    public boolean consumeBinderFromInventory(ServerPlayer player, int inventorySlot) {
        ItemStack stack = inventoryStack(player, inventorySlot);
        if (stack.isEmpty() || !BloodDemonArtAlchemyCatalog.matches(stack, "kimetsunoyaibamultiplayer:potion_effect_binder")) {
            return false;
        }
        consumeInventoryItem(player, inventorySlot, stack);
        setDirty();
        return true;
    }

    public boolean applyHeldCatalyst(ServerPlayer player, int ignoredSlotIndex) {
        int selected = player.getInventory().selected;
        return unlockCatalystFromInventory(player, selected);
    }

    public boolean applyHeldAmplifier(ServerPlayer player, int slotIndex) {
        int selected = player.getInventory().selected;
        return applyAmplifierFromInventory(player, slotIndex, selected);
    }

    public boolean bindPrimaryEffectToMove(ServerPlayer player, int slotIndex, MoveType move) {
        return bindMoveEffect(player, slotIndex, move, EffectBindingSource.PRIMARY, PotionSetting.none(), false);
    }

    public boolean bindSecondaryEffectToMove(ServerPlayer player, int slotIndex, MoveType move) {
        return bindMoveEffect(player, slotIndex, move, EffectBindingSource.SECONDARY, PotionSetting.none(), false);
    }

    public boolean bindCustomEffectToMove(ServerPlayer player, int slotIndex, MoveType move, int inventorySlot) {
        ItemStack stack = inventoryStack(player, inventorySlot);
        PotionSetting setting = potionSettingFromItem(stack);
        if (stack.isEmpty() || setting == null) {
            return false;
        }
        return bindMoveEffect(player, slotIndex, move, EffectBindingSource.CUSTOM, setting, true, inventorySlot);
    }

    public boolean setCoreParticle(ServerPlayer player, boolean primary, ParticleStyle newStyle) {
        PlayerArtData data = getOrCreate(player);
        CoreSettings current = data.coreSettings();
        ParticleStyle existing = primary ? current.primaryParticle() : current.secondaryParticle();
        if (existing.equals(newStyle)) {
            return true;
        }
        if (player.experienceLevel < 1) {
            return false;
        }
        player.giveExperienceLevels(-1);
        data.setCoreSettings(primary
            ? new CoreSettings(newStyle, current.secondaryParticle(), current.primaryPotion(), current.secondaryPotion(), current.chatColor())
            : new CoreSettings(current.primaryParticle(), newStyle, current.primaryPotion(), current.secondaryPotion(), current.chatColor()));
        setDirty();
        return true;
    }

    public boolean setCorePotionFromInventory(ServerPlayer player, boolean primary, int inventorySlot, boolean selfEffect) {
        ItemStack stack = inventoryStack(player, inventorySlot);
        PotionSetting setting = potionSettingFromItem(stack);
        if (stack.isEmpty() || setting == null) {
            return false;
        }
        setting = new PotionSetting(setting.effectId(), selfEffect, Math.max(2, setting.durationSeconds()), Math.max(1, setting.amplifier()));
        consumeInventoryItem(player, inventorySlot, stack);
        PlayerArtData data = getOrCreate(player);
        CoreSettings current = data.coreSettings();
        data.setCoreSettings(primary
            ? new CoreSettings(current.primaryParticle(), current.secondaryParticle(), setting, current.secondaryPotion(), current.chatColor())
            : new CoreSettings(current.primaryParticle(), current.secondaryParticle(), current.primaryPotion(), setting, current.chatColor()));
        setDirty();
        return true;
    }

    public boolean togglePotionTargeting(ServerPlayer player, boolean primary) {
        PlayerArtData data = getOrCreate(player);
        CoreSettings current = data.coreSettings();
        PotionSetting original = primary ? current.primaryPotion() : current.secondaryPotion();
        PotionSetting updated = new PotionSetting(original.effectId(), !original.selfEffect(), original.durationSeconds(), original.amplifier());
        data.setCoreSettings(primary
            ? new CoreSettings(current.primaryParticle(), current.secondaryParticle(), updated, current.secondaryPotion(), current.chatColor())
            : new CoreSettings(current.primaryParticle(), current.secondaryParticle(), current.primaryPotion(), updated, current.chatColor()));
        setDirty();
        return true;
    }

    public boolean setChatColor(ServerPlayer player, int chatColor) {
        PlayerArtData data = getOrCreate(player);
        CoreSettings current = data.coreSettings();
        data.setCoreSettings(new CoreSettings(current.primaryParticle(), current.secondaryParticle(), current.primaryPotion(), current.secondaryPotion(), chatColor));
        setDirty();
        return true;
    }

    public static int maxMovesForSlot(int slotIndex) {
        return Math.min(6, Math.max(1, slotIndex + 1));
    }

    private boolean bindMoveEffect(ServerPlayer player, int slotIndex, MoveType move, EffectBindingSource source,
                                   PotionSetting customPotion, boolean consumeChosenPotion) {
        return bindMoveEffect(player, slotIndex, move, source, customPotion, consumeChosenPotion, -1);
    }

    private boolean bindMoveEffect(ServerPlayer player, int slotIndex, MoveType move, EffectBindingSource source,
                                   PotionSetting customPotion, boolean consumeChosenPotion, int effectInventorySlot) {
        PlayerArtData data = getOrCreate(player);
        if (!data.hasFilledSlot(slotIndex) || move == null || !data.slots().get(slotIndex).moves().contains(move)) {
            return false;
        }
        int binderSlot = findFirstBinderSlot(player);
        if (binderSlot < 0) {
            return false;
        }
        ItemStack binderStack = inventoryStack(player, binderSlot);
        consumeInventoryItem(player, binderSlot, binderStack);
        if (consumeChosenPotion && effectInventorySlot >= 0) {
            consumeInventoryItem(player, effectInventorySlot, inventoryStack(player, effectInventorySlot));
        }
        CustomFormSlot slot = data.slots().get(slotIndex);
        slot.setBindingForMove(move, new MoveEffectBinding(move.serializedName(), source, customPotion));
        setDirty();
        return true;
    }

    private static int findFirstBinderSlot(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (BloodDemonArtAlchemyCatalog.matches(player.getInventory().items.get(i), "kimetsunoyaibamultiplayer:potion_effect_binder")) {
                return i;
            }
        }
        return -1;
    }

    private static ItemStack inventoryStack(ServerPlayer player, int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().items.size()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().items.get(inventorySlot);
    }

    private static void consumeInventoryItem(ServerPlayer player, int inventorySlot, ItemStack stackBefore) {
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().items.size()) {
            return;
        }
        ItemStack live = player.getInventory().items.get(inventorySlot);
        if (live.isEmpty()) {
            return;
        }
        ItemStack snapshot = stackBefore.isEmpty() ? live.copy() : stackBefore.copy();
        live.shrink(1);
        ItemStack returnStack = BloodDemonArtAlchemyCatalog.containerReturn(snapshot);
        if (!returnStack.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(player, returnStack);
        }
    }

    private static PotionSetting potionSettingFromItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (BloodDemonArtAlchemyCatalog.isInfusion(stack)) {
            return new PotionSetting(
                BloodDemonArtAlchemyCatalog.infusionEffectId(stack),
                true,
                Math.max(2, BloodDemonArtAlchemyCatalog.infusionDurationSeconds(stack)),
                Math.max(1, BloodDemonArtAlchemyCatalog.infusionAmplifier(stack))
            );
        }
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
            if (effects.isEmpty()) {
                return null;
            }
            MobEffectInstance effect = effects.get(0);
            ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (effectId == null) {
                return null;
            }
            return new PotionSetting(effectId.toString(), true, Math.max(2, effect.getDuration() / 20), effect.getAmplifier() + 1);
        }
        return null;
    }

    public static int getMaxSlots() {
        return MAX_SLOTS;
    }

    public static final class PlayerArtData {
        private CoreSettings coreSettings;
        private final List<String> catalystIds;
        private final List<CustomFormSlot> slots;
        private int selectedSlot;

        private PlayerArtData(CoreSettings coreSettings, List<String> catalystIds, List<CustomFormSlot> slots, int selectedSlot) {
            this.coreSettings = coreSettings;
            this.catalystIds = catalystIds;
            this.slots = slots;
            this.selectedSlot = selectedSlot;
        }

        public static PlayerArtData createDefault(ServerPlayer player) {
            List<CustomFormSlot> slots = new ArrayList<>(MAX_SLOTS);
            for (int i = 0; i < MAX_SLOTS; i++) {
                slots.add(CustomFormSlot.empty());
            }
            return new PlayerArtData(CoreSettings.defaults(), new ArrayList<>(), slots, -1);
        }

        public static PlayerArtData fromTag(CompoundTag tag) {
            CoreSettings core = CoreSettings.fromTag(tag.getCompound("core"));
            List<String> catalystIds = readStringList(tag, "catalystIds");
            List<CustomFormSlot> slots = new ArrayList<>(MAX_SLOTS);
            ListTag slotsTag = tag.getList("slots", Tag.TAG_COMPOUND);
            for (int i = 0; i < MAX_SLOTS; i++) {
                if (i < slotsTag.size()) {
                    slots.add(CustomFormSlot.fromTag(slotsTag.getCompound(i)));
                } else {
                    slots.add(CustomFormSlot.empty());
                }
            }
            int selectedSlot = tag.contains("selectedSlot", Tag.TAG_INT) ? tag.getInt("selectedSlot") : -1;
            return new PlayerArtData(core, catalystIds, slots, selectedSlot);
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("core", coreSettings.toTag());
            tag.put("catalystIds", writeStringList(catalystIds));
            ListTag slotsTag = new ListTag();
            for (CustomFormSlot slot : slots) {
                slotsTag.add(slot.toTag());
            }
            tag.put("slots", slotsTag);
            tag.putInt("selectedSlot", selectedSlot);
            return tag;
        }

        public CoreSettings coreSettings() {
            return coreSettings;
        }

        public void setCoreSettings(CoreSettings coreSettings) {
            this.coreSettings = coreSettings;
        }

        public List<String> catalystIds() {
            return catalystIds;
        }

        public List<MoveType> availableMoves() {
            List<MoveType> available = new ArrayList<>(MoveType.basicMoves());
            for (String catalystId : catalystIds) {
                MoveType unlocked = BloodDemonArtAlchemyCatalog.catalystMove(catalystId);
                if (unlocked != null && !available.contains(unlocked)) {
                    available.add(unlocked);
                }
            }
            return available;
        }

        public List<CustomFormSlot> slots() {
            return slots;
        }

        public int selectedSlot() {
            return selectedSlot;
        }

        public void setSelectedSlot(int selectedSlot) {
            this.selectedSlot = selectedSlot;
        }

        public boolean hasFilledSlot(int slotIndex) {
            return slotIndex >= 0 && slotIndex < slots.size() && slots.get(slotIndex).filled();
        }
    }

    public record CoreSettings(
        ParticleStyle primaryParticle,
        ParticleStyle secondaryParticle,
        PotionSetting primaryPotion,
        PotionSetting secondaryPotion,
        int chatColor
    ) {
        public static CoreSettings defaults() {
            return new CoreSettings(
                new ParticleStyle("minecraft:dust", 0xD12F4B, 1.0F, ""),
                new ParticleStyle("minecraft:smoke", 0xFFFFFF, 0.8F, ""),
                PotionSetting.none(),
                PotionSetting.none(),
                0xAA1E2F
            );
        }

        public static CoreSettings fromTag(CompoundTag tag) {
            return new CoreSettings(
                ParticleStyle.fromTag(tag.getCompound("primaryParticle")),
                ParticleStyle.fromTag(tag.getCompound("secondaryParticle")),
                PotionSetting.fromTag(tag.getCompound("primaryPotion")),
                PotionSetting.fromTag(tag.getCompound("secondaryPotion")),
                tag.contains("chatColor", Tag.TAG_INT) ? tag.getInt("chatColor") : defaults().chatColor
            );
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("primaryParticle", primaryParticle.toTag());
            tag.put("secondaryParticle", secondaryParticle.toTag());
            tag.put("primaryPotion", primaryPotion.toTag());
            tag.put("secondaryPotion", secondaryPotion.toTag());
            tag.putInt("chatColor", chatColor);
            return tag;
        }
    }

    public record ParticleStyle(String particleId, int color, float size, String blockStateId) {
        public static ParticleStyle fromTag(CompoundTag tag) {
            return new ParticleStyle(
                tag.getString("particleId"),
                tag.contains("color", Tag.TAG_INT) ? tag.getInt("color") : 0xFFFFFF,
                tag.contains("size", Tag.TAG_FLOAT) ? tag.getFloat("size") : 1.0F,
                tag.getString("blockStateId")
            );
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("particleId", particleId);
            tag.putInt("color", color);
            tag.putFloat("size", size);
            tag.putString("blockStateId", blockStateId);
            return tag;
        }
    }

    public record PotionSetting(String effectId, boolean selfEffect, int durationSeconds, int amplifier) {
        public static PotionSetting none() {
            return new PotionSetting("", true, 2, 0);
        }

        public static PotionSetting fromTag(CompoundTag tag) {
            return new PotionSetting(
                tag.getString("effectId"),
                !tag.contains("selfEffect", Tag.TAG_BYTE) || tag.getBoolean("selfEffect"),
                tag.contains("durationSeconds", Tag.TAG_INT) ? tag.getInt("durationSeconds") : 2,
                tag.contains("amplifier", Tag.TAG_INT) ? tag.getInt("amplifier") : 0
            );
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("effectId", effectId);
            tag.putBoolean("selfEffect", selfEffect);
            tag.putInt("durationSeconds", durationSeconds);
            tag.putInt("amplifier", amplifier);
            return tag;
        }

        public MobEffect resolveEffect() {
            return ForgeRegistries.MOB_EFFECTS.getValue(net.minecraft.resources.ResourceLocation.tryParse(effectId));
        }
    }

    public static final class CustomFormSlot {
        private final boolean filled;
        private String name;
        private final List<MoveType> moves;
        private final List<String> amplifierIds;
        private final List<MoveEffectBinding> effectBindings;

        private CustomFormSlot(boolean filled, String name, List<MoveType> moves, List<String> amplifierIds,
                               List<MoveEffectBinding> effectBindings) {
            this.filled = filled;
            this.name = name;
            this.moves = moves;
            this.amplifierIds = amplifierIds;
            this.effectBindings = effectBindings;
        }

        public static CustomFormSlot empty() {
            return new CustomFormSlot(false, "", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public static CustomFormSlot filled(String name) {
            return new CustomFormSlot(true, name, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public static CustomFormSlot fromTag(CompoundTag tag) {
            boolean filled = tag.getBoolean("filled");
            String name = tag.getString("name");
            List<MoveType> moves = new ArrayList<>();
            ListTag movesTag = tag.getList("moves", Tag.TAG_STRING);
            for (int i = 0; i < movesTag.size(); i++) {
                MoveType move = MoveType.byName(movesTag.getString(i));
                if (move != null) {
                    moves.add(move);
                }
            }
            List<MoveEffectBinding> effectBindings = new ArrayList<>();
            ListTag bindingsTag = tag.getList("effectBindings", Tag.TAG_COMPOUND);
            for (int i = 0; i < bindingsTag.size(); i++) {
                effectBindings.add(MoveEffectBinding.fromTag(bindingsTag.getCompound(i)));
            }
            List<String> amplifierIds = readStringList(tag, "amplifierIds");
            if (amplifierIds.isEmpty() && tag.contains("amplifierId", Tag.TAG_STRING) && !tag.getString("amplifierId").isBlank()) {
                amplifierIds.add(tag.getString("amplifierId"));
            }
            return new CustomFormSlot(
                filled,
                name,
                moves,
                amplifierIds,
                effectBindings
            );
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("filled", filled);
            tag.putString("name", name);
            ListTag movesTag = new ListTag();
            for (MoveType move : moves) {
                movesTag.add(StringTag.valueOf(move.serializedName()));
            }
            tag.put("moves", movesTag);
            tag.put("amplifierIds", writeStringList(amplifierIds));
            ListTag bindingsTag = new ListTag();
            for (MoveEffectBinding binding : effectBindings) {
                bindingsTag.add(binding.toTag());
            }
            tag.put("effectBindings", bindingsTag);
            return tag;
        }

        public boolean filled() {
            return filled;
        }

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<MoveType> moves() {
            return moves;
        }

        public List<String> amplifierIds() {
            return amplifierIds;
        }

        public List<MoveEffectBinding> effectBindings() {
            return effectBindings;
        }

        public MoveEffectBinding bindingForMove(MoveType move) {
            for (MoveEffectBinding binding : effectBindings) {
                if (binding.moveId().equals(move.serializedName())) {
                    return binding;
                }
            }
            return new MoveEffectBinding(move.serializedName(), EffectBindingSource.NONE, PotionSetting.none());
        }

        public void setBindingForMove(MoveType move, MoveEffectBinding newBinding) {
            effectBindings.removeIf(binding -> binding.moveId().equals(move.serializedName()));
            effectBindings.add(newBinding);
        }

        public Map<BloodDemonArtAlchemyCatalog.AmplifierKind, Integer> amplifierCounts() {
            Map<BloodDemonArtAlchemyCatalog.AmplifierKind, Integer> counts = new ConcurrentHashMap<>();
            for (String amplifierId : amplifierIds) {
                BloodDemonArtAlchemyCatalog.AmplifierKind kind = BloodDemonArtAlchemyCatalog.amplifierKind(amplifierId);
                counts.put(kind, counts.getOrDefault(kind, 0) + 1);
            }
            return counts;
        }

        public BloodDemonArtAlchemyCatalog.AmplifierKind dominantAmplifierKind() {
            BloodDemonArtAlchemyCatalog.AmplifierKind bestKind = BloodDemonArtAlchemyCatalog.AmplifierKind.NONE;
            int bestCount = 0;
            for (Map.Entry<BloodDemonArtAlchemyCatalog.AmplifierKind, Integer> entry : amplifierCounts().entrySet()) {
                if (entry.getKey() != BloodDemonArtAlchemyCatalog.AmplifierKind.NONE && entry.getValue() > bestCount) {
                    bestKind = entry.getKey();
                    bestCount = entry.getValue();
                }
            }
            return bestKind;
        }

        public int cooldownSeconds() {
            int total = 0;
            for (MoveType move : moves) {
                total += move.cooldownSeconds();
            }
            return total;
        }
    }

    public record MoveEffectBinding(String moveId, EffectBindingSource bindingSource, PotionSetting customPotion) {
        public static MoveEffectBinding fromTag(CompoundTag tag) {
            return new MoveEffectBinding(
                tag.getString("moveId"),
                EffectBindingSource.byName(tag.getString("bindingSource")),
                tag.contains("customPotion", Tag.TAG_COMPOUND) ? PotionSetting.fromTag(tag.getCompound("customPotion")) : PotionSetting.none()
            );
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("moveId", moveId);
            tag.putString("bindingSource", bindingSource.serializedName());
            tag.put("customPotion", customPotion.toTag());
            return tag;
        }
    }

    public enum EffectBindingSource {
        NONE("none"),
        PRIMARY("primary"),
        SECONDARY("secondary"),
        CUSTOM("custom");

        private final String serializedName;

        EffectBindingSource(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static EffectBindingSource byName(String name) {
            for (EffectBindingSource value : values()) {
                if (value.serializedName.equals(name)) {
                    return value;
                }
            }
            return NONE;
        }
    }

    public enum MoveType {
        PUNCH_RIGHT("punch_right", "Punch Right", "Uses the right punch animation and fires a primary-particle shockwave.", 5, 2, 10, true),
        PUNCH_LEFT("punch_left", "Punch Left", "Uses the left punch animation and fires a primary-particle shockwave.", 5, 2, 10, true),
        FRONT_FLIP("front_flip", "Front Flip", "Launches forward while leaving a secondary-particle trail.", 5, 4, 15, true),
        MELEE_COMBO("melee_combo", "Melee Combo", "A four-hit flurry using punch and kick animations.", 12, 7, 15, true),
        WITHER_SKULL("wither_skull", "Wither Skull", "Launches a guided wither skull that keeps correcting toward your current look direction.", 8, 5, 20, false),
        BLAZE_BARRAGE("blaze_barrage", "Blaze Barrage", "Fires three consecutive particle volleys using both your primary and secondary particle styles.", 9, 5, 18, false),
        GUARDIAN_LASER("guardian_laser", "Guardian Laser", "Projects a piercing beam forward that damages targets in a straight line.", 10, 6, 24, false);

        private final String serializedName;
        private final String displayName;
        private final String description;
        private final int xpCost;
        private final int cooldownSeconds;
        private final int durationTicks;
        private final boolean basicMove;

        MoveType(String serializedName, String displayName, String description, int xpCost, int cooldownSeconds, int durationTicks, boolean basicMove) {
            this.serializedName = serializedName;
            this.displayName = displayName;
            this.description = description;
            this.xpCost = xpCost;
            this.cooldownSeconds = cooldownSeconds;
            this.durationTicks = durationTicks;
            this.basicMove = basicMove;
        }

        public String serializedName() {
            return serializedName;
        }

        public String displayName() {
            return displayName;
        }

        public String description() {
            return description;
        }

        public int xpCost() {
            return xpCost;
        }

        public int cooldownSeconds() {
            return cooldownSeconds;
        }

        public int durationTicks() {
            return durationTicks;
        }

        public boolean basicMove() {
            return basicMove;
        }

        public static MoveType byName(String name) {
            for (MoveType move : values()) {
                if (move.serializedName.equals(name)) {
                    return move;
                }
            }
            return null;
        }

        public static List<MoveType> basicMoves() {
            List<MoveType> basic = new ArrayList<>();
            for (MoveType move : values()) {
                if (move.basicMove) {
                    basic.add(move);
                }
            }
            return basic;
        }
    }

    public static List<CustomFormSlot> immutableSlots(PlayerArtData data) {
        return Collections.unmodifiableList(data.slots());
    }

    private static List<String> readStringList(CompoundTag tag, String key) {
        List<String> result = new ArrayList<>();
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return result;
        }
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    private static ListTag writeStringList(List<String> values) {
        ListTag tag = new ListTag();
        for (String value : values) {
            tag.add(StringTag.valueOf(value));
        }
        return tag;
    }
}
