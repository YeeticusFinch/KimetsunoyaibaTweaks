package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.BaseKnYForms;
import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class BreathingFormAnnouncementHelper {
    private BreathingFormAnnouncementHelper() {
    }

    public static void announceCustomForm(LivingEntity source, Component formName, String techniqueColor) {
        if (source == null || formName == null) {
            return;
        }

        boolean enabled = source instanceof Player
            ? Config.playersAnnounceBreathingForms
            : Config.entitiesAnnounceBreathingForms;
        if (!enabled) {
            return;
        }

        announce(source, formName, techniqueColor);
    }

    public static void announceCustomForm(LivingEntity source, Component formName, int color) {
        if (source == null || formName == null) {
            return;
        }

        boolean enabled = source instanceof Player
            ? Config.playersAnnounceBreathingForms
            : Config.entitiesAnnounceBreathingForms;
        if (!enabled) {
            return;
        }

        announce(source, formName, color);
    }

    public static void announceBaseModForm(LivingEntity source, int formId) {
        BaseKnYForms.BaseForm baseForm = BaseKnYForms.forms.get(formId);
        if (baseForm == null || baseForm.name == null || baseForm.name.isBlank()) {
            return;
        }

        boolean enabled = source instanceof Player
            ? Config.playersAnnounceBreathingForms
            : Config.entitiesAnnounceBreathingForms;
        if (!enabled) {
            return;
        }

        announce(source, LocalizationHelper.breathingForm(formId), baseForm.color);
    }

    private static void announce(LivingEntity source, Component formName, String colorCode) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double radius = Config.breathingFormAnnouncementRadius;
        double radiusSq = radius * radius;
        MutableComponent message = Component.literal("<" + source.getName().getString() + "> ");
        MutableComponent formComponent = formName.copy();
        ChatFormatting color = parseLegacyColor(colorCode);
        if (color != null) {
            formComponent.withStyle(color);
        }
        message.append(formComponent);

        for (ServerPlayer nearbyPlayer : serverLevel.getEntitiesOfClass(
            ServerPlayer.class,
            new AABB(source.blockPosition()).inflate(radius)
        )) {
            if (nearbyPlayer.distanceToSqr(source) <= radiusSq) {
                nearbyPlayer.sendSystemMessage(message);
            }
        }
    }

    private static void announce(LivingEntity source, Component formName, int color) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double radius = Config.breathingFormAnnouncementRadius;
        double radiusSq = radius * radius;
        MutableComponent message = Component.literal("<" + source.getName().getString() + "> ");
        MutableComponent formComponent = formName.copy();
        formComponent.withStyle(style -> style.withColor(color));
        message.append(formComponent);

        for (ServerPlayer nearbyPlayer : serverLevel.getEntitiesOfClass(
            ServerPlayer.class,
            new AABB(source.blockPosition()).inflate(radius)
        )) {
            if (nearbyPlayer.distanceToSqr(source) <= radiusSq) {
                nearbyPlayer.sendSystemMessage(message);
            }
        }
    }

    private static ChatFormatting parseLegacyColor(String colorCode) {
        if (colorCode == null || colorCode.length() < 2 || colorCode.charAt(0) != '§') {
            return null;
        }
        return ChatFormatting.getByCode(colorCode.charAt(1));
    }
}
