# Blood Demon Art Alchemy Integration Plan

This document bridges the current custom blood demon art builder with the planned alchemy system in [alchemy_system.md](alchemy_system.md).

## Goals

- Keep the existing custom blood demon art builder functional while we layer in item-driven costs.
- Add alchemy-backed customization without losing the current XP-based progression where it still makes sense.
- Treat all blood demon art vial items as first-class system inputs with stable ids and predictable UI hooks.

## Builder Changes Needed

### 1. Core effect slots become effect input slots

The current builder stores `primaryPotion` and `secondaryPotion`.

That needs to broaden into two effect input slots that accept:

- vanilla potions
- effect infusions for otherwise uncraftable effects

Implementation note:

- The current saved-data shape can keep its existing effect payload fields for now, but the UI and validation layer should stop thinking in terms of "only potions".

### 2. Forms need catalyst state

Each blood demon art form now needs:

- free/default move availability
- one catalyst slot
- a record of which extra move the inserted catalyst unlocks

Implementation note:

- The save model should store a stable `catalystId`, not a display name.
- Move unlock rules should come from a shared alchemy catalog, not be hard-coded inside the screen.

### 3. Moves need effect binding state

Each move in a form needs binder-backed binding metadata:

- `none`
- `primary`
- `secondary`

Implementation note:

- The binder item is a crafting/editor cost.
- The saved data only needs to remember the chosen binding source for each move.

### 4. Forms need amplifier upgrade state

Each form needs an upgrade slot where an amplifier vial can be consumed and converted into persistent form state.

Implementation note:

- This should be stored as a stable `amplifierId`.
- Using the upgrade button should consume the vial and return an empty vial.

### 5. XP costs become mixed costs

The builder currently uses XP-only costs.

After alchemy:

- some actions stay XP-based
- some actions require specific items
- some actions require both XP and an alchemy item

Recommended split:

- `Create Form`: XP only
- `Add non-free move`: XP only or XP + catalyst depending on move family
- `Unlock catalyst move`: catalyst item
- `Bind move to primary/secondary`: binder item
- `Apply amplifier upgrade`: amplifier vial item
- `Get custom demon art item`: XP only

## Suggested Phases

### Phase 1: Data + planning foundation

- Add canonical alchemy catalog ids for catalyst, infusion, and amplifier concepts.
- Expand saved data so forms can already remember catalysts, bindings, and amplifiers.
- Update the builder summary screen to surface those new slots, even if they are not editable yet.

### Phase 2: Server action layer

- Add packet actions for:
  - inserting catalyst
  - clearing catalyst
  - binding move effect source
  - applying amplifier
- Validate all costs server-side against inventory and XP.

### Phase 3: Item registration + recipes

- Register the vial/binder/catalyst item family.
- Add alchemy table, microscope, crafting table, and brewing recipes.
- Add an empty vial return path for all consumed system vials.

### Phase 4: Full builder/editor UI

- Add form editor sub-screen.
- Show free moves vs catalyst-unlocked moves.
- Add effect slot pickers and per-move binding controls.
- Add amplifier upgrade UI with preview text.

### Phase 5: Runtime logic

- Make move execution respect primary/secondary binding selection.
- Apply amplifier modifiers to damage, duration, range, defense, or movement values.
- Surface missing-input feedback cleanly to the player.

## Initial Catalog Decisions

These are the first ids to standardize around:

- Effect inputs:
  - `minecraft:potion`
  - `kimetsunoyaibamultiplayer:blindness_infusion`
  - `kimetsunoyaibamultiplayer:darkness_infusion`
  - `kimetsunoyaibamultiplayer:wither_infusion`
- Catalysts:
  - `kimetsunoyaibamultiplayer:feral_catalyst`
  - `kimetsunoyaibamultiplayer:acrobat_catalyst`
- Amplifiers:
  - `kimetsunoyaibamultiplayer:damage_amplifier_vial`
  - `kimetsunoyaibamultiplayer:harmful_effect_amplifier_vial`
  - `kimetsunoyaibamultiplayer:beneficial_effect_amplifier_vial`
  - `kimetsunoyaibamultiplayer:duration_amplifier_vial`
  - `kimetsunoyaibamultiplayer:defense_amplifier_vial`
  - `kimetsunoyaibamultiplayer:range_amplifier_vial`
  - `kimetsunoyaibamultiplayer:speed_amplifier_vial`

These are placeholders, but they give us stable ids to build packets, saves, and UI around.

## Presentation Rules

Blood demon art builder-usable vial items should share presentation rules:

- enchant glint enabled
- bold display names

This includes:

- effect infusions
- blood demon art catalysts
- potion effect binders
- amplifier vials

Regular ingredients should not use the same premium presentation style:

- no enchant glint
- regular-weight names
- optional color styling is still fine

## Asset TODO

- Empty vial
- Crude vial
- Cruel vial
- Refined vial
- Potion/effect infusion family
- Blood demon art catalyst family
- Potion effect binder
- Amplifier vial family
- Extract and culture ingredient family
- Builder/form-editor slot icons for primary, secondary, catalyst, binder, and amplifier

## Current Foundation Status

The code foundation added in this pass should cover:

- alchemy-aware form state in saved data
- a starter alchemy catalog with canonical ids
- builder summary fields that acknowledge catalyst/binding/amplifier hooks

That gives the next implementation pass a clean place to attach real UI actions, recipes, and registered items.
