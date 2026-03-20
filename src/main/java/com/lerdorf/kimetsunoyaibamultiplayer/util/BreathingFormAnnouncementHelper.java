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

    public static void announceCustomForm(LivingEntity source, String techniqueName, String techniqueColor, String formName) {
        if (source == null || formName == null || formName.isBlank()) {
            return;
        }

        boolean enabled = source instanceof Player
            ? Config.playersAnnounceBreathingForms
            : Config.entitiesAnnounceBreathingForms;
        if (!enabled) {
            return;
        }

        announce(source, buildTechniqueAwareFormName(techniqueName, formName), techniqueColor);
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

        announce(source, buildTechniqueAwareFormName(resolveBaseModTechniqueName(formId), baseForm.name), baseForm.color);
    }

    private static void announce(LivingEntity source, String formName, String colorCode) {
        if (!(source.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double radius = Config.breathingFormAnnouncementRadius;
        double radiusSq = radius * radius;
        MutableComponent message = Component.literal("<" + source.getName().getString() + "> ");
        MutableComponent formComponent = Component.literal(formName);
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

    private static String buildTechniqueAwareFormName(String techniqueName, String formName) {
        if (techniqueName == null || techniqueName.isBlank()) {
            return formName;
        }

        String normalizedTechnique = techniqueName.trim().toLowerCase();
        String normalizedForm = formName.trim().toLowerCase();
        if (normalizedForm.contains(normalizedTechnique)) {
            return formName;
        }

        return techniqueName + " " + formName;
    }

    private static String resolveBaseModTechniqueName(int formId) {
        int styleRange = (formId / 100) * 100;
        return switch (styleRange) {
            case 0 -> "Bamboo Breathing";
            case 100 -> "Water Breathing";
            case 200 -> "Beast Breathing";
            case 300 -> "Thunder Breathing";
            case 400 -> "Flame Breathing";
            case 500 -> "Wind Breathing";
            case 600, 1600 -> "Stone Breathing";
            case 700 -> "Mist Breathing";
            case 800 -> "Serpent Breathing";
            case 900 -> "Sound Breathing";
            case 1100 -> "Moon Breathing";
            case 1200 -> "Sun Breathing";
            case 1300 -> "Flower Breathing";
            case 1400 -> "Insect Breathing";
            case 1500 -> "Love Breathing";
            case 1800 -> "Sakura Breathing";
            default -> "";
        };
    }

    private static ChatFormatting parseLegacyColor(String colorCode) {
        if (colorCode == null || colorCode.length() < 2 || colorCode.charAt(0) != '§') {
            return null;
        }
        return ChatFormatting.getByCode(colorCode.charAt(1));
    }
}
