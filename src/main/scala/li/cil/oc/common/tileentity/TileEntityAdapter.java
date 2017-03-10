package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.network.AbstractEnvironment;
import li.cil.oc.common.tileentity.traits.BlockActivationListener;
import li.cil.oc.common.tileentity.traits.NeighborBlockChangeListener;
import li.cil.oc.common.tileentity.traits.OpenSides;
import li.cil.oc.common.tileentity.traits.NeighborTileEntityChangeListener;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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

public final class TileEntityAdapter extends AbstractTileEntitySingleEnvironment implements Adapter, BlockActivationListener, NeighborBlockChangeListener, NeighborTileEntityChangeListener, Analyzable, ITickable, OpenSides.OpenSidesHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final Environment environment = new EnvironmentAdapter(this);
    private final OpenSides sides = new OpenSides(this, true);
    private final NBTTagCompound[] blockData = new NBTTagCompound[EnumFacing.VALUES.length];
    private final EnvironmentBlock[] blockEnvironments = new EnvironmentBlock[EnumFacing.VALUES.length];

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String SIDES_TAG = "sides";
    private static final String BLOCKS_TAG = "blocks";

    private final List<ITickable> updatingEnvironments = new ArrayList<>();

    // ----------------------------------------------------------------------- //

    public TileEntityAdapter() {
        for (int i = 0; i < blockData.length; i++) {
            blockData[i] = new NBTTagCompound();
        }
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleEnvironment

    @Override
    protected Environment getEnvironment() {
        return environment;
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
        sides.deserializeNBT((NBTTagByte) nbt.getTag(SIDES_TAG));
        final NBTTagList blocks = nbt.getTagList(BLOCKS_TAG, NBT.TAG_COMPOUND);
        if (blocks.tagCount() == blockData.length) {
            for (int i = 0; i < blockData.length; i++) {
                blockData[i] = blocks.getCompoundTagAt(i);
            }
        } else if (blocks.tagCount() > 0) {
            OpenComputers.log().warn("blockData length mismatch. Not loading data.");
        }
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        nbt.setTag(SIDES_TAG, sides.serializeNBT());

        for (final EnumFacing side : EnumFacing.VALUES) {
            writeToBlockData(side);
        }

        final NBTTagList blocks = new NBTTagList();
        for (final NBTTagCompound data : blockData) {
            blocks.appendTag(data);
        }
        nbt.setTag(BLOCKS_TAG, blocks);
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
    // OpenSidesHost

    @Override
    public void onSideOpenChanged(final EnumFacing side) {
        checkEnvironment(side);

        final World world = getWorld();
        final SoundEvent sound = sides.isSideOpen(side) ? SoundEvents.BLOCK_PISTON_EXTEND : SoundEvents.BLOCK_PISTON_CONTRACT;
        final Vec3d position = getHostPosition();
        world.playSound(null, position.xCoord, position.yCoord, position.zCoord, sound, SoundCategory.BLOCKS, 0.5f, world.rand.nextFloat() * 0.25f + 0.7f);
        world.notifyNeighborsOfStateChange(getPos(), getBlockType(), false);

//      ServerPacketSender.sendAdapterState(this)
//      getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        return sides.onActivated(player, hand, getPos(), side);
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

    private void initialize() {
        if (isInvalid() || !hasWorld()) {
            return;
        }

        final Node node = environment.getNode();
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

        final Node node = environment.getNode();
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
                return; // Nothing to change, current environment is still valid.
            }
            disposeEnvironment(side);
            clearBlockData(side);
        }

        final EnvironmentBlock blockEnvironment = driver.createEnvironment(world, blockPos, side.getOpposite());
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
            final EnvironmentBlock blockEnvironment = blockEnvironments[side.ordinal()];

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

    private static final class EnvironmentAdapter extends AbstractEnvironment implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Bus);
            DEVICE_INFO.put(DeviceAttribute.Description, "Adapter");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor());
            DEVICE_INFO.put(DeviceAttribute.Product, "Multiplug Ext.1");
        }

        // ----------------------------------------------------------------------- //

        EnvironmentAdapter(final EnvironmentHost host) {
            super(host);
        }

        // ----------------------------------------------------------------------- //
        // AbstractEnvironment

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
