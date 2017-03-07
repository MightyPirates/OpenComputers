package li.cil.oc.util;

import li.cil.oc.api.util.Pitch;
import li.cil.oc.api.util.Yaw;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.ArrayUtils;

public final class RotationUtils {
    public static EnumFacing toLocal(final Pitch pitch, final Yaw yaw, final EnumFacing value) {
        return TRANSLATIONS[pitch.ordinal()][yaw.ordinal()][value.ordinal()];
    }

    public static EnumFacing toGlobal(final Pitch pitch, final Yaw yaw, final EnumFacing value) {
        return INVERSE_TRANSLATION[pitch.ordinal()][yaw.ordinal()][value.ordinal()];
    }

    // ----------------------------------------------------------------------- //

    /**
     * Translates facings based on a block's pitch and yaw. The base
     * forward direction is facing south with no pitch. The outer array is for
     * the three different pitch states, the inner for the four different yaw
     * states.
     */
    private static final EnumFacing[][][] TRANSLATIONS = {
            // Pitch = Down
            {
                    {EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.DOWN, EnumFacing.UP, EnumFacing.WEST, EnumFacing.EAST}, // Yaw = South
                    {EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP, EnumFacing.DOWN}, // Yaw = West
                    {EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.UP, EnumFacing.DOWN, EnumFacing.EAST, EnumFacing.WEST}, // Yaw = North
                    {EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.DOWN, EnumFacing.UP} // Yaw = East
            },
            // Pitch = Up
            {
                    {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN, EnumFacing.WEST, EnumFacing.EAST}, // Yaw = South
                    {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.UP}, // Yaw = West
                    {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.DOWN, EnumFacing.UP, EnumFacing.EAST, EnumFacing.WEST}, // Yaw = North
                    {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN} // Yaw = East
            },
            // Pitch = Forward
            {
                    {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST}, // Yaw = South
                    {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.NORTH}, // Yaw = West
                    {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST}, // Yaw = North
                    {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH} // Yaw = East
            }
    };
    private static final EnumFacing[][][] INVERSE_TRANSLATION = new EnumFacing[3][4][6];

    static {
        for (final Pitch pitch : Pitch.VALUES) {
            for (final Yaw yaw : Yaw.VALUES) {
                final EnumFacing[] regular = TRANSLATIONS[pitch.ordinal()][yaw.ordinal()];
                final EnumFacing[] inverse = INVERSE_TRANSLATION[pitch.ordinal()][yaw.ordinal()];
                for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                    final EnumFacing facing = EnumFacing.VALUES[i];
                    final int index = ArrayUtils.indexOf(regular, facing);
                    inverse[i] = EnumFacing.getFront(index);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private RotationUtils() {
    }
}
