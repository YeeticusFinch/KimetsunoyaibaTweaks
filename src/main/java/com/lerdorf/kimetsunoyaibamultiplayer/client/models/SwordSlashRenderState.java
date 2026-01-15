package com.lerdorf.kimetsunoyaibamultiplayer.client.models;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * GeoAnimatable for sword slash rendering with animation support
 * Used by SwordSlashModel for GeckoLib integration
 * Supports "base" and "reverse" animations for texture flipping
 */
@OnlyIn(Dist.CLIENT)
public class SwordSlashRenderState implements GeoAnimatable {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private String animationName = "base"; // Default to base animation
    private long lastUpdateTick = 0;

    /**
     * Set the animation to play ("base" or "reverse")
     */
    public void setAnimation(String animationName) {
        this.animationName = animationName;
        this.lastUpdateTick = System.currentTimeMillis();
    }

    /**
     * Get the current animation name
     */
    public String getAnimation() {
        return this.animationName;
    }

	@Override
	public void registerControllers(ControllerRegistrar controllers) {
        // Controller for sword slash animations
        // Animation file: sword_slash.animation.json
        // Animations: "base" (normal), "reverse" (horizontally flipped)
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            String currentAnimation = getAnimation();

            // Build animation resource location
            // Format: "kimetsunoyaibamultiplayer.sword_slash.<animation>"
            // Example: "kimetsunoyaibamultiplayer.sword_slash.reverse"
            String animationResourceName = "kimetsunoyaibamultiplayer.sword_slash." + currentAnimation;

            state.getController().setAnimation(RawAnimation.begin()
                .then(animationResourceName,
                      software.bernie.geckolib.core.animation.Animation.LoopType.LOOP));

            return software.bernie.geckolib.core.object.PlayState.CONTINUE;
        }));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object object) {
		// Return milliseconds for smooth animation
		return (System.currentTimeMillis() - lastUpdateTick) / 50.0;
	}

}
