# Bridger Block and World Generation

The Bridger Block is a bridge authoring block, not just a decorative marker.
It stores the bridge type, movement style, facing, length limits, endcap rules,
and preview settings used to generate the actual bridge geometry.

## How It Works

- Place a Bridger Block to define a bridge origin.
- Open the menu to choose the bridge type and movement pattern.
- The block entity stores the settings and drives preview rendering.
- The preview shows what the final bridge would look like, but it is not the real bridge yet.

## World Generation Use

When a structure is placed, the generator should not leave the Bridger Block behind.
Instead:

1. Detect the Bridger Block during structure placement.
2. Read its bridge settings from the block entity.
3. Replace the marker with the actual bridge blocks or segments.
4. Apply the chosen endcaps or merge rules after the bridge is built.

This makes the Bridger Block useful as a generation control point.
It acts like a blueprint node that turns into a real bridge during worldgen.

## Linking Bridges Together

For bridge raycasts or volume-casts:

- Scan forward along the configured bridge movement.
- If the scan hits another Bridger Block of the same bridge type,
  and that block is facing the opposite direction, link them together.
- If no matching bridger is found, stop at the configured max length
  and apply the normal end behavior.

This allows two bridger endpoints to meet in the middle and form one continuous bridge.

## Practical Rule of Thumb

- `Bridger Block` = control node
- `Same bridge type + opposite facing` = connect the bridge
- `Structure placement` = replace the marker with the real bridge

