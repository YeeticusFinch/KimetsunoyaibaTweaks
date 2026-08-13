# Alchemy Asset Checklist

These are the assets still intended for the art pass. The current code uses placeholder models so the system can run before custom art is finished.

## Shared item textures

- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/empty_vial.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/full_vial.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/empty_petri_dish.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/full_petri_dish.png`

`full_vial.png` and `full_petri_dish.png` should keep a tintable fill region so the existing item color handler can recolor them per item.

## Unique item textures

- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/amethyst_lens.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/minced_human_flesh.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/bone_dust.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/calcite_powder.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/fermented_orchid.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/immortal_daisy.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/item/potion_effect_binder.png`

## Optional custom block textures

The alchemy table currently uses vanilla crafting table textures as a placeholder. If you want it to have its own art, add:

- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/block/alchemy_table_top.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/block/alchemy_table_side.png`
- `src/main/resources/assets/kimetsunoyaibamultiplayer/textures/block/alchemy_table_bottom.png`

## Notes

- No dedicated screen texture is required right now. The microscope and alchemy table UIs are drawn in code.
- Most vials, extracts, tissues, cultures, infusions, catalysts, and amplifiers already share common models so they can swap to the shared vial and petri textures once those are added.
- Special alchemy items already have enchant glint and bold names in code.
