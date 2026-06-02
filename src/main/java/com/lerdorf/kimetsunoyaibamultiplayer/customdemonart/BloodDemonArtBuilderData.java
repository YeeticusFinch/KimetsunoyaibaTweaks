package com.lerdorf.kimetsunoyaibamultiplayer.customdemonart;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class BloodDemonArtBuilderData {
    private final int xpLevel;
    private final int muzanBlood;
    private final int unlockedSlots;
    private final boolean hasCustomItem;
    private final int selectedSlot;
    private final String artName;
    private final int chatColor;
    private final String primaryParticle;
    private final int primaryParticleColor;
    private final float primaryParticleSize;
    private final String primaryParticleBlockStateId;
    private final String secondaryParticle;
    private final int secondaryParticleColor;
    private final float secondaryParticleSize;
    private final String secondaryParticleBlockStateId;
    private final String primaryPotion;
    private final String secondaryPotion;
    private final boolean primaryPotionSelfEffect;
    private final boolean secondaryPotionSelfEffect;
    private final int primaryPotionDurationSeconds;
    private final int secondaryPotionDurationSeconds;
    private final int primaryPotionAmplifier;
    private final int secondaryPotionAmplifier;
    private final List<MoveView> unlockedMoves;
    private final List<String> unlockedCatalysts;
    private final List<FormSlotView> slots;

    public BloodDemonArtBuilderData(int xpLevel, int muzanBlood, int unlockedSlots, boolean hasCustomItem, int selectedSlot,
                                    String artName, int chatColor,
                                    String primaryParticle, int primaryParticleColor, float primaryParticleSize, String primaryParticleBlockStateId,
                                    String secondaryParticle, int secondaryParticleColor, float secondaryParticleSize, String secondaryParticleBlockStateId,
                                    String primaryPotion, String secondaryPotion,
                                    boolean primaryPotionSelfEffect, boolean secondaryPotionSelfEffect,
                                    int primaryPotionDurationSeconds, int secondaryPotionDurationSeconds,
                                    int primaryPotionAmplifier, int secondaryPotionAmplifier,
                                    List<MoveView> unlockedMoves, List<String> unlockedCatalysts, List<FormSlotView> slots) {
        this.xpLevel = xpLevel;
        this.muzanBlood = muzanBlood;
        this.unlockedSlots = unlockedSlots;
        this.hasCustomItem = hasCustomItem;
        this.selectedSlot = selectedSlot;
        this.artName = artName == null || artName.isBlank() ? CustomBloodDemonArtSavedData.DEFAULT_ART_NAME : artName;
        this.chatColor = chatColor;
        this.primaryParticle = primaryParticle;
        this.primaryParticleColor = primaryParticleColor;
        this.primaryParticleSize = primaryParticleSize;
        this.primaryParticleBlockStateId = primaryParticleBlockStateId;
        this.secondaryParticle = secondaryParticle;
        this.secondaryParticleColor = secondaryParticleColor;
        this.secondaryParticleSize = secondaryParticleSize;
        this.secondaryParticleBlockStateId = secondaryParticleBlockStateId;
        this.primaryPotion = primaryPotion;
        this.secondaryPotion = secondaryPotion;
        this.primaryPotionSelfEffect = primaryPotionSelfEffect;
        this.secondaryPotionSelfEffect = secondaryPotionSelfEffect;
        this.primaryPotionDurationSeconds = primaryPotionDurationSeconds;
        this.secondaryPotionDurationSeconds = secondaryPotionDurationSeconds;
        this.primaryPotionAmplifier = primaryPotionAmplifier;
        this.secondaryPotionAmplifier = secondaryPotionAmplifier;
        this.unlockedMoves = List.copyOf(unlockedMoves);
        this.unlockedCatalysts = List.copyOf(unlockedCatalysts);
        this.slots = List.copyOf(slots);
    }

    public BloodDemonArtBuilderData(FriendlyByteBuf buf) {
        this.xpLevel = buf.readVarInt();
        this.muzanBlood = buf.readVarInt();
        this.unlockedSlots = buf.readVarInt();
        this.hasCustomItem = buf.readBoolean();
        this.selectedSlot = buf.readVarInt();
        this.artName = buf.readUtf();
        this.chatColor = buf.readInt();
        this.primaryParticle = buf.readUtf();
        this.primaryParticleColor = buf.readInt();
        this.primaryParticleSize = buf.readFloat();
        this.primaryParticleBlockStateId = buf.readUtf();
        this.secondaryParticle = buf.readUtf();
        this.secondaryParticleColor = buf.readInt();
        this.secondaryParticleSize = buf.readFloat();
        this.secondaryParticleBlockStateId = buf.readUtf();
        this.primaryPotion = buf.readUtf();
        this.secondaryPotion = buf.readUtf();
        this.primaryPotionSelfEffect = buf.readBoolean();
        this.secondaryPotionSelfEffect = buf.readBoolean();
        this.primaryPotionDurationSeconds = buf.readVarInt();
        this.secondaryPotionDurationSeconds = buf.readVarInt();
        this.primaryPotionAmplifier = buf.readVarInt();
        this.secondaryPotionAmplifier = buf.readVarInt();
        this.unlockedMoves = buf.readList(MoveView::new);
        this.unlockedCatalysts = buf.readList(FriendlyByteBuf::readUtf);
        this.slots = buf.readList(FormSlotView::new);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(xpLevel);
        buf.writeVarInt(muzanBlood);
        buf.writeVarInt(unlockedSlots);
        buf.writeBoolean(hasCustomItem);
        buf.writeVarInt(selectedSlot);
        buf.writeUtf(artName);
        buf.writeInt(chatColor);
        buf.writeUtf(primaryParticle);
        buf.writeInt(primaryParticleColor);
        buf.writeFloat(primaryParticleSize);
        buf.writeUtf(primaryParticleBlockStateId);
        buf.writeUtf(secondaryParticle);
        buf.writeInt(secondaryParticleColor);
        buf.writeFloat(secondaryParticleSize);
        buf.writeUtf(secondaryParticleBlockStateId);
        buf.writeUtf(primaryPotion);
        buf.writeUtf(secondaryPotion);
        buf.writeBoolean(primaryPotionSelfEffect);
        buf.writeBoolean(secondaryPotionSelfEffect);
        buf.writeVarInt(primaryPotionDurationSeconds);
        buf.writeVarInt(secondaryPotionDurationSeconds);
        buf.writeVarInt(primaryPotionAmplifier);
        buf.writeVarInt(secondaryPotionAmplifier);
        buf.writeCollection(unlockedMoves, (packetBuffer, move) -> move.write(packetBuffer));
        buf.writeCollection(unlockedCatalysts, FriendlyByteBuf::writeUtf);
        buf.writeCollection(slots, (packetBuffer, slot) -> slot.write(packetBuffer));
    }

    public int xpLevel() {
        return xpLevel;
    }

    public int muzanBlood() {
        return muzanBlood;
    }

    public int unlockedSlots() {
        return unlockedSlots;
    }

    public boolean hasCustomItem() {
        return hasCustomItem;
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public String artName() {
        return artName;
    }

    public int chatColor() {
        return chatColor;
    }

    public String primaryParticle() {
        return primaryParticle;
    }

    public int primaryParticleColor() {
        return primaryParticleColor;
    }

    public float primaryParticleSize() {
        return primaryParticleSize;
    }

    public String primaryParticleBlockStateId() {
        return primaryParticleBlockStateId;
    }

    public String secondaryParticle() {
        return secondaryParticle;
    }

    public int secondaryParticleColor() {
        return secondaryParticleColor;
    }

    public float secondaryParticleSize() {
        return secondaryParticleSize;
    }

    public String secondaryParticleBlockStateId() {
        return secondaryParticleBlockStateId;
    }

    public String primaryPotion() {
        return primaryPotion;
    }

    public String secondaryPotion() {
        return secondaryPotion;
    }

    public boolean primaryPotionSelfEffect() {
        return primaryPotionSelfEffect;
    }

    public boolean secondaryPotionSelfEffect() {
        return secondaryPotionSelfEffect;
    }

    public int primaryPotionDurationSeconds() {
        return primaryPotionDurationSeconds;
    }

    public int secondaryPotionDurationSeconds() {
        return secondaryPotionDurationSeconds;
    }

    public int primaryPotionAmplifier() {
        return primaryPotionAmplifier;
    }

    public int secondaryPotionAmplifier() {
        return secondaryPotionAmplifier;
    }

    public List<MoveView> unlockedMoves() {
        return unlockedMoves;
    }

    public List<String> unlockedCatalysts() {
        return unlockedCatalysts;
    }

    public List<FormSlotView> slots() {
        return slots;
    }

    public static BloodDemonArtBuilderData fromPlayerData(int xpLevel, int muzanBlood, int unlockedSlots, boolean hasCustomItem,
                                                          CustomBloodDemonArtSavedData.PlayerArtData playerData) {
        List<FormSlotView> slots = new ArrayList<>();
        for (int i = 0; i < playerData.slots().size(); i++) {
            CustomBloodDemonArtSavedData.CustomFormSlot slot = playerData.slots().get(i);
            List<MoveView> moveSummaries = new ArrayList<>();
            for (CustomBloodDemonArtSavedData.MoveType move : slot.moves()) {
                CustomBloodDemonArtSavedData.MoveEffectBinding binding = slot.bindingForMove(move);
                moveSummaries.add(MoveView.fromMove(move, binding.bindingSource().serializedName()));
            }
            List<String> amplifiers = new ArrayList<>();
            slot.amplifierCounts().forEach((kind, count) -> {
                if (kind != null && kind != com.lerdorf.kimetsunoyaibamultiplayer.alchemy.BloodDemonArtAlchemyCatalog.AmplifierKind.NONE && count > 0) {
                    amplifiers.add(kind.name().toLowerCase() + " x" + count);
                }
            });
            slots.add(new FormSlotView(
                i,
                i < unlockedSlots,
                slot.filled(),
                slot.name(),
                moveSummaries,
                slot.cooldownSeconds(),
                amplifiers
            ));
        }
        List<MoveView> unlockedMoves = new ArrayList<>();
        for (CustomBloodDemonArtSavedData.MoveType move : playerData.availableMoves()) {
            unlockedMoves.add(MoveView.fromMove(move, "none"));
        }
        CustomBloodDemonArtSavedData.PotionSetting primaryPotion = playerData.coreSettings().primaryPotion();
        CustomBloodDemonArtSavedData.PotionSetting secondaryPotion = playerData.coreSettings().secondaryPotion();
        return new BloodDemonArtBuilderData(
            xpLevel,
            muzanBlood,
            unlockedSlots,
            hasCustomItem,
            playerData.selectedSlot(),
            playerData.artName(),
            playerData.coreSettings().chatColor(),
            playerData.coreSettings().primaryParticle().particleId(),
            playerData.coreSettings().primaryParticle().color(),
            playerData.coreSettings().primaryParticle().size(),
            playerData.coreSettings().primaryParticle().blockStateId(),
            playerData.coreSettings().secondaryParticle().particleId(),
            playerData.coreSettings().secondaryParticle().color(),
            playerData.coreSettings().secondaryParticle().size(),
            playerData.coreSettings().secondaryParticle().blockStateId(),
            primaryPotion.effectId(),
            secondaryPotion.effectId(),
            primaryPotion.selfEffect(),
            secondaryPotion.selfEffect(),
            primaryPotion.durationSeconds(),
            secondaryPotion.durationSeconds(),
            primaryPotion.amplifier(),
            secondaryPotion.amplifier(),
            unlockedMoves,
            new ArrayList<>(playerData.catalystIds()),
            slots
        );
    }

    public record FormSlotView(int index, boolean unlocked, boolean filled, String name, List<MoveView> moves, int cooldownSeconds,
                               List<String> amplifiers) {
        public FormSlotView(FriendlyByteBuf buf) {
            this(
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(),
                readMoves(buf),
                buf.readVarInt(),
                readStrings(buf)
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(index);
            buf.writeBoolean(unlocked);
            buf.writeBoolean(filled);
            buf.writeUtf(name);
            buf.writeCollection(moves, (packetBuffer, move) -> move.write(packetBuffer));
            buf.writeVarInt(cooldownSeconds);
            buf.writeVarInt(amplifiers.size());
            for (String amplifier : amplifiers) {
                buf.writeUtf(amplifier);
            }
        }

        private static List<MoveView> readMoves(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<MoveView> moves = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                moves.add(new MoveView(buf));
            }
            return moves;
        }

        private static List<String> readStrings(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<String> strings = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                strings.add(buf.readUtf());
            }
            return strings;
        }
    }

    public record MoveView(String id, String name, String description, int xpCost, int cooldownSeconds, String bindingSource) {
        public MoveView(FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf());
        }

        public static MoveView fromMove(CustomBloodDemonArtSavedData.MoveType move, String bindingSource) {
            return new MoveView(
                move.serializedName(),
                move.displayName(),
                move.description(),
                move.xpCost(),
                move.cooldownSeconds(),
                bindingSource == null || bindingSource.isBlank() ? "none" : bindingSource
            );
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(id);
            buf.writeUtf(name);
            buf.writeUtf(description);
            buf.writeVarInt(xpCost);
            buf.writeVarInt(cooldownSeconds);
            buf.writeUtf(bindingSource);
        }
    }
}
