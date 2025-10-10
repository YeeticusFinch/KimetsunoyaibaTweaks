# Critical Bug Prevention

## log4j LinkageError Crashes

### Problem
Persistent crashes with:
```
java.lang.LinkageError: loader constraint violation: loader 'MC-BOOTSTRAP' wants to load interface org.apache.logging.log4j.util.MessageSupplier
```

### Root Causes

1. **Infinite Event Recursion**
   - `onLivingAttack` calling `entity.hurt()` triggers more `LivingAttackEvent`
   - Creates infinite loop until exception/stack overflow

2. **log4j ClassLoader Conflicts**
   - Multiple mods bundle their own log4j classes
   - Creates classloader conflict with multiple `MessageSupplier` versions
   - MC-BOOTSTRAP can't resolve which version to use

3. **Uncaught Exceptions in Event Handlers**
   - Exceptions bubble up to EventBus.handleException()
   - EventBus tries to log using log4j → LinkageError
   - Original exception masked by logging error

### Solutions

#### 1. Prevent Event Recursion (CRITICAL)

Use `ThreadLocal<Boolean>` flag:

```java
private static final ThreadLocal<Boolean> IS_PROCESSING_AOE = ThreadLocal.withInitial(() -> false);

@SubscribeEvent
public void onLivingAttack(LivingAttackEvent event) {
    try {
        if (IS_PROCESSING_AOE.get()) return; // Prevent recursion

        // ... existing code ...

        IS_PROCESSING_AOE.set(true);
        try {
            for (LivingEntity entity : nearbyEntities) {
                entity.hurt(attacker.level().damageSources().playerAttack(attacker), damage);
            }
        } finally {
            IS_PROCESSING_AOE.set(false);
        }
    } catch (Exception e) {
        // Always catch
    }
}
```

**Why ThreadLocal?** Multiple threads may process events simultaneously - each needs its own flag.

#### 2. Disable log4j in Custom Logging

Replace log4j with System.out:

```java
public static void debug(String message, Object... args) {
    if (Config.logDebug)
        //LOGGER.debug(message, args);  // DISABLED - causes LinkageError
        System.out.println("[DEBUG] " + format(message, args));
}
```

#### 3. Exclude log4j from Dependencies

In `build.gradle`:

```gradle
implementation(fg.deobf("dependency:artifact:version")) {
    exclude group: 'org.apache.logging.log4j', module: 'log4j-api'
    exclude group: 'org.apache.logging.log4j', module: 'log4j-core'
    exclude group: 'org.slf4j', module: 'slf4j-api'
}
```

#### 4. Comprehensive Try-Catch

Wrap ALL event handlers:

```java
@SubscribeEvent
public static void onEvent(SomeEvent event) {
    try {
        // Event handler logic
    } catch (Exception e) {
        // Use System.err, NOT Log.debug()
        if (Config.logDebug) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

### Prevention Rules

#### Rule 1: Never Call hurt() in LivingAttackEvent Without Protection

**BAD:**
```java
@SubscribeEvent
public void onLivingAttack(LivingAttackEvent event) {
    entity.hurt(damageSource, damage); // Infinite recursion!
}
```

**GOOD:**
```java
@SubscribeEvent
public void onLivingAttack(LivingAttackEvent event) {
    if (IS_PROCESSING_FLAG.get()) return;

    IS_PROCESSING_FLAG.set(true);
    try {
        entity.hurt(damageSource, damage);
    } finally {
        IS_PROCESSING_FLAG.set(false);
    }
}
```

#### Rule 2: Always Wrap Event Handlers in Try-Catch

**BAD:**
```java
@SubscribeEvent
public void onSomeEvent(SomeEvent event) {
    riskyOperation(); // Exception escapes to EventBus
}
```

**GOOD:**
```java
@SubscribeEvent
public void onSomeEvent(SomeEvent event) {
    try {
        riskyOperation();
    } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
    }
}
```

#### Rule 3: Never Use log4j in Exception Handlers

**BAD:**
```java
catch (Exception e) {
    Log.error("Error", e); // Can trigger LinkageError!
}
```

**GOOD:**
```java
catch (Exception e) {
    System.err.println("Error: " + e.getMessage());
    e.printStackTrace();
}
```

#### Rule 4: Exclude log4j from External Dependencies

Always add exclusions when adding dependencies.

#### Rule 5: Watch for Event Chains

Common recursion chains:
- `LivingAttackEvent` → `hurt()` → `LivingAttackEvent`
- `LivingHurtEvent` → `hurt()` → `LivingHurtEvent`
- `PlayerInteractEvent` → `useOn()` → `PlayerInteractEvent`
- `BlockEvent.BreakEvent` → `destroyBlock()` → `BlockEvent.BreakEvent`

**Always** use flags or early returns to prevent infinite loops.

### Debugging LinkageError

1. **Check stack trace** for actual source (before LinkageError)
2. **Look for event recursion** patterns
3. **Add recursion protection** with ThreadLocal flags
4. **Replace log4j calls** with System.out/err
5. **Test incrementally** after each fix

### Version History
- **v1.5.13** - ShoulderSurfing integration (caused crashes)
- **v1.5.14** - Added try-catch blocks
- **v1.5.15** - More granular try-catch
- **v1.5.16** - Disabled ShoulderSurfing
- **v1.5.17** - Disabled log4j in Log class
- **v1.5.18** - **FINAL FIX**: Event recursion protection
