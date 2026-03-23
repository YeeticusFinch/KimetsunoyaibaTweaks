package com.lerdorf.kimetsunoyaibamultiplayer.items;

import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.PlayerBreathingData;
import com.lerdorf.kimetsunoyaibamultiplayer.effects.VermilionEyeEffect;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingFormAnnouncementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.LocalizationHelper;
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

        // Load player data from NBT if on server
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);

        // Determine selected form from our index, and variation from tracked value (not breathes)
        int baseFormIndex = data.getCurrentFormIndex();
        int variationIndex = data.getCurrentVariationIndex();

        // Get the current form to access its form ID (use base form index)
        BreathingForm form = technique.getForm(baseFormIndex);
        if (form == null) {
            return InteractionResultHolder.pass(stack);
        }

        // Get registered sword info for variation lookup
        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(this);
        String swordId = registeredSword != null ? registeredSword.getSwordId() : this.toString();

        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug(
                "[BreathingSwordItem] Form ID: " + form.getFormId() +
                ", Sword ID: " + swordId +
                ", Current variation index: " + variationIndex
            );
        }

        // Clamp variation to available variations
        int totalVariations = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry
            .getVariationCount(form.getFormId(), swordId);
        if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("[BreathingSwordItem] Total variations found: " + totalVariations);
        }

        if (totalVariations == 0 && variationIndex != 0) {
            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug(
                    "[BreathingSwordItem] No variations found for this form/sword. Resetting variation index to 0."
                );
            }
            variationIndex = 0;
            data.setCurrentVariationIndex(0);
        } else if (variationIndex > totalVariations) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.warn(
                "[BreathingSwordItem] Variation index " + variationIndex +
                " exceeds total " + totalVariations +
                " for form " + form.getFormId() +
                " and sword " + swordId +
                ". Resetting to 0."
            );
            variationIndex = 0;
            data.setCurrentVariationIndex(0);
        }

        // Check for active variation using form ID
        com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation variation = null;
        if (variationIndex > 0) {
            variation = com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry.getVariation(
                form.getFormId(), variationIndex, swordId);
        }

        // Determine which effect to use and which cooldown
        String displayName;
        int cooldownSeconds;

        if (variation != null) {
            // Use variation
            displayName = variation.getName();
            cooldownSeconds = variation.getCooldownSeconds();

            // Debug logging
            if (!level.isClientSide && com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("BreathingSwordItem.use() - Server side (VARIATION)");
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Player: " + player.getName().getString());
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Sword: " + this.getClass().getSimpleName());
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Technique: " + technique.getName());
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Form Index: " + baseFormIndex);
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Variation Index: " + variationIndex);
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Variation: " + variation.getName());
            }

            // Check cooldowns
            if (com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.hasCoolTime(player)) {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }

            if (!player.getCooldowns().isOnCooldown(this)) {
                // Execute variation effect ONLY on server
                if (!level.isClientSide) {
                    variation.getEffect().execute(player, level, form.getFormId());
                    BreathingFormAnnouncementHelper.announceCustomForm(
                        player, variation.getDisplayName(), technique.getTechniqueColor());
                    // Apply Vermilion Eye cooldown reduction if active (40% faster cooldowns)
                    int cooldownTicks = VermilionEyeEffect.applyCooldownReductionTicks(player, cooldownSeconds * 20);
                    player.getCooldowns().addCooldown(this, cooldownTicks);
                }

                // Send action bar message
                player.displayClientMessage(variation.getDisplayName().copy().withStyle(style -> style.withColor(0x55FFFF)), true);
                return InteractionResultHolder.success(stack);
            } else {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        } else {
            // Use base form
            displayName = form.getName();
            cooldownSeconds = form.getCooldownSeconds();

            // Debug logging
            if (!level.isClientSide && com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("BreathingSwordItem.use() - Server side");
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Player: " + player.getName().getString());
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Sword: " + this.getClass().getSimpleName());
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Technique: " + technique.getName());
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Form Index: " + baseFormIndex);
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Form: " + form.getName());
            }

            // Check if player has cool_time effect from KnY mod (prevents ability usage)
            if (com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.KnYEffects.hasCoolTime(player)) {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }

            // Check item cooldown
            if (!player.getCooldowns().isOnCooldown(this)) {
                // Execute the form effect ONLY on server
                if (!level.isClientSide) {
                    form.execute(player, level);
                    BreathingFormAnnouncementHelper.announceCustomForm(
                        player, form.getDisplayName(), technique.getTechniqueColor());
                    // Apply Vermilion Eye cooldown reduction if active (40% faster cooldowns)
                    int cooldownTicks = VermilionEyeEffect.applyCooldownReductionTicks(player, cooldownSeconds * 20);
                    player.getCooldowns().addCooldown(this, cooldownTicks);
                }

                // Send action bar message (both sides for immediate feedback)
                player.displayClientMessage(form.getDisplayName().copy().withStyle(style -> style.withColor(0x55FFFF)), true);
                return InteractionResultHolder.success(stack);
            } else {
                // Still on cooldown
                if (level.isClientSide) {
                    player.displayClientMessage(Component.literal("§cAbility on cooldown!"), true);
                }
                return InteractionResultHolder.fail(stack);
            }
        }
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
        PlayerBreathingData.PlayerData data = PlayerBreathingData.getOrCreate(player);

        if (backward) {
            data.cycleFormBackward(technique.getFormCount());
        } else {
            data.cycleForm(technique.getFormCount());
        }

        int newIndex = data.getCurrentFormIndex();
        BreathingForm form = technique.getForm(newIndex);

        // CRITICAL: Also update the "breathes" NBT tag with the form's ID
        // This is what the execution system reads when activating forms
        if (form != null && !player.level().isClientSide) {
            double formId = form.getFormId();
            player.getPersistentData().putDouble("breathes", formId);

            // Also update the cached base mod breathes value
            data.setBaseModBreathesValue(formId);

            if (com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
                com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("Updated breathes tag to formId: " + formId);
            }
        }

        // Debug logging
        if (!player.level().isClientSide && com.lerdorf.kimetsunoyaibamultiplayer.Config.logDebug) {
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("BreathingSwordItem.cycleForm() - Server side");
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Player: " + player.getName().getString());
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Technique: " + technique.getName());
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Direction: " + (backward ? "backward" : "forward"));
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  New Index: " + newIndex);
            com.lerdorf.kimetsunoyaibamultiplayer.Log.debug("  Form: " + (form != null ? form.getName() : "null"));
        }

        // Save to NBT if on server
        if (!player.level().isClientSide) {
            PlayerBreathingData.saveToNBT(player);

            // Broadcast form change to all clients
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                // Reset variation on form change
                PlayerBreathingData.PlayerData pdata = PlayerBreathingData.getOrCreate(player);
                pdata.setCurrentVariationIndex(0);
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.VariationIndexSyncPacket(
                        player.getUUID(), 0),
                    serverPlayer
                );
                // Sync form index (for custom sword tracking)
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClients(
                    new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.FormSyncPacket(
                        player.getUUID(),
                        newIndex
                    )
                );

                // Sync breathes value (for form execution and display)
                if (form != null) {
                    com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToPlayer(
                        new com.lerdorf.kimetsunoyaibamultiplayer.network.packets.BreathesValueSyncPacket(
                            player.getUUID(),
                            form.getFormId()
                        ),
                        serverPlayer
                    );
                }
            }
        }

        if (form != null) {
            // Send chat message about the new form (unless suppressed by config)
            // Using bold text and technique-specific colors
            // Only send on server to avoid duplicate messages
            if (!player.level().isClientSide && !com.lerdorf.kimetsunoyaibamultiplayer.Config.suppressFormCycleChat) {
                player.sendSystemMessage(
                    LocalizationHelper.breathingStyleFromFormId(form.getFormId(), technique.getName()).copy()
                        .append(Component.literal(" "))
                        .append(form.getDisplayName())
                );
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
        // Vanilla SwordItem always damages mainhand. For Beast/Inosuke dual-wielding,
        // split durability loss 50/50 between mainhand and offhand swords.
        ItemStack mainHand = attacker.getMainHandItem();
        ItemStack offHand = attacker.getOffhandItem();

        boolean dualWieldBeastSwords = isBeastOrInosukeSword(mainHand) && isBeastOrInosukeSword(offHand);

        if (dualWieldBeastSwords) {
            boolean damageOffhand = attacker.getRandom().nextBoolean();
            ItemStack durabilityTarget = damageOffhand ? offHand : mainHand;
            EquipmentSlot breakSlot = damageOffhand ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;

            durabilityTarget.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(breakSlot));
        } else {
            stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }

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

        return true;
    }

    private static boolean isBeastOrInosukeSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof BreathingSwordItem breathingSword)) {
            return false;
        }

        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(breathingSword);
        if (registeredSword == null || registeredSword.getSwordId() == null) {
            return false;
        }

        String swordId = registeredSword.getSwordId();
        return "nichirinsword_beast".equals(swordId) || "nichirinsword_inosuke".equals(swordId);
    }
}
