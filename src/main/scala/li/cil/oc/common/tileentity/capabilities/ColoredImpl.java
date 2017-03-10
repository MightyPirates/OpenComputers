package li.cil.oc.common.tileentity.capabilities;

import li.cil.oc.api.tileentity.Colored;
import net.minecraft.nbt.NBTTagInt;

public class ColoredImpl implements Colored {
    public interface ColoredHost {
        default void onColorChanged() {
        }
    }

    // ----------------------------------------------------------------------- //
    // Persisted data.

    private int color = 0;

    // ----------------------------------------------------------------------- //
    // Computed data.

    private final ColoredHost host;

    // ----------------------------------------------------------------------- //

    public ColoredImpl(final ColoredHost host) {
        this.host = host;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public boolean setColor(final int value) {
        if (value == color) {
            return false;
        }
        color = value;
        host.onColorChanged();
        return true;
    }

    @Override
    public boolean consumesDye() {
        return false;
    }

    @Override
    public boolean controlsConnectivity() {
        return false;
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagInt serializeNBT() {
        return new NBTTagInt(color);
    }

    @Override
    public void deserializeNBT(final NBTTagInt nbt) {
        color = nbt.getInt();
    }
}
