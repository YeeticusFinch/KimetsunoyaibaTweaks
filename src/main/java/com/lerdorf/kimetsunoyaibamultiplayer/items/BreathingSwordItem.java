package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Base class for breathing technique swords
 */
public abstract class BreathingSwordItem extends SwordItem {

    public BreathingSwordItem(Properties properties) {
        super(Tiers.DIAMOND, 0, -2.4F, properties); // 1 + 3 (base) = 4.5 damage (4 damage visible in game)
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
        BreathingTechnique technique = getBreathingTechnique();
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player.getUUID());

        int formIndex = data.getCurrentFormIndex();
        BreathingForm form = technique.getForm(formIndex);

        if (form != null) {
            // Check item cooldown
            if (!player.getCooldowns().isOnCooldown(this)) {
                // Execute the form effect
                form.getEffect().execute(player, level);

                // Apply item cooldown
                player.getCooldowns().addCooldown(this, form.getCooldownSeconds() * 20);

                // Send action bar message
                player.displayClientMessage(
                    Component.literal("§b" + form.getName()),
                    true
                );

                return InteractionResultHolder.success(stack);
            } else {
                // Still on cooldown
                if (level.isClientSide) {
                    player.displayClientMessage(
                        Component.literal("§cAbility on cooldown!"),
                        true
                    );
                }
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }
    
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> modifiers = super.getDefaultAttributeModifiers(slot);

        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            // copy others (like attack speed)
            builder.putAll(modifiers);

            // Replace attack damage with exactly 4.5
            builder.put(Attributes.ATTACK_DAMAGE, 
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 4.5, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return modifiers;
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
                Component.literal("§ " + technique.getName() + ": §b" + form.getName())
            );
        }
    }
}
