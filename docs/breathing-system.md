# Breathing Technique System

Complete guide for the breathing technique system with custom nichirin swords.

## Core Components

### 1. BreathingSwordItem (`items/BreathingSwordItem.java`)
- Abstract base class for all breathing swords
- Diamond tier stats: 3 attack damage, -2.4 attack speed
- Handles right-click activation of breathing forms
- Manages form cycling with R key
- Color-coded display (§6 for technique, §b for form name)

### 2. BreathingForm (`breathingtechnique/BreathingForm.java`)
- Data class for single breathing technique form
- Contains: name, description, cooldown, effect executor
- Effect is BiConsumer<Player, Level> lambda

### 3. BreathingTechnique (`breathingtechnique/BreathingTechnique.java`)
- Container for complete set of breathing forms
- Manages form indexing and retrieval

### 4. AnimationHelper (`breathingtechnique/AnimationHelper.java`)
- Centralized animation playback system
- `playAnimation(player, name)` - full animation
- `playAnimation(player, name, maxTicks)` - timed animation (10 ticks for attacks)
- `playAnimationOnLayer(player, name, maxTicks, speed, layer)` - full control
- Handles client-side playback and network sync

### 5. AbilityScheduler (`breathingtechnique/AbilityScheduler.java`)
- Server-side task scheduler
- `scheduleOnce(player, action, delayTicks)` - one-time delayed
- `scheduleRepeating(player, action, intervalTicks, durationTicks)` - repeated

### 6. PlayerBreathingData (`breathingtechnique/PlayerBreathingData.java`)
- Per-player breathing technique state
- Thread-safe UUID-based storage
- Tracks selected form index

## Form Cycling System

- Press **R key** to cycle through forms
- Current form stored in `PlayerBreathingData`
- Selection displayed in chat with color codes
- Persists during gameplay session

## Cooldown System

- Individual cooldown per form (in seconds)
- Displayed as hotbar item cooldown overlay
- Cooldown message shown when on cooldown
- Cooldowns per-item (all swords of same type share cooldown)

## Attack Systems

### Basic Attack Animations
- Left-click triggers animations via `BreathingSwordAnimationHandler`
- Animations on air clicks, block clicks, entity hits
- **10-tick limit** for basic attacks (prevents animation lock)
- 5% chance overhead, otherwise alternates left/right
- Synchronized across multiplayer

### AOE Attack System
- **Automatic AoE** on all left-click attacks (3x3x3 cube)
- Triggered via `LivingAttackEvent`
- Damage to all entities except primary target and attacker
- Sweep attack particles for visual feedback
- Range: 2 blocks forward from eye position

### Particle System
- **Particle filtering**: `speed_attack_sword` and `ragnaraku` skip automatic particles
- Breathing forms control their own effects
- Config: `SwordParticleMapping.java` + `config/particles.toml`

## Adding New Breathing Forms

### Basic Template

```java
public static BreathingForm newForm() {
    return new BreathingForm(
        "Form Name",
        "Description",
        cooldownSeconds,
        (player, level) -> {
            // CRITICAL: Enable attack animations for forms with attack sequences
            setCancelAttackSwing(player, false);

            // Play initial animation
            AnimationHelper.playAnimation(player, "animation_name");

            // Use AbilityScheduler for multi-phase abilities
            final int[] tickCounter = {0};
            AbilityScheduler.scheduleRepeating(player, () -> {
                int currentTick = tickCounter[0]++;

                // Attack animations with speed and layer control
                if (currentTick % attackInterval == 0) {
                    // Layer 4000 = overlay, speed 2.0f = double speed
                    AnimationHelper.playAnimationOnLayer(player, "sword_to_left", 10, 2.0f, 4000);

                    // Apply damage
                    AABB hitbox = player.getBoundingBox().inflate(range);
                    List<LivingEntity> targets = level.getEntitiesOfClass(
                        LivingEntity.class, hitbox, e -> e != player && e.isAlive()
                    );
                    for (LivingEntity target : targets) {
                        float damage = DamageCalculator.calculateScaledDamage(player, baseDamage);
                        target.hurt(level.damageSources().playerAttack(player), damage);
                    }
                }

                // Cleanup on last tick
                if (currentTick >= totalTicks - 1) {
                    player.setNoGravity(false);
                    MovementHelper.setStepHeight(player, originalStepHeight);
                }
            }, 1, totalTicks);

            // Spawn particles on server only
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, count, 0, 0, 0, 0);
            }
        }
    );
}
```

## Animation System Details

### Multi-Layer Animation System
- **Layer 3000**: Base ability animations (e.g., `ragnaraku1`)
- **Layer 4000**: Attack animations during abilities (overlay, doesn't overwrite base)
- Use `playAnimationOnLayer()` for full control

### Animation Speed Control
- `1.0f` = normal, `2.0f` = double, `3.0f` = triple, `0.5f` = half
- Implemented via `SpeedControlledAnimation` wrapper
- Example: `playAnimationOnLayer(player, "sword_to_left", 10, 2.0f, 4000)`

### Attack Animation Suppression
- Forms set `cancelAttackSwing` flag via capability
- **MUST** call `setCancelAttackSwing(player, false)` at start of forms with attacks
- Enables `BreathingSwordAnimationHandler.onAttack()` to play animations

### Network Synchronization
- Forms run **SERVER-SIDE**
- `AnimationHelper` auto-sends `AnimationSyncPacket` to ALL clients
- Packet includes speed and layer (v1.5.20+)
- Local player receives own packets to see animations during abilities

### Common Animation Names
- **Attacks**: `sword_to_left`, `sword_to_right`, `sword_overhead`, `sword_to_upper`
- **Special**: `speed_attack_sword`, `ragnaraku1-3`, `kamusari3`, `sword_rotate`
- Attack animations: 10 ticks max for responsiveness

## Particle Patterns

```java
// Forward thrust
ParticleHelper.spawnForwardThrust(serverLevel, startPos, direction, distance, particleType, count);

// Circular pattern
ParticleHelper.spawnCircleParticles(serverLevel, center, radius, particleType, count);

// Tornado/spiral (in repeating task)
double angle = currentTick * Math.PI / 10;
double x = centerX + Math.cos(angle) * radius;
double z = centerZ + Math.sin(angle) * radius;
double y = centerY + (currentTick * 0.1); // vertical offset

// ALWAYS spawn on server
if (level instanceof ServerLevel serverLevel) {
    serverLevel.sendParticles(...);
}

// Prevent spam: only spawn every N ticks
if (currentTick % 3 == 0) {
    // spawn particles
}
```

## Movement & Physics

### MovementHelper Utilities
- `setVelocity(player, x, y, z)` - Sets velocity with server sync
- `addVelocity(player, dx, dy, dz)` - Adds to current velocity
- `setRotation(player, yaw, pitch)` - Sets rotation (ShoulderSurfing sync)
- `lookAt(player, targetPos)` - Makes player face position
- `setStepHeight(player, height)` - Allows climbing blocks (default 0.6, use 1.8+)
- `stepUp(player, vx, vy, vz)` - Auto-climb blocks in movement direction

### Movement Patterns

**Circular Movement** (Ice Breathing Second Form):
```java
double currentAngle = startAngle + (currentTick * angularVelocity * speedMultiplier);
Vec3 position = MovementHelper.calculateCirclePosition(center, radius, angle);
// Combine forward motion (70%) + position correction (30%)
MovementHelper.lookAt(player, centerPosition); // Always face center
```

**Hovering** (Ice Breathing Third Form):
```java
double targetY = player.getY() + 4.0;
if (currentY < targetY) {
    setVelocity(player, vx, 0.3, vz); // Ascent
} else {
    setVelocity(player, vx, 0, vz); // Hover (Y = 0)
}
player.setNoGravity(true); // At start
player.setNoGravity(false); // At end
```

**Fast Dash with Auto-Climb** (Ice Breathing Fifth Form):
```java
setVelocity(player, dashDir.x * speed, player.getDeltaMovement().y, dashDir.z * speed);
MovementHelper.stepUp(player, targetX, targetY, targetZ); // Each tick
// Preserve jump/gravity: keep original Y velocity
```

## Damage Calculation

```java
// Always use DamageCalculator
float damage = DamageCalculator.calculateScaledDamage(player, baseDamage);
// Scales with player's attack damage attribute (including strength)

// Base damage values
// Light: 3-5, Medium: 6-8, Heavy: 10-12, Ultimate: 15+

// Apply damage
target.hurt(level.damageSources().playerAttack(player), damage);
```

## Task Scheduling Patterns

**One-time Delayed:**
```java
AbilityScheduler.scheduleOnce(player, () -> {
    // Execute after delay
}, delayTicks);
```

**Repeating (RECOMMENDED):**
```java
final int[] tickCounter = {0};
AbilityScheduler.scheduleRepeating(player, () -> {
    int currentTick = tickCounter[0]++;

    if (currentTick % interval == 0) {
        // Periodic action
    }

    if (currentTick >= totalTicks - 1) {
        // Cleanup
    }
}, 1, totalTicks);
```

**Multi-Phase:**
```java
// Phase 1: Immediate
AnimationHelper.playAnimation(player, "thrust_anim");

// Phase 2: Delayed barrage
AbilityScheduler.scheduleOnce(player, () -> {
    AbilityScheduler.scheduleRepeating(player, () -> {
        // Rapid attacks
    }, 1, durationTicks);
}, delayTicks);
```

## Sound Design

```java
level.playSound(null, player.blockPosition(), SoundEvent, SoundSource.PLAYERS, volume, pitch);

// Common sounds
SoundEvents.PLAYER_ATTACK_SWEEP
SoundEvents.GLASS_BREAK
SoundEvents.SNOW_BREAK

// Prevent spam: play only every N attacks
if (attackCount % 3 == 0) {
    // play sound
}

// Volume: 0.5-1.0, Pitch: 0.8-1.5
```

## Debugging

```java
// Enable debug logging in config
logDebug = true

// Server logs
"Second Form: Playing attack animation 'sword_to_left' on layer 4000"

// Client logs
"Successfully applied animation kimetsunoyaiba:sword_to_left to player {name} (speed=2.0, layer=4000)"

// Test commands
/testanim - Test animation sync
/testanimc - Test client-side animation
```
