# Kimetsu no Yaiba Mod Add-on Development Guide

This guide explains how to create add-ons for the Demon Slayer Minecraft mod, including new breathing techniques, abilities, animations, and advancement integration.

## Mod Architecture Overview

The Kimetsu no Yaiba mod is built using **MCreator** and Forge 1.20.1 with these key dependencies:
- **GeckoLib** (animations for entities/items)
- **PlayerAnimator** (player-specific animations)
- **javafml** loader version [47,)

### Key Systems
- **Breathing Techniques**: Cycle-based ability system with R key switching
- **Special Attacks**: Right-click activated abilities tied to sword types
- **Animation System**: GeckoLib format 1.8.0 with bone-based animations
- **Advancement System**: Progressive unlocking based on usage counts (20, 40, 60, 80, 100 attacks)

## Creating an Add-on Mod

### 1. Project Setup

Create a new Forge mod with `mods.toml`:

```toml
modLoader="javafml"
loaderVersion="[47,)"
license="Your License"

[[mods]]
modId="yourmodid"
version="1.0.0"
displayName="Your Kimetsu Add-on"

# Required Dependencies
[[dependencies.yourmodid]]
    modId="kimetsunoyaiba"
    mandatory=true
    versionRange="[2,)"
    ordering="AFTER"
    side="BOTH"

[[dependencies.yourmodid]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1]"
    ordering="AFTER"
    side="BOTH"

[[dependencies.yourmodid]]
    modId="geckolib"
    mandatory=true
    versionRange="[4.0,)"
    ordering="AFTER"
    side="BOTH"

[[dependencies.yourmodid]]
    modId="playeranimator"
    mandatory=true
    versionRange="[1.0,)"
    ordering="AFTER"
    side="BOTH"
```

### 2. Key Bindings Integration

The original mod uses these keybindings:
- `"Change Breathes Or Blood Demon Art"` - R key for cycling techniques
- `"Special Attack"` - Right-click abilities
- `"Awakening Demon Slayer Mark"` - Mark activation

**Example Java Implementation:**
```java
// Register your keybinding
public static final KeyMapping CYCLE_CUSTOM_BREATHING = new KeyMapping(
    "key.yourmod.cycle_custom_breathing",
    KeyConflictContext.IN_GAME,
    InputConstants.Type.KEYSYM,
    GLFW.GLFW_KEY_T, // Different key to avoid conflicts
    "key.categories.kimetsunoyaiba"
);

@SubscribeEvent
public static void onKeyInput(InputEvent.Key event) {
    if (CYCLE_CUSTOM_BREATHING.consumeClick()) {
        // Handle cycling through your custom breathing techniques
        PacketDistributor.SERVER.noArg().send(new CycleBreathingPacket());
    }
}
```

## Adding New Breathing Techniques

### 3. Breathing Technique System

**Data Structure Pattern:**
Based on the language files, breathing techniques follow this pattern:
- `kimetsu.breath.water` → "Water Breathing"
- `kimetsu.breath.beast` → "Beast Breathing"
- `kimetsu.breath.wind` → "Wind Breathing"

**Implementation Steps:**

1. **Create Custom Breathing Enum:**
```java
public enum CustomBreathingType {
    COSMIC("cosmic", 0x4B0082),
    LUNAR("lunar", 0x708090),
    SOLAR("solar", 0xFFD700);

    private final String name;
    private final int color;

    CustomBreathingType(String name, int color) {
        this.name = name;
        this.color = color;
    }
}
```

2. **Extend Existing Cycling System:**
```java
@Mixin(BreathingHandler.class) // Replace with actual class name
public class BreathingHandlerMixin {

    @Inject(method = "cycleBreathing", at = @At("HEAD"), cancellable = true)
    private void addCustomBreathing(Player player, CallbackInfo ci) {
        // Add your breathing techniques to the cycle
        CustomBreathingCapability cap = player.getCapability(CUSTOM_BREATHING_CAP);
        if (cap != null) {
            cap.cycleToNext();
            ci.cancel(); // Override default behavior if needed
        }
    }
}
```

### 4. Animation System

**Animation File Structure:**
Place animations in `assets/yourmodid/player_animation/`

**Example Custom Breathing Animation:**
```json
{
    "format_version": "1.8.0",
    "animations": {
        "breath_cosmic1": {
            "animation_length": 1.5,
            "bones": {
                "right_arm": {
                    "rotation": {
                        "0.0": {"vector": [0, 0, 0]},
                        "0.5": {"vector": [-45, 30, 90]},
                        "1.0": {"vector": [-90, 60, 120]},
                        "1.5": {"vector": [0, 0, 0]}
                    },
                    "position": {
                        "0.5": {"vector": [0, -1, -2]},
                        "1.0": {"vector": [0, -2, -3]}
                    }
                },
                "left_arm": {
                    "rotation": {
                        "0.0": {"vector": [0, 0, 0]},
                        "0.5": {"vector": [-45, -30, -90]},
                        "1.0": {"vector": [-90, -60, -120]},
                        "1.5": {"vector": [0, 0, 0]}
                    }
                },
                "body": {
                    "rotation": {
                        "0.5": {"vector": [10, 0, 0]},
                        "1.0": {"vector": [20, 0, 0]}
                    }
                }
            }
        }
    },
    "geckolib_format_version": 2
}
```

**Animation Integration:**
```java
@SubscribeEvent
public static void onBreathingAttack(PlayerBreathingAttackEvent event) {
    Player player = event.getPlayer();
    CustomBreathingType breathing = getPlayerBreathing(player);

    switch(breathing) {
        case COSMIC:
            triggerAnimation(player, "breath_cosmic1");
            break;
        case LUNAR:
            triggerAnimation(player, "breath_lunar1");
            break;
    }
}

private static void triggerAnimation(Player player, String animationName) {
    if (player.level().isClientSide) {
        PlayerAnimationApi.setPlayerAnimation(player,
            new ResourceLocation("yourmodid", animationName));
    }
}
```

## Modifying Existing Systems

### 5. Extending the Cycling System

**Hook into Existing Keybind Events:**
```java
@SubscribeEvent
public static void onChangeBreathing(ChangeBreathingEvent event) {
    Player player = event.getPlayer();

    // Check if player has unlocked custom breathing
    if (hasCustomBreathingUnlocked(player)) {
        // Add custom breathing to cycle
        addCustomBreathingToCycle(player, event.getCurrentBreathing());
    }
}
```

### 6. Sword Ability Extensions

**Right-click Ability Integration:**
```java
@SubscribeEvent
public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
    ItemStack stack = event.getItemStack();
    Player player = event.getEntity();

    if (isNichirinSword(stack)) {
        CustomBreathingType breathing = getPlayerBreathing(player);
        if (breathing != null) {
            executeCustomSpecialAttack(player, breathing, stack);
            event.setCanceled(true);
        }
    }
}

private static void executeCustomSpecialAttack(Player player, CustomBreathingType breathing, ItemStack sword) {
    switch(breathing) {
        case COSMIC:
            // Create cosmic projectiles
            spawnCosmicProjectiles(player);
            break;
        case LUNAR:
            // Area damage with lunar effects
            performLunarSlash(player);
            break;
    }
}
```

## Advancement System Integration

### 7. Custom Advancements

**Advancement File Structure:** `data/yourmodid/advancements/`

**Example Custom Breathing Advancement:**
```json
{
  "display": {
    "icon": {
      "item": "yourmodid:cosmic_nichirin_sword"
    },
    "title": {
      "translate": "advancements.cosmic_breathing_20.title"
    },
    "description": {
      "translate": "advancements.cosmic_breathing_20.descr"
    },
    "frame": "task",
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": false
  },
  "criteria": {
    "cosmic_breathing_20": {
      "trigger": "yourmodid:breathing_attack",
      "conditions": {
        "breathing_type": "cosmic",
        "attack_count": 20
      }
    }
  },
  "parent": "kimetsunoyaiba:mizunoto"
}
```

**Custom Advancement Trigger:**
```java
public class BreathingAttackTrigger extends SimpleCriterionTrigger<BreathingAttackInstance> {

    public static final ResourceLocation ID = new ResourceLocation("yourmodid", "breathing_attack");

    public void trigger(ServerPlayer player, String breathingType, int attackCount) {
        this.trigger(player, (instance) ->
            instance.matches(breathingType, attackCount));
    }
}

// Register in your mod's advancement trigger registry
@SubscribeEvent
public static void onBreathingAttack(CustomBreathingAttackEvent event) {
    if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
        ModAdvancementTriggers.BREATHING_ATTACK.trigger(serverPlayer,
            event.getBreathingType(), event.getAttackCount());
    }
}
```

### 8. Progression Chain Integration

**Link with Existing Advancements:**
```json
{
  "parent": "kimetsunoyaiba:breathing_100",
  "display": {
    "title": {"translate": "advancements.master_cosmic_breathing.title"},
    "description": {"translate": "advancements.master_cosmic_breathing.descr"},
    "frame": "challenge"
  },
  "criteria": {
    "master_cosmic": {
      "trigger": "yourmodid:breathing_mastery",
      "conditions": {
        "breathing_type": "cosmic",
        "mastery_level": 5
      }
    }
  }
}
```

## Language Integration

### 9. Localization

**Add to `assets/yourmodid/lang/en_us.json`:**
```json
{
  "kimetsu.breath.cosmic": "Cosmic Breathing",
  "kimetsu.breath.lunar": "Lunar Breathing",
  "kimetsu.breath.solar": "Solar Breathing",

  "key.yourmodid.cycle_custom_breathing": "Cycle Custom Breathing",

  "advancements.cosmic_breathing_20.title": "Cosmic Initiate",
  "advancements.cosmic_breathing_20.descr": "Attack 20 times with Cosmic Breathing",

  "item.yourmodid.cosmic_nichirin_sword": "Cosmic Nichirin Sword"
}
```

## Development Best Practices

### 10. Compatibility Guidelines

1. **Use Mixins carefully** - Only inject where necessary to avoid conflicts
2. **Event-driven architecture** - Hook into existing events rather than overriding methods
3. **Capability system** - Store custom data using Forge capabilities
4. **Client-server sync** - Use proper packet handling for multiplayer compatibility
5. **Resource location naming** - Use your modid as namespace to avoid conflicts

### 11. Testing Checklist

- [ ] Breathing techniques cycle properly with R key
- [ ] Right-click abilities work with all sword types
- [ ] Animations play correctly on client and sync to server
- [ ] Advancements unlock at correct thresholds
- [ ] Multiplayer synchronization works
- [ ] Compatible with existing breathing techniques
- [ ] No keybinding conflicts
- [ ] Proper integration with Hashira progression system

## Advanced Features

### 12. Custom Particle Effects

```java
@SubscribeEvent
public static void onBreathingParticles(BreathingAttackEvent event) {
    Level world = event.getPlayer().level();
    if (world.isClientSide) {
        CustomBreathingType breathing = getPlayerBreathing(event.getPlayer());
        spawnBreathingParticles(world, event.getPlayer(), breathing);
    }
}
```

### 13. Sound Integration

Add custom breathing sounds in `assets/yourmodid/sounds/`:
```json
{
  "cosmic_breathing_attack": {
    "category": "player",
    "subtitle": "subtitles.cosmic_breathing_attack",
    "sounds": [
      "yourmodid:breathing/cosmic_attack1",
      "yourmodid:breathing/cosmic_attack2"
    ]
  }
}
```

This comprehensive guide provides the foundation for creating sophisticated add-ons that seamlessly integrate with the existing Kimetsu no Yaiba mod architecture.