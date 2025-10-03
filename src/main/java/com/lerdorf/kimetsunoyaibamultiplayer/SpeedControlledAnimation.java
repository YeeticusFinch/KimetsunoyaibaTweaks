package com.lerdorf.kimetsunoyaibamultiplayer;

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;

public class SpeedControlledAnimation extends KeyframeAnimationPlayer {
    private final float speed;

    public SpeedControlledAnimation(KeyframeAnimation anim, float speed) {
        super(anim);
        this.speed = speed;
    }

    @Override
    public void tick() {
        // Advance more/less depending on speed
        float remainder = speed;
        while (remainder > 1f) {
            super.tick(); // full extra ticks
            remainder -= 1f;
        }
        if (Math.random() < remainder) {
            super.tick(); // fractional speed
        }
    }
}
