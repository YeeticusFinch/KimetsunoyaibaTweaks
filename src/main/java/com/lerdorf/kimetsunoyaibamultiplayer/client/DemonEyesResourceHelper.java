package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.KimetsunoyaibaMultiplayer;
import com.lerdorf.kimetsunoyaibamultiplayer.util.DemonEyesHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public final class DemonEyesResourceHelper {
    private static final Pattern DEMON_EYES_PATTERN = Pattern.compile("textures/entity/demon_eyes_(\\d+)\\.png");

    private DemonEyesResourceHelper() {
    }

    public static List<Integer> getAvailableIndices() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return List.of(DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX);
        }

        List<Integer> indices = new ArrayList<>();
        minecraft.getResourceManager().listResources("textures/entity", location ->
            KimetsunoyaibaMultiplayer.MODID.equals(location.getNamespace()) &&
                DEMON_EYES_PATTERN.matcher(location.getPath()).matches()
        ).keySet().forEach(location -> {
            Matcher matcher = DEMON_EYES_PATTERN.matcher(location.getPath());
            if (matcher.matches()) {
                indices.add(Integer.parseInt(matcher.group(1)));
            }
        });

        if (indices.isEmpty()) {
            indices.add(DemonEyesHelper.DEFAULT_DEMON_EYES_INDEX);
        }
        indices.sort(Comparator.naturalOrder());
        return indices;
    }

    public static ResourceLocation getTexture(int index) {
        return ResourceLocation.fromNamespaceAndPath(
            KimetsunoyaibaMultiplayer.MODID,
            "textures/entity/demon_eyes_" + Math.max(0, index) + ".png"
        );
    }
}
