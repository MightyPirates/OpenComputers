package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.tileentity.capabilities.RedstoneAwareImpl;
import li.cil.oc.common.tileentity.traits.LocationTileEntityProxy;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public final class TileEntityRedstoneIO extends AbstractTileEntitySingleNodeContainer implements LocationTileEntityProxy, RedstoneAwareImpl.RedstoneAwareHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final li.cil.oc.server.component.Redstone.Vanilla environment = new li.cil.oc.server.component.Redstone.Vanilla(this);
    private final RedstoneAwareImpl redstone = new RedstoneAwareImpl(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_REDSTONE = "redstone";

    // Signal names.
    private static final String REDSTONE_CHANGED_SIGNAL = "redstone.changed";

    // ----------------------------------------------------------------------- //

    public TileEntityRedstoneIO() {
        environment.wakeNeighborsOnly_$eq(false);
        redstone.setOutputEnabled(true);
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        redstone.scheduleInputUpdate();
    }

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        nbt.setTag(TAG_REDSTONE, redstone.serializeNBT());
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        redstone.deserializeNBT((NBTTagCompound) nbt.getTag(TAG_REDSTONE));
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return environment;
    }

    // ----------------------------------------------------------------------- //
    // LocationTileEntityProxy

    @Override
    public TileEntity getTileEntity() {
        return this;
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
}
