package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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

	private static final double CUSTOM_DAMAGE = 6.5; // +4.5 attack damage (base punch is 1, +1 from entity base, total shown: +4.5)
    private static final double ATTACK_SPEED = -2.4F;

    public BreathingSwordItem(Properties properties) {
        // Pass 0 for attack damage - we override with custom attribute modifiers
        super(Tiers.DIAMOND, 0, (float) ATTACK_SPEED, properties);
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
            // Check if player has cool_time effect from KnY mod (prevents ability usage)
            if (com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.hasCoolTime(player)) {
                if (level.isClientSide) {
                    player.displayClientMessage(
                        Component.literal("§cAbility on cooldown!"),
                        true
                    );
                }
                return InteractionResultHolder.fail(stack);
            }

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
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

            // Attack damage: base entity damage is 1, we add 4.5 to make the tooltip show "+4.5 Attack Damage"
            builder.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 3.5, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", ATTACK_SPEED, AttributeModifier.Operation.ADDITION));

            return builder.build();
        }
        return super.getDefaultAttributeModifiers(slot);
    }

    /**
     * Cycle to the next form (called when R key is pressed)
     */
    public void cycleForm(Player player) {
        cycleForm(player, false);
    }

    /**
     * Cycle to the next or previous form
     * @param player The player cycling forms
     * @param backward If true, cycle backward; if false, cycle forward
     */
    public void cycleForm(Player player, boolean backward) {
        BreathingTechnique technique = getBreathingTechnique();
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player.getUUID());

        if (backward) {
            data.cycleFormBackward(technique.getFormCount());
        } else {
            data.cycleForm(technique.getFormCount());
        }

        int newIndex = data.getCurrentFormIndex();
        BreathingForm form = technique.getForm(newIndex);

        if (form != null) {
            // Send chat message about the new form (unless suppressed by config)
            // Using bold text and technique-specific colors
            if (!com.lerdorf.kimetsunoyaibamultiplayer.Config.suppressFormCycleChat) {
                String techniqueColor = technique.getTechniqueColor();
                String formColor = technique.getFormColor();

                // Format: §l§<color>Technique Name§r - §l§<color>Form Name
                String message = "§l" + techniqueColor + technique.getName() + "§r - §l" + formColor + form.getName();

                player.sendSystemMessage(Component.literal(message));
            }
        }
    }

    /**
     * Called when this sword successfully hits a living entity.
     * Plays the sweep attack sound for all nichirin swords, plus any custom swing sound
     * that was registered via the API.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        if (!attacker.level().isClientSide) {
            // Play sweep attack sound (default for all nichirin swords)
            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);

            // Play custom swing sound if registered
            SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(this);
            if (registeredSword != null && registeredSword.getSwingSound() != null) {
                attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    registeredSword.getSwingSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        return result;
    }
}
