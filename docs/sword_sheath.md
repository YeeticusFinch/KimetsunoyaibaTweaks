# Sword Sheath System Documentation

This document explains the sword sheath and display system used in the Kimetsuno Yaiba Multiplayer mod. The system displays nichirin swords on player models (slot-based) and on GeckoLib entities (combat-based) when they are not actively being held.

## Table of Contents

1. [Player Sword Display System (Working)](#player-sword-display-system-working)
2. [Entity Sword Display System (GeckoLib Entities)](#entity-sword-display-system-geckolib-entities)
3. [Shared Components](#shared-components)
4. [Configuration](#configuration)

---

## Player Sword Display System (Working)

**⚠️ CRITICAL: This system works perfectly and must not be broken!**

The player sword display system is fully functional and handles displaying swords and sheaths on player models when they switch hotbar slots.

### Architecture Overview

The player system uses a **render layer approach** that integrates with Minecraft's player rendering pipeline. This is clean, performant, and completely separate from entity logic.

### Key Components

#### 1. SwordDisplayRenderer.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/renderer/SwordDisplayRenderer.java`

**Purpose:** Render layer that draws swords and sheaths on player models.

**How it works:**
- Extends `RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>`
- Registered as a render layer on player renderer (see `SwordDisplayRendererSetup.java`)
- Called automatically during player rendering
- Retrieves display state from `SwordDisplayTracker`
- Renders sword + sheath when sword is not held
- Renders sheath only when sword is drawn (if sheath persists)

**Key Methods:**
- `render()` - Main entry point, renders left and right sword displays
- `renderSwordWithSheath()` - Renders sword with sheath behind it
- `renderSheathOnly()` - Renders just the sheath (for persistent sheaths)
- `renderSwordOnHip()` - Applies hip position transforms from config
- `renderSwordOnBack()` - Applies back position transforms from config

**Code Flow:**
```java
render()
├─ Gets SwordDisplayState for player UUID
├─ Left side:
│  ├─ Has sword? → renderSwordWithSheath() with left hip/back position
│  └─ Sheath persists? → renderSheathOnly() with left position
└─ Right side:
   ├─ Has sword? → renderSwordWithSheath() with right hip/back position
   └─ Sheath persists? → renderSheathOnly() with right position
```

---

#### 2. SwordDisplayTracker.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SwordDisplayTracker.java`

**Purpose:** Tracks which swords should be displayed on which players by monitoring inventory changes.

**How it works:**
- Called every client tick via `tick()` method
- Monitors player hotbar slot changes
- Detects when player switches from nichirin sword to another item
- Updates display state for all players in view
- Syncs display state to server via `SwordDisplaySyncPacket`

**Data Structures:**
```java
class SwordDisplayState {
    SlotSwordEntry leftDisplay;   // Sword on left hip/back
    SlotSwordEntry rightDisplay;  // Sword on right hip/back
    Item leftSheathItem;          // Sheath item for left
    Item rightSheathItem;         // Sheath item for right
    boolean leftSheathPersists;   // Should left sheath stay when sword drawn?
    boolean rightSheathPersists;  // Should right sheath stay when sword drawn?
}

class SlotSwordEntry {
    int hotbarSlot;                                  // Which hotbar slot (0-8)
    ItemStack sword;                                 // The sword item
    SwordDisplayPosition displayPosition;            // HIP or BACK
}
```

**Key Logic - Slot Change Detection:**

1. **Player switches FROM nichirin sword TO something else:**
   - Previous slot had nichirin sword → Add it to display (sheathing the sword)
   - Display state: sword appears on hip/back with sheath

2. **Player switches FROM something TO nichirin sword that's displayed:**
   - Current slot has nichirin sword that's already displayed → Remove from display (drawing the sword)
   - If sheath is temporary (Uzui, Inosuke) → Spawn poof particles, remove sheath
   - If sheath is persistent (Rengoku, Kanroji, default) → Keep sheath visible, remove sword from display

3. **Player removes sword from hotbar:**
   - Monitor each displayed slot's contents
   - If item in slot changes or is removed → Remove from display

**Sheath Persistence Logic:**
```java
// When drawing sword:
if (sheathInfo.persistsWhenDrawn()) {
    // Keep sheath visible on hip/back (Rengoku, Kanroji, default sheaths)
    state.leftSheathItem = sheathItem;
    state.leftSheathPersists = true;
} else {
    // Remove sheath with poof particles (Uzui, Inosuke sheaths)
    spawnSheathDisappearParticles(player, position, isLeft);
}
```

**Key Methods:**
- `tick()` - Called every client tick, updates all players
- `updatePlayerSwordDisplay()` - Updates display for one player
- `addSword()` - Adds a sword to left or right display
- `removeSlot()` - Removes sword from display by hotbar slot
- `spawnSheathDisappearParticles()` - Spawns poof particles when temporary sheath disappears
- `sendDisplayUpdateToServer()` - Syncs display state via network packet

---

#### 3. SwordDisplayRendererSetup.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SwordDisplayRendererSetup.java`

**Purpose:** Registers the SwordDisplayRenderer as a render layer on player skins.

**How it works:**
- Subscribes to `EntityRenderersEvent.AddLayers` (Forge MOD bus)
- Adds `SwordDisplayRenderer` to all player skin variants (default, slim)
- Only runs on client side

**Registration Code:**
```java
@SubscribeEvent
public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
    // Get player renderers for both skin types
    LivingEntityRenderer<?, ?> defaultSkin = event.getSkin(PlayerRenderer.DefaultSkin.STEVE);
    LivingEntityRenderer<?, ?> slimSkin = event.getSkin(PlayerRenderer.DefaultSkin.ALEX);

    // Add render layer to each
    if (defaultSkin instanceof PlayerRenderer playerRenderer) {
        playerRenderer.addLayer(new SwordDisplayRenderer(playerRenderer, itemInHandRenderer));
    }
    // ... same for slim skin
}
```

---

### Player System Data Flow

```
┌─────────────────────────────────────────────────────────┐
│                   CLIENT TICK                            │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  SwordDisplayTracker.tick()            │
    │  (called every client tick)            │
    └───────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  Detect hotbar slot changes            │
    │  - Player switched from sword?         │
    │  - Player switched to displayed sword? │
    │  - Hotbar contents changed?            │
    └───────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  Update SwordDisplayState              │
    │  - Add/remove sword from display       │
    │  - Update sheath persistence           │
    │  - Track hotbar slots                  │
    └───────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  Send SwordDisplaySyncPacket           │
    │  (network sync to server)              │
    └───────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                 PLAYER RENDERING                         │
└─────────────────────────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  PlayerRenderer.render()               │
    │  (Minecraft's player rendering)        │
    └───────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  SwordDisplayRenderer.render()         │
    │  (our render layer)                    │
    └───────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  Get SwordDisplayState for player UUID │
    └───────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  Render swords and sheaths             │
    │  - Left hip/back (if has sword/sheath) │
    │  - Right hip/back (if has sword/sheath)│
    │  - Apply config positions & rotations  │
    └───────────────────────────────────────┘
```

---

### Player System Example Scenarios

#### Scenario 1: Player Sheathes Rengoku's Sword (Persistent Sheath)

1. **Initial State:**
   - Player holding Rengoku sword in slot 0
   - Display: Nothing on hip/back

2. **Player presses '2' (switches to slot 1):**
   - `SwordDisplayTracker` detects slot change: 0 → 1
   - Previous slot (0) had Rengoku sword
   - Adds Rengoku sword to display (left side)
   - Gets sheath info: persistent sheath (default behavior)
   - Display: Rengoku sword + sheath on left hip/back

3. **Player presses '1' (switches back to slot 0):**
   - Current slot (0) has Rengoku sword that's displayed
   - Removes Rengoku sword from display
   - Sheath is persistent → Keep sheath visible
   - Display: Only sheath on left hip/back (sword in hand)

4. **Player presses '2' again:**
   - Current slot (1) has no sword
   - Previous slot (0) has Rengoku sword (but already drawn, not displayed)
   - No change to display
   - Display: Only sheath on left hip/back (sword in hand)

#### Scenario 2: Player Sheathes Uzui's Swords (Temporary Sheaths)

1. **Initial State:**
   - Player holding Uzui sword in slot 0
   - Display: Nothing on hip/back

2. **Player presses '2':**
   - Adds Uzui sword to display
   - Gets sheath info: temporary sheath (Uzui-specific)
   - Display: Uzui sword + sheath on left hip/back

3. **Player presses '1' (switches back to Uzui sword):**
   - Removes Uzui sword from display
   - Sheath is temporary → Spawn poof particles, remove sheath
   - Display: Nothing on hip/back (sword in hand, sheath disappeared)

---

## Entity Sword Display System (GeckoLib Entities)

**Status: Working for this mod's GeckoLib entities and base mod GeckoLib entities (namespace `kimetsunoyaiba`).**
This does not apply to players or GhostlyClone.

### Architecture Overview

The entity system mirrors the player system but uses a GeckoLib render layer and never modifies
entity equipment state during rendering.

### Key Components

#### 1. GeoSwordDisplayLayer.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/entities/client/GeoSwordDisplayLayer.java`

**Purpose:** Render layer that draws swords and sheaths on GeckoLib entities.

**How it works:**
- Runs as a `GeoRenderLayer` on the entity renderer (no render events).
- Checks mainhand for nichirin swords and skips sheath-exempt swords.
- Uses `EntityCombatStateTracker` for combat state (10s cooldown).
- Anchors to the `body` bone so rotation and scaling follow the model.
- Applies entity yaw rotation, then hip/back transforms from `SwordDisplayConfig`.
- Applies optional entity-only translation/rotation flips and offsets from `sword_display.entity_display.hip` and `sword_display.entity_display.back`.
- Uses `SwordDisplayConfig.scale` and the sheath scale in `SheathModelRenderer`.
- Out of combat: renders sword + sheath on hip/back.
- In combat: renders sheath only if it persists (`persistsWhenDrawn()`).

#### 2. GeoEquipmentLayer.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/entities/client/GeoEquipmentLayer.java`

**Purpose:** Hides the mainhand sword when the entity is out of combat (render-only).

**How it works:**
- For mainhand bones, returns `ItemStack.EMPTY` when a nichirin sword is equipped
  and the entity is not in combat.


#### 3. GenericItemLayerMixin.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/GenericItemLayerMixin.java`

**Purpose:** Hides base mod entity mainhand swords when they are out of combat.

**How it works:**
- Injects into the base mod `GenericItemLayer` (a `BlockAndItemGeoLayer`)
- Returns `ItemStack.EMPTY` for mainhand bones when the entity is not in combat

#### 4. EntityCombatStateTracker.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/EntityCombatStateTracker.java`

**Combat rules:**
- Active target (`Mob.getTarget()` alive)
- Recently hurt (last 20 ticks)
- Breathing form animation/cooldown (BreathingSlayerEntity)
- Swinging (attack or air swing)
- 10 second cooldown after last combat activity

#### 5. Renderer Registration
- `MuichiroRenderer` and `KanrojiRenderer` add `GeoSwordDisplayLayer`.
- API entities should add the layer in their GeckoLib renderer.
- Base mod entities (namespace `kimetsunoyaiba`) add the layer via `SwordDisplayRendererSetup`.

### Render Flow (High Level)

1. `GeoEquipmentLayer` hides the mainhand nichirin sword when out of combat.
2. `GeoSwordDisplayLayer` renders sword + sheath on hip/back when out of combat.
3. When in combat, the sword stays in hand and only persistent sheaths render.

### Scope and Limitations

- This mod's GeckoLib entities opt in by adding `GeoSwordDisplayLayer`.
- Base mod GeckoLib living entities (namespace `kimetsunoyaiba`) are supported via `SwordDisplayRendererSetup` + `GenericItemLayerMixin`.
- Non-GeckoLib entities are still unsupported.
- If a model uses a different main body bone, update `ANCHOR_BONE_NAME` in `GeoSwordDisplayLayer`.


## Shared Components

These components are used by both player and entity systems (or intended to be).

### 1. SwordSheathRegistry.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SwordSheathRegistry.java`

**Purpose:** Maps swords to their sheath items and persistence behavior.

**Key Concepts:**

**Sheath Types:**
- **Persistent Sheaths:** Stay visible when sword is drawn (Rengoku, Kanroji, default sheaths)
- **Temporary Sheaths:** Disappear with poof particles when sword is drawn (Uzui, Inosuke)

**SheathInfo Class:**
```java
class SheathInfo {
    Item sheathItem;           // Which item is the sheath
    boolean persistsWhenDrawn; // Should sheath stay visible when sword drawn?
}
```

**Registration Methods:**
```java
// Set default sheath for all swords
SwordSheathRegistry.setDefaultSheath(SheathItems.SWORD_SHEATH.get());

// Register persistent sheath (stays when drawn)
SwordSheathRegistry.registerPersistentSheath(
    swordItem,
    SheathItems.SWORD_SHEATH_RENGOKU.get()
);

// Register temporary sheath (disappears when drawn)
SwordSheathRegistry.registerTemporarySheath(
    swordItem,
    SheathItems.SWORD_SHEATH_UZUI.get()
);
```

**Query Methods:**
```java
// Get full sheath info
SheathInfo info = SwordSheathRegistry.getSheathInfo(swordStack);

// Get just the sheath item
Item sheathItem = SwordSheathRegistry.getSheathItem(swordStack);

// Check if sheath persists
boolean persists = SwordSheathRegistry.sheathPersistsWhenDrawn(swordStack);
```

**Fallback Behavior:**
- If sword has no registered sheath → Use default sheath (persistent)
- If no default sheath set → Return null (no sheath)

---

### 2. SheathModelRenderer.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SheathModelRenderer.java`

**Purpose:** Helper for rendering sheath models.

**How it works:**
- Caller applies sword scale and positioning
- This applies global sheath scale + per-sheath scale
- Renders sheath using Minecraft's item renderer

**Scale System:**
```java
// Global config scale (e.g., 0.5 = half size)
SwordDisplayConfig.scale = 0.5

// Global sheath scale multiplier (e.g., 1.2 = 20% bigger than sword)
SwordDisplayConfig.sheathScale = 1.2

// Per-sheath scale (for sheaths that need custom sizing)
SheathModelRenderer.registerSheathScale(SheathItems.SWORD_SHEATH_KANROJI.get(), 1.5f);

// Final sheath size = swordScale * sheathScale * perSheathScale
// Example: 0.5 * 1.2 * 1.5 = 0.9 (90% of normal size)
```

**Rendering Method:**
```java
public static void renderSheath(Item sheathItem, PoseStack poseStack,
                                MultiBufferSource buffer, int packedLight, int levelId) {
    // Apply global sheath scale * per-sheath scale
    float combinedScale = SwordDisplayConfig.sheathScale * getSheathScale(sheathItem);
    poseStack.scale(combinedScale, combinedScale, combinedScale);

    // Render sheath item
    Minecraft.getInstance().getItemRenderer().renderStatic(
        new ItemStack(sheathItem),
        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
        packedLight,
        OverlayTexture.NO_OVERLAY,
        poseStack,
        buffer,
        level,
        levelId
    );
}
```

---

### 3. SwordDisplayConfig.java
**Location:** `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordDisplayConfig.java`

**Purpose:** Configuration for sword display positions, scales, and per-sword overrides.

**Configuration Options:**

**Global Settings:**
- `enabled` (boolean) - Enable/disable sword display feature
- `default_position` (HIP/BACK) - Default position for swords
- `scale` (double, 0.1-5.0) - Scale of displayed swords
- `sheath_scale` (double, 0.1-5.0) - Additional scale for sheaths
- `render_sheaths` (boolean) - Enable/disable sheath rendering

**Per-Sword Position Overrides:**
```toml
sword_position_overrides = [
    "kimetsunoyaiba:nichirinsword_uzui=BACK",
    "kimetsunoyaiba:nichirinsword_inosuke=HIP"
]
```

**Hip Position (Left & Right):**
- `translate_x/y/z` - Position offset from body center
- `rotate_x/y/z` - Rotation in degrees

**Back Position (Left & Right):**
- `translate_x/y/z` - Position offset from body center
- `rotate_x/y/z` - Rotation in degrees

**Entity Display Overrides (Non-Player):**
- `entity_display.back.translation.offset_x/y/z` - Additional translation offsets for entity back display
- `entity_display.back.translation.flip_x/y/z` - Flip base back translations (multiplies by -1)
- `entity_display.back.rotation.offset_x/y/z` - Additional rotation offsets in degrees for back display
- `entity_display.back.rotation.flip_x/y/z` - Flip base back rotations (multiplies by -1)
- `entity_display.hip.translation.offset_x/y/z` - Additional translation offsets for entity hip display
- `entity_display.hip.translation.flip_x/y/z` - Flip base hip translations (multiplies by -1)
- `entity_display.hip.rotation.offset_x/y/z` - Additional rotation offsets in degrees for hip display
- `entity_display.hip.rotation.flip_x/y/z` - Flip base hip rotations (multiplies by -1)

**Example Config Values:**
```toml
[sword_display]
    enabled = true
    default_position = "HIP"
    scale = 1.0
    sheath_scale = 1.0
    render_sheaths = true

[sword_display.hip_position]
    # Left hip
    left_translate_x = 0.3
    left_translate_y = 0.55
    left_translate_z = -0.1
    left_rotate_z = 0.0
    left_rotate_y = 180.0
    left_rotate_x = -65.0

    # Right hip
    right_translate_x = -0.3
    right_translate_y = 0.55
    right_translate_z = -0.1
    right_rotate_z = 0.0
    right_rotate_y = 180.0
    right_rotate_x = -65.0

[sword_display.back_position]
    # Left back
    left_translate_x = 0.3
    left_translate_y = -0.1
    left_translate_z = 0.2
    left_rotate_z = 35.0
    left_rotate_y = 90.0
    left_rotate_x = 0.0

    # Right back
    right_translate_x = -0.3
    right_translate_y = -0.1
    right_translate_z = 0.2
    right_rotate_z = -35.0
    right_rotate_y = 90.0
    right_rotate_x = 0.0

#Entity Display Overrides (non-player)
[sword_display.entity_display]

[sword_display.entity_display.back]

#Translation overrides for entity sword/sheath display on back
[sword_display.entity_display.back.translation]
#Entity translation X offset (added after optional flip)
#Range: -5.0 ~ 5.0
offset_x = 0.0
#Entity translation Y offset (added after optional flip)
#Range: -5.0 ~ 5.0
offset_y = 1.5
#Entity translation Z offset (added after optional flip)
#Range: -5.0 ~ 5.0
offset_z = 0.0
#Flip entity translation X (multiply base by -1)
flip_x = false
#Flip entity translation Y (multiply base by -1)
flip_y = true
#Flip entity translation Z (multiply base by -1)
flip_z = false

#Rotation overrides for entity sword/sheath display on back
[sword_display.entity_display.back.rotation]
#Entity rotation X offset in degrees (added after optional flip)
#Range: -360.0 ~ 360.0
offset_x = 180.0
#Entity rotation Y offset in degrees (added after optional flip)
#Range: -360.0 ~ 360.0
offset_y = 0.0
#Entity rotation Z offset in degrees (added after optional flip)
#Range: -360.0 ~ 360.0
offset_z = 0.0
#Flip entity rotation X (multiply base by -1)
flip_x = false
#Flip entity rotation Y (multiply base by -1)
flip_y = true
#Flip entity rotation Z (multiply base by -1)
flip_z = true

[sword_display.entity_display.hip]

#Translation overrides for entity sword/sheath display on hip
[sword_display.entity_display.hip.translation]
#Entity translation X offset (added after optional flip)
#Range: -5.0 ~ 5.0
offset_x = 0.0
#Entity translation Y offset (added after optional flip)
#Range: -5.0 ~ 5.0
offset_y = 1.5
#Entity translation Z offset (added after optional flip)
#Range: -5.0 ~ 5.0
offset_z = 0.0
#Flip entity translation X (multiply base by -1)
flip_x = false
#Flip entity translation Y (multiply base by -1)
flip_y = true
#Flip entity translation Z (multiply base by -1)
flip_z = false

#Rotation overrides for entity sword/sheath display on hip
[sword_display.entity_display.hip.rotation]
#Entity rotation X offset in degrees (added after optional flip)
#Range: -360.0 ~ 360.0
offset_x = 0.0
#Entity rotation Y offset in degrees (added after optional flip)
#Range: -360.0 ~ 360.0
offset_y = 0.0
#Entity rotation Z offset in degrees (added after optional flip)
#Range: -360.0 ~ 360.0
offset_z = 180.0
#Flip entity rotation X (multiply base by -1)
flip_x = false
#Flip entity rotation Y (multiply base by -1)
flip_y = false
#Flip entity rotation Z (multiply base by -1)
flip_z = false
```

---

## Configuration

### File Location
Config file: `config/kimetsunoyaibamultiplayer/sword_display.toml`

### Testing Positions

**To adjust sword positions in-game:**

1. Open config file with text editor
2. Modify translation/rotation values
3. Save file
4. Run `/reload` command in-game (reloads configs)
5. View changes immediately

**Coordinate System:**
- **X axis:** Left (-) to Right (+) of player
- **Y axis:** Down (-) to Up (+)
- **Z axis:** Behind (-) to Front (+) of player

**Rotation System:**
- **X rotation:** Pitch (forward/backward tilt)
- **Y rotation:** Yaw (left/right turn)
- **Z rotation:** Roll (clockwise/counterclockwise)
- All values in degrees (-360 to 360)

### Common Adjustments

**Moving sword forward/back on hip:**
- Increase `hip_right_translate_z` (more forward)
- Decrease `hip_right_translate_z` (more backward)

**Moving sword higher/lower:**
- Increase `hip_right_translate_y` (higher)
- Decrease `hip_right_translate_y` (lower)

**Tilting sword angle:**
- Adjust `hip_right_rotate_x` (forward/backward tilt)
- Adjust `hip_right_rotate_z` (clockwise/counterclockwise)

**Making sheath bigger/smaller:**
- Adjust `sheath_scale` (global multiplier)
- Or use `SheathModelRenderer.registerSheathScale()` for specific sheaths

---

## Implementation Notes

### Why Player System Works

- Proper integration via render layers
- No inventory or equipment state changes during rendering
- Clean separation of tracking, rendering, and config
- Network sync for multiplayer

### Entity System (GeckoLib) Notes

- Uses `GeoSwordDisplayLayer` instead of render events.
- Uses `GeoEquipmentLayer` to hide swords from hands when out of combat.
- Anchors to the `body` bone so rotation and scaling follow the model.
- Combat state and sheath poof effects come from `EntityCombatStateTracker`.

### Legacy/Experimental Code

- `EntitySwordDisplayRenderer.java` is disabled and kept for reference only.
- `BlockAndItemGeoLayerMixin.java` is not relied on for this system.

### Base Mod Compatibility

- Base mod GeckoLib entities are supported via the base renderer layer hookup and `GenericItemLayerMixin`.


## Related Files

### Player System Files
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/renderer/SwordDisplayRenderer.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SwordDisplayTracker.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SwordDisplayRendererSetup.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/network/packets/SwordDisplaySyncPacket.java`

### Entity System Files (GeckoLib)
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/entities/client/GeoSwordDisplayLayer.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/entities/client/GeoEquipmentLayer.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/EntityCombatStateTracker.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/GenericItemLayerMixin.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/entities/client/MuichiroRenderer.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/entities/client/KanrojiRenderer.java`

### Legacy/Disabled Entity Files
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/EntitySwordDisplayRenderer.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/mixin/BlockAndItemGeoLayerMixin.java`

### Shared Files
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SwordSheathRegistry.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SheathModelRenderer.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/config/SwordDisplayConfig.java`
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/items/SheathItems.java`

### Registration Files
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/SheathRegistration.java` - Registers sheaths for specific swords


## Summary

- Player system: fully functional and unchanged.
- Entity system: working for this mod's GeckoLib entities that opt in, using `GeoSwordDisplayLayer` and combat tracking.
- Base mod GeckoLib entities: supported via layer hookup + GenericItemLayer mixin.
