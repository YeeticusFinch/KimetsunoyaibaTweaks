package com.lerdorf.kimetsunoyaibamultiplayer.alchemy;

import com.lerdorf.kimetsunoyaibamultiplayer.util.WisteriaResistanceHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public class WisteriaResistanceCureBrewingRecipe implements IBrewingRecipe {
    private static final String CURE_ID = "kimetsunoyaibamultiplayer:wisteria_resistance_cure";
    private final Kind kind;

    public WisteriaResistanceCureBrewingRecipe(Kind kind) {
        this.kind = kind;
    }

    @Override
    public boolean isInput(ItemStack input) {
        return switch (kind) {
            case BASE -> BloodDemonArtAlchemyCatalog.matches(input, "kimetsunoyaibamultiplayer:wisteria_infusion");
            case POTENCY, DURATION -> BloodDemonArtAlchemyCatalog.matches(input, CURE_ID);
        };
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return BloodDemonArtAlchemyCatalog.matches(ingredient, kind.ingredientId);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }

        return switch (kind) {
            case BASE -> BloodDemonArtAlchemyCatalog.withInfusionProperties(
                BloodDemonArtAlchemyCatalog.stack(CURE_ID),
                WisteriaResistanceHelper.DEFAULT_DURATION_SECONDS,
                WisteriaResistanceHelper.DEFAULT_AMPLIFIER
            );
            case POTENCY -> potencyOutput(input);
            case DURATION -> durationOutput(input);
        };
    }

    private static ItemStack potencyOutput(ItemStack input) {
        int amplifier = BloodDemonArtAlchemyCatalog.infusionAmplifier(input);
        if (amplifier >= WisteriaResistanceHelper.MAX_AMPLIFIER) {
            return ItemStack.EMPTY;
        }
        ItemStack output = input.copy();
        output.setCount(1);
        return BloodDemonArtAlchemyCatalog.withInfusionProperties(
            output,
            BloodDemonArtAlchemyCatalog.infusionDurationSeconds(input),
            Math.min(WisteriaResistanceHelper.MAX_AMPLIFIER, amplifier + 1)
        );
    }

    private static ItemStack durationOutput(ItemStack input) {
        int durationSeconds = BloodDemonArtAlchemyCatalog.infusionDurationSeconds(input);
        if (durationSeconds >= WisteriaResistanceHelper.EXTENDED_DURATION_SECONDS) {
            return ItemStack.EMPTY;
        }
        ItemStack output = input.copy();
        output.setCount(1);
        return BloodDemonArtAlchemyCatalog.withInfusionProperties(
            output,
            WisteriaResistanceHelper.EXTENDED_DURATION_SECONDS,
            BloodDemonArtAlchemyCatalog.infusionAmplifier(input)
        );
    }

    public enum Kind {
        BASE("kimetsunoyaibamultiplayer:vitality_culture"),
        POTENCY("minecraft:glowstone_dust"),
        DURATION("minecraft:redstone");

        private final String ingredientId;

        Kind(String ingredientId) {
            this.ingredientId = ingredientId;
        }
    }
}
