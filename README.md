# Kimetsunoyaiba Tweaks (Forge)
#### aka *The Forgotten Necessities* update
A comprehensive Forge mod that enhances the [Kimetsu no Yaiba mod](https://www.curseforge.com/minecraft/mc-mods/kimetsunoyaiba) with multiplayer features, visual effects, and gameplay enhancements.

---

## ✨ Features

### 🎭 Animation Synchronization
- **Multiplayer Animation Support**: Makes PlayerAnimator animations visible to all players in multiplayer
- Detects when a player starts/stops an animation
- Sends animation data (player, animation name, looping flag) to the server
- Server rebroadcasts animations to all nearby clients
- Uses MobPlayerAnimator to display animations on other players
- Works in both **LAN** and **dedicated servers**

### ⚔️ Sword Display System
- **Visual Sword Display**: Nichirin swords are displayed on your character when not being held
- Automatically places swords on your hip or back when stored in inventory
- Supports up to 2 swords displayed simultaneously (left and right positions)
- Swords disappear from display when equipped or removed from inventory
- **Fully Configurable**:
  - Choose between hip or back placement
  - Adjust position, rotation, and scale for each sword independently
  - Separate configurations for left/right and hip/back positions
  - Config location: `config/kimetsunoyaibamultiplayer/sword_display.toml`

### ✨ Particle Effects
- **Sword Swing Particles**: Configurable particle effects when swinging nichirin swords
- Particle mapping system for different sword types
- Trigger modes: Animation-based or attack-based
- Customizable colors, sizes, and particle types
- Config location: `config/kimetsunoyaibamultiplayer/particles.toml`

### 🦅 Enhanced Crow System
- **Quest Markers**: Crows display markers when carrying quest information
- **Custom Models**: GeckoLib-based 3D crow models
- **Animation Support**: Flying animations for kasugai crows
- **Waypoint Navigation**: Crows fly to quest locations
- Config location: `config/kimetsunoyaibamultiplayer/entities.toml`

### 🔫 Gun Animation Support
- Dynamic gun animations for rifle and other firearms
- Automatic detection and animation playback
- Compatible with Kimetsu no Yaiba firearms

---

## 🛠 Requirements
- **Minecraft Forge** `1.20.x` (other versions may work but are untested).  
- **PlayerAnimator** mod (installed on both client and server).  
- **MobPlayerAnimator** mod (installed on both client and server).  
- Other mods that use PlayerAnimator (e.g. Kimetsu no Yaiba).  

---

## 📦 Installation
1. Download the latest release of:
   - `PlayerAnimator`
   - `MobPlayerAnimator`
   - `KimetsunoyaibaMultiplayer (this mod)`

2. Place both `.jar` files in your `mods/` folder (client and server).  

3. Launch Minecraft with Forge.

---

## 🎮 Commands

The mod includes several test and debug commands:

- `/testanim` - (Server) Plays test animation on all connected players
- `/testanimc` - (Client) Tests client-side animation and syncing
- `/testparticles` - Tests sword particle effects
- `/debugparticles` - Displays particle configuration debug info
- `/testanim <animation>` - Plays a specific animation
- `/testcrowquest` - Tests crow quest marker system
- `/debugcrow` - Shows debug information for nearby crows
- `/testcrowrender` - Spawns marker entities at crow positions to debug visibility

All commands require operator permission level 2.

---
