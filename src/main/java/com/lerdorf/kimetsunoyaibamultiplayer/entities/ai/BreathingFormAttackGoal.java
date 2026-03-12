package com.lerdorf.kimetsunoyaibamultiplayer.entities.ai;

import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingFormVariation;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingTechnique;
import com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.VariationRegistry;
import com.lerdorf.kimetsunoyaibamultiplayer.entities.BreathingSlayerEntity;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModFormExecutionHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BreathingFormAnnouncementHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.BaseModStyleMapping;
import com.lerdorf.kimetsunoyaibamultiplayer.util.SunBreathingLevelHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;
import com.lerdorf.kimetsunoyaibamultiplayer.items.BreathingSwordItem;
import com.lerdorf.kimetsunoyaibamultiplayer.items.NichirinSwordBlack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AI goal that makes breathing slayer entities use their breathing forms in combat
 */
public class BreathingFormAttackGoal extends Goal {
    private final BreathingSlayerEntity entity;
    private final double attackRange = 24.0D;
    private final int minCooldownTicks = 40; // 2 seconds minimum between forms (non-Muichiro)
    private final double formUseChance = 0.15; // 15% chance per second to use form (non-Muichiro)

    public BreathingFormAttackGoal(BreathingSlayerEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.entity.isAlive() || this.entity.isDeadOrDying()) {
            return false;
        }

        if (this.entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity demonSlayer
            && demonSlayer.isActionLocked()) {
            return false;
        }

        LivingEntity target = this.entity.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Check if target is in range
        double distanceSq = this.entity.distanceToSqr(target);
        if (distanceSq > attackRange * attackRange) {
            return false;
        }

        // Check if breathing form is on cooldown
        if (this.entity.isBreathingFormOnCooldown()) {
            return false;
        }

        // Muichiro-specific checks
        if (this.entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroEntity muichiro) {
            // Cannot use abilities during transformation
            if (muichiro.isTransforming()) {
                return false;
            }
            // Otherwise cast as soon as cooldown finishes
            return true;
        }

        // Kanroji-specific checks (use forms aggressively like Muichiro)
        if (this.entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.KanrojiEntity kanroji) {
            // Cannot use abilities during transformation
            if (kanroji.isTransforming()) {
                return false;
            }
            // Otherwise cast as soon as cooldown finishes
            return true;
        }

        // Demon slayer senior tiers (level 4+) should cast immediately when cooldown ends,
        // as long as they are in combat (already guaranteed by target/range checks above).
        if (this.entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.DemonSlayerEntity demonSlayer
            && demonSlayer.getPowerLevel() >= 4) {
            return true;
        }

        // For other entities: Random chance to use form (checked every tick)
        return this.entity.getRandom().nextDouble() < (formUseChance / 20.0);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // One-shot goal
    }

    @Override
    public void start() {
        if (!this.entity.isAlive() || this.entity.isDeadOrDying()) {
            return;
        }

        LivingEntity target = this.entity.getTarget();
        if (target == null || !target.isAlive()) return;

        // IMPORTANT: Use the actual held stack first so TrainingSword NBT is visible.
        ItemStack swordStack = this.entity.getMainHandItem();
        if (swordStack.isEmpty()) {
            swordStack = this.entity.getEquippedSword();
        }

        // Pick a breathing form from the currently equipped sword's own technique (if available),
        // not from the broad style technique on the entity.
        BreathingTechnique technique = resolveTechniqueForSword(swordStack);
        BreathingForm form = pickFormForCurrentState(technique, target, swordStack);

        if (form != null) {
            if (!this.entity.isAlive() || this.entity.isDeadOrDying()) {
                return;
            }

            // Face the target immediately before executing the form
            faceTarget(target);

            // Check if this form has variations for the equipped sword
            BreathingFormVariation selectedVariation = null;
            if (!swordStack.isEmpty()) {
                SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(swordStack.getItem());
                String swordId = registeredSword != null ? registeredSword.getSwordId() : null;

                if (swordId != null) {
                    List<BreathingFormVariation> variations = VariationRegistry.getVariations(form.getFormId(), swordId);

                    // 50% chance to use a variation if available
                    if (!variations.isEmpty() && this.entity.getRandom().nextDouble() < 0.5) {
                        // Pick a random variation from the available ones
                        int randomIndex = this.entity.getRandom().nextInt(variations.size());
                        selectedVariation = variations.get(randomIndex);
                    }
                }
            }

            // Execute either the variation or the base form
            if (selectedVariation != null) {
                // Execute the variation's effect (formId is auto-injected)
                selectedVariation.getEffect().execute(this.entity, this.entity.level(), form.getFormId());
                BreathingFormAnnouncementHelper.announceCustomForm(
                    this.entity, technique.getName(), technique.getTechniqueColor(), selectedVariation.getName());
            } else {
                // Execute the base breathing form (formId is auto-injected)
                form.execute(this.entity, this.entity.level());
                BreathingFormAnnouncementHelper.announceCustomForm(
                    this.entity, technique.getName(), technique.getTechniqueColor(), form.getName());
            }

            // Set cooldown (use base form's cooldown)
            int cooldownTicks;
            if (this.entity instanceof com.lerdorf.kimetsunoyaibamultiplayer.entities.MuichiroEntity) {
                // 30% faster: wait only 60% of original cooldown
                cooldownTicks = Math.max(1, (int)Math.round(form.getCooldownSeconds() * 12.0));
            } else {
                cooldownTicks = Math.max(form.getCooldownSeconds() * 20, minCooldownTicks);
            }
            this.entity.setBreathingFormCooldown(cooldownTicks);
            return;
        }

        // If we had a sword-specific technique but no form was valid right now (distance constraints, etc.),
        // do not fall back to style-wide/base-mod form pools.
        if (technique != null && technique.getForms() != null && !technique.getForms().isEmpty()) {
            return;
        }

        // Fallback path: base-mod style execution for swords whose style is known but has no registered technique object.
        String styleId = resolveCurrentStyleId(swordStack);
        if (styleId == null) {
            return;
        }
        int styleRange = BaseModStyleMapping.getBreathesRange(styleId);
        if (styleRange <= 0) {
            return;
        }

        java.util.List<Integer> forms = resolveAllowedBaseModForms(swordStack, styleId, styleRange);

        if (swordStack.getItem() instanceof NichirinSwordBlack) {
            for (com.lerdorf.kimetsunoyaibamultiplayer.breathingtechnique.BreathingForm sunForm :
                SunBreathingLevelHelper.createUnlockedSunForms(SunBreathingLevelHelper.getSunBreathingLevel(this.entity))) {
                forms.add(sunForm.getFormId());
            }
        }

        if (forms.isEmpty()) {
            return;
        }

        faceTarget(target);

        int formId;
        if (isFirstFormOnlyRestricted(swordStack)) {
            formId = forms.get(0);
        } else {
            formId = forms.get(this.entity.getRandom().nextInt(forms.size()));
        }

        BreathingFormAnnouncementHelper.announceBaseModForm(this.entity, formId);
        BaseModFormExecutionHelper.executeBaseModForm(this.entity, this.entity.level(), formId);
        this.entity.setBreathingFormCooldown(Math.max(minCooldownTicks, 60));
    }

    private BreathingForm pickFormForCurrentState(BreathingTechnique technique, LivingEntity target, ItemStack swordStack) {
        if (technique == null || technique.getForms() == null || technique.getForms().isEmpty()) {
            return null;
        }

        if (isFirstFormOnlyRestricted(swordStack)) {
            return technique.getForm(0);
        }

        return pickWeightedFormConstrained(technique, target);
    }

    private boolean isFirstFormOnlyRestricted(ItemStack swordStack) {
        return this.entity.getPowerLevel() == 0 || TrainingSwordHelper.isTrainingSword(swordStack);
    }

    private BreathingTechnique resolveTechniqueForSword(ItemStack swordStack) {
        if (swordStack != null && !swordStack.isEmpty()) {
            if (swordStack.getItem() instanceof NichirinSwordBlack blackSword) {
                BreathingTechnique effective = blackSword.getEffectiveTechnique(swordStack, this.entity);
                if (effective != null) {
                    return effective;
                }
            }

            if (swordStack.getItem() instanceof BreathingSwordItem breathingSword) {
                BreathingTechnique swordTechnique = breathingSword.getBreathingTechnique();
                if (swordTechnique != null) {
                    return swordTechnique;
                }
            }
        }

        return this.entity.getBreathingTechnique();
    }

    private java.util.List<Integer> resolveAllowedBaseModForms(ItemStack swordStack, String styleId, int styleRange) {
        Set<Integer> forms = new LinkedHashSet<>();
        for (int formId : BaseModStyleMapping.getFormsForStyle(styleRange)) {
            if (isBaseModFormAllowedForSword(swordStack, styleId, formId)) {
                forms.add(formId);
            }
        }
        return new java.util.ArrayList<>(forms);
    }

    private boolean isBaseModFormAllowedForSword(ItemStack swordStack, String styleId, int formId) {
        if (swordStack == null || swordStack.isEmpty()) {
            return true;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(swordStack.getItem());
        String itemPath = itemId != null ? itemId.getPath().toLowerCase() : "";

        if ("flame_breathing".equals(styleId)
                && (itemPath.equals("nichirinsword_flame") || itemPath.equals("nichirinsword_flame"))) {
            return formId >= 401 && formId <= 405;
        }

        if ("water_breathing".equals(styleId)
            && (itemPath.equals("nichirinsword_water") || itemPath.equals("nichirinsword_black"))) {
            return formId >= 101 && formId <= 110;
        }

        if ("moon_breathing".equals(styleId) && itemPath.equals("nichirinswordmoon")) {
            return formId == 1101 || formId == 1102 || formId == 1103 || formId == 1105 || formId == 1106;
        }

        return true;
    }

    private String resolveCurrentStyleId(ItemStack swordStack) {
        if (swordStack.isEmpty()) {
            return null;
        }

        if (swordStack.getItem() instanceof NichirinSwordBlack) {
            String assigned = NichirinSwordBlack.ensureStyleAssigned(swordStack, this.entity.getRandom());
            if (assigned != null && !assigned.isEmpty()) {
                return assigned;
            }
        }

        SwordRegistry.RegisteredSword registeredSword = SwordRegistry.getSword(swordStack.getItem());
        if (registeredSword != null) {
            return registeredSword.getStyleId();
        }

        SwordMetadataRegistry.SwordMetadata metadata = SwordMetadataRegistry.getMetadata(swordStack.getItem());
        if (metadata != null) {
            return metadata.getStyleId();
        }

        return null;
    }

    private BreathingForm pickWeightedFormConstrained(BreathingTechnique technique, LivingEntity target) {
        java.util.List<BreathingForm> forms = technique.getForms();
        if (forms == null || forms.isEmpty()) return null;

        double dSq = this.entity.distanceToSqr(target);
        java.util.List<BreathingForm> allowed = new java.util.ArrayList<>();
        for (BreathingForm f : forms) {
            if (isFormAllowedForDistance(f, dSq)) {
                allowed.add(f);
            }
        }
        if (allowed.isEmpty()) {
            return null;
        }

        double total = 0.0;
        double[] weights = new double[allowed.size()];
        for (int i = 0; i < allowed.size(); i++) {
            int cd = Math.max(1, allowed.get(i).getCooldownSeconds());
            double w = 1.0 / cd; // smaller cooldown -> higher weight
            weights[i] = w;
            total += w;
        }

        double r = this.entity.getRandom().nextDouble() * total;
        double cumulative = 0.0;
        for (int i = 0; i < allowed.size(); i++) {
            cumulative += weights[i];
            if (r <= cumulative) {
                return allowed.get(i);
            }
        }
        return allowed.get(allowed.size() - 1);
    }

    private boolean isFormAllowedForDistance(BreathingForm form, double dSq) {
        String n = form.getName();
        if (n == null) return true;
        String name = n.toLowerCase();
        // firstForm & secondForm: within 5 blocks
        if (name.startsWith("first form") || name.startsWith("second form")) {
            return dSq <= 25.0;
        }
        // thirdForm: within 10 blocks
        if (name.startsWith("third form")) {
            return dSq <= 100.0;
        }
        // fourthForm: more than 10 blocks
        if (name.startsWith("fourth form")) {
            return dSq > 100.0;
        }
        // fifthForm: >10 and <50
        if (name.startsWith("fifth form")) {
            return dSq > 100.0 && dSq < 2500.0;
        }
        // sixthForm: >5 and <50
        if (name.startsWith("sixth form")) {
            return dSq > 25.0 && dSq < 2500.0;
        }
        // seventhForm: within 20 blocks
        if (name.startsWith("seventh form")) {
            return dSq <= 400.0;
        }
        // default allow
        return true;
    }

    private void faceTarget(LivingEntity target) {
        // Set look control for immediate facing
        this.entity.getLookControl().setLookAt(target, 180.0F, 180.0F);
        // Also adjust body/head rotation to face target
        double dx = target.getX() - this.entity.getX();
        double dz = target.getZ() - this.entity.getZ();
        float yaw = (float)(Math.atan2(dz, dx) * (180D/Math.PI)) - 90.0F;
        this.entity.setYRot(yaw);
        this.entity.setYHeadRot(yaw);
    }
}
