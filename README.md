# Kimetsunoyaiba Multiplayer (Forge)

A Forge mod that adds **multiplayer synchronization** for mods using the [PlayerAnimator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) library.  

Specifically this mod was made such that animations from the [Kimetsunoyaiba mod](https://www.curseforge.com/minecraft/mc-mods/demonslayer) are visible in multiplayer.

By default, PlayerAnimator animations are **only visible on the local client**. This mod fixes that limitation by sending animation state over the network, allowing **all players** in a world or server to see each other’s custom animations (for example, in the [Kimetsu no Yaiba mod](https://www.curseforge.com/minecraft/mc-mods/kimetsunoyaiba)).

---

## ✨ Features
- Detects when a player starts/stops an animation.  
- Sends animation data (player, animation name, looping flag) to the server.  
- Server rebroadcasts this animation to nearby clients.  
- Other clients play the animation on the correct player using MobPlayerAnimator (since PlayerAnimator only animates the client player).  
- Works in both **LAN** and **dedicated servers**, as long as everyone has this mod installed.  

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
