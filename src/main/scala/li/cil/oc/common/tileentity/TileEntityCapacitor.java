package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.network.AbstractEnvironment;
import li.cil.oc.common.tileentity.traits.NeighborBlockChangeListener;
import li.cil.oc.common.tileentity.traits.ComparatorOutputOverride;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class TileEntityCapacitor extends AbstractTileEntitySingleEnvironment implements ComparatorOutputOverride, NeighborBlockChangeListener {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final EnvironmentCapacitor environment = new EnvironmentCapacitor(this);

    // ----------------------------------------------------------------------- //
    // AbstractTileEntityEnvironmentHost

    @Override
    protected Environment getEnvironment() {
        return environment;
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void dispose() {
        super.dispose();
        if (isServer()) {
            getCapacitors(getIndirectNeighbors()).
                    forEach(TileEntityCapacitor::scheduleCapacityUpdate);
        }
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        scheduleCapacityUpdate();
    }

    // ----------------------------------------------------------------------- //
    // ComparatorOutputOverride

    @Override
    public int getComparatorValue() {
        final Connector connector = (Connector) environment.getNode();
        if (connector != null) {
            return (int) Math.round(15 * connector.getLocalBuffer() / connector.getLocalBufferSize());
        } else {
            return 0;
        }
    }

    // ----------------------------------------------------------------------- //
    // NeighborBlockChangeListener

    @Override
    public void onBlockChanged(final BlockPos neighborPos) {
        scheduleCapacityUpdate();
    }

    // ----------------------------------------------------------------------- //

    private void scheduleCapacityUpdate() {
        final IThreadListener thread = getWorld().getMinecraftServer();
        if (thread != null) {
            thread.addScheduledTask(this::updateCapacity);
        }
    }

    private void updateCapacity() {
        if (isInvalid() || !hasWorld()) { // Disposed in the meantime? E.g. when multiple capacitors get unloaded.
            return;
        }

        final double baseBufferSize = Settings.get().bufferCapacitor();
        final double directAdjacencyBonus = Settings.get().bufferCapacitorAdjacencyBonus() * getCapacitors(getDirectNeighbors()).count();
        final double indirectAdjacencyBonus = Settings.get().bufferCapacitorAdjacencyBonus() * getCapacitors(getIndirectNeighbors()).count();

        final Connector connector = (Connector) getEnvironment().getNode();
        assert connector != null : "updateCapacity called on client side? Don't.";
        connector.setLocalBufferSize(baseBufferSize + directAdjacencyBonus + indirectAdjacencyBonus);
    }

    private Stream<TileEntityCapacitor> getCapacitors(final Stream<BlockPos> positions) {
        final World world = getWorld();
        return positions.
                filter(world::isBlockLoaded).
                map(world::getTileEntity).
                filter(tileEntity -> tileEntity instanceof TileEntityCapacitor).
                map(tileEntity -> (TileEntityCapacitor) tileEntity);
    }

    private Stream<BlockPos> getDirectNeighbors() {
        return Arrays.stream(EnumFacing.VALUES).map(facing -> getPos().offset(facing, 1));
    }

    private Stream<BlockPos> getIndirectNeighbors() {
        return Arrays.stream(EnumFacing.VALUES).map(facing -> getPos().offset(facing, 2));
    }

    // ----------------------------------------------------------------------- //

    private static final class EnvironmentCapacitor extends AbstractEnvironment implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Power);
            DEVICE_INFO.put(DeviceAttribute.Description, "Battery");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor());
            DEVICE_INFO.put(DeviceAttribute.Product, "CapBank3x");
            DEVICE_INFO.put(DeviceAttribute.Capacity, String.valueOf(getMaxCapacity()));
        }

        EnvironmentCapacitor(final EnvironmentHost host) {
            super(host);
        }

        @Override
        protected Node createNode() {
            // Start with maximum theoretical capacity, gets reduced after validation.
            // This is done so that we don't lose energy while loading.
            return Network.newNode(this, Visibility.NETWORK).withConnector(getMaxCapacity()).create();
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        private static double getMaxCapacity() {
            return Settings.get().bufferCapacitor() + Settings.get().bufferCapacitorAdjacencyBonus() * (6 + 6 / 2);
        }
    }
}
