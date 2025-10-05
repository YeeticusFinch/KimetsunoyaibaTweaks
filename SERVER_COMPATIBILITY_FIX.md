# Server Compatibility Fix Guide

## Problem Summary

The mod currently crashes on dedicated servers because **client-only code is being referenced in packet handlers**. When the server tries to load packet classes, it encounters references to client-only classes like `SwordDisplayTracker`, `AnimationSyncHandler`, and `Minecraft.getInstance()`, causing `ClassNotFoundException` or `NoClassDefFoundError`.

## Root Cause

In Minecraft Forge, the server and client run different code. Classes in the `client` package and any code using `Minecraft.getInstance()` or `ClientLevel` are **only available on the physical client**. When a dedicated server tries to load these classes, it crashes.

The problem occurs in network packet handlers that directly call client-only methods:

### Problematic Files:

1. **`SwordDisplaySyncPacket.java`** (line 83)
   ```java
   SwordDisplayTracker.updateRemotePlayerDisplay(playerUUID, leftHipSword, rightHipSword);
   // ❌ SwordDisplayTracker is client-only!
   ```

2. **`PlayerRotationSyncPacket.java`** (line 122)
   ```java
   Minecraft mc = Minecraft.getInstance();
   // ❌ Minecraft.getInstance() doesn't exist on server!
   ```

3. **`AnimationSyncPacket.java`** (line 175)
   ```java
   AnimationSyncHandler.handleAnimationSync(...);
   // ❌ AnimationSyncHandler is client-only!
   ```

4. **`BonePositionTracker.java`** (lines 6, 38)
   ```java
   import net.minecraft.client.Minecraft;
   Minecraft mc = Minecraft.getInstance();
   // ❌ Client-only imports and API calls
   ```

5. **`SwordParticleHandler.java`** (lines 7, 58)
   ```java
   import net.minecraft.client.Minecraft;
   Minecraft mc = Minecraft.getInstance();
   // ❌ Client-only imports and API calls
   ```

## Solution: Client Proxy Pattern

The proper fix is to use Forge's **DistExecutor** or **proxy pattern** to ensure client-only code is never loaded on the server.

### Step 1: Create Client Proxy Interface

Create `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/proxy/IClientProxy.java`:

```java
package com.lerdorf.kimetsunoyaibamultiplayer.proxy;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/**
 * Proxy interface for client-only operations
 * Server implementation will have empty methods
 */
public interface IClientProxy {

    /**
     * Handle sword display sync on client
     */
    void handleSwordDisplaySync(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword);

    /**
     * Handle animation sync on client
     */
    void handleAnimationSync(UUID playerUUID, ResourceLocation animationId, int currentTick,
                            int animationLength, boolean isLooping, boolean stopAnimation,
                            ItemStack swordItem, ResourceLocation particleType,
                            float speed, int layerPriority);

    /**
     * Handle player rotation sync on client
     */
    void handleRotationSync(UUID playerUUID, float yaw, float pitch, float headYaw);

    /**
     * Spawn sword particles on client
     */
    void spawnSwordParticles(UUID entityUUID, String animationName, int animationTick,
                            ParticleOptions particleType);
}
```

### Step 2: Create Client Proxy Implementation

Create `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/client/ClientProxy.java`:

```java
package com.lerdorf.kimetsunoyaibamultiplayer.client;

import com.lerdorf.kimetsunoyaibamultiplayer.Config;
import com.lerdorf.kimetsunoyaibamultiplayer.Log;
import com.lerdorf.kimetsunoyaibamultiplayer.proxy.IClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/**
 * Client-side proxy implementation
 * Only loaded on physical client
 */
public class ClientProxy implements IClientProxy {

    @Override
    public void handleSwordDisplaySync(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword) {
        if (Config.logDebug) {
            Log.info("Client received sword display sync for player {}: left={}, right={}",
                playerUUID,
                leftHipSword.isEmpty() ? "empty" : leftHipSword.getItem().toString(),
                rightHipSword.isEmpty() ? "empty" : rightHipSword.getItem().toString());
        }

        SwordDisplayTracker.updateRemotePlayerDisplay(playerUUID, leftHipSword, rightHipSword);
    }

    @Override
    public void handleAnimationSync(UUID playerUUID, ResourceLocation animationId, int currentTick,
                                    int animationLength, boolean isLooping, boolean stopAnimation,
                                    ItemStack swordItem, ResourceLocation particleType,
                                    float speed, int layerPriority) {
        if (Config.logDebug) {
            Log.info("Client received animation sync for player {}: animation={}, tick={}, stop={}, speed={}, layer={}",
                playerUUID, animationId, currentTick, stopAnimation, speed, layerPriority);
        }

        AnimationSyncHandler.handleAnimationSync(playerUUID, animationId, currentTick, animationLength,
                                                isLooping, stopAnimation, null, swordItem, particleType,
                                                speed, layerPriority);
    }

    @Override
    public void handleRotationSync(UUID playerUUID, float yaw, float pitch, float headYaw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(playerUUID);
            if (player != null) {
                // Always update the entity rotation
                player.setYRot(yaw);
                player.setXRot(pitch);
                player.setYHeadRot(headYaw);
                player.yRotO = yaw;
                player.xRotO = pitch;
                player.yHeadRotO = headYaw;

                // If this is the local client player, update the camera too
                if (player == mc.player) {
                    mc.player.setYRot(yaw);
                    mc.player.setXRot(pitch);
                    mc.player.setYHeadRot(headYaw);

                    mc.player.yRotO = yaw;
                    mc.player.xRotO = pitch;
                    mc.player.yHeadRotO = headYaw;

                    // Update ShoulderSurfing camera if present
                    com.lerdorf.kimetsunoyaibamultiplayer.compat.ShoulderSurfingCompat.setShoulderCameraRotation(yaw, pitch);
                }

                if (Config.logDebug) {
                    Log.debug("Client received rotation sync for player {}: yaw={}, pitch={}, headYaw={}",
                        player.getName().getString(), yaw, pitch, headYaw);
                }
            }
        }
    }

    @Override
    public void spawnSwordParticles(UUID entityUUID, String animationName, int animationTick,
                                   ParticleOptions particleType) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            LivingEntity entity = (LivingEntity) mc.level.getEntity(entityUUID.hashCode());
            if (entity != null && particleType != null) {
                com.lerdorf.kimetsunoyaibamultiplayer.particles.BonePositionTracker.spawnRadialRibbonParticles(
                    entity, animationName, animationTick, particleType);
            }
        }
    }
}
```

### Step 3: Create Server Proxy Stub

Create `src/main/java/com/lerdorf/kimetsunoyaibamultiplayer/proxy/ServerProxy.java`:

```java
package com.lerdorf.kimetsunoyaibamultiplayer.proxy;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/**
 * Server-side proxy implementation
 * All methods are empty since server doesn't handle client rendering
 */
public class ServerProxy implements IClientProxy {

    @Override
    public void handleSwordDisplaySync(UUID playerUUID, ItemStack leftHipSword, ItemStack rightHipSword) {
        // Server doesn't handle client-side display
    }

    @Override
    public void handleAnimationSync(UUID playerUUID, ResourceLocation animationId, int currentTick,
                                    int animationLength, boolean isLooping, boolean stopAnimation,
                                    ItemStack swordItem, ResourceLocation particleType,
                                    float speed, int layerPriority) {
        // Server doesn't handle client-side animations
    }

    @Override
    public void handleRotationSync(UUID playerUUID, float yaw, float pitch, float headYaw) {
        // Server doesn't handle client-side camera rotation
    }

    @Override
    public void spawnSwordParticles(UUID entityUUID, String animationName, int animationTick,
                                   ParticleOptions particleType) {
        // Server doesn't spawn particles
    }
}
```

### Step 4: Add Proxy Field to Main Mod Class

Update `KimetsunoyaibaMultiplayer.java`:

```java
package com.lerdorf.kimetsunoyaibamultiplayer;

import com.lerdorf.kimetsunoyaibamultiplayer.proxy.IClientProxy;
import net.minecraftforge.fml.DistExecutor;
// ... other imports

@Mod(KimetsunoyaibaMultiplayer.MODID)
public class KimetsunoyaibaMultiplayer {
    public static final String MODID = "kimetsunoyaibamultiplayer";

    // Add proxy field
    public static final IClientProxy CLIENT_PROXY = DistExecutor.safeRunForDist(
        () -> () -> new com.lerdorf.kimetsunoyaibamultiplayer.client.ClientProxy(),
        () -> () -> new com.lerdorf.kimetsunoyaibamultiplayer.proxy.ServerProxy()
    );

    // ... rest of class
}
```

### Step 5: Update Packet Handlers

#### Fix `SwordDisplaySyncPacket.java`:

```java
public boolean handle(Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context ctx = supplier.get();
    ctx.enqueueWork(() -> {
        if (ctx.getDirection().getReceptionSide().isServer()) {
            // Server received update from client - relay to all other clients
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                if (Config.logDebug) {
                    Log.info("Server received sword display sync from player {}",
                        sender.getName().getString());
                }

                // Relay to all other clients
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClientsExcept(
                    new SwordDisplaySyncPacket(playerUUID, leftHipSword, rightHipSword, displayPosition),
                    sender
                );
            }
        } else {
            // Client received update - use proxy to handle
            KimetsunoyaibaMultiplayer.CLIENT_PROXY.handleSwordDisplaySync(
                playerUUID, leftHipSword, rightHipSword
            );
        }
    });
    ctx.setPacketHandled(true);
    return true;
}
```

#### Fix `AnimationSyncPacket.java`:

```java
public boolean handle(Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context ctx = supplier.get();
    ctx.enqueueWork(() -> {
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                if (Config.logDebug) {
                    Log.info("Server received animation sync from player {}",
                        sender.getName().getString());
                }

                // Relay to all other clients
                AnimationSyncPacket relayPacket = new AnimationSyncPacket(
                    playerUUID, animationId, currentTick, animationLength,
                    isLooping, stopAnimation, animationData, speed, layerPriority
                );
                com.lerdorf.kimetsunoyaibamultiplayer.network.ModNetworking.sendToAllClientsExcept(
                    relayPacket, sender
                );
            }
        } else {
            // Client received update - use proxy to handle
            KimetsunoyaibaMultiplayer.CLIENT_PROXY.handleAnimationSync(
                playerUUID, animationId, currentTick, animationLength,
                isLooping, stopAnimation, swordItem, particleType, speed, layerPriority
            );
        }
    });
    ctx.setPacketHandled(true);
    return true;
}
```

#### Fix `PlayerRotationSyncPacket.java`:

```java
public boolean handle(Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context ctx = supplier.get();
    ctx.enqueueWork(() -> {
        // This packet only goes from server -> client
        if (ctx.getDirection().getReceptionSide().isClient()) {
            // Use proxy to handle client-side rotation
            KimetsunoyaibaMultiplayer.CLIENT_PROXY.handleRotationSync(
                playerUUID, yaw, pitch, headYaw
            );
        }
    });
    ctx.setPacketHandled(true);
    return true;
}
```

## Step 6: Remove Client-Only Imports from Packet Classes

Remove these imports from packet classes:
- `import net.minecraft.client.Minecraft;`
- `import net.minecraft.client.multiplayer.ClientLevel;`
- `import com.lerdorf.kimetsunoyaibamultiplayer.client.*` (any client package imports)

## Step 7: Test on Dedicated Server

1. Build the mod: `./gradlew build`
2. Copy the JAR from `build/libs/` to server's `mods/` folder
3. Start the server
4. Verify no `ClassNotFoundException` or `NoClassDefFoundError` occurs
5. Join the server with a client and test all features:
   - Sword animations
   - Sword display on hips
   - Breathing techniques
   - Particle effects
   - Multiplayer sync

## Additional Notes

### Why This Works

- **Server loads packets**: Packet classes are loaded by both client and server
- **Server never loads proxies**: The `ClientProxy` class is never loaded on server because `DistExecutor.safeRunForDist()` only loads the appropriate proxy
- **Client-only code isolated**: All references to `Minecraft.getInstance()`, `ClientLevel`, and client-only handlers are now in `ClientProxy`
- **Safe method calls**: Calling `CLIENT_PROXY.method()` is safe because on the server it calls `ServerProxy.method()` which does nothing

### Alternative: DistExecutor.unsafeRunWhenOn()

Instead of the proxy pattern, you can also use:

```java
if (ctx.getDirection().getReceptionSide().isClient()) {
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
        // Client-only code here
        SwordDisplayTracker.updateRemotePlayerDisplay(...);
    });
}
```

However, the proxy pattern is cleaner and easier to maintain.

### Common Pitfalls to Avoid

1. ❌ **Don't import client classes in packet handlers**
   ```java
   import com.lerdorf.kimetsunoyaibamultiplayer.client.SwordDisplayTracker; // BAD!
   ```

2. ❌ **Don't use Minecraft.getInstance() in packet classes**
   ```java
   Minecraft mc = Minecraft.getInstance(); // BAD!
   ```

3. ❌ **Don't call client methods directly in packets**
   ```java
   AnimationSyncHandler.handleAnimationSync(...); // BAD!
   ```

4. ✅ **Do use the proxy pattern**
   ```java
   KimetsunoyaibaMultiplayer.CLIENT_PROXY.handleAnimationSync(...); // GOOD!
   ```

## Summary

By implementing the proxy pattern, we ensure that:
- Server never tries to load client-only classes
- All client-specific logic is isolated in `ClientProxy`
- Packet handlers remain safe for both client and server
- The mod works correctly on dedicated servers

This is a standard Forge pattern used by many mods to maintain client/server compatibility.
