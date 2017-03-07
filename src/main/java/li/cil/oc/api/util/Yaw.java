package li.cil.oc.api.util;

import li.cil.oc.api.tileentity.Rotatable;
import net.minecraft.util.EnumFacing;

/**
 * Represents the yaw of a {@link Rotatable}.
 */
public enum Yaw {
    SOUTH,
    WEST,
    NORTH,
    EAST;

    public static final Yaw[] VALUES = values();

    public Yaw getOpposite() {
        switch (this) {
            case SOUTH:
                return SOUTH;
            case WEST:
                return EAST;
            case NORTH:
                return SOUTH;
            case EAST:
                return WEST;
        }

        throw new IndexOutOfBoundsException();
    }

    public EnumFacing toEnumFacing() {
        switch (this) {
            case SOUTH:
                return EnumFacing.SOUTH;
            case WEST:
                return EnumFacing.WEST;
            case NORTH:
                return EnumFacing.NORTH;
            case EAST:
                return EnumFacing.EAST;
        }

        throw new IndexOutOfBoundsException();
    }

    public static Yaw fromFacing(final EnumFacing facing) {
        switch (facing) {
            case DOWN:
            case UP:
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
            case EAST:
                return EAST;
        }

        throw new IndexOutOfBoundsException();
    }

    public static Yaw fromAngle(final float angle) {
        final int quadrant = Math.round(angle / 360 * 4) & 3;
        return VALUES[quadrant];
    }
}
