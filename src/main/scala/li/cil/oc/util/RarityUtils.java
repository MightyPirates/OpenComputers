package li.cil.oc.util;

import net.minecraft.item.EnumRarity;
import net.minecraft.util.math.MathHelper;

public final class RarityUtils {
    private static final EnumRarity[] TIER_TO_RARITY = new EnumRarity[]{
            EnumRarity.COMMON,
            EnumRarity.UNCOMMON,
            EnumRarity.RARE,
            EnumRarity.EPIC
    };

    public static EnumRarity fromTier(final int tier) {
        return TIER_TO_RARITY[MathHelper.clamp(tier, 0, TIER_TO_RARITY.length - 1)];
    }

    // ----------------------------------------------------------------------- //

    private RarityUtils() {
    }
}
