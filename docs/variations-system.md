# Breathing Form Variations System

This document describes how variations are stored, cycled, executed, and displayed for both custom swords and base‑mod swords.

---

## High‑Level Rules

- **Base mod swords**: Use **form names** (strings) for variation matching. The form name is captured from chat display.
- **Custom swords**: Use form IDs (integers) as before - no changes.
- The selected variation is tracked separately as `currentVariationIndex` (0 = base form, 1+ = variations) in `PlayerBreathingData`, persisted to player NBT.
- When cycling forms (R / Shift+R or the dedicated reverse key) or swapping swords, the variation index is reset to 0.
- If a variation index is out of range or the form has no variations, the index is reset to 0.
- Color for variation names uses the base form's color (extracted from the cached form name).

---

## State & Persistence

- **PlayerBreathingData.PlayerData**
  - `currentFormIndex` (custom swords only)
  - `currentVariationIndex`
  - `baseModBreathesValue` and `baseModFormName` (server caches for base‑mod swords)
  - `lastSwordKey` (tracks main‑hand sword to reset variation on swap)
- NBT keys:
  - `CustomBreathingFormIndex`
  - `CustomVariationIndex`
  - `breathes` (vanilla/base‑mod tag; always a base form ID)

Variation index is synced with `VariationIndexSyncPacket`. Breathes is synced with `BreathesValueSyncPacket` (used only for client‑side display cache, not for client NBT writes).

---

## Cycling Variations (G / Shift+G)

1. Client sends `CycleFormVariationPacket` (direction).
2. Server logic:
   - **Custom swords (`BreathingSwordItem`)**
     - Determine base form from `currentFormIndex`.
     - Compute variation count from `VariationRegistry` for that form/sword using form ID.
     - Compute new `currentVariationIndex` (wraps 0..N).
     - Sync `VariationIndexSyncPacket`.
     - Chat message uses one color (technique form color) for both style + form/variation name.
   - **Base‑mod swords**
     - Get cached form name from `PlayerBreathingData.baseModFormName`.
     - Use **substring matching** to find variations (`VariationRegistry.getVariationCountBySubstring`).
     - Compute new `currentVariationIndex` (wraps 0..N).
     - Sync `VariationIndexSyncPacket`.
     - Chat message color extracted from cached form name (first two characters if they're a color code; otherwise defaults to aqua).

---

## Cycling Forms (R / Shift+R and dedicated reverse key)

- Custom swords call `BreathingSwordItem.cycleForm`, which:
  - Updates `currentFormIndex`.
  - Writes `breathes` NBT to the new base form ID.
  - Syncs form (`FormSyncPacket`), breathes (`BreathesValueSyncPacket`), and resets variation (`VariationIndexSyncPacket` with 0).
- Base‑mod swords (`CycleBreathingFormPacket`):
  - Computes next/previous form from the style’s form list (`BaseModStyleMapping.getFormsForStyle`).
  - Always strips any encoded/garbage variation and writes only the base form ID to `breathes`.
  - Resets `currentVariationIndex` to 0 and syncs breathes + variation index.

---

## Sword Swap Reset

- Server tick in `KimetsunoyaibaMultiplayer.onPlayerTick`: if the main‑hand sword (including select offset) changes, `currentVariationIndex` is reset to 0, saved to NBT, and `VariationIndexSyncPacket` is sent.

---

## Executing a Form (Right‑click)

- **Custom swords (`BreathingSwordItem.use`)**
  - Reads `currentVariationIndex` (clamped to available variations). If form has no variations, index is reset to 0.
  - If variation index > 0 and a variation exists, executes the variation's effect; otherwise executes the base form.
  - Uses the base form ID for all execution paths; variation selection is separate.

- **Base‑mod swords**
  - Base mod still runs its base form; our `BaseModVariationHandler` intercepts right‑click on server:
    - Gets cached form name from `PlayerBreathingData.baseModFormName`.
    - Uses **substring matching** to find the variation (`VariationRegistry.getVariationBySubstring`).
    - If variation index > 0 and a variation exists, executes the variation effect and cancels the base form execution.
    - If variation is missing, variation index is reset to 0.
    - No breathes value manipulation - base mod's NBT is left untouched.

---

## Display & Colors

- Overlay (`BreathingDisplayOverlay` / `BreathingInfoDetector`):
  - Uses base form colored text (cached from chat when available) and rebuilds variation display with the base form’s leading color code.
  - Variation count indicator is `(currentVariationIndex+1)/(totalVariations+1)`, so base shows `1/N`.
- Chat messages for variation cycling:
  - Custom: single color from the technique’s `formColor`.
  - Base mod: single color taken from cached base form text (first two chars if they’re a color code), otherwise aqua (`§b`).

---

## Validation / Guardrails

- `BaseKnYForms.forms` contains all base mod form IDs mapped to their names and colors.
- Variations are registered by form ID, which automatically registers by form name via `BaseKnYForms` lookup.
- For base mod swords, substring matching is used to find variations, eliminating reliance on unreliable breathes values.
- If a form ID is not in `BaseKnYForms`, variations are not accessible by name (warning logged).

---

## Reset Conditions Summary

- Form cycle (forward/backward, any keybinding path) → variation = 0
- Sword swap (main hand changes) → variation = 0
- Variation cycle where the target form has zero variations → variation = 0
- Missing/invalid variation when executing → variation = 0

---

## Networking Quick Reference

- **Client → Server**
  - `CycleBreathingFormPacket` (direction)
  - `CycleFormVariationPacket` (direction)
- **Server → Client**
  - `VariationIndexSyncPacket` (variation index)
  - `FormSyncPacket` (custom swords, form index)

**Note:** `BreathesValueSyncPacket` is no longer used for base mod swords (form names are captured from chat display).

All client‑only classes are guarded; server handlers never load client‑side classes. Networking handlers that touch client APIs are wrapped in `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)`.
