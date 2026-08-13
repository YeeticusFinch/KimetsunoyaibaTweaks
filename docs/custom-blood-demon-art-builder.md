# Custom Blood Demon Art Builder

This document tracks the first implementation pass for the player-authored blood demon art system.

## Goal

Give demon players a dedicated builder opened from the meditation `Skills` tab where they can author and manage custom blood demon arts that persist in the world save and work in multiplayer.

## Current Structure

- `Meditation -> Skills -> Open Blood Demon Art Builder` now opens a separate builder window for demon players.
- Custom art data is stored in `CustomBloodDemonArtSavedData`, a `SavedData` world file rooted in the overworld data storage.
- Data is keyed by player UUID, which keeps it server-authoritative and multiplayer-safe.
- A new `custom_demon_art` item exists and uses the owning player's skin through a GeckoLib item renderer.
- The existing blood demon art cycle key path now supports the custom item.

## Stored Data Model

Each player profile currently stores:

- Core settings
- Primary particle id, color, and size
- Secondary particle id, color, and size
- Primary potion setting
- Secondary potion setting
- Chat color
- Up to 10 form slots
- Selected slot

Each form slot stores:

- Whether the slot is filled
- Form name
- Move list
- Derived cooldown

## Slot Rules

- Unlocked slots = `muzan blood / 10`
- Maximum unlocked slots = `10`
- Creating a blank form in an unlocked empty slot currently costs `10 XP levels`

## Current Move Runtime

The runtime currently supports these move definitions:

- `Punch Right`
- `Punch Left`
- `Front Flip`
- `Melee Combo`

Forms execute moves sequentially using the existing `AbilityScheduler`.

## Builder Window: First Pass

The dedicated builder screen currently shows:

- Current XP level
- Current Muzan blood count
- Unlocked slot count
- Whether the player already owns the custom demon art item
- Current core particle and potion ids
- Slot rows showing locked, empty, or filled states

Current builder actions:

- `Get Item (5 XP)` if the player does not already have the custom demon art item
- `Create (10)` on unlocked empty slots to create a blank form shell

## Planned Next Steps

- Add full particle picker UI with live preview and color/size sliders
- Add potion selection, target/self toggles, duration, and amplifier costs
- Add form editor sub-screen
- Add move picker with costs, descriptions, and slot-based move caps
- Add rename fields for forms
- Add slot/form selection syncing back to the custom item
- Improve cooldown and damage scaling to match the exact intended formulas
- Add richer visual previews for particles and move effects inside the builder

## Notes

- The separate builder window is intentional. The meditation menu stays the entry point, but the builder gets its own screen so the system can grow without overcrowding the existing skills tab.
- The current implementation is a foundation pass: persistent data, item ownership, slot creation, and sequential form runtime are now in place so deeper editing can be layered on top cleanly.
