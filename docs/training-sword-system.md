# Training Sword System

The Training Sword System allows any nichirin sword to be converted into a training sword with restricted functionality. This is useful for training scenarios where players should only have access to basic forms.

## Overview

Training swords are nichirin swords that have been marked with a special NBT tag. When a sword is marked as a training sword:

1. **Form Restriction**: Only the 1st Form of the breathing style is accessible
2. **Cycle Prevention**: Attempting to cycle forms will reset to 1st Form and display a message
3. **Reduced Damage**: Training swords deal 1 less damage than normal swords (3.5 instead of 4.5)
4. **Persistence**: The training sword tag persists even if the item is renamed in an anvil

## Supported Swords

The training sword system works with:
- **Base mod swords** (from KimetsunoYaiba-ver3)
- **Custom mod swords** (from kimetsunoyaibamultiplayer)
- **Addon mod swords** (registered via our API)

Detection uses `SwordParticleMapping.isKimetsunoyaibaSword()` which handles all three cases.

## Commands

### /trainingsword

Converts the held sword to training mode. Requires OP level 2.

```
/trainingsword        - Convert held sword to training mode
/trainingsword remove - Remove training mode from held sword
```

When a sword is converted to training mode:
1. The `TrainingSword` NBT tag is added
2. A damage reduction modifier is applied (-1 damage, so 3.5 instead of 4.5)
3. The display name is modified (e.g., "Nichirin Sword (Water)" becomes "Nichirin Training Sword (Water)")
4. The player is immediately reset to the 1st Form of their breathing style

## API Usage

### TrainingSwordHelper

The main utility class is `TrainingSwordHelper` located in `com.lerdorf.kimetsunoyaibamultiplayer.util`.

#### Checking if a sword is a training sword

```java
import com.lerdorf.kimetsunoyaibamultiplayer.util.TrainingSwordHelper;

ItemStack sword = player.getMainHandItem();
if (TrainingSwordHelper.isTrainingSword(sword)) {
    // This is a training sword
}
```

#### Converting a sword to a training sword

```java
ItemStack sword = player.getMainHandItem();

// Method 1: Just add the tag and rename (no form reset)
boolean success = TrainingSwordHelper.makeTrainingSword(sword);

// Method 2: Add tag, rename, AND reset player to first form (recommended)
boolean success = TrainingSwordHelper.makeTrainingSword(sword, player);
if (success) {
    // Sword was converted successfully
    // The item now has the TrainingSword NBT tag
    // The display name has been modified
    // Player is now on 1st Form (if using the player overload)
}
```

#### Resetting to first form

```java
// Manually reset a player to their first form
TrainingSwordHelper.resetToFirstForm(sword, player);
```

#### Removing the training sword tag

```java
ItemStack trainingSword = player.getMainHandItem();
boolean success = TrainingSwordHelper.removeTrainingSword(trainingSword);
if (success) {
    // Training sword tag removed
    // Display name reset to default
}
```

## Naming Conventions

When converting a sword to a training sword, the display name is modified:

| Original Name | Training Sword Name |
|---------------|---------------------|
| Nichirin Sword (Water) | Nichirin Training Sword (Water) |
| Nichirin Naginata (Forest) | Nichirin Training Naginata (Forest) |
| Flame Blade | Training Flame Blade |

Rules:
1. If the name contains "Nichirin ", insert "Training " after "Nichirin "
2. Otherwise, prepend "Training " to the front
3. If "Training" is already in the name, no changes are made

## NBT Tag

Training swords are identified by the `TrainingSword` boolean NBT tag on the ItemStack:

```nbt
{
  TrainingSword: 1b
}
```

This allows detection to work even after the item has been renamed in an anvil.

## Form Cycling Behavior

When a player attempts to cycle forms (R key) while holding a training sword:

1. The form is reset to the 1st Form of the breathing style
2. The variation index is reset to 0
3. An action bar message is displayed: `[Training Sword] Only the 1st Form is available on a training sword.`

This behavior is handled in `CycleBreathingFormPacket.handleTrainingSwordCycleAttempt()`.

### Custom Swords (BreathingSwordItem)

For custom breathing swords:
- Form index is reset to 0 in `PlayerBreathingData`
- The `breathes` NBT value is updated to the first form's ID
- Form index and breathes value are synced to clients

### Base Mod Swords

For base mod swords:
- The `breathes` NBT value is reset to the first form for the style
- First form is determined by `BaseModStyleMapping.getFormsForStyle(style)[0]`
- The new breathes value is synced to clients

## Implementation Files

- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/util/TrainingSwordHelper.java` - Main utility class
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/commands/TrainingSwordCommand.java` - /trainingsword command
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/network/packets/CycleBreathingFormPacket.java` - Server-side form cycling restriction logic
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/KeyInputHandler.java` - Client-side key input interception for base mod swords
- `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/KimetsunoyaibaMultiplayer.java` - Server-side failsafe in `onPlayerTick()`

## Technical Details: Base Mod Sword Interception

For base mod swords, the cycling is normally handled by the base mod directly (KimetsunoYaiba-ver3). Our mod intercepts this in `KeyInputHandler`:

1. When the cycle key (R) is pressed, we check if the held item is a training sword
2. If it's a **base mod training sword**:
   - Consume the base mod's keybind (`CHANGE_BREATHES_AND_BLOOD_ART.consumeClick()`)
   - Display the training sword message to the player
   - Send a `CycleBreathingFormPacket` to the server to ensure first form is set
3. If it's a **custom mod training sword**:
   - The packet handler on the server side handles the restriction

This dual approach ensures training swords work for both:
- Base mod swords (intercepted on client side before base mod processes the key)
- Custom mod swords (handled on server side in the packet handler)

## Server-Side Failsafe

In addition to client-side key interception, there is a server-side failsafe in `KimetsunoyaibaMultiplayer.onPlayerTick()` that catches any form cycling that slips through (e.g., when the player holds down the cycle key):

1. Every server tick, check if the player is holding a training sword
2. If the current `breathes` value is not the first form for that breathing style, reset it
3. Update the sword's `select` NBT to 0 and sync to clients

This ensures that even if the base mod manages to cycle the form before our key interception runs, the server will immediately reset it back to the first form.

## Future Enhancements

Potential future additions:
- Command to convert swords to/from training mode
- Config option to customize the restriction level (e.g., allow first 2 forms)
- Visual indicator on the sword (different texture/color)
- Training sword crafting recipe
