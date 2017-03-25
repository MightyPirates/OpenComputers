package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractNodeContainer;
import li.cil.oc.api.util.Location;
import li.cil.oc.common.GuiType;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.inventory.ComponentManager;
import li.cil.oc.common.inventory.ItemHandlerComponents;
import li.cil.oc.common.tileentity.capabilities.ColoredImpl;
import li.cil.oc.common.tileentity.capabilities.RotatableImpl;
import li.cil.oc.common.tileentity.traits.BlockActivationListener;
import li.cil.oc.common.tileentity.traits.ItemHandlerHostTileEntityProxy;
import li.cil.oc.common.tileentity.traits.LocationTileEntityProxy;
import li.cil.oc.common.tileentity.traits.MachineHostImpl;
import li.cil.oc.common.tileentity.traits.NodeContainerHostTileEntity;
import li.cil.oc.util.DyeUtils;
import li.cil.oc.util.GUIUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class TileEntityCase extends AbstractTileEntitySingleNodeContainer implements BlockActivationListener, ColoredImpl.ColoredHost, MachineHostImpl.Computer, ItemHandlerHostTileEntityProxy, ITickable, LocationTileEntityProxy, RotatableImpl.RotatableHost, ComponentManager.ComponentInventoryHost {
    protected abstract int getTier();

    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainer nodeContainer = new NodeContainerCase(this);
    private final ItemHandlerComponents inventory = new ItemHandlerCase(this, getTier());
    private final ComponentManager components = new ComponentManager(this, new NodeContainerHostTileEntity(this));
    private final MachineHostImpl machineHost = new MachineHostImpl(this, new NodeContainerHostTileEntity(this), inventory);
    private final ColoredImpl colored = new ColoredImpl(this);
    private final RotatableImpl rotatable = new RotatableImpl(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_COLORED = "colored";
    private static final String TAG_COMPONENTS = "components";
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_MACHINE = "machine";
    private static final String TAG_ROTATABLE = "rotatable";
    private static final String TAG_RUNNING = "running";

    // Used on client side to check whether to render disk activity/network indicators.
    private long lastFileSystemActivity = 0;
    private long lastNetworkActivity = 0;
    private boolean isRunning = false;

    // ----------------------------------------------------------------------- //

    public TileEntityCase() {
        colored.setColor(DyeUtils.rgbFromDye(DyeUtils.dyeFromTier(getTier())));
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        inventory.deserializeNBT((NBTTagList) nbt.getTag(TAG_INVENTORY));
        components.deserializeNBT((NBTTagList) nbt.getTag(TAG_COMPONENTS));
        colored.deserializeNBT((NBTTagInt) nbt.getTag(TAG_COLORED));
        rotatable.deserializeNBT((NBTTagByte) nbt.getTag(TAG_ROTATABLE));
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        nbt.setTag(TAG_INVENTORY, inventory.serializeNBT());
        nbt.setTag(TAG_COMPONENTS, components.serializeNBT());
        nbt.setTag(TAG_COLORED, colored.serializeNBT());
        nbt.setTag(TAG_ROTATABLE, rotatable.serializeNBT());
    }

    @Override
    protected void readFromNBTForServer(final NBTTagCompound nbt) {
        super.readFromNBTForServer(nbt);
        machineHost.deserializeNBT((NBTTagCompound) nbt.getTag(TAG_MACHINE));
    }

    @Override
    protected void writeToNBTForServer(final NBTTagCompound nbt) {
        super.writeToNBTForServer(nbt);
        nbt.setTag(TAG_MACHINE, machineHost.serializeNBT());
    }

    @Override
    protected void readFromNBTForClient(final NBTTagCompound nbt) {
        super.readFromNBTForClient(nbt);
        isRunning = nbt.getBoolean(TAG_RUNNING);
    }

    @Override
    protected void writeToNBTForClient(final NBTTagCompound nbt) {
        super.writeToNBTForClient(nbt);
        nbt.setBoolean(TAG_RUNNING, isRunning);
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return nodeContainer;
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (player.isSneaking()) {
            if (isServer()) {
                if (!machineHost.getMachine().isRunning()) {
                    machineHost.getMachine().start();
                }
            }
        } else {
            GUIUtils.openGUI(this, GuiType.Case());
        }
        return true;
    }

    // ----------------------------------------------------------------------- //
    // ComponentInventoryHost

    @Override
    public Node getItemNode() {
        return machineHost.getMachine().getNode();
    }

    @Override
    public IItemHandler getComponentItems() {
        return inventory;
    }

    // ----------------------------------------------------------------------- //
    // Computer

    @Override
    public void onRunningChanged() {
        isRunning = machineHost.getMachine().isRunning();
//    markDirty()
//    ServerPacketSender.sendComputerState(this)
//    getBlockType match {
//      case block: common.block.Case => getWorld.setBlockState(getPos, getWorld.getBlockState(getPos).withProperty(PropertyRunning.Running, Boolean.box(isRunning)))
//      case _ =>
//    }
    }

    // ----------------------------------------------------------------------- //
    // ItemHandlerHost

    @Override
    public void onItemAdded(final int slot, final ItemStack stack) {
        components.initializeComponent(slot, stack);
    }

    @Override
    public void onItemRemoved(final int slot, final ItemStack stack) {
        components.disposeComponent(slot, stack);

        if (isServer()) {
            final DriverItem driver = Driver.driverFor(stack);
            if (driver != null && Objects.equals(driver.slot(stack), Slot.CPU)) {
                machineHost.getMachine().stop();
            }
        }
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        machineHost.update();
//    if (isServer && isCreative && getWorld.getTotalWorldTime % Settings.Power.tickFrequency == 0) {
//      // Creative case, make it generate power.
//      getNode.asInstanceOf[EnergyNode].changeEnergy(Double.PositiveInfinity)
//    }
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerCase extends AbstractNodeContainer implements DeviceInfo {
        // ----------------------------------------------------------------------- //
        // Computed data.

        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.System);
            DEVICE_INFO.put(DeviceAttribute.Description, "Computer");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            DEVICE_INFO.put(DeviceAttribute.Product, "Blocker");
        }

        // ----------------------------------------------------------------------- //

        NodeContainerCase(final Location location) {
            super(location);
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

    private static final class ItemHandlerCase extends ItemHandlerComponents {
        private final int tier;

        ItemHandlerCase(final ItemHandlerHost host, final int tier) {
            super(host, InventorySlots.computer()[tier].length);
            this.tier = tier;
        }

        @Override
        public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
            return InventorySlots.isValidForSlot(InventorySlots.computer()[tier], slot, stack, TileEntityCase.class);
        }
    }
}
