package li.cil.oc.common.tileentity.traits;

import net.minecraft.nbt.NBTTagByte;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * @author Vexatos, Sangar
 */
public final class OpenSides implements INBTSerializable<NBTTagByte> {
    public interface OpenSidesHost {
        void onSideOpenChanged(final EnumFacing side);
    }

    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final boolean[] openSides = new boolean[EnumFacing.VALUES.length];

    // ----------------------------------------------------------------------- //
    // Computed data.

    private final OpenSidesHost host;

    // ----------------------------------------------------------------------- //

    public OpenSides(final OpenSidesHost host, final boolean defaultState) {
        this.host = host;
        Arrays.fill(openSides, defaultState);
    }

    public OpenSides(final OpenSidesHost host) {
        this(host, false);
    }

    public boolean isSideOpen(@Nullable final EnumFacing side) {
        return side != null && openSides[side.ordinal()];
    }

    public void setSideOpen(final EnumFacing side, final boolean value) {
        if (value != openSides[side.ordinal()]) {
            openSides[side.ordinal()] = value;
            host.onSideOpenChanged(side);
        }
    }

    public void toggleSide(final EnumFacing side) {
        openSides[side.ordinal()] = !openSides[side.ordinal()];
        host.onSideOpenChanged(side);
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagByte serializeNBT() {
        int compressed = 0;
        for (int i = 0; i < openSides.length; i++) {
            if (openSides[i]) {
                compressed |= 1 << i;
            }
        }
        return new NBTTagByte((byte) compressed);
    }

    @Override
    public void deserializeNBT(final NBTTagByte nbt) {
        final int compressed = nbt.getByte() & 0xFF;
        for (int i = 0; i < openSides.length; i++) {
            openSides[i] = (compressed & (1 << i)) != 0;
        }
    }
}
