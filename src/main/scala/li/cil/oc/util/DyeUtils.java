package li.cil.oc.util;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

public final class DyeUtils {
    public static EnumDyeColor dyeFromTier(final int tier) {
        return TIER_TO_DYE[MathHelper.clamp(tier, 0, TIER_TO_DYE.length - 1)];
    }

    @Nullable
    public static EnumDyeColor dyeFromName(final String dyeName) {
        if (NAME_TO_DYE.containsKey(dyeName)) {
            return EnumDyeColor.values()[NAME_TO_DYE.get(dyeName)];
        }
        return null;
    }

    public static String nameFromDye(final EnumDyeColor dyeColor) {
        return DYE_TO_NAME[dyeColor.ordinal()];
    }

    public static int rgbFromDye(final EnumDyeColor dyeColor) {
        return DYE_TO_RGB.get(dyeColor.ordinal());
    }

    public static int rgbFromDyeName(final String dyeName) {
        if (NAME_TO_DYE.containsKey(dyeName)) {
            return rgbFromDye(EnumDyeColor.values()[NAME_TO_DYE.get(dyeName)]);
        }
        return 0xFF00FF;
    }

    @Nullable
    public static String findDyeName(final ItemStack stack) {
        for (final String dyeName : DYE_TO_NAME) {
            final NonNullList<ItemStack> ores = OreDictionary.getOres(dyeName);
            for (final ItemStack ore : ores) {
                if (OreDictionary.itemMatches(stack, ore, false)) {
                    return dyeName;
                }
            }
        }

        return null;
    }

    public static EnumDyeColor findDye(final ItemStack stack) {
        final String dyeName = findDyeName(stack);
        if (dyeName != null) {
            return EnumDyeColor.values()[NAME_TO_DYE.get(dyeName)];
        }

        return EnumDyeColor.MAGENTA;
    }

    public static boolean isDye(final ItemStack stack) {
        return findDyeName(stack) != null;
    }

    // ----------------------------------------------------------------------- //

    private static final String[] DYE_TO_NAME = new String[]{
            "dyeBlack",
            "dyeRed",
            "dyeGreen",
            "dyeBrown",
            "dyeBlue",
            "dyePurple",
            "dyeCyan",
            "dyeLightGray",
            "dyeGray",
            "dyePink",
            "dyeLime",
            "dyeYellow",
            "dyeLightBlue",
            "dyeMagenta",
            "dyeOrange",
            "dyeWhite"
    };
    private static final EnumDyeColor[] TIER_TO_DYE = new EnumDyeColor[]{
            EnumDyeColor.SILVER,
            EnumDyeColor.YELLOW,
            EnumDyeColor.CYAN,
            EnumDyeColor.MAGENTA

    };
    private static final TIntIntMap DYE_TO_RGB = new TIntIntHashMap();
    private static final TObjectIntMap<String> NAME_TO_DYE = new TObjectIntHashMap<>();

    static {
        DYE_TO_RGB.put(EnumDyeColor.BLACK.ordinal(), 0x444444);
        DYE_TO_RGB.put(EnumDyeColor.RED.ordinal(), 0xB3312C);
        DYE_TO_RGB.put(EnumDyeColor.GREEN.ordinal(), 0x339911);
        DYE_TO_RGB.put(EnumDyeColor.BROWN.ordinal(), 0x51301A);
        DYE_TO_RGB.put(EnumDyeColor.BLUE.ordinal(), 0x6666FF);
        DYE_TO_RGB.put(EnumDyeColor.PURPLE.ordinal(), 0x7B2FBE);
        DYE_TO_RGB.put(EnumDyeColor.CYAN.ordinal(), 0x66FFFF);
        DYE_TO_RGB.put(EnumDyeColor.SILVER.ordinal(), 0xABABAB);
        DYE_TO_RGB.put(EnumDyeColor.GRAY.ordinal(), 0x666666);
        DYE_TO_RGB.put(EnumDyeColor.PINK.ordinal(), 0xD88198);
        DYE_TO_RGB.put(EnumDyeColor.LIME.ordinal(), 0x66FF66);
        DYE_TO_RGB.put(EnumDyeColor.YELLOW.ordinal(), 0xFFFF66);
        DYE_TO_RGB.put(EnumDyeColor.LIGHT_BLUE.ordinal(), 0xAAAAFF);
        DYE_TO_RGB.put(EnumDyeColor.MAGENTA.ordinal(), 0xC354CD);
        DYE_TO_RGB.put(EnumDyeColor.ORANGE.ordinal(), 0xEB8844);
        DYE_TO_RGB.put(EnumDyeColor.WHITE.ordinal(), 0xF0F0F0);

        for (int dyeId = 0; dyeId < DYE_TO_NAME.length; dyeId++) {
            NAME_TO_DYE.put(DYE_TO_NAME[dyeId], dyeId);
        }
    }

    // ----------------------------------------------------------------------- //

    private DyeUtils() {
    }
}
