package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.network.AbstractTileEntityNodeContainer;
import li.cil.oc.common.tileentity.traits.*;
import li.cil.oc.util.BlockPosUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The adapter maintains node containers for blocks ({@link NodeContainerBlock}) for
 * any block adjacent to the adapter block.
 * <p>
 * Network topology consists of the adapter block itself having a network-reachable
 * node providing the adapter's device information. All created block node containers
 * are directly connected to this single node.
 */
public final class TileEntityAdapter extends AbstractTileEntitySingleNodeContainer implements Adapter, Analyzable, BlockActivationListener, ITickable, NeighborBlockChangeListener, NeighborTileEntityChangeListener, OpenSides.OpenSidesHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NBTTagCompound[] blockData = new NBTTagCompound[EnumFacing.VALUES.length];
    private final NodeContainerBlock[] blockEnvironments = new NodeContainerBlock[EnumFacing.VALUES.length];
    private final NodeContainer nodeContainer = new NodeContainerAdapter(this);
    private final OpenSides sides = new OpenSides(this, true);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_BLOCKS = "blocks";
    private static final String TAG_SIDES = "sides";

    private final NodeContainerHostTileEntity nodeContainerHost = new NodeContainerHostTileEntity(this);
    private final List<ITickable> updatingEnvironments = new ArrayList<>();

    // ----------------------------------------------------------------------- //

    public TileEntityAdapter() {
        for (int i = 0; i < blockData.length; i++) {
            blockData[i] = new NBTTagCompound();
        }
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
//        final IThreadListener thread = getWorld().getMinecraftServer();
//        if (thread != null) {
//            thread.addScheduledTask(this::initialize);
//        }
        initialize(); // TODO See if this is fine. If not, use the above.
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void dispose() {
        super.dispose();
        for (final EnumFacing side : EnumFacing.VALUES) {
            disposeEnvironment(side);
        }
    }

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        final NBTTagList blocks = nbt.getTagList(TAG_BLOCKS, NBT.TAG_COMPOUND);
        if (blocks.tagCount() == blockData.length) {
            for (int i = 0; i < blockData.length; i++) {
                blockData[i] = blocks.getCompoundTagAt(i);
            }
        } else if (blocks.tagCount() > 0) {
            OpenComputers.log().warn("blockData length mismatch. Not loading data.");
        }
        sides.deserializeNBT((NBTTagByte) nbt.getTag(TAG_SIDES));
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        for (final EnumFacing side : EnumFacing.VALUES) {
            writeToBlockData(side);
        }
        final NBTTagList blocks = new NBTTagList();
        for (final NBTTagCompound data : blockData) {
            blocks.appendTag(data);
        }
        nbt.setTag(TAG_BLOCKS, blocks);
        nbt.setTag(TAG_SIDES, sides.serializeNBT());
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return nodeContainer;
    }

    // ----------------------------------------------------------------------- //
    // Analyzable

    @Nullable
    @Override
    public Node[] onAnalyze(final EntityPlayer player, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final Node[] nodes = new Node[blockEnvironments.length];
        for (int i = 0; i < nodes.length; i++) {
            if (blockEnvironments[i] != null) {
                nodes[i] = blockEnvironments[i].getNode();
            }
        }
        return nodes;
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        return sides.onActivated(player, hand, getPos(), side);
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        for (int i = updatingEnvironments.size() - 1; i >= 0; i--) {
            // As with component inventories, backwards and with the extra check
            // to allow environments to suicide in their update without it
            // breaking the update loop.
            if (i < updatingEnvironments.size()) {
                updatingEnvironments.get(i).update();
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // NeighborBlockChangeListener

    @Override
    public void onBlockChanged(final BlockPos neighborPos) {
        final BlockPos direction = neighborPos.subtract(getPos());
        final EnumFacing side = EnumFacing.getFacingFromVector(direction.getX(), direction.getY(), direction.getZ());
        checkEnvironment(side);
    }

    // ----------------------------------------------------------------------- //
    // NeighborTileEntityChangeListener

    @Override
    public void onTileEntityChanged(final BlockPos neighborPos) {
        final BlockPos direction = neighborPos.subtract(getPos());
        final EnumFacing side = EnumFacing.getFacingFromVector(direction.getX(), direction.getY(), direction.getZ());
        checkEnvironment(side);
    }

    // ----------------------------------------------------------------------- //
    // OpenSidesHost

    @Override
    public void onSideOpenChanged(final EnumFacing side) {
        checkEnvironment(side);

        final World world = getWorld();
        final SoundEvent sound = sides.isSideOpen(side) ? SoundEvents.BLOCK_PISTON_EXTEND : SoundEvents.BLOCK_PISTON_CONTRACT;
        final Vec3d position = BlockPosUtils.getCenter(getPos());
        world.playSound(null, position.xCoord, position.yCoord, position.zCoord, sound, SoundCategory.BLOCKS, 0.5f, world.rand.nextFloat() * 0.25f + 0.7f);
        world.notifyNeighborsOfStateChange(getPos(), getBlockType(), false);

//      ServerPacketSender.sendAdapterState(this)
//      getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
    }

    // ----------------------------------------------------------------------- //

    private void initialize() {
        if (isInvalid() || !hasWorld()) {
            return;
        }

        final Node node = nodeContainer.getNode();
        if (node == null) {
            return;
        }

        Network.joinNewNetwork(node);
        for (final EnumFacing side : EnumFacing.VALUES) {
            checkEnvironment(side);
        }
    }

    private void checkEnvironment(final EnumFacing side) {
        if (!sides.isSideOpen(side)) {
            disposeEnvironment(side);
            clearBlockData(side);
            return;
        }

        final Node node = nodeContainer.getNode();
        if (node == null || node.getNetwork() == null) {
            return;
        }

        final World world = getWorld();
        final BlockPos blockPos = getPos().offset(side);
        if (!world.isBlockLoaded(blockPos)) {
            writeToBlockData(side);
            disposeEnvironment(side);
            // Don't clear data. We might still need it when the block gets loaded.
            return;
        }

        final DriverBlock driver = Driver.driverFor(world, blockPos, side.getOpposite());
        if (driver == null) {
            disposeEnvironment(side);
            clearBlockData(side);
            return;
        }

        if (blockEnvironments[side.ordinal()] != null) {
            if (driver.isValid(world, blockPos, side.getOpposite(), blockEnvironments[side.ordinal()])) {
                return; // Nothing to change, current nodeContainer is still valid.
            }
            disposeEnvironment(side);
            clearBlockData(side);
        }

        final NodeContainerBlock blockEnvironment = driver.createEnvironment(world, blockPos, side.getOpposite(), nodeContainerHost);
        if (blockEnvironment == null) {
            return;
        }

        blockEnvironments[side.ordinal()] = blockEnvironment;

        if (blockEnvironment instanceof ITickable) {
            updatingEnvironments.add((ITickable) blockEnvironment);
        }

        readFromBlockData(side);

        final Node blockNode = blockEnvironment.getNode();
        if (blockNode != null) {
            node.connect(blockNode);
        }
    }

    private void disposeEnvironment(final EnumFacing side) {
        if (blockEnvironments[side.ordinal()] != null) {
            final NodeContainerBlock blockEnvironment = blockEnvironments[side.ordinal()];

            blockEnvironments[side.ordinal()] = null;

            if (blockEnvironment instanceof ITickable) {
                updatingEnvironments.remove(blockEnvironment);
            }

            final Node blockNode = blockEnvironment.getNode();
            if (blockNode != null) {
                blockNode.remove();
            }

            blockEnvironment.onDispose();
        }
    }

    private void readFromBlockData(final EnumFacing side) {
        if (blockEnvironments[side.ordinal()] != null) {
            blockEnvironments[side.ordinal()].deserializeNBT(blockData[side.ordinal()]);
        }
    }

    private void writeToBlockData(final EnumFacing side) {
        if (blockEnvironments[side.ordinal()] != null) {
            blockData[side.ordinal()] = blockEnvironments[side.ordinal()].serializeNBT();
        }
    }

    private void clearBlockData(final EnumFacing side) {
        blockData[side.ordinal()] = new NBTTagCompound();
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerAdapter extends AbstractTileEntityNodeContainer implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Bus);
            DEVICE_INFO.put(DeviceAttribute.Description, "Adapter");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor());
            DEVICE_INFO.put(DeviceAttribute.Product, "Multiplug Ext.1");
        }

        // ----------------------------------------------------------------------- //

        NodeContainerAdapter(final TileEntity host) {
            super(host);
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NETWORK).create();
        }

        // ----------------------------------------------------------------------- //
        // DeviceInfo

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }
    }
}
