package li.cil.oc.api.util;

import li.cil.oc.api.tileentity.Rotatable;
import net.minecraft.util.EnumFacing;

/**
 * Represents the pitch of a {@link Rotatable}.
 */
public enum Pitch {
    DOWN,
    UP,
    FORWARD;

    public static final Pitch[] VALUES = values();

    public Pitch getOpposite() {
        switch (this) {
            case DOWN:
                return UP;
            case UP:
                return DOWN;
            case FORWARD:
                return FORWARD;
        }

        throw new IndexOutOfBoundsException();
    }

    public EnumFacing toEnumFacing() {
        switch (this) {
            case DOWN:
                return EnumFacing.DOWN;
            case UP:
                return EnumFacing.UP;
            case FORWARD:
                return EnumFacing.NORTH;
        }

        throw new IndexOutOfBoundsException();
    }

    public static Pitch fromFacing(final EnumFacing facing) {
        switch (facing) {
            case DOWN:
                return DOWN;
            case UP:
                return UP;
            case NORTH:
            case SOUTH:
            case WEST:
            case EAST:
                return FORWARD;
        }

        throw new IndexOutOfBoundsException();
    }
}
