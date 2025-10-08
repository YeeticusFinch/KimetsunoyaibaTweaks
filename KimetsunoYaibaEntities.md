# Kimetsunoyaiba Mod - Entity System Architecture

This document explains how entities and breathing techniques work in the Kimetsunoyaiba mod (decompiled from ver2-forge-1.20.1). This serves as a reference guide for creating addon mods that extend or integrate with the Kimetsunoyaiba mod.

## Table of Contents
1. [Entity Architecture](#entity-architecture)
2. [Rendering System](#rendering-system)
   - [GeckoLib Armor Rendering](#geckolib-armor-rendering)
   - [Held Item Rendering](#held-item-rendering)
3. [Animation System](#animation-system)
4. [Breathing Technique System](#breathing-technique-system)
5. [AI and Procedure System](#ai-and-procedure-system)
6. [Equipment and Swords](#equipment-and-swords)
7. [How Abilities are Triggered](#how-abilities-are-triggered)
8. [Creating Custom Entities](#creating-custom-entities)

---

## Entity Architecture

### Base Entity Structure

All humanoid slayer entities in the Kimetsunoyaiba mod follow this pattern:

**Class**: `net.mcreator.kimetsunoyaiba.entity.KanaeEntity` (example)

```java
public class KanaeEntity extends Monster implements GeoEntity
```

**Key Components**:
1. **Extends `Monster`** - Uses vanilla Minecraft's Monster class as base
2. **Implements `GeoEntity`** - Enables GeckoLib animations and custom models

### Entity Data Accessors

Entities use synchronized entity data for network communication:

```java
public static final EntityDataAccessor<Boolean> SHOOT;
public static final EntityDataAccessor<String> ANIMATION;
public static final EntityDataAccessor<String> TEXTURE;
```

- `SHOOT`: Controls ranged attack behavior
- `ANIMATION`: Stores the current animation name for synchronization
- `TEXTURE`: Allows runtime texture switching (e.g., "kanae")

### Animation System Fields

```java
private final AnimatableInstanceCache cache;
private boolean swinging;
private boolean lastloop;
private long lastSwing;
public String animationprocedure;  // CRITICAL: This triggers animations
String prevAnim;
```

**`animationprocedure` is the key field** - When set to an animation name, the entity will play that animation. Setting it to "empty" stops the animation.

### Constructor Pattern

```java
public KanaeEntity(EntityType<KanaeEntity> type, Level world) {
    super(type, world);
    this.cache = GeckoLibUtil.createInstanceCache(this);
    this.animationprocedure = "empty";
    this.prevAnim = "empty";

    // Equipment setup - CRITICAL: Entities are equipped in constructor
    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(KimetsunoyaibaModItems.NICHIRINSWORD_KANAE.get()));
    this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(KimetsunoyaibaModItems.CLOTHES_KOCHO_CHESTPLATE.get()));
    this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(KimetsunoyaibaModItems.UNIFORM_LEGGINGS.get()));
    this.setItemSlot(EquipmentSlot.FEET, new ItemStack(KimetsunoyaibaModItems.UNIFORM_KOCHO_BOOTS.get()));
}
```

**Important**: Equipment is set in the constructor, not dynamically. Each entity has a pre-defined sword and armor set.

### AI Goals Registration

Entities register their AI goals in `registerGoals()`:

```java
protected void registerGoals() {
    super.registerGoals();

    // Combat goal with custom targeting
    this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
        protected double getAttackReachSqr(LivingEntity entity) {
            return 0.0; // Melee range
        }

        public boolean canUse() {
            return super.canUse() && TestTargetingProcedure.execute(entity);
        }
    });

    // Hurt by target goal
    this.targetSelector.addGoal(2, new HurtByTargetGoal(this));

    // Target demon entities (goals 3-40)
    this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, DomaEntity.class, false, false) {
        public boolean canUse() {
            return super.canUse() && TestTargeting3Procedure.execute();
        }
    });

    // ... many more demon targeting goals ...

    // Target players conditionally (goal 41)
    this.targetSelector.addGoal(41, new NearestAttackableTargetGoal(this, Player.class, false, false) {
        public boolean canUse() {
            return super.canUse() && TestTargeting2Procedure.execute(world, entity);
        }
    });

    // Basic movement goals (43-47)
    this.goalSelector.addGoal(43, new RandomStrollGoal(this, 1.0));
    this.goalSelector.addGoal(44, new RandomLookAroundGoal(this));
    this.goalSelector.addGoal(45, new FloatGoal(this));
    this.goalSelector.addGoal(46, new MoveBackToVillageGoal(this, 0.6, false));
    this.goalSelector.addGoal(47, new OpenDoorGoal(this, true));
}
```

**Pattern**: Slayer entities target demons primarily, and only attack players if certain conditions are met (faction system, demon transformation, etc.)

---

## Rendering System

### Entity Renderer Structure

All humanoid entities use a GeckoLib-based renderer with multiple render layers:

**Class**: `net.mcreator.kimetsunoyaiba.client.renderer.KanaeRenderer` (example)

```java
public class KanaeRenderer extends DynamicGeoEntityRenderer<KanaeEntity> {
    public KanaeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new KanaeModel());

        // Set shadow size
        this.shadowRadius = 0.5f;

        // Add armor rendering layer
        this.addRenderLayer(new GenericArmorLayer(this));

        // Add held item rendering layer
        this.addRenderLayer(new GenericItemLayer(this));
    }

    @Override
    public RenderType getRenderType(KanaeEntity animatable, ResourceLocation texture,
                                     MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(this.getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, KanaeEntity entity, BakedGeoModel model,
                         MultiBufferSource bufferSource, VertexConsumer buffer,
                         boolean isReRender, float partialTick, int packedLight,
                         int packedOverlay, float red, float green, float blue, float alpha) {
        float scale = 1.0f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                       partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    protected float getDeathMaxRotation(KanaeEntity entityLivingBaseIn) {
        return 0.0f; // No rotation on death
    }
}
```

**Key Renderer Components**:
1. **Extends `DynamicGeoEntityRenderer`** - GeckoLib's entity renderer for animated entities
2. **GenericArmorLayer** - Renders vanilla armor on GeckoLib bones
3. **GenericItemLayer** - Renders held items (swords, shields) on GeckoLib bones

### GeckoLib Armor Rendering

The Kimetsunoyaiba mod uses GeckoLib's built-in armor rendering system via `ItemArmorGeoLayer`.

**Class**: `net.mcreator.kimetsunoyaiba.GenericArmorLayer`

```java
public class GenericArmorLayer<T extends LivingEntity> extends ItemArmorGeoLayer<T> {
    public GenericArmorLayer(DynamicGeoEntityRenderer<T> geoRenderer) {
        super(geoRenderer);
    }

    /**
     * Maps GeckoLib bone names to armor pieces
     * Returns which armor ItemStack should be rendered on this bone
     */
    @Nullable
    @Override
    protected ItemStack getArmorItemForBone(GeoBone bone, T animatable) {
        String boneName = bone.getName();

        switch (boneName) {
            // Boots - rendered on boot bones
            case "LeftLeg2":
            case "RightLeg2":
            case "armorLeftBoot":
            case "armorRightBoot":
                return this.bootsStack; // Provided by ItemArmorGeoLayer

            // Leggings - rendered on leg bones
            case "LeftLeg":
            case "RightLeg":
            case "armorLeftLeg":
            case "armorRightLeg":
                return this.leggingsStack;

            // Chestplate - rendered on body and arm bones
            case "Body":
            case "LeftArm":
            case "RightArm":
            case "armorBody":
            case "armorLeftArm":
            case "armorRightArm":
                return this.chestplateStack;

            // Helmet - rendered on head bone
            case "Head":
            case "hat":
            case "armorHead":
                return this.helmetStack;

            default:
                return null; // Don't render armor on this bone
        }
    }

    /**
     * Maps GeckoLib bone names to equipment slots
     * Tells GeckoLib which equipment slot this bone represents
     */
    @Nonnull
    @Override
    protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, T animatable) {
        String boneName = bone.getName();

        switch (boneName) {
            case "LeftLeg2":
            case "RightLeg2":
            case "armorLeftBoot":
            case "armorRightBoot":
                return EquipmentSlot.FEET;

            case "LeftLeg":
            case "armorLeftLeg":
                return EquipmentSlot.LEGS;

            case "Body":
            case "armorBody":
                return EquipmentSlot.CHEST;

            case "Head":
            case "armorHead":
                return EquipmentSlot.HEAD;

            default:
                return super.getEquipmentSlotForBone(bone, stack, animatable);
        }
    }

    /**
     * Maps GeckoLib bones to vanilla HumanoidModel parts
     * This allows vanilla armor models to be positioned correctly
     */
    @Nonnull
    @Override
    protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot,
                                           ItemStack stack, T animatable,
                                           HumanoidModel<?> baseModel) {
        String boneName = bone.getName();

        switch (boneName) {
            case "LeftLeg":
            case "LeftLeg2":
            case "armorLeftLeg":
            case "armorLeftBoot":
                return baseModel.leftLeg; // Vanilla model part

            case "RightLeg":
            case "RightLeg2":
            case "armorRightLeg":
            case "armorRightBoot":
                return baseModel.rightLeg;

            case "RightArm":
            case "armorRightArm":
                return baseModel.rightArm;

            case "LeftArm":
            case "armorLeftArm":
                return baseModel.leftArm;

            case "Body":
            case "armorBody":
                return baseModel.body;

            case "Head":
            case "hat":
            case "armorHead":
                return baseModel.head;

            default:
                return super.getModelPartForBone(bone, slot, stack, animatable, baseModel);
        }
    }
}
```

**How Armor Rendering Works**:

1. **Entity wears armor** via `setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE))`
2. **GenericArmorLayer is added** to the entity renderer
3. **For each GeckoLib bone**, `getArmorItemForBone()` is called
4. **If bone should have armor**, return the appropriate armor ItemStack
5. **GeckoLib automatically**:
   - Gets the vanilla armor model for that ItemStack
   - Maps it to the correct HumanoidModel part via `getModelPartForBone()`
   - Renders the 3D armor model attached to the GeckoLib bone
   - Applies armor texture and color (for leather armor)

**Bone Naming Convention**:
- The mod supports both vanilla bone names (`Head`, `Body`, `LeftArm`, etc.)
- AND custom armor bone names (`armorHead`, `armorBody`, `armorLeftArm`, etc.)
- This allows compatibility with different GeckoLib model structures

**Key Advantages**:
- ✅ Works with ANY vanilla armor (diamond, iron, netherite, etc.)
- ✅ Works with modded armor that extends ArmorItem
- ✅ Armor automatically follows bone animations
- ✅ No custom armor models needed - uses vanilla armor models
- ✅ Supports armor enchantment glint
- ✅ Supports leather armor dyeing

### Held Item Rendering

The Kimetsunoyaiba mod renders items held in main hand and offhand using `BlockAndItemGeoLayer`.

**Class**: `net.mcreator.kimetsunoyaiba.GenericItemLayer`

```java
public class GenericItemLayer<T extends LivingEntity> extends BlockAndItemGeoLayer<T> {
    private final DynamicGeoEntityRenderer<T> renderer;

    public GenericItemLayer(DynamicGeoEntityRenderer<T> renderer) {
        super(renderer);
        this.renderer = renderer;
    }

    /**
     * Maps GeckoLib bone names to held items
     * Returns which ItemStack should be rendered on this bone
     */
    @Nullable
    @Override
    protected ItemStack getStackForBone(GeoBone bone, T animatable) {
        ItemStack mainHandItem = animatable.getMainHandItem();
        ItemStack offhandItem = animatable.getOffhandItem();
        ItemStack headItem = animatable.getItemBySlot(EquipmentSlot.HEAD);

        // Special handling: Don't render head item if it's an armor piece
        // (armor is handled by GenericArmorLayer instead)
        Item item = headItem.getItem();
        if (item instanceof ArmorItem armorItem) {
            if (armorItem.getEquipmentSlot().getName().equals("head")) {
                headItem = ItemStack.EMPTY;
            }
        }

        String boneName = bone.getName();

        switch (boneName) {
            // Main hand - primary weapon position
            case "itemMainHand":
                return mainHandItem;

            // Offhand - shield position
            case "itemOffHand":
                return offhandItem;

            // Alternative main hand positions (for dual wielding animations)
            case "itemMainHand2":
            case "itemMainHand3":
                return mainHandItem;

            // Alternative offhand positions
            case "itemOffHand2":
            case "itemOffHand3":
                return offhandItem;

            // Head item (non-armor, like pumpkins)
            case "Head":
            case "armorHead":
                return headItem;

            default:
                return null; // Don't render item on this bone
        }
    }

    /**
     * Determines how the item should be rendered (perspective)
     */
    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
        String boneName = bone.getName();

        switch (boneName) {
            case "itemOffHand":
            case "itemMainHand":
            case "itemOffHand2":
            case "itemOffHand3":
            case "itemMainHand2":
            case "itemMainHand3":
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

            case "Head":
            case "armorHead":
                return ItemDisplayContext.HEAD;

            default:
                return ItemDisplayContext.NONE;
        }
    }

    /**
     * Custom rendering logic for items on bones
     * Applies transformations (rotation, scale, position)
     */
    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                      T animatable, MultiBufferSource bufferSource,
                                      float partialTick, int packedLight, int packedOverlay) {
        ItemStack mainHandItem = animatable.getMainHandItem();
        ItemStack offhandItem = animatable.getOffhandItem();
        ItemStack headItem = animatable.getItemBySlot(EquipmentSlot.HEAD);

        float scaleFactor = 1.0f;

        // Transform held items (swords, tools, etc.)
        if (stack == mainHandItem || stack == offhandItem) {
            poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f)); // Rotate to proper angle

            // Special handling for shields
            if (stack.getItem() instanceof ShieldItem && stack == offhandItem) {
                poseStack.translate(0.0f, -0.25f, 0.0f); // Move shield position
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f)); // Flip shield
            }
        }
        // Transform head items
        else if (stack == headItem) {
            scaleFactor = 0.625f; // Smaller scale for head items
            poseStack.translate(0.0f, 0.25f, 0.0f); // Move up slightly
            poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
        }

        // Call parent to actually render the item
        super.renderStackForBone(poseStack, bone, stack, animatable, bufferSource,
                                 partialTick, packedLight, packedOverlay);
    }
}
```

**How Held Item Rendering Works**:

1. **Entity holds items** via `setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD))`
2. **GenericItemLayer is added** to the entity renderer
3. **For each GeckoLib bone**, `getStackForBone()` is called
4. **If bone should hold an item**, return the appropriate ItemStack
5. **GeckoLib automatically**:
   - Renders the item as a 3D model
   - Applies custom transformations (rotation, scale, position)
   - Attaches item to the bone so it follows animations
   - Supports item enchantment glint

**Bone Naming Convention for Items**:
- `itemMainHand` - Primary main hand position
- `itemOffHand` - Primary offhand position
- `itemMainHand2`, `itemMainHand3` - Alternative positions for dual wielding
- `itemOffHand2`, `itemOffHand3` - Alternative offhand positions

**Supported Items**:
- ✅ Swords and tools (nichirin swords, diamond swords, etc.)
- ✅ Shields (with special rotation handling)
- ✅ Blocks (rendered as item in hand)
- ✅ Any vanilla or modded item

**Custom Transformations**:
- **Swords**: Rotated -90° on X axis for proper orientation
- **Shields**: Translated and rotated 180° for correct position
- **Head items**: Scaled down to 62.5% and moved up 0.25 blocks

### Renderer Registration Pattern

When adding rendering to your custom entity:

```java
public class MyCustomRenderer extends DynamicGeoEntityRenderer<MyCustomEntity> {
    public MyCustomRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MyCustomModel());

        // IMPORTANT: Add layers in this order
        this.addRenderLayer(new GenericArmorLayer(this));  // First: armor
        this.addRenderLayer(new GenericItemLayer(this));   // Second: items

        // Optional: Add other layers (glow, eyes, etc.)
    }
}
```

**Layer Order Matters**: Armor should be added before items to ensure correct rendering order.

---

## Animation System

### GeckoLib Animation Controllers

Entities use two animation controllers:

```java
public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    data.add(new AnimationController(this, "movement", 1, this::movementPredicate));
    data.add(new AnimationController(this, "procedure", 1, this::procedurePredicate));
}
```

#### 1. Movement Controller

Handles automatic animations based on entity state:

```java
private PlayState movementPredicate(AnimationState event) {
    if (!this.animationprocedure.equals("empty")) {
        return PlayState.STOP; // Don't play movement anims during ability animations
    }

    if (event.isMoving() && !isSprinting() && !isSwimming()) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
    }

    if (isDead()) {
        return event.setAndContinue(RawAnimation.begin().thenPlay("death"));
    }

    if (isBlocking()) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("guard"));
    }

    if (isSwimming() || (isSprinting() && event.isMoving())) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("sprint"));
    }

    return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
}
```

#### 2. Procedure Controller

Handles ability animations triggered by `animationprocedure` field:

```java
private PlayState procedurePredicate(AnimationState event) {
    // Check if we need to start or change animation
    if ((!this.animationprocedure.equals("empty") &&
         event.getController().getAnimationState() == AnimationController.State.STOPPED) ||
        (!this.animationprocedure.equals(this.prevAnim) &&
         !this.animationprocedure.equals("empty"))) {

        // Force reset if changing animation
        if (!this.animationprocedure.equals(this.prevAnim)) {
            event.getController().forceAnimationReset();
        }

        // Play the animation
        event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));

        // Auto-clear animation when done (for non-looping animations)
        if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
            this.animationprocedure = "empty";
            event.getController().forceAnimationReset();
        }
    }
    else if (this.animationprocedure.equals("empty")) {
        this.prevAnim = "empty";
        return PlayState.STOP;
    }

    this.prevAnim = this.animationprocedure;
    return PlayState.CONTINUE;
}
```

**How to Trigger Animations**:
```java
entity.animationprocedure = "hana_2"; // Start animation
// Animation will play and automatically reset to "empty" when done
```

---

## Breathing Technique System

### How Breathing Techniques Work

**CRITICAL FINDING**: Breathing techniques are **NOT** in the sword items. They are in **procedure files** that are called by entity AI.

### Procedure-Based System

Each breathing technique is a separate procedure class:

**Example**: `net.mcreator.kimetsunoyaiba.procedures.BreathesHana2Procedure`

```java
public class BreathesHana2Procedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        // Uses NBT data "cnt1" as a counter
        double cnt1 = entity.getPersistentData().getDouble("cnt1");

        // Animation trigger
        if (cnt1 == 0.0) {
            if (entity instanceof KanaeEntity kanae) {
                kanae.animationprocedure = "hana_2"; // Trigger animation
            }
        }

        // Duration check
        if (cnt1 < 10.0) { // Technique lasts 10 ticks (0.5 seconds)
            // Modify entity velocity
            entity.setDeltaMovement(new Vec3(vx, vy, vz));

            // Spawn particles (on server side)
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    x, y, z,
                    count, 0, 0, 0, 0
                );
            }

            // Apply damage to nearby entities
            AABB hitbox = entity.getBoundingBox().inflate(range);
            List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class, hitbox, e -> e != entity
            );
            for (LivingEntity target : targets) {
                target.hurt(world.damageSources().mob(entity), damage);
            }

            // Increment counter
            entity.getPersistentData().putDouble("cnt1", cnt1 + 1);
        } else {
            // Reset counter when done
            entity.getPersistentData().putDouble("cnt1", 0.0);
        }
    }
}
```

**Pattern**: All breathing technique procedures follow this structure:
1. Use NBT counter (`cnt1`, `cnt2`, etc.) for timing
2. Set `animationprocedure` field to trigger animation
3. Apply effects (velocity, particles, damage) over multiple ticks
4. Reset counter when complete

---

## AI and Procedure System

### AI Procedure Pattern

Each entity has an AI procedure that is called during the entity's tick update:

**Example**: `net.mcreator.kimetsunoyaiba.procedures.AIkanawoProcedure`

```java
public class AIkanawoProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        if (entity.isAlive()) {
            // Call common procedures
            ActiveHashiraProcedure.execute(world, x, y, z, entity);
            BahuKamabokoProcedure.execute(world, x, y, z, entity);

            // Mode-based breathing technique selection
            double mode = entity.getPersistentData().getDouble("mode");

            if (mode == 1.0) {
                BreathesHana2Procedure.execute(world, x, y, z, entity);
            }
            else if (mode == 2.0) {
                BreathesHana3Procedure.execute(world, x, y, z, entity);
            }
            else if (mode == 3.0) {
                BreathesHana4Procedure.execute(world, x, y, z, entity);
            }
            else if (mode == 4.0) {
                BreathesHana5Procedure.execute(world, x, y, z, entity);
            }
            // ... more modes ...

            // Target-based logic
            if (entity instanceof Mob mob && mob.getTarget() instanceof LivingEntity target) {
                double distance = entity.distanceTo(target);

                if (distance < 10.0) {
                    // Close range - use specific technique
                    entity.getPersistentData().putDouble("mode", 1.0);
                }
                else if (distance < 20.0) {
                    // Medium range - use different technique
                    entity.getPersistentData().putDouble("mode", 2.0);
                }
                // ... distance-based technique selection ...
            }
        }
    }
}
```

### How AI Procedures are Called

In the entity's `tick()` method:

```java
public void tick() {
    super.tick();
    AIkanawoProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
    this.refreshDimensions();
}
```

**Pattern**:
- AI procedure is called every tick
- Uses NBT `mode` value to select which breathing technique to use
- Techniques are selected based on combat conditions (distance to target, health, etc.)

---

## Equipment and Swords

### Sword Items

**Class**: `net.mcreator.kimetsunoyaiba.item.NichirinswordKanaeItem`

```java
public class NichirinswordKanaeItem extends SwordItem {
    public NichirinswordKanaeItem() {
        super(new CustomTier(), 3, -2.4f, new Item.Properties());
    }

    // Called when sword hits an entity
    public boolean hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity attacker) {
        boolean retval = super.hurtEnemy(itemstack, entity, attacker);
        ActiveRedSwordProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, itemstack);
        return retval;
    }

    // Called when right-clicking
    public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
        InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
        StartBreathesProcedure.execute(world, entity, ar.getObject());
        return ar;
    }

    // Called when swinging sword
    public boolean onEntitySwing(ItemStack itemstack, LivingEntity entity) {
        boolean retval = super.onEntitySwing(itemstack, entity);
        SwingKaminariProcedure.execute(entity);
        return retval;
    }

    // Called every tick when sword is in inventory
    public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        if (selected) {
            ChangingBreathesProcedure.execute(entity, itemstack);
        }
    }
}
```

**Sword Purpose**: Swords trigger procedures when used, but the actual breathing techniques are hardcoded in entity AI procedures, not in sword items.

---

## How Abilities are Triggered

### For Entities (NPCs)

**Flow**:
1. Entity is spawned with a specific sword equipped (in constructor)
2. Every tick, the entity's AI procedure is called
3. AI procedure checks combat conditions (target distance, health, etc.)
4. AI procedure sets `mode` NBT value based on conditions
5. Based on `mode` value, specific breathing technique procedure is called
6. Breathing technique procedure sets `animationprocedure` field to trigger animation
7. Breathing technique applies effects (particles, damage, velocity)

**Example Code Flow**:
```
KanaeEntity.tick()
  → AIkanawoProcedure.execute()
    → if (mode == 1.0) BreathesHana2Procedure.execute()
      → entity.animationprocedure = "hana_2"
      → Apply effects (particles, damage, movement)
```

**NOT done through swords**: The sword is just equipment. Abilities are hardcoded in AI procedures.

### For Players

**Flow** (different from entities):
1. Player equips a nichirin sword
2. Player right-clicks or swings sword
3. Sword's `use()` or `onEntitySwing()` method calls a procedure
4. Procedure (like `StartBreathesProcedure`) checks player's breathing style
5. Based on player's breathing style NBT data, appropriate technique is triggered

**Example**:
```
Player right-clicks with NichirinswordKanaeItem
  → NichirinswordKanaeItem.use()
    → StartBreathesProcedure.execute()
      → Check player NBT "breath_style"
      → If breath_style == "flower", call BreathesHana1Procedure
        → Apply effects to player
```

---

## Creating Custom Entities

### Step-by-Step Guide

#### 1. Create Entity Class

```java
public class MyCustomSlayerEntity extends Monster implements GeoEntity {
    public static final EntityDataAccessor<String> ANIMATION;
    public static final EntityDataAccessor<String> TEXTURE;
    private final AnimatableInstanceCache cache;
    public String animationprocedure;
    String prevAnim;

    public MyCustomSlayerEntity(EntityType<MyCustomSlayerEntity> type, Level world) {
        super(type, world);
        this.cache = GeckoLibUtil.createInstanceCache(this);
        this.animationprocedure = "empty";
        this.prevAnim = "empty";

        // Equip with custom sword and armor
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(MyModItems.MY_NICHIRIN_SWORD.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
    }

    // Implement GeckoLib methods...
}
```

#### 2. Create AI Procedure

```java
public class AIMySlayerProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        if (entity.isAlive()) {
            double mode = entity.getPersistentData().getDouble("mode");

            // Select technique based on mode
            if (mode == 1.0) {
                MyBreathingForm1Procedure.execute(world, x, y, z, entity);
            }
            else if (mode == 2.0) {
                MyBreathingForm2Procedure.execute(world, x, y, z, entity);
            }

            // Set mode based on combat conditions
            if (entity instanceof Mob mob && mob.getTarget() != null) {
                double distance = entity.distanceTo(mob.getTarget());

                if (distance < 10.0) {
                    entity.getPersistentData().putDouble("mode", 1.0);
                } else {
                    entity.getPersistentData().putDouble("mode", 2.0);
                }
            }
        }
    }
}
```

#### 3. Create Breathing Technique Procedures

```java
public class MyBreathingForm1Procedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null) return;

        double cnt1 = entity.getPersistentData().getDouble("cnt1");

        if (cnt1 == 0.0) {
            // Trigger animation
            if (entity instanceof MyCustomSlayerEntity slayer) {
                slayer.animationprocedure = "my_form_1_animation";
            }
        }

        if (cnt1 < 20.0) { // 1 second duration
            // Apply effects
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    x, y + 1, z,
                    10, 0.5, 0.5, 0.5, 0.1
                );
            }

            // Damage nearby entities
            AABB hitbox = entity.getBoundingBox().inflate(3.0);
            List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class, hitbox, e -> e != entity && e.isAlive()
            );
            for (LivingEntity target : targets) {
                target.hurt(world.damageSources().mob(entity), 8.0f);
            }

            entity.getPersistentData().putDouble("cnt1", cnt1 + 1);
        } else {
            entity.getPersistentData().putDouble("cnt1", 0.0);
        }
    }
}
```

#### 4. Wire Up Entity Tick

```java
@Override
public void tick() {
    super.tick();
    AIMySlayerProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
    this.refreshDimensions();
}
```

#### 5. Register Animation Controllers

```java
@Override
public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    data.add(new AnimationController(this, "movement", 1, this::movementPredicate));
    data.add(new AnimationController(this, "procedure", 1, this::procedurePredicate));
}

// Copy the movementPredicate and procedurePredicate methods from KanaeEntity
```

### Key Patterns to Follow

1. **Animation Triggering**: Always set `animationprocedure` field, never call animation methods directly
2. **NBT Counters**: Use `cnt1`, `cnt2` etc. for timing multi-tick abilities
3. **Mode Selection**: Use `mode` NBT value to select which technique to use
4. **Server-Side Particles**: Always check `if (world instanceof ServerLevel)` before spawning particles
5. **Equipment in Constructor**: Set all equipment in entity constructor, not dynamically
6. **Procedure-Based**: Create separate procedure classes for each technique, don't put logic in entity class

---

## Summary

### Key Architectural Decisions

1. **Entities are hardcoded** - Each entity has specific equipment set in constructor
2. **Breathing techniques are procedures** - NOT in swords, but in separate procedure classes
3. **AI drives abilities** - Entity AI procedures select and trigger breathing techniques based on combat state
4. **NBT-based state machine** - Uses `mode` for technique selection, `cnt1`/`cnt2` for timing
5. **Animation via field** - Setting `animationprocedure` string triggers animations
6. **GeckoLib for all rendering** - Uses `biped.geo.json` model for humanoid entities

### For Addon Developers

To create entities compatible with Kimetsunoyaiba mod:

1. ✅ Extend `Monster` and implement `GeoEntity`
2. ✅ Use `biped.geo.json` model structure
3. ✅ Equip entities with nichirin swords in constructor
4. ✅ Create AI procedures that call breathing technique procedures
5. ✅ Use NBT data (`mode`, `cnt1`, etc.) for state management
6. ✅ Trigger animations via `animationprocedure` field
7. ✅ Follow the two-controller pattern (movement + procedure)

### What Swords Do vs Don't Do

**Swords DO**:
- Provide stats (attack damage, durability)
- Trigger procedures when used/swung (for players)
- Display particles on attack
- Get equipped on entities

**Swords DON'T**:
- Contain breathing technique logic
- Determine which abilities entity can use
- Trigger entity abilities (entities use hardcoded AI procedures)

**The breathing techniques are in the entity's AI, not in the sword.**
