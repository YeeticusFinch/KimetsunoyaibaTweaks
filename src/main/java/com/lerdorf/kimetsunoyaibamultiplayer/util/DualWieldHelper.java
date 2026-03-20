package com.lerdorf.kimetsunoyaibamultiplayer.util;

import com.lerdorf.kimetsunoyaibamultiplayer.api.SwordMetadataRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Shared dual-wield validation for gameplay systems outside EnhancedBeastForms.
 *
 * A valid dual-wield state requires:
 * - non-empty mainhand and offhand items
 * - both items present in SwordMetadataRegistry
 * - both metadata entries marked dualWielding=true
 * - matching style IDs on both swords
 */
public final class DualWieldHelper {

    private DualWieldHelper() {}

    public static boolean isDualWielding(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offHand = entity.getOffhandItem();
        if (mainHand.isEmpty() || offHand.isEmpty()) {
            return false;
        }

        SwordMetadataRegistry.SwordMetadata mainMeta = SwordMetadataRegistry.getMetadata(mainHand.getItem());
        SwordMetadataRegistry.SwordMetadata offMeta = SwordMetadataRegistry.getMetadata(offHand.getItem());
        if (mainMeta == null || offMeta == null) {
            return false;
        }

        if (!mainMeta.isDualWielding() || !offMeta.isDualWielding()) {
            return false;
        }

        String mainStyle = mainMeta.getStyleId();
        String offStyle = offMeta.getStyleId();
        if (mainStyle == null || offStyle == null || mainStyle.isEmpty() || offStyle.isEmpty()) {
            return false;
        }

        return mainStyle.equals(offStyle);
    }
}
