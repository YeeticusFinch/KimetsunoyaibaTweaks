package com.lerdorf.kimetsunoyaibamultiplayer.items;

/**
 * The Daughter's Silk Manipulation blood demon art item.
 *
 * Behaves exactly like {@link BloodDemonArtItem}, while rendering through the
 * normal item model at models/item/daughter_demon_art.json.
 */
public class DaughterDemonArtItem extends BloodDemonArtItem {
    public DaughterDemonArtItem(String artId, float attackDamage, float attackSpeed, Properties properties) {
        super(artId, attackDamage, attackSpeed, properties);
    }
}
