# Kimetsu no Yaiba Mod - Breathing Forms System Analysis

This document explains how breathing forms work in the decompiled Kimetsunoyaiba mod (ver2-forge-1.20.1) and provides guidance for adding new forms and implementing backward cycling.

## Table of Contents
1. [Breathing Form Number System](#breathing-form-number-system)
2. [Detecting Current Breathing Technique and Form](#detecting-current-breathing-technique-and-form)
3. [How Form Cycling Works](#how-form-cycling-works)
4. [How Forms are Executed](#how-forms-are-executed)
5. [Adding New Forms](#adding-new-forms)
6. [Implementing Backward Cycling (Shift+R)](#implementing-backward-cycling-shiftr)
7. [Multi-Style Swords (Black Nichirin & Tanjiro's Sword)](#multi-style-swords-black-nichirin--tanjiros-sword)

---

## Breathing Form Number System

### The NBT Data Storage

The mod stores the current breathing form in the player's **persistent NBT data** using the key `"breathes"` as a **double value**.

### Form Numbering Convention

Forms are numbered using a **hundreds-digit system**:

```
[Breathing Style (hundreds)] + [Form Number (ones/tens)]
```

**Examples from Flame Breathing** (`PlayerBreathFlameProcedure.java`):
- **301** = Flame Breathing, Form 1 (Unknowing Fire)
- **302** = Flame Breathing, Form 2 (Rising Scorching Sun)
- **303** = Flame Breathing, Form 3 (Blazing Universe)
- **304** = Flame Breathing, Form 4 (Blooming Flame Undulation)
- **305** = Flame Breathing, Form 5 (Flame Tiger)
- **309** = Flame Breathing, Form 9 (Rengoku)

**Examples from Sun Breathing**:
- **1220** = Sun Breathing (Hi no Kokyu), Form 10

### Complete Breathing Style Ranges

Based on `BreathesRandomProcedure.java` and various procedures:

| Breathing Style | Number Range | Forms Available | Notes |
|----------------|--------------|-----------------|-------|
| **Water** | 100-113 | ~13 forms | Excludes 108-110 |
| **Thunder** | 300-309 | ~9 forms | Excludes 306-308 |
| **Flame** | 400-406 | 6 forms | |
| **Wind** | 500-509 | 9 forms | |
| **Stone** | 600-611 | 11 forms | Excludes 610 |
| **Mist** | 700-707 | 7 forms | Excludes 706 |
| **Serpent** | 800-805 | 5 forms | |
| **Sound** | 900-904 | 4 forms | |
| **Moon** | 1100-1116 | ~16 forms | Excludes 1104, 1111-1113, 1115 |
| **Sun** | 1200-1212 | 12 forms | |
| **Flower** | 1300-1305 | 5 forms | Excludes 1302-1303 |
| **Insect** | 1401-1406 | 5 forms | Excludes 1403 |
| **Love** | 1500-1506 | 6 forms | Excludes 1503-1504 |

### Special Form Numbers

- **X20-X25** range often indicates "skill mode" or special states
- Some numbers are **deliberately excluded** (e.g., form doesn't exist or is reserved)

---

## How Form Cycling Works

### The R Key (CHANGE_BREATHES_AND_BLOOD_ART)

**File**: `KimetsunoyaibaModKeyMappings.java`

The R key (keycode **82**) triggers the `ChangeBreathesAndBloodArtMessage` network packet.

```java
CHANGE_BREATHES_AND_BLOOD_ART = new KeyMapping(82, "key.categories.kimetsunoyaiba") {
    private boolean isDownOld = false;

    public void m_7249_(final boolean isDown) {
        super.m_7249_(isDown);
        if (this.isDownOld != isDown && isDown) {
            // Key pressed
            KimetsunoyaibaMod.PACKET_HANDLER.sendToServer(
                new ChangeBreathesAndBloodArtMessage(0, 0)
            );
            ChangeBreathesAndBloodArtMessage.pressAction(player, 0, 0);
        }
        else if (this.isDownOld != isDown && !isDown) {
            // Key released
            int dt = (int)(System.currentTimeMillis() - CHANGE_BREATHES_AND_BLOOD_ART_LASTPRESS);
            KimetsunoyaibaMod.PACKET_HANDLER.sendToServer(
                new ChangeBreathesAndBloodArtMessage(1, dt)
            );
            ChangeBreathesAndBloodArtMessage.pressAction(player, 1, dt);
        }
        this.isDownOld = isDown;
    }
};
```

### Network Message Handler

**File**: `ChangeBreathesAndBloodArtMessage.java`

```java
public static void pressAction(Player entity, int type, int pressedms) {
    Level world = entity.m_9236_();
    double x = entity.m_20185_();
    double y = entity.m_20186_();
    double z = entity.m_20189_();

    if (!world.m_46805_(entity.m_20183_())) {
        return;
    }

    if (type == 0) {
        // Key pressed - Start form change
        ChangeArtProcedure.execute(entity);
    }
    if (type == 1) {
        // Key released - Finish form change
        FinishArtProcedure.execute(entity);
    }
}
```

### Form Cycling Logic

**File**: `ChangeArtProcedure.java`

```java
public static void execute(Entity entity) {
    if (entity == null) {
        return;
    }
    // Sets a flag to indicate form cycling mode is active
    entity.getPersistentData().m_128379_("ChangeArt", true);
}
```

**File**: `FinishArtProcedure.java`

```java
public static void execute(Entity entity) {
    if (entity == null) {
        return;
    }
    // Clears the form cycling mode flag
    entity.getPersistentData().m_128379_("ChangeArt", false);
}
```

### The Actual Cycling Logic

**File**: `ChangingBreathesProcedure.java` (partially decompiled)

This procedure is called every tick while a nichirin sword is **selected in the hotbar** (via `inventoryTick` method).

**Logic** (from bytecode analysis):
1. Checks if `ChangeArt` NBT flag is **true**
2. If true:
   - Sets `ChangeArt` flag back to **false**
   - Sets a `change_flag` on the **item's NBT**
   - **Increments the `breathes` NBT value** to cycle to the next form

**Key Insight**: The cycling appears to increment the `breathes` value by +1, moving to the next form in sequence.

---

## How Forms are Executed

### Right-Click Activation

**File**: `NichirinswordKanaeItem.java` (example sword)

```java
public InteractionResultHolder<ItemStack> m_7203_(Level world, Player entity, InteractionHand hand) {
    InteractionResultHolder<ItemStack> ar = super.m_7203_(world, entity, hand);
    // Triggers the breathing form when right-clicking
    StartBreathesProcedure.execute(world, entity, ar.m_19095_());
    return ar;
}
```

### Form Execution Pipeline

**File**: `StartBreathesProcedure.java` (large file, ~2700+ lines)

**High-level logic**:

1. **Checks conditions**:
   - Player is not suffocating
   - Form is not in "skill mode" (breathes % 100 between 20-25)
   - Counter `cnt1` >= 3 (cooldown)

2. **If `change_flag` is false** (normal execution):
   - Calls `ChangingBreathesProcedure` to handle form cycling

3. **Routes to correct breathing style handler** based on `breathes` value:
   - Checks hundreds digit to determine breathing style
   - Calls the appropriate `PlayerBreath[Style]Procedure.execute()`

**Example routing logic** (from bytecode):
```java
double breathes = entity.getPersistentData().getDouble("breathes");

if (breathes >= 300 && breathes < 400) {
    // Flame Breathing
    PlayerBreathFlameProcedure.execute(world, x, y, z, entity);
}
else if (breathes >= 1200 && breathes < 1300) {
    // Sun Breathing
    PlayerBreathSunProcedure.execute(world, x, y, z, entity);
}
// ... etc
```

### Breathing Style Dispatcher

**File**: `PlayerBreathFlameProcedure.java` (example)

```java
public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
    double breathes = entity.getPersistentData().m_128459_("breathes");

    if (breathes == 301.0) {
        // Execute Form 1
        BreathesHono1Procedure.execute(world, x, y, z, entity);
    }
    else if (breathes == 302.0) {
        // Execute Form 2
        BreathesHono2Procedure.execute(world, x, y, z, entity);
    }
    else if (breathes == 303.0) {
        // Execute Form 3
        BreathesHono3Procedure.execute(world, x, y, z, entity);
    }
    // ... more forms ...
    else {
        // Default/fallback - usually a basic swing animation
        SwingHono1Procedure.execute(world, x, y, z, entity);
    }
}
```

### Individual Form Execution

**File**: `BreathesHi10Procedure.java` (Sun Breathing Form 10 example)

```java
public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
    if (entity == null) {
        return;
    }

    // Increment counter for this form
    entity.getPersistentData().m_128347_("cnt1",
        entity.getPersistentData().m_128459_("cnt1") + 1.0);

    // Stop entity fall damage
    entity.f_19789_ = 0.0f;

    if (entity.getPersistentData().m_128459_("cnt1") == 1.0) {
        // First tick - initialization

        if (entity instanceof Player) {
            GetPowerFowardProcedure.execute(world, entity);
        } else {
            GetPowerProcedure.execute(world, entity);
        }

        // Spawn particles
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.m_8767_(ParticleTypes.f_123744_, x, y, z, 20, 0.0, 0.0, 0.0, 0.5);
            serverLevel.m_8767_(ParticleTypes.f_123796_, x, y, z, 20, 0.0, 0.0, 0.0, 0.5);
            serverLevel.m_8767_(ParticleTypes.f_123751_, x, y, z, 5, 0.5, 0.5, 0.5, 0.5);
        }

        // Play sound
        world.playSound(null, BlockPos.m_274561_(x, y, z),
            SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0f, 1.0f);

        // Set animation attributes
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.getAttribute(KimetsunoyaibaModAttributes.ANIMATION_1.get())
                .setBaseValue(-6.0);
            livingEntity.getAttribute(KimetsunoyaibaModAttributes.ANIMATION_2.get())
                .setBaseValue(7.0);
        }

        PlayAnimationProcedure.execute(world, entity);
    }

    // Continuous particles
    if (world instanceof ServerLevel serverLevel) {
        serverLevel.m_8767_(ParticleTypes.f_123744_, x, y, z, 20, 0.5, 0.5, 0.5, 0.1);
        serverLevel.m_8767_(ParticleTypes.f_123751_, x, y, z, 2, 0.5, 0.5, 0.5, 0.1);
    }

    // Apply velocity boost (first 3 ticks)
    if (entity.getPersistentData().m_128459_("cnt1") < 3.0) {
        entity.setDeltaMovement(new Vec3(
            entity.getPersistentData().m_128459_("x_power") * 1.0,
            Math.max(entity.getPersistentData().m_128459_("y_power") * 1.0, 0.5),
            entity.getPersistentData().m_128459_("z_power") * 1.0
        ));
    }

    // Main form execution (ticks 1-10, while cnt2 > -450)
    if (entity.getPersistentData().m_128459_("cnt1") < 10.0
        && entity.getPersistentData().m_128459_("cnt2") > -450.0) {
        // ... attack logic, more particles, damage, etc. ...
    }
}
```

**Common Pattern**:
- Use `cnt1` as a **tick counter** for the form duration
- Use `cnt2` for **secondary timing** (often related to rotation/angle)
- Reset `cnt1` to 0 when form completes
- Apply effects, particles, damage, movement, etc. over multiple ticks

---

## Adding New Forms

### Step 1: Determine Form Number

Choose a number in your breathing style's range that is **not already used**.

**Example**: Adding Flame Breathing Form 6

- Existing forms: 301, 302, 303, 304, 305, 309
- Available number: **306** (currently skipped in the range 306-308)

### Step 2: Create Form Procedure

Create a new Java class following the naming convention:

**File**: `BreathesHono6Procedure.java`

```java
package net.mcreator.kimetsunoyaiba.procedures;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;

public class BreathesHono6Procedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) {
            return;
        }

        // Increment tick counter
        entity.getPersistentData().putDouble("cnt1",
            entity.getPersistentData().getDouble("cnt1") + 1.0);

        // Disable fall damage during technique
        entity.fallDistance = 0.0f;

        if (entity.getPersistentData().getDouble("cnt1") == 1.0) {
            // First tick - initialization

            // Calculate forward power (movement direction)
            if (entity instanceof Player) {
                GetPowerFowardProcedure.execute(world, entity);
            } else {
                GetPowerProcedure.execute(world, entity);
            }

            // Spawn initial particles
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    x, y, z, 30, 1.0, 1.0, 1.0, 0.1);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    x, y, z, 10, 0.5, 0.5, 0.5, 0.05);
            }

            // Play sound
            if (world instanceof Level level && !level.isClientSide()) {
                level.playSound(null, BlockPos.containing(x, y, z),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
            }

            // Set animation (custom attributes used by GeckoLib)
            if (entity instanceof LivingEntity livingEntity) {
                // Set ANIMATION_1 and ANIMATION_2 attributes
                // These control which animation plays
                AttributeInstance anim1 = livingEntity.getAttribute(
                    KimetsunoyaibaModAttributes.ANIMATION_1.get());
                if (anim1 != null) {
                    anim1.setBaseValue(-5.0); // Animation ID
                }

                AttributeInstance anim2 = livingEntity.getAttribute(
                    KimetsunoyaibaModAttributes.ANIMATION_2.get());
                if (anim2 != null) {
                    anim2.setBaseValue(6.0); // Animation variant
                }
            }

            PlayAnimationProcedure.execute(world, entity);
        }

        // Continuous particles during form execution
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                x, y + 1.0, z, 5, 0.3, 0.3, 0.3, 0.05);
        }

        // Apply movement boost (first 5 ticks)
        if (entity.getPersistentData().getDouble("cnt1") < 5.0) {
            entity.setDeltaMovement(new Vec3(
                entity.getPersistentData().getDouble("x_power") * 1.5,
                Math.max(entity.getPersistentData().getDouble("y_power") * 1.5, 0.3),
                entity.getPersistentData().getDouble("z_power") * 1.5
            ));
        }

        // Main attack phase (ticks 1-15)
        if (entity.getPersistentData().getDouble("cnt1") < 15.0) {
            // Deal damage to entities in range
            AABB hitbox = entity.getBoundingBox().inflate(4.0);

            List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class, hitbox,
                e -> e != entity && e.isAlive()
            );

            for (LivingEntity target : targets) {
                // Apply damage
                target.hurt(world.damageSources().playerAttack((Player)entity), 10.0f);

                // Knockback
                Vec3 knockback = target.position().subtract(entity.position())
                    .normalize().scale(1.5);
                target.setDeltaMovement(target.getDeltaMovement().add(
                    knockback.x, 0.3, knockback.z));

                // Apply fire
                target.setSecondsOnFire(5);
            }
        }

        // Reset counter when form completes
        if (entity.getPersistentData().getDouble("cnt1") >= 15.0) {
            entity.getPersistentData().putDouble("cnt1", 0.0);
        }
    }
}
```

### Step 3: Register Form in Breathing Style Dispatcher

**File**: `PlayerBreathFlameProcedure.java`

Add a new `else if` branch:

```java
public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
    double breathes = entity.getPersistentData().getDouble("breathes");

    if (breathes == 301.0) {
        BreathesHono1Procedure.execute(world, x, y, z, entity);
    }
    else if (breathes == 302.0) {
        BreathesHono2Procedure.execute(world, x, y, z, entity);
    }
    else if (breathes == 303.0) {
        BreathesHono3Procedure.execute(world, x, y, z, entity);
    }
    else if (breathes == 304.0) {
        BreathesHono4Procedure.execute(world, x, y, z, entity);
    }
    else if (breathes == 305.0) {
        BreathesHono5Procedure.execute(world, x, y, z, entity);
    }
    // NEW FORM ADDED HERE
    else if (breathes == 306.0) {
        BreathesHono6Procedure.execute(world, x, y, z, entity);
    }
    else if (breathes == 309.0) {
        BreathesHonoProcedure.execute(world, x, y, z, entity);
    }
    else {
        SwingHono1Procedure.execute(world, x, y, z, entity);
    }
}
```

### Step 4: Update Form Cycling Logic

**File**: `ChangingBreathesProcedure.java` (you'll need to modify)

Make sure the form cycling knows about your new form:

```java
// When incrementing breathes value, handle wrapping
double currentBreathes = entity.getPersistentData().getDouble("breathes");
double newBreathes;

if (currentBreathes == 305.0) {
    // Was on form 5, go to form 6 (NEW)
    newBreathes = 306.0;
}
else if (currentBreathes == 306.0) {
    // Was on form 6, skip to form 9 (or wrap)
    newBreathes = 309.0;
}
else if (currentBreathes == 309.0) {
    // Wrap back to form 1
    newBreathes = 301.0;
}
else {
    // Normal increment
    newBreathes = currentBreathes + 1.0;
}

entity.getPersistentData().putDouble("breathes", newBreathes);
```

### Step 5: Test Your New Form

1. Equip a Flame Breathing nichirin sword
2. Press **R** to cycle to form **306**
3. **Right-click** to activate the form
4. Verify particles, damage, movement, and cooldown work correctly

---

## Implementing Backward Cycling (Shift+R)

To add backward cycling when the player holds **Shift** while pressing **R**, you need to modify the keybind handler and cycling logic.

### Step 1: Detect Shift Key in KeyMapping

**File**: `KimetsunoyaibaModKeyMappings.java` (modify)

```java
CHANGE_BREATHES_AND_BLOOD_ART = new KeyMapping(82, "key.categories.kimetsunoyaiba") {
    private boolean isDownOld = false;

    public void m_7249_(final boolean isDown) {
        super.m_7249_(isDown);

        // Get current player to check shift state
        Player player = Minecraft.getInstance().player;
        boolean isShiftHeld = (player != null && player.isShiftKeyDown());

        if (this.isDownOld != isDown && isDown) {
            // Key pressed
            // Send shift state as part of the message
            int direction = isShiftHeld ? -1 : 1; // -1 = backward, 1 = forward

            KimetsunoyaibaMod.PACKET_HANDLER.sendToServer(
                new ChangeBreathesAndBloodArtMessage(0, direction)
            );
            ChangeBreathesAndBloodArtMessage.pressAction(player, 0, direction);
            CHANGE_BREATHES_AND_BLOOD_ART_LASTPRESS = System.currentTimeMillis();
        }
        else if (this.isDownOld != isDown && !isDown) {
            // Key released
            int dt = (int)(System.currentTimeMillis() - CHANGE_BREATHES_AND_BLOOD_ART_LASTPRESS);
            KimetsunoyaibaMod.PACKET_HANDLER.sendToServer(
                new ChangeBreathesAndBloodArtMessage(1, dt)
            );
            ChangeBreathesAndBloodArtMessage.pressAction(player, 1, dt);
        }
        this.isDownOld = isDown;
    }
};
```

**Note**: The `pressedms` parameter is being repurposed to carry the **direction** (-1 or 1) instead of milliseconds.

### Step 2: Modify Network Message

**File**: `ChangeBreathesAndBloodArtMessage.java` (modify)

```java
public class ChangeBreathesAndBloodArtMessage {
    int type;
    int direction; // Renamed from pressedms

    public ChangeBreathesAndBloodArtMessage(int type, int direction) {
        this.type = type;
        this.direction = direction;
    }

    // ... buffer methods stay the same ...

    public static void pressAction(Player entity, int type, int direction) {
        Level world = entity.level();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        if (!world.hasChunkAt(entity.blockPosition())) {
            return;
        }

        if (type == 0) {
            // Key pressed - Start form change with direction
            ChangeArtProcedure.execute(entity, direction);
        }
        if (type == 1) {
            // Key released - Finish form change
            FinishArtProcedure.execute(entity);
        }
    }
}
```

### Step 3: Update ChangeArtProcedure

**File**: `ChangeArtProcedure.java` (modify)

```java
public class ChangeArtProcedure {
    public static void execute(Entity entity, int direction) {
        if (entity == null) {
            return;
        }

        // Set cycling mode flag
        entity.getPersistentData().putBoolean("ChangeArt", true);

        // Store the direction for ChangingBreathesProcedure to use
        entity.getPersistentData().putInt("CycleDirection", direction);
    }
}
```

### Step 4: Implement Backward Cycling Logic

**File**: `ChangingBreathesProcedure.java` (modify)

```java
public static void execute(Entity entity, ItemStack itemstack) {
    if (entity == null) {
        return;
    }

    if (entity.getPersistentData().getBoolean("ChangeArt")) {
        // Clear the flag
        entity.getPersistentData().putBoolean("ChangeArt", false);

        // Set item flag
        itemstack.getOrCreateTag().putBoolean("change_flag", true);

        // Get cycle direction (-1 = backward, 1 = forward)
        int direction = entity.getPersistentData().getInt("CycleDirection");

        // Get current breathing value
        double currentBreathes = entity.getPersistentData().getDouble("breathes");

        // Determine breathing style (hundreds digit)
        int breathingStyle = (int)(currentBreathes / 100) * 100;

        // Cycle the form
        double newBreathes = cycleBreathingForm(currentBreathes, breathingStyle, direction);

        // Set new breathing value
        entity.getPersistentData().putDouble("breathes", newBreathes);

        // Clear direction
        entity.getPersistentData().putInt("CycleDirection", 0);
    }
}

/**
 * Cycles through breathing forms with wraparound
 * @param current Current form number
 * @param style Breathing style (e.g., 300 for Flame)
 * @param direction 1 for forward, -1 for backward
 * @return Next form number
 */
private static double cycleBreathingForm(double current, int style, int direction) {
    // Define form lists for each breathing style
    int[] waterForms = {101, 102, 103, 104, 105, 106, 107, 111, 112, 113};
    int[] flameForms = {301, 302, 303, 304, 305, 306, 309}; // Added 306
    int[] thunderForms = {300, 301, 302, 303, 304, 305, 309};
    int[] windForms = {500, 501, 502, 503, 504, 505, 506, 507, 508, 509};
    int[] stoneForms = {600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 611};
    int[] mistForms = {700, 701, 702, 703, 704, 705, 707};
    int[] serpentForms = {800, 801, 802, 803, 804, 805};
    int[] soundForms = {900, 901, 902, 903, 904};
    int[] moonForms = {1101, 1102, 1103, 1105, 1106, 1107, 1108, 1109, 1110,
                       1114, 1116};
    int[] sunForms = {1200, 1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208,
                      1209, 1210, 1211, 1212};
    int[] flowerForms = {1300, 1301, 1304, 1305};
    int[] insectForms = {1401, 1402, 1404, 1405, 1406};
    int[] loveForms = {1500, 1501, 1502, 1505, 1506};

    // Select the appropriate form list
    int[] forms;
    switch (style) {
        case 100: forms = waterForms; break;
        case 300: forms = flameForms; break;
        case 400: forms = thunderForms; break;
        case 500: forms = windForms; break;
        case 600: forms = stoneForms; break;
        case 700: forms = mistForms; break;
        case 800: forms = serpentForms; break;
        case 900: forms = soundForms; break;
        case 1100: forms = moonForms; break;
        case 1200: forms = sunForms; break;
        case 1300: forms = flowerForms; break;
        case 1400: forms = insectForms; break;
        case 1500: forms = loveForms; break;
        default: return current; // Unknown style, don't change
    }

    // Find current index in the array
    int currentIndex = -1;
    for (int i = 0; i < forms.length; i++) {
        if (forms[i] == (int)current) {
            currentIndex = i;
            break;
        }
    }

    // If current form not found, default to first form
    if (currentIndex == -1) {
        return forms[0];
    }

    // Calculate new index with wraparound
    int newIndex = currentIndex + direction;

    if (newIndex < 0) {
        // Wrap to last form
        newIndex = forms.length - 1;
    } else if (newIndex >= forms.length) {
        // Wrap to first form
        newIndex = 0;
    }

    return forms[newIndex];
}
```

### Step 5: Display Form Name to Player

**Optional Enhancement**: Show a message when cycling forms.

Add to `ChangingBreathesProcedure.java`:

```java
// After setting newBreathes
if (entity instanceof Player player) {
    String formName = getBreathingFormName(newBreathes);
    player.displayClientMessage(
        Component.literal("§6Selected Form: §b" + formName),
        true // Show as action bar (above hotbar)
    );
}

private static String getBreathingFormName(double breathes) {
    // Map form numbers to readable names
    return switch ((int)breathes) {
        case 301 -> "Unknowing Fire";
        case 302 -> "Rising Scorching Sun";
        case 303 -> "Blazing Universe";
        case 304 -> "Blooming Flame Undulation";
        case 305 -> "Flame Tiger";
        case 306 -> "Scorching Crimson Mirror"; // NEW FORM
        case 309 -> "Rengoku";
        // ... add all other forms ...
        default -> "Form " + ((int)breathes % 100);
    };
}
```

### Step 6: Test Backward Cycling

1. Equip a nichirin sword
2. Press **R** to cycle forward through forms
3. Hold **Shift** and press **R** to cycle backward
4. Verify wraparound works (last form → first form when going backward)

---

## Summary

### Key Takeaways

1. **Breathing forms are stored as doubles** in player NBT data with the key `"breathes"`
2. **Form numbering follows a hundreds-digit system**: `[Style (hundreds)] + [Form (ones/tens)]`
3. **R key triggers form cycling** via network packets
4. **Right-click activates the current form** via `StartBreathesProcedure`
5. **Each form is a separate procedure** with tick-based execution using `cnt1`, `cnt2` counters
6. **Dispatcher procedures route to the correct form** based on the `breathes` value

### To Add a New Form

1. Choose an unused number in your breathing style's range
2. Create a `Breathes[Name][Number]Procedure.java` class
3. Add the form to the dispatcher procedure (`PlayerBreath[Style]Procedure.java`)
4. Update cycling logic to include the new form number
5. Test activation and cycling

### To Implement Shift+R Backward Cycling

1. Modify `KimetsunoyaibaModKeyMappings.java` to detect shift key
2. Pass direction (-1 or 1) through the network packet
3. Update `ChangeArtProcedure` to store the direction
4. Modify `ChangingBreathesProcedure` to cycle backward when direction is -1
5. Use form arrays and index-based cycling with wraparound logic

---

## Multi-Style Swords (Black Nichirin & Tanjiro's Sword)

### Special Swords with Multiple Breathing Techniques

The **Black Nichirin Sword** (`NitirintouBlackItem`) and **Tanjiro's Swords** (`NitirintouTanjirouItem`, `NichirinswordTanjiro2Item`) have a unique ability: they can **switch between different breathing techniques**.

These swords allow the player to use:
- **Water Breathing** (100-113 range)
- **Hinokami Kagura** / **Sun Breathing** (1200-1212 range)

### How Multi-Style Switching Works

#### ItemStack NBT Storage

Unlike regular swords that only have one breathing style, multi-style swords store the **breathing technique offset** in the **ItemStack's NBT data** using the key `"select"`.

**From StartBreathesProcedure.java** (lines 909-916 from bytecode):

```java
// For multi-style swords, the final breathes value is calculated as:
entity.getPersistentData().putDouble("breathes",
    BASE_FORM_NUMBER + itemstack.getOrCreateTag().getDouble("select")
);
```

**Example**:
- Base form for Cherry Blossom sword: `1801.0`
- If `itemstack NBT "select"` = `0.0` → Final breathes = `1801.0`
- If `itemstack NBT "select"` = `-700.0` → Final breathes = `1101.0` (different style)

#### Select Name Display

Multi-style swords also store a **display name** for the currently selected style in `"select_name"` NBT tag.

**From StartBreathesProcedure.java** (lines 940-944):

```java
if (player != null && !world.isClientSide()) {
    // Display the selected breathing style name to the player
    player.displayClientMessage(
        Component.literal(itemstack.getOrCreateTag().getString("select_name")),
        true  // Show as action bar (above hotbar)
    );
}
```

### Detecting the Current Breathing Technique (Multi-Style Swords)

For multi-style swords, you need to check **both** the player NBT and the item NBT:

```java
public static String getMultiStyleBreathingInfo(Player player, ItemStack heldSword) {
    // Get the base form from player NBT
    double playerBreathes = player.getPersistentData().getDouble("breathes");

    // Get the style offset from item NBT (if it exists)
    double selectOffset = heldSword.getOrCreateTag().getDouble("select");

    // Calculate actual breathes value
    double actualBreathes = playerBreathes + selectOffset;

    // Determine breathing style
    int breathingStyle = (int)(actualBreathes / 100) * 100;
    int formNumber = (int)actualBreathes % 100;

    String styleName = switch (breathingStyle) {
        case 100 -> "Water";
        case 1200 -> "Sun (Hinokami Kagura)";
        default -> "Unknown";
    };

    return styleName + " Breathing, Form " + formNumber;
}
```

### Reading the Select Name

Get the human-readable name of the currently selected breathing style:

```java
public static String getSelectedBreathingStyleName(ItemStack sword) {
    CompoundTag tag = sword.getOrCreateTag();

    if (tag.contains("select_name")) {
        return tag.getString("select_name");
    }

    return "Unknown Style";
}
```

### Checking if a Sword is Multi-Style

Determine if a sword supports multiple breathing techniques:

```java
public static boolean isMultiStyleSword(ItemStack sword) {
    Item item = sword.getItem();

    // Check against known multi-style swords
    return item == KimetsunoyaibaModItems.NITIRINTOU_BLACK.get() ||
           item == KimetsunoyaibaModItems.NITIRINTOU_TANJIROU.get() ||
           item == KimetsunoyaibaModItems.NICHIRINSWORD_TANJIRO_2.get();
}
```

### How Players Switch Between Breathing Techniques

The switching mechanism likely involves:

1. **Pressing a specific key** (possibly M key - Demon Slayer Mark key, or a custom keybind)
2. **Cycling through available styles** stored in the sword's configuration
3. **Updating the `select` offset** to point to a different breathing style range
4. **Updating the `select_name`** to display the new style name

**Example switching logic** (conceptual):

```java
public static void switchBreathingStyle(Player player, ItemStack sword) {
    CompoundTag tag = sword.getOrCreateTag();

    // Get current offset
    double currentOffset = tag.getDouble("select");

    // Define available styles for this sword
    // Black Nichirin: Water (0 offset) and Sun (+1100 offset)
    double[] availableOffsets = {0.0, 1100.0};  // Water = +0, Sun = +1100
    String[] styleNames = {"Water Breathing", "Sun Breathing (Hinokami Kagura)"};

    // Find current index
    int currentIndex = 0;
    for (int i = 0; i < availableOffsets.length; i++) {
        if (Math.abs(availableOffsets[i] - currentOffset) < 0.1) {
            currentIndex = i;
            break;
        }
    }

    // Cycle to next style
    int nextIndex = (currentIndex + 1) % availableOffsets.length;

    // Update sword NBT
    tag.putDouble("select", availableOffsets[nextIndex]);
    tag.putString("select_name", styleNames[nextIndex]);

    // Display message to player
    player.displayClientMessage(
        Component.literal("§6Switched to: §b" + styleNames[nextIndex]),
        true
    );
}
```

### Complete Detection Example (Universal)

Detect breathing technique for **any** sword (single-style or multi-style):

```java
public static BreathingInfo getBreathingInfo(Player player, ItemStack heldSword) {
    // Get player's base breathes value
    double playerBreathes = player.getPersistentData().getDouble("breathes");

    // Check if sword modifies the breathing style
    double selectOffset = 0.0;
    String styleName = "";

    if (heldSword != null && !heldSword.isEmpty()) {
        CompoundTag tag = heldSword.getOrCreateTag();

        if (tag.contains("select")) {
            // Multi-style sword - has offset
            selectOffset = tag.getDouble("select");
        }

        if (tag.contains("select_name")) {
            // Get display name from item
            styleName = tag.getString("select_name");
        }
    }

    // Calculate actual breathes value
    double actualBreathes = playerBreathes + selectOffset;
    int breathingStyle = (int)(actualBreathes / 100) * 100;
    int formNumber = (int)actualBreathes % 100;

    // If no style name from item, determine from breathes value
    if (styleName.isEmpty()) {
        styleName = switch (breathingStyle) {
            case 100 -> "Water Breathing";
            case 300 -> "Thunder Breathing";
            case 400 -> "Flame Breathing";
            case 500 -> "Wind Breathing";
            case 600 -> "Stone Breathing";
            case 700 -> "Mist Breathing";
            case 800 -> "Serpent Breathing";
            case 900 -> "Sound Breathing";
            case 1100 -> "Moon Breathing";
            case 1200 -> "Sun Breathing";
            case 1300 -> "Flower Breathing";
            case 1400 -> "Insect Breathing";
            case 1500 -> "Love Breathing";
            default -> "Unknown";
        };
    }

    return new BreathingInfo(styleName, formNumber, breathingStyle, actualBreathes);
}

public static class BreathingInfo {
    public final String styleName;
    public final int formNumber;
    public final int styleRange;
    public final double fullBreathesValue;

    public BreathingInfo(String styleName, int formNumber, int styleRange, double fullBreathesValue) {
        this.styleName = styleName;
        this.formNumber = formNumber;
        this.styleRange = styleRange;
        this.fullBreathesValue = fullBreathesValue;
    }

    @Override
    public String toString() {
        return styleName + ", Form " + formNumber;
    }
}
```

### Key Takeaways for Multi-Style Swords

1. **Regular swords**: Breathing form stored only in `player NBT "breathes"`
2. **Multi-style swords**: Use `itemstack NBT "select"` as an **offset** added to player's breathes value
3. **Display name**: Multi-style swords store the style name in `itemstack NBT "select_name"`
4. **To detect current style**: Calculate `actualBreathes = playerBreathes + selectOffset`
5. **Switching mechanism**: Change the `select` offset to jump between breathing style ranges

---

**Author**: Claude Code
**Date**: 2025-10-08
**Source**: Decompiled Kimetsunoyaiba ver2-forge-1.20.1
