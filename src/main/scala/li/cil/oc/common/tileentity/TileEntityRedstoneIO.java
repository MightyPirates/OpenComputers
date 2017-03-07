package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.tileentity.traits.RedstoneAwareImpl;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public final class TileEntityRedstoneIO extends AbstractTileEntityEnvironmentHost implements RedstoneAwareImpl.RedstoneAwareHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final li.cil.oc.server.component.Redstone.Vanilla environment = new li.cil.oc.server.component.Redstone.Vanilla(this);
    private final RedstoneAwareImpl redstone = new RedstoneAwareImpl(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String REDSTONE_TAG = "redstone";

    // Signal names.
    private static final String REDSTONE_CHANGED_SIGNAL = "redstone.changed";

    // ----------------------------------------------------------------------- //

    public TileEntityRedstoneIO() {
        environment.wakeNeighborsOnly_$eq(false);
        redstone.setOutputEnabled(true);
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntityEnvironmentHost

    @Override
    protected Environment getEnvironment() {
        return environment;
    }

    // ----------------------------------------------------------------------- //
    // RedstoneAwareHost

    @Override
    public void onRedstoneInputChanged(final EnumFacing side, final int oldValue, final int newValue) {
        final Node node = environment.getNode();
        if (node != null) {
            node.sendToNeighbors(REDSTONE_CHANGED_SIGNAL, side, oldValue, newValue);
        }
    }

    @Override
    public void onRedstoneOutputChanged(final EnumFacing side, final int oldValue, final int newValue) {
    }

    @Override
    public void onRedstoneOutputEnabledChanged() {
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        nbt.setTag(REDSTONE_TAG, redstone.serializeNBT());
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        redstone.deserializeNBT((NBTTagCompound) nbt.getTag(REDSTONE_TAG));
    }
}
