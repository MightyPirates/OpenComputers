package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnergyNode;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractTileEntityNodeContainer;
import li.cil.oc.common.tileentity.traits.ComparatorOutputOverride;
import li.cil.oc.common.tileentity.traits.NeighborBlockChangeListener;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The capacitor provides a varying amount of energy storage, depending on
 * the number of (directly and indirectly) adjacent other capacitor blocks.
 * <p>
 * Network topology consists of a single network-reachable node providing
 * the capacitor's device information and managing its stored energy.
 */
public final class TileEntityCapacitor extends AbstractTileEntitySingleNodeContainer implements ComparatorOutputOverride, NeighborBlockChangeListener {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainerCapacitor environment = new NodeContainerCapacitor(this);

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        scheduleCapacityUpdate();
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
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return environment;
    }

    // ----------------------------------------------------------------------- //
    // ComparatorOutputOverride

    @Override
    public int getComparatorValue() {
        final EnergyNode connector = (EnergyNode) environment.getNode();
        if (connector != null) {
            return (int) Math.round(15 * connector.getEnergyStored() / connector.getEnergyCapacity());
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

        final double baseBufferSize = Settings.get().bufferCapacitor;
        final double directAdjacencyBonus = Settings.get().bufferCapacitorAdjacencyBonus * getCapacitors(getDirectNeighbors()).count();
        final double indirectAdjacencyBonus = Settings.get().bufferCapacitorAdjacencyBonus * getCapacitors(getIndirectNeighbors()).count();

        final EnergyNode connector = (EnergyNode) getNodeContainer().getNode();
        assert connector != null : "updateCapacity called on client side? Don't.";
        connector.setEnergyCapacity(baseBufferSize + directAdjacencyBonus + indirectAdjacencyBonus);
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

    private static final class NodeContainerCapacitor extends AbstractTileEntityNodeContainer implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Power);
            DEVICE_INFO.put(DeviceAttribute.Description, "Battery");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor());
            DEVICE_INFO.put(DeviceAttribute.Product, "CapBank3x");
            DEVICE_INFO.put(DeviceAttribute.Capacity, String.valueOf(getMaxCapacity()));
        }

        NodeContainerCapacitor(final TileEntity host) {
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
            return Settings.get().bufferCapacitor + Settings.get().bufferCapacitorAdjacencyBonus * (6 + 6 / 2);
        }
    }
}
