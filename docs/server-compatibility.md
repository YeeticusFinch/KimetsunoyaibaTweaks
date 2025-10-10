# Dedicated Server Compatibility

## Problem Overview

Minecraft Forge mods must work on both **physical clients** and **dedicated servers**.

Dedicated servers do NOT have access to client-only classes:
- `net.minecraft.client.Minecraft`
- `net.minecraft.client.multiplayer.ClientLevel`
- `net.minecraft.client.player.LocalPlayer`
- `net.minecraft.client.gui.screens.Screen`
- etc.

Loading these classes on server crashes with:
```
java.lang.RuntimeException: Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER
```

Enforced by Forge's **RuntimeDistCleaner**.

## Root Causes

1. **Static Field Initialization** - `DistExecutor.safeRunForDist()` loads classes at compile time
2. **Import Statements** - ANY import loads the class, even if never used
3. **Client Classes in Non-Client Packages** - Classes outside `client` package not protected
4. **Wrong Command Registration** - Client commands on server-side event
5. **Network Packet Handlers** - Direct client code access without DistExecutor

## Solutions

### 1. Remove Static CLIENT_PROXY Field

**BAD:**
```java
public static final IClientProxy CLIENT_PROXY = DistExecutor.safeRunForDist(
    () -> ClientProxy::new,
    () -> ServerProxy::new
); // Loads ClientProxy on server!
```

**GOOD:**
```java
// Remove entirely - use DistExecutor.unsafeRunWhenOn() at call sites
```

### 2. Remove Client Imports from Server-Accessible Classes

**BAD:**
```java
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
```

**GOOD:**
```java
// No client imports in server-accessible classes
// Use fully qualified names inside DistExecutor blocks
```

### 3. Move Client-Only Classes to Client Package

**CRITICAL**: Classes using client classes MUST be in `client` package.

**BAD Structure:**
```
src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/
├── particles/
│   ├── BonePositionTracker.java      # Uses Minecraft - WRONG!
│   └── SwordParticleHandler.java     # Uses ClientLevel - WRONG!
```

**GOOD Structure:**
```
src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/
├── client/
│   └── particles/
│       ├── BonePositionTracker.java  # ✓
│       └── SwordParticleHandler.java # ✓
```

### 4. Proper Network Packet Handling

**BAD:**
```java
public boolean handle(Supplier<NetworkEvent.Context> supplier) {
    ctx.enqueueWork(() -> {
        Minecraft mc = Minecraft.getInstance(); // Crashes server!
    });
}
```

**GOOD:**
```java
public boolean handle(Supplier<NetworkEvent.Context> supplier) {
    ctx.enqueueWork(() -> {
        if (ctx.getDirection().getReceptionSide().isClient()) {
            Dist clientDist = Dist.CLIENT;
            DistExecutor.unsafeRunWhenOn(clientDist, () -> () -> {
                // Fully qualified names
                net.minecraft.client.Minecraft mc =
                    net.minecraft.client.Minecraft.getInstance();
            });
        }
    });
}
```

### 5. Separate Client and Server Command Registration

**BAD:**
```java
@SubscribeEvent
public void onRegisterCommands(RegisterCommandsEvent event) {
    TestParticlesCommand.register(event.getDispatcher()); // Uses client code!
}
```

**GOOD:**
```java
@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Bus.FORGE)
public class ClientCommandHandler {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        TestParticlesCommand.register(event.getDispatcher());
    }
}
```

### 6. Client Package Classification

**MUST be in `client` package:**
- Particle Handlers: `BonePositionTracker`, `SwordParticleHandler`
- Animation Handlers: `AnimationSyncHandler`, `AnimationTracker`
- Render Handlers: `SwordDisplayRenderer`, `BreathingDisplayOverlay`
- Client Proxies: `ClientProxy`, `ClientCommandHandler`
- Input Handlers: `ModKeyBindings`, `BreathingFormCycleHandler`
- Anything importing `net.minecraft.client.*` or `net.minecraftforge.client.*`

**CAN be outside `client` package:**
- Items: `BreathingSwordItem`, etc.
- Network Packets: (must use DistExecutor for client code)
- Breathing Techniques: `BreathingForm`, `IceBreathingForms`, etc.
- Server Logic: `AbilityScheduler`, `MovementHelper`, `DamageCalculator`
- Particle Mappings: `SwordParticleMapping` (uses only vanilla types)

## Prevention Rules

### Rule 1: Never Import Client Classes Outside Client Package

Check every non-client class for:
```java
import net.minecraft.client.*;
import net.minecraftforge.client.*;
```

If you need client functionality:
1. Move entire class to `client` package, OR
2. Use `DistExecutor.unsafeRunWhenOn()` with fully qualified names

### Rule 2: Test on Dedicated Server Early

```bash
./gradlew runServer
```

If crash with "Attempted to load class ... for invalid dist DEDICATED_SERVER":
1. Find class name in error
2. Search codebase for imports/references
3. Move to client package or wrap with DistExecutor

### Rule 3: Package Structure Is Critical

```
src/main/java/com/yourmod/
├── client/              # Client-only code
│   ├── particles/
│   ├── renderer/
│   └── gui/
├── network/packets/     # Server-safe with DistExecutor
├── items/               # Server-safe
└── YourMod.java        # Server-safe
```

### Rule 4: Use DistExecutor Correctly

**For static fields - DON'T:**
```java
// BAD - loads at compile time
public static final IProxy PROXY = DistExecutor.safeRunForDist(...);
```

**For method calls - DO:**
```java
// GOOD - only loads on correct dist
if (level.isClientSide) {
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
        net.minecraft.client.Minecraft.getInstance().doSomething();
    });
}
```

### Rule 5: Check Compiled JAR

```bash
cd build/libs
jar xf yourmod-*.jar
find . -name "*.class" ! -path "*/client/*" -exec javap -v {} \; | grep "minecraft/client"
```

If any non-client classes reference client classes - problem!

## Testing Checklist

Before release:
- [ ] `./gradlew clean build` succeeds
- [ ] `./gradlew runClient` starts
- [ ] **`./gradlew runServer` starts** (CRITICAL)
- [ ] No client imports in non-client packages
- [ ] All particle handlers in `client/particles/`
- [ ] All GUI in `client/gui/`
- [ ] Client commands via `RegisterClientCommandsEvent`
- [ ] Network packets use `DistExecutor.unsafeRunWhenOn()`
- [ ] Multiplayer test: server + client, verify sync

## Debugging Server Crashes

1. **Read error** - tells you which class
2. **Find references**: `grep -r "ClassName" src/main/java/`
3. **Check imports** - even unused imports load classes
4. **Check static fields** - run at class load time
5. **Move to client package** or use DistExecutor
6. **Rebuild and test**: `./gradlew clean build && ./gradlew runServer`

## Success Criteria

Server should:
- ✅ Start without crashes
- ✅ Load mod successfully
- ✅ Accept client connections
- ✅ Sync animations to clients
- ✅ Handle breathing techniques server-side
- ✅ Not load any client-only classes

**Key insight**: Client-only code must be in `client` package and accessed only via DistExecutor.
