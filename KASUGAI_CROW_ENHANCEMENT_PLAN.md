# Kasugai Crow Enhancement Plan
## GeckoLib Conversion & Flying Dodge Behavior

### Project Overview
Create a Forge mod that replaces the existing traditional Minecraft model `Modelcrow` with a modern GeckoLib model, adding sophisticated flying dodge mechanics where the crow becomes temporarily invulnerable and flies in circles 20-30 blocks above ground when taking damage.

---

## Phase 1: Project Setup

### 1.1 Create New Forge Mod Project
```toml
# mods.toml
modLoader="javafml"
loaderVersion="[47,)"
license="MIT"

[[mods]]
modId="kasugaicrowenhanced"
version="1.0.0"
displayName="Kasugai Crow Enhanced"

# Dependencies
[[dependencies.kasugaicrowenhanced]]
    modId="kimetsunoyaiba"
    mandatory=true
    versionRange="[2,)"
    ordering="AFTER"
    side="BOTH"

[[dependencies.kasugaicrowenhanced]]
    modId="geckolib"
    mandatory=true
    versionRange="[4.0,)"
    ordering="AFTER"
    side="BOTH"
```

### 1.2 Project Structure
```
src/main/java/com/yourname/kasugaicrowenhanced/
├── KasugaiCrowEnhanced.java (Main mod class)
├── client/
│   ├── models/
│   │   └── EnhancedKasugaiCrowModel.java
│   └── renderers/
│       └── EnhancedKasugaiCrowRenderer.java
├── entities/
│   └── EnhancedKasugaiCrowEntity.java
├── mixins/
│   ├── KasugaiCrowEntityMixin.java
│   └── KasugaiCrowRendererMixin.java
└── network/
    └── SyncFlyingStatePacket.java

assets/kasugaicrowenhanced/
├── geo/
│   └── kasugai_crow_enhanced.geo.json
├── animations/
│   └── kasugai_crow_enhanced.animation.json
└── textures/
    └── entity/
        └── kasugai_crow_enhanced.png
```

---

## Phase 2: GeckoLib Model Creation

### 2.1 Design New Crow Geometry
Create `kasugai_crow_enhanced.geo.json`:
```json
{
  "format_version": "1.12.0",
  "minecraft:geometry": [
    {
      "description": {
        "identifier": "geometry.kasugai_crow_enhanced",
        "texture_width": 64,
        "texture_height": 64,
        "visible_bounds_width": 3,
        "visible_bounds_height": 2.5,
        "visible_bounds_offset": [0, 1, 0]
      },
      "bones": [
        {
          "name": "root",
          "pivot": [0, 0, 0]
        },
        {
          "name": "body",
          "parent": "root",
          "pivot": [0, 6, 0],
          "cubes": [...]
        },
        {
          "name": "head",
          "parent": "body",
          "pivot": [0, 8, -4]
        },
        {
          "name": "left_wing",
          "parent": "body",
          "pivot": [3, 7, 0]
        },
        {
          "name": "right_wing",
          "parent": "body",
          "pivot": [-3, 7, 0]
        },
        {
          "name": "tail",
          "parent": "body",
          "pivot": [0, 6, 6]
        },
        {
          "name": "left_leg",
          "parent": "body",
          "pivot": [1, 4, 2]
        },
        {
          "name": "right_leg",
          "parent": "body",
          "pivot": [-1, 4, 2]
        }
      ]
    }
  ]
}
```

### 2.2 Create Enhanced Animations
Design animations in `kasugai_crow_enhanced.animation.json`:

#### Core Animations:
1. **idle** - Ground idle state
2. **walk** - Ground walking
3. **dodge_takeoff** - Rapid ascent with wing spread
4. **flying_circles** - Circular flying pattern
5. **landing** - Descent and ground landing

```json
{
  "format_version": "1.8.0",
  "animations": {
    "idle": {
      "loop": true,
      "animation_length": 4.0,
      "bones": {
        "body": {
          "rotation": {
            "0.0": {"vector": [0, 0, 0]},
            "2.0": {"vector": [2.5, 0, 0]},
            "4.0": {"vector": [0, 0, 0]}
          }
        },
        "head": {
          "rotation": {
            "0.0": {"vector": [0, 0, 0]},
            "1.0": {"vector": [0, 15, 0]},
            "2.0": {"vector": [0, 0, 0]},
            "3.0": {"vector": [0, -15, 0]},
            "4.0": {"vector": [0, 0, 0]}
          }
        }
      }
    },

    "dodge_takeoff": {
      "animation_length": 1.0,
      "bones": {
        "body": {
          "position": {
            "0.0": {"vector": [0, 0, 0]},
            "1.0": {"vector": [0, 25, 0]}
          },
          "rotation": {
            "0.0": {"vector": [0, 0, 0]},
            "0.2": {"vector": [-10, 0, 0]},
            "1.0": {"vector": [0, 0, 0]}
          }
        },
        "left_wing": {
          "rotation": {
            "0.0": {"vector": [0, 0, 0]},
            "0.1": {"vector": [0, 0, -90]},
            "0.3": {"vector": [0, 0, -45]},
            "1.0": {"vector": [0, 0, -15]}
          }
        },
        "right_wing": {
          "rotation": {
            "0.0": {"vector": [0, 0, 0]},
            "0.1": {"vector": [0, 0, 90]},
            "0.3": {"vector": [0, 0, 45]},
            "1.0": {"vector": [0, 0, 15]}
          }
        }
      }
    },

    "flying_circles": {
      "loop": true,
      "animation_length": 2.0,
      "bones": {
        "left_wing": {
          "rotation": {
            "0.0": {"vector": [0, 0, -15]},
            "0.5": {"vector": [0, 0, -45]},
            "1.0": {"vector": [0, 0, -15]},
            "1.5": {"vector": [0, 0, -45]},
            "2.0": {"vector": [0, 0, -15]}
          }
        },
        "right_wing": {
          "rotation": {
            "0.0": {"vector": [0, 0, 15]},
            "0.5": {"vector": [0, 0, 45]},
            "1.0": {"vector": [0, 0, 15]},
            "1.5": {"vector": [0, 0, 45]},
            "2.0": {"vector": [0, 0, 15]}
          }
        }
      }
    }
  }
}
```

---

## Phase 3: Entity Behavior Implementation

### 3.1 Enhanced Entity Class
```java
public class EnhancedKasugaiCrowEntity extends MobEntity implements GeoEntity {

    // State management
    public enum CrowState {
        GROUNDED,
        DODGING,
        FLYING,
        LANDING
    }

    private CrowState currentState = CrowState.GROUNDED;
    private int flyingTimer = 0;
    private final int FLYING_DURATION = 400; // 20 seconds at 20 TPS
    private Vec3 circleCenter;
    private float circleAngle = 0f;
    private final float CIRCLE_RADIUS = 25f;

    // Dodge mechanics
    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        if (currentState == CrowState.GROUNDED) {
            // Trigger dodge instead of taking damage
            initiateFlightDodge();
            return false; // No damage taken
        } else if (currentState == CrowState.FLYING) {
            // Vulnerable while flying
            return super.hurt(damageSource, amount);
        }
        return false;
    }

    private void initiateFlightDodge() {
        currentState = CrowState.DODGING;
        circleCenter = this.position().add(0, 25, 0);
        flyingTimer = FLYING_DURATION;

        // Play dodge takeoff animation
        triggerAnim("controller.kasugai_crow", "dodge_takeoff");

        // Add upward velocity
        this.setDeltaMovement(this.getDeltaMovement().add(0, 1.5, 0));
    }

    @Override
    public void tick() {
        super.tick();

        switch (currentState) {
            case DODGING:
                handleDodgeState();
                break;
            case FLYING:
                handleFlyingState();
                break;
            case LANDING:
                handleLandingState();
                break;
        }
    }

    private void handleDodgeState() {
        // Check if reached flight altitude
        if (this.position().y >= circleCenter.y - 2) {
            currentState = CrowState.FLYING;
            triggerAnim("controller.kasugai_crow", "flying_circles");
        }
    }

    private void handleFlyingState() {
        flyingTimer--;

        // Circular flight pattern
        circleAngle += 0.1f; // Adjust speed as needed
        double x = circleCenter.x + Math.cos(circleAngle) * CIRCLE_RADIUS;
        double z = circleCenter.z + Math.sin(circleAngle) * CIRCLE_RADIUS;
        double y = circleCenter.y + Math.sin(circleAngle * 2) * 3; // Slight vertical variation

        Vec3 targetPos = new Vec3(x, y, z);
        Vec3 currentPos = this.position();
        Vec3 direction = targetPos.subtract(currentPos).normalize().scale(0.2);

        this.setDeltaMovement(direction);

        // Face movement direction
        this.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));

        if (flyingTimer <= 0) {
            currentState = CrowState.LANDING;
            triggerAnim("controller.kasugai_crow", "landing");
        }
    }

    private void handleLandingState() {
        // Gradual descent to ground
        if (this.onGround()) {
            currentState = CrowState.GROUNDED;
            triggerAnim("controller.kasugai_crow", "idle");
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.1, 0));
        }
    }

    // GeckoLib methods
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller.kasugai_crow", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<EnhancedKasugaiCrowEntity> state) {
        switch (currentState) {
            case GROUNDED:
                if (state.isMoving()) {
                    state.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
                } else {
                    state.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                }
                break;
            case FLYING:
                state.getController().setAnimation(RawAnimation.begin().then("flying_circles", Animation.LoopType.LOOP));
                break;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
```

### 3.2 GeckoLib Model & Renderer
```java
public class EnhancedKasugaiCrowModel extends GeoModel<EnhancedKasugaiCrowEntity> {
    @Override
    public ResourceLocation getModelResource(EnhancedKasugaiCrowEntity animatable) {
        return new ResourceLocation("kasugaicrowenhanced", "geo/kasugai_crow_enhanced.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EnhancedKasugaiCrowEntity animatable) {
        return new ResourceLocation("kasugaicrowenhanced", "textures/entity/kasugai_crow_enhanced.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EnhancedKasugaiCrowEntity animatable) {
        return new ResourceLocation("kasugaicrowenhanced", "animations/kasugai_crow_enhanced.animation.json");
    }
}

public class EnhancedKasugaiCrowRenderer extends GeoEntityRenderer<EnhancedKasugaiCrowEntity> {
    public EnhancedKasugaiCrowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new EnhancedKasugaiCrowModel());
        this.shadowRadius = 0.3f;
    }
}
```

---

## Phase 4: Integration Strategy

### 4.1 Mixin Approach (Recommended)
Replace original crow behavior using Mixins:

```java
@Mixin(KasugaiCrowEntity.class)
public class KasugaiCrowEntityMixin {

    @Redirect(method = "hurt", at = @At("HEAD"))
    private boolean redirectHurt(KasugaiCrowEntity instance, DamageSource damageSource, float amount) {
        // Replace with enhanced behavior
        EnhancedKasugaiCrowEntity enhanced = convertToEnhanced(instance);
        return enhanced.hurt(damageSource, amount);
    }
}

@Mixin(KasugaiCrowRenderer.class)
public class KasugaiCrowRendererMixin {

    @Redirect(method = "render", at = @At("HEAD"))
    private void redirectRender(/* parameters */) {
        // Use enhanced renderer instead
        EnhancedKasugaiCrowRenderer.render(/* parameters */);
    }
}
```

### 4.2 Entity Replacement Strategy
```java
@SubscribeEvent
public static void onEntitySpawn(EntityJoinLevelEvent event) {
    if (event.getEntity() instanceof KasugaiCrowEntity originalCrow) {
        Level world = event.getLevel();

        // Create enhanced version
        EnhancedKasugaiCrowEntity enhancedCrow = new EnhancedKasugaiCrowEntity(world);
        enhancedCrow.copyPosition(originalCrow);

        // Replace entity
        originalCrow.remove(Entity.RemovalReason.DISCARDED);
        world.addFreshEntity(enhancedCrow);
    }
}
```

---

## Phase 5: Configuration & Balancing

### 5.1 Configuration Options
Create config file for tuning:
```java
public class CrowConfig {
    public static final ForgeConfigSpec.DoubleValue FLIGHT_HEIGHT;
    public static final ForgeConfigSpec.IntValue FLIGHT_DURATION;
    public static final ForgeConfigSpec.DoubleValue CIRCLE_RADIUS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DODGE_MECHANIC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        FLIGHT_HEIGHT = builder
            .comment("Height crow flies to when dodging (blocks)")
            .defineInRange("flightHeight", 25.0, 10.0, 50.0);

        FLIGHT_DURATION = builder
            .comment("How long crow stays flying (ticks)")
            .defineInRange("flightDuration", 400, 200, 1200);

        // ... more config options
    }
}
```

### 5.2 Balance Considerations
- **Dodge cooldown**: Prevent spam dodging
- **Flight vulnerability**: Crow takes more damage while flying
- **Energy system**: Limited dodges before needing to rest
- **Player interaction**: Maybe crows land on players they trust

---

## Phase 6: Testing & Polish

### 6.1 Test Cases
1. **Damage Dodge**: Verify crow dodges all ground damage
2. **Flight Pattern**: Confirm circular flying behavior
3. **Landing**: Ensure proper return to ground
4. **Animation Sync**: Check animation timing
5. **Multiplayer**: Test client-server synchronization

### 6.2 Performance Optimization
- Limit number of simultaneously flying crows
- Optimize pathfinding during flight
- Reduce particle effects if needed
- Cache animation states

### 6.3 Compatibility
- Test with other Kimetsu no Yaiba mod features
- Verify no conflicts with existing crow behaviors
- Check texture compatibility

---

## Phase 7: Deployment

### 7.1 Build Configuration
```gradle
dependencies {
    implementation 'software.bernie.geckolib:geckolib-forge-1.20.1:4.4.9'
    compileOnly 'net.kimetsu.mod:kimetsunoyaiba:2.+'
}
```

### 7.2 Installation Instructions
1. Install required dependencies (GeckoLib, Kimetsu no Yaiba mod)
2. Drop enhanced crow mod into mods folder
3. Configure settings via config file
4. Test in creative world first

---

## Potential Challenges & Solutions

### Technical Challenges
1. **Mixin Conflicts**: Use specific target selectors
2. **Animation Timing**: Implement state machine carefully
3. **Client-Server Sync**: Use custom packets for state sync
4. **Performance**: Limit concurrent flying entities

### Design Challenges
1. **Balance**: Make flying state vulnerable but defensive
2. **AI Behavior**: Smooth transitions between states
3. **Visual Polish**: Ensure animations look natural
4. **User Experience**: Clear visual indicators of crow state

---

## Future Enhancements
1. **Advanced AI**: Crows remember threat locations
2. **Flock Behavior**: Multiple crows coordinate dodges
3. **Environmental Interaction**: Use terrain for evasion
4. **Player Bonding**: Tamed crows have different behaviors
5. **Sound Effects**: Custom audio for each animation state

This plan provides a comprehensive roadmap for creating a sophisticated flying dodge mechanic while converting the crow to use modern GeckoLib animations.