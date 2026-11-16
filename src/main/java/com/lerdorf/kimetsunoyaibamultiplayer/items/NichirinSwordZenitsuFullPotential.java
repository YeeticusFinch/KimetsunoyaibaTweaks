package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.ThunderBreathingForms;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Zenitsu's Full Potential sword with First Form variations and forms 1-7. */
public class NichirinSwordZenitsuFullPotential extends BreathingSwordItem {
    private static final BreathingTechnique THUNDER_BREATHING =
        ThunderBreathingForms.createZenitsuFullPotentialBreathing();
    private static final double ATTACK_DAMAGE_MODIFIER = 10.0D;

    public NichirinSwordZenitsuFullPotential(Properties properties) {
        super(properties);
    }

    @Override
    public BreathingTechnique getBreathingTechnique() {
        return THUNDER_BREATHING;
    }

    @Override
    protected double getAttackDamageModifier() {
        return ATTACK_DAMAGE_MODIFIER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && ThunderBreathingForms.handleZenitsuClick(player)) {
            return InteractionResultHolder.success(stack);
        }
        return super.use(level, player, hand);
    }
}
