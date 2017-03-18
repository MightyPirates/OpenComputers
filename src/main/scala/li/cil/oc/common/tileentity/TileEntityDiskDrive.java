package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.network.AbstractTileEntityNodeContainer;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Sound;
import li.cil.oc.common.inventory.ComponentManager;
import li.cil.oc.common.inventory.ItemHandlerComponents;
import li.cil.oc.common.tileentity.capabilities.RotatableImpl;
import li.cil.oc.common.tileentity.traits.BlockActivationListener;
import li.cil.oc.common.tileentity.traits.ComparatorOutputOverride;
import li.cil.oc.common.tileentity.traits.ItemHandlerHostTileEntityProxy;
import li.cil.oc.common.tileentity.traits.NodeContainerHostTileEntity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The disk drive is a single-slot item component hosting block which only
 * accepts floppy disk items.
 * <p>
 * Network topology consists of a single, network-visible node providing
 * the disk drive's device information and component callbacks. Nodes created
 * for inserted floppy disks are directly connected to this single node, and
 * their component part is forced into a network-visible state.
 */
public final class TileEntityDiskDrive extends AbstractTileEntitySingleNodeContainer implements Analyzable, BlockActivationListener, ComparatorOutputOverride, ComponentManager.ComponentInventoryHost, ItemHandlerHostTileEntityProxy, RotatableImpl.RotatableHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainer nodeContainer = new NodeContainerDiskDrive(this);
    private final ItemHandlerDiskDrive inventory = new ItemHandlerDiskDrive(this);
    private final ComponentManager components = new ComponentManager(this, new NodeContainerHostTileEntity(this));
    private final RotatableImpl rotatable = new RotatableImpl(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_COMPONENTS = "components";
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_ROTATABLE = "rotatable";

    private static final int FLOPPY_SLOT = 0;

    // Used on client side to check whether to render disk activity indicators.
    public long lastAccess;

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        components.initialize();
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void dispose() {
        super.dispose();
        components.dispose();
    }

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        inventory.deserializeNBT((NBTTagList) nbt.getTag(TAG_INVENTORY));
        components.deserializeNBT((NBTTagList) nbt.getTag(TAG_COMPONENTS));
        rotatable.deserializeNBT((NBTTagByte) nbt.getTag(TAG_ROTATABLE));
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        nbt.setTag(TAG_INVENTORY, inventory.serializeNBT());
        nbt.setTag(TAG_COMPONENTS, components.serializeNBT());
        nbt.setTag(TAG_ROTATABLE, rotatable.serializeNBT());
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
        final NodeContainer nodeContainer = components.getEnvironment(FLOPPY_SLOT);
        if (nodeContainer != null) {
            return new Node[]{nodeContainer.getNode()};
        }
        return new Node[0];
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final ItemStack heldItem = player.getHeldItem(hand);
        final boolean isDiskInDrive = !inventory.getStackInSlot(FLOPPY_SLOT).isEmpty();
        final boolean isHoldingDisk = inventory.isItemValidForSlot(FLOPPY_SLOT, heldItem);
        if (isDiskInDrive) {
            if (!world.isRemote) {
                dropSlot(FLOPPY_SLOT, 1, rotatable.getFacing());
            }
        }
        if (isHoldingDisk) {
            // Insert the disk.
            inventory.setStackInSlot(FLOPPY_SLOT, player.inventory.decrStackSize(player.inventory.currentItem, 1));
        }
        return isDiskInDrive || isHoldingDisk;
    }

    // ----------------------------------------------------------------------- //
    // ComparatorOutputOverride

    @Override
    public int getComparatorValue() {
        return inventory.getStackInSlot(FLOPPY_SLOT).isEmpty() ? 0 : 15;
    }

    // ----------------------------------------------------------------------- //
    // ComponentInventoryHost

    @Override
    public Node getItemNode() {
        final Node node = nodeContainer.getNode();
        assert node != null : "getItemNode called on client? Don't.";
        return node;
    }

    @Override
    public IItemHandler getComponentItems() {
        return inventory;
    }

    // ----------------------------------------------------------------------- //
    // ItemHandlerHost

    @Override
    public void onItemAdded(final int slot, final ItemStack stack) {
        components.initializeComponent(slot, stack);

        final NodeContainer nodeContainer = components.getEnvironment(FLOPPY_SLOT);
        if (nodeContainer != null) {
            final Node node = nodeContainer.getNode();
            if (node instanceof ComponentNode) {
                final ComponentNode component = (ComponentNode) node;
                component.setVisibility(Visibility.NETWORK);
            }
        }
//      ServerPacketSender.sendFloppyChange(this, stack)
    }

    @Override
    public void onItemRemoved(final int slot, final ItemStack stack) {
        components.disposeComponent(slot, stack);
//      ServerPacketSender.sendFloppyChange(this)
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerDiskDrive extends AbstractTileEntityNodeContainer implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Disk);
            DEVICE_INFO.put(DeviceAttribute.Description, "Floppy disk drive");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor());
            DEVICE_INFO.put(DeviceAttribute.Product, "Spinner 520p1");
        }

        private final TileEntityDiskDrive diskDrive;

        NodeContainerDiskDrive(final TileEntityDiskDrive host) {
            super(host);
            this.diskDrive = host;
        }

        @Callback(doc = "function():boolean -- Checks whether some medium is currently in the drive.")
        public Object[] isEmpty(final Context context, final Arguments args) {
            return new Object[]{diskDrive.components.getEnvironment(FLOPPY_SLOT) == null};
        }

        @Callback(doc = "function([velocity:number]):boolean -- Eject the currently present medium from the drive.")
        public Object[] eject(final Context context, final Arguments args) {
            final double velocity = MathHelper.clamp(args.optDouble(0, 0), 0, 1);
            final ItemStack ejected = diskDrive.inventory.extractItem(FLOPPY_SLOT, 1, false);
            if (ejected.isEmpty()) {
                return new Object[]{false};
            }

            final World world = getLocation().getHostWorld();
            final EnumFacing facing = diskDrive.rotatable.getFacing();
            final Vec3d pos = getLocation().getHostPosition().addVector(facing.getFrontOffsetX(), facing.getFrontOffsetY(), facing.getFrontOffsetZ());
            final EntityItem entity = new EntityItem(world, pos.xCoord, pos.yCoord, pos.zCoord, ejected);
            entity.setPickupDelay(15);
            entity.addVelocity(
                    facing.getFrontOffsetX() * velocity,
                    facing.getFrontOffsetY() * velocity,
                    facing.getFrontOffsetZ() * velocity);
            world.spawnEntity(entity);
            return new Object[]{true};
        }

        // ----------------------------------------------------------------------- //
        // NodeContainer

        @Override
        public void onConnect(final Node node) {
            super.onConnect(node);
            if (node == getNode()) {
                diskDrive.components.connect();
            }
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NETWORK).withComponent("disk_drive").create();
        }

        // ----------------------------------------------------------------------- //
        // DeviceInfo

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }
    }

    private static final class ItemHandlerDiskDrive extends ItemHandlerComponents {
        ItemHandlerDiskDrive(final ItemHandlerHost host) {
            super(host, InventorySlots.diskDrive().length);
        }

        @Override
        public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
            return InventorySlots.isValidForSlot(InventorySlots.diskDrive(), slot, stack, TileEntityDiskDrive.class);
        }
    }
}
