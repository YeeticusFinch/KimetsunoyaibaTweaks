# Avoiding Biome Feature Circular Dependencies (1.20.1 Forge)

This guide documents practical patterns to prevent the dreaded
"Feature order cycle found" crashes (FeatureSorter) when adding or
modifying biomes and worldgen data.

## TL;DR Checklist

- Keep a one‑way layering: configured_feature → placed_feature → biome
- Use per‑biome (or per‑variant) features; avoid cross‑referencing
- Do not make features that (directly or indirectly) reference each other
- Keep decoration steps appropriate and simple (vegetation in vegetation step)
- Prefer vanilla sapling survival checks (`would_survive` with `oak_sapling`)
- Avoid custom biome tags/filters inside configured features that can feed back
- If mixing variants, duplicate features with unique names instead of re‑using
- When in doubt, bisect by removing features from the biome until the cycle disappears

## What Causes Feature Cycles

FeatureSorter topologically orders features per generation step. A cycle
occurs when feature/placement A depends on B while B depends on A (possibly
indirectly), or when chains of references force an order that cannot be
resolved.

Typical pitfalls:

- Cross‑variant reuse: Biome A uses a placed_feature defined for Biome B,
  and that placed_feature or its configured_feature references data used
  in Biome A again (e.g., via tags or nested selectors).
- Over‑generic features: A single generic placed_feature is referenced by
  multiple biomes and internally selects/filters by biome/tag, creating
  hard‑to‑see feedback.
- Nesting/indirection: configured_feature → placed_feature → configured_feature
  chains that loop back via tags or shared selectors.

## Best Practices

1) One‑way layering

- Don’t reference placed_feature from configured_feature; keep the DAG as:
  configured_feature (pure structure) → placed_feature (placement rules) → biome (feature lists).

2) Per‑biome isolation

- Give each biome (and each color variant) its own configured_feature and
  placed_feature files. Example in this repo:
  - `worldgen/configured_feature/wisteria_forest_grass_wisteria.json`
  - `worldgen/placed_feature/wisteria_forest_grass_placed_wisteria.json`
  - Biome uses only its matching `..._wisteria` features, not `..._cyan`/`..._cream`.

3) Avoid cross‑variant imports

- Do not include `wisteria_tree_cyan_placed` in the base `wisteria_forest` biome,
  or vice‑versa. Duplicate any shared logic under variant‑specific names.

4) Keep placement simple and local

- Use standard placement types (`count`, `in_square`, `heightmap`, `biome`,
  simple `block_predicate_filter`).
- Avoid clever biome/tag filtering inside configured features; let the biome’s
  feature list decide where things run.

5) Use survival checks with vanilla saplings

- For trees, use `would_survive` with a vanilla sapling state. This avoids
  referencing mod blocks that may also be placed in the same step.

6) Separate decoration steps properly

- Keep vegetation in the vegetation step; don’t create features that place
  other features in the same step through indirection.

7) Namespace clearly and prefer copy‑not‑reference

- Name features with clear suffixes (`_wisteria`, `_cyan`, `_cream`).
- Prefer duplicating lightweight JSON over sharing a feature that is used by
  multiple biomes (shared features are a common source of cycles).

## Debugging a Cycle

1) Read the crash

- If you see `Feature order cycle found` referencing a biome key, the cycle is
  somewhere in that biome’s feature list.

2) Bisect the features array

- Temporarily comment/remove half the features in the biome JSON. If the crash
  goes away, the removed half contains the cycle. Repeat until you isolate the
  offending feature.

3) Inspect referenced files

- Walk the chain: biome → placed_feature → configured_feature (and any nested
  features/providers). Look for any cross‑references to other biomes/variants or
  to features that could depend back on the original.

4) Replace shared features with per‑biome copies

- If a generic feature is used by multiple biomes, copy it and rename for each
  biome, then remove any cross‑biome logic inside.

## Patterns Used In This Repo

- Variant isolation:
  - Base: `wisteria_forest` uses only `..._wisteria` placed/configured features
  - Variants: `wisteria_forest_cyan` / `wisteria_forest_cream` use only their
    own `..._cyan` / `..._cream` features

- Simple tree placement with vanilla survival check:
  - `placed_feature/wisteria_tree_lavender_placed.json` uses `would_survive`
    with `oak_sapling` and does not call back into other custom features.

- No cross‑variant trees in the base biome:
  - See `worldgen/biome/wisteria_forest.json` — only lavender and pink
    wisteria trees are included.

## Code‑Side Tips (Biome Injection)

- If you inject biomes via `MultiNoiseBiomeSource`, avoid writing/reading and
  mutating the same parameter lists in ways that depend on features. Treat biome
  injection and feature registration as separate phases.
- Keep climate bands for variants disjoint (e.g., separate `weirdness` bands)
  to avoid heavy mixing, which reduces the temptation to share features.

## Pre‑Commit Safety Checks

- Grep for cross‑variant references (e.g., `rg -n "wisteria_tree_.*_placed" worldgen/biome`)
- Ensure every biome references only its own placed/configured features
- Confirm no configured_feature includes anything of type `placed_feature`
- Sanity‑load a world; scan logs for `FeatureSorter` warnings/errors

Following these patterns will keep your feature graph acyclic, stable, and easy
to reason about as you grow your biome set.

