package com.lerdorf.kimetsunoyaibamultiplayer.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class QuestStepDefinition {
    private final String id;
    private final String title;
    private final String description;
    private final QuestStepType type;
    private final ResourceLocation targetId;
    private final String targetKey;
    private final int requiredCount;
    private final BiConsumer<ServerPlayer, QuestRuntimeContext> onStart;
    private final BiConsumer<ServerPlayer, QuestRuntimeContext> onTick;
    private final BiConsumer<ServerPlayer, QuestRuntimeContext> onComplete;
    private final BiPredicate<ServerPlayer, QuestRuntimeContext> customCheck;
    private final BiFunction<ServerPlayer, QuestRuntimeContext, BlockPos> markerResolver;

    private QuestStepDefinition(String id, String title, String description, QuestStepType type,
                                ResourceLocation targetId, String targetKey, int requiredCount,
                                BiConsumer<ServerPlayer, QuestRuntimeContext> onStart,
                                BiConsumer<ServerPlayer, QuestRuntimeContext> onTick,
                                BiConsumer<ServerPlayer, QuestRuntimeContext> onComplete,
                                BiPredicate<ServerPlayer, QuestRuntimeContext> customCheck,
                                BiFunction<ServerPlayer, QuestRuntimeContext, BlockPos> markerResolver) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.targetId = targetId;
        this.targetKey = targetKey;
        this.requiredCount = requiredCount;
        this.onStart = onStart;
        this.onTick = onTick;
        this.onComplete = onComplete;
        this.customCheck = customCheck;
        this.markerResolver = markerResolver;
    }

    public static Builder builder(String id, String title, String description, QuestStepType type) {
        return new Builder(id, title, description, type);
    }

    public static QuestStepDefinition enterStructure(String id, String title, String description, ResourceLocation structureId) {
        return builder(id, title, description, QuestStepType.ENTER_STRUCTURE).targetId(structureId).build();
    }

    public static QuestStepDefinition enterBiome(String id, String title, String description, ResourceLocation biomeId) {
        return builder(id, title, description, QuestStepType.ENTER_BIOME).targetId(biomeId).build();
    }

    public static QuestStepDefinition killEntity(String id, String title, String description, ResourceLocation entityId) {
        return builder(id, title, description, QuestStepType.KILL_ENTITY).targetId(entityId).build();
    }

    public static QuestStepDefinition killQuestTarget(String id, String title, String description, String targetKey) {
        return builder(id, title, description, QuestStepType.KILL_ENTITY).targetKey(targetKey).build();
    }

    public static QuestStepDefinition talkToEntity(String id, String title, String description, String targetKey) {
        return builder(id, title, description, QuestStepType.TALK_TO_ENTITY).targetKey(targetKey).build();
    }

    public static QuestStepDefinition obtainItem(String id, String title, String description, ResourceLocation itemId, int count) {
        return builder(id, title, description, QuestStepType.OBTAIN_ITEM).targetId(itemId).requiredCount(count).build();
    }

    public static QuestStepDefinition waitForNight(String id, String title, String description) {
        return builder(id, title, description, QuestStepType.WAIT_FOR_NIGHT).build();
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public QuestStepType type() {
        return type;
    }

    public ResourceLocation targetId() {
        return targetId;
    }

    public String targetKey() {
        return targetKey;
    }

    public int requiredCount() {
        return requiredCount;
    }

    public BiConsumer<ServerPlayer, QuestRuntimeContext> onStart() {
        return onStart;
    }

    public BiConsumer<ServerPlayer, QuestRuntimeContext> onTick() {
        return onTick;
    }

    public BiConsumer<ServerPlayer, QuestRuntimeContext> onComplete() {
        return onComplete;
    }

    public BiPredicate<ServerPlayer, QuestRuntimeContext> customCheck() {
        return customCheck;
    }

    public BiFunction<ServerPlayer, QuestRuntimeContext, BlockPos> markerResolver() {
        return markerResolver;
    }

    public static class Builder {
        private final String id;
        private final String title;
        private final String description;
        private final QuestStepType type;
        private ResourceLocation targetId;
        private String targetKey = "";
        private int requiredCount = 1;
        private BiConsumer<ServerPlayer, QuestRuntimeContext> onStart = (player, context) -> { };
        private BiConsumer<ServerPlayer, QuestRuntimeContext> onTick = (player, context) -> { };
        private BiConsumer<ServerPlayer, QuestRuntimeContext> onComplete = (player, context) -> { };
        private BiPredicate<ServerPlayer, QuestRuntimeContext> customCheck = (player, context) -> false;
        private BiFunction<ServerPlayer, QuestRuntimeContext, BlockPos> markerResolver = (player, context) -> null;

        private Builder(String id, String title, String description, QuestStepType type) {
            this.id = Objects.requireNonNull(id);
            this.title = Objects.requireNonNull(title);
            this.description = Objects.requireNonNull(description);
            this.type = Objects.requireNonNull(type);
        }

        public Builder targetId(ResourceLocation targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder targetKey(String targetKey) {
            this.targetKey = targetKey;
            return this;
        }

        public Builder requiredCount(int requiredCount) {
            this.requiredCount = requiredCount;
            return this;
        }

        public Builder onStart(BiConsumer<ServerPlayer, QuestRuntimeContext> onStart) {
            this.onStart = onStart;
            return this;
        }

        public Builder onTick(BiConsumer<ServerPlayer, QuestRuntimeContext> onTick) {
            this.onTick = onTick;
            return this;
        }

        public Builder onComplete(BiConsumer<ServerPlayer, QuestRuntimeContext> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        public Builder customCheck(BiPredicate<ServerPlayer, QuestRuntimeContext> customCheck) {
            this.customCheck = customCheck;
            return this;
        }

        public Builder markerResolver(BiFunction<ServerPlayer, QuestRuntimeContext, BlockPos> markerResolver) {
            this.markerResolver = markerResolver;
            return this;
        }

        public QuestStepDefinition build() {
            return new QuestStepDefinition(id, title, description, type, targetId, targetKey, requiredCount,
                onStart, onTick, onComplete, customCheck, markerResolver);
        }
    }
}
