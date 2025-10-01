package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

/**
 * Base class for breathing technique swords
 */
public abstract class BreathingSwordItem extends SwordItem {

    public BreathingSwordItem(Properties properties) {
        super(Tiers.DIAMOND, 3, -2.4F, properties);
    }

    /**
     * Get the breathing technique for this sword
     */
    public abstract BreathingTechnique getBreathingTechnique();

    /**
     * Handle right-click to activate current form
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            BreathingTechnique technique = getBreathingTechnique();
            PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player.getUUID());

            int formIndex = data.getCurrentFormIndex();
            BreathingForm form = technique.getForm(formIndex);

            if (form != null) {
                long currentTick = level.getGameTime();
                long ticksSinceLastUse = currentTick - data.getLastUsedTick();

                if (ticksSinceLastUse >= form.getCooldownTicks()) {
                    // Execute the form effect
                    form.getEffect().execute(player, level);

                    // Update last used time
                    data.setLastUsedTick(currentTick);

                    // Send action bar message
                    player.displayClientMessage(
                        Component.literal("§b" + form.getName()),
                        true
                    );
                } else {
                    int secondsRemaining = (int) ((form.getCooldownTicks() - ticksSinceLastUse) / 20);
                    player.displayClientMessage(
                        Component.literal("§cCooldown: " + secondsRemaining + "s"),
                        true
                    );
                }
            }
        }

        return InteractionResultHolder.success(stack);
    }

    /**
     * Cycle to the next form (called when R key is pressed)
     */
    public void cycleForm(Player player) {
        BreathingTechnique technique = getBreathingTechnique();
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player.getUUID());

        data.cycleForm(technique.getFormCount());

        int newIndex = data.getCurrentFormIndex();
        BreathingForm form = technique.getForm(newIndex);

        if (form != null) {
            // Send chat message about the new form
            player.sendSystemMessage(
                Component.literal("§6Selected: §b" + form.getName())
            );
        }
    }
}
