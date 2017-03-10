package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.network.AbstractEnvironment;
import li.cil.oc.common.GuiType;
import li.cil.oc.common.Sound;
import li.cil.oc.common.inventory.ComponentInventory;
import li.cil.oc.common.inventory.ItemHandlerComponents;
import li.cil.oc.common.inventory.ItemHandlerHosted;
import li.cil.oc.common.tileentity.capabilities.RotatableImpl;
import li.cil.oc.common.tileentity.traits.BlockActivationListener;
import li.cil.oc.common.tileentity.traits.ComparatorOutputOverride;
import li.cil.oc.util.GUIUtils;
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
import java.util.Objects;

public final class TileEntityDiskDrive extends AbstractTileEntitySingleEnvironment implements BlockActivationListener, ComparatorOutputOverride, Analyzable, RotatableImpl.RotatableHost, ComponentInventory.ComponentInventoryHost, ItemHandlerHosted.ItemHandlerHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final Environment environment = new EnvironmentDiskDrive(this);
    private final RotatableImpl rotatable = new RotatableImpl(this);
    private final ComponentInventory components = new ComponentInventory(this);
    private final ItemHandlerDiskDrive inventory = new ItemHandlerDiskDrive(this, 1);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_ROTATABLE = "rotatable";
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_COMPONENTS = "components";

    private static final int FLOPPY_SLOT = 0;

    // Used on client side to check whether to render disk activity indicators.
    public long lastAccess;

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleEnvironment

    @Override
    protected Environment getEnvironment() {
        return environment;
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        nbt.setTag(TAG_ROTATABLE, rotatable.serializeNBT());
        nbt.setTag(TAG_INVENTORY, inventory.serializeNBT());
        nbt.setTag(TAG_COMPONENTS, components.serializeNBT());
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        rotatable.deserializeNBT((NBTTagByte) nbt.getTag(TAG_ROTATABLE));
        inventory.deserializeNBT((NBTTagList) nbt.getTag(TAG_INVENTORY));
        components.deserializeNBT((NBTTagList) nbt.getTag(TAG_COMPONENTS));
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
        if (player.isSneaking()) {
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
        } else {
            GUIUtils.openGUI(this, GuiType.DiskDrive());
            return true;
        }
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
        final Node node = environment.getNode();
        assert node != null : "getItemNode called on client? Don't.";
        return node;
    }

    @Override
    public IItemHandler getComponentItems() {
        return inventory;
    }

    @Nullable
    @Override
    public Node[] onAnalyze(final EntityPlayer player, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final Environment environment = components.getEnvironment(FLOPPY_SLOT);
        if (environment != null) {
            return new Node[]{environment.getNode()};
        }
        return new Node[0];
    }

    // ----------------------------------------------------------------------- //
    // ItemHandlerHost

    @Override
    public void onItemAdded(final int slot, final ItemStack stack) {
        components.onItemAdded(slot, stack);
        final Environment environment = components.getEnvironment(FLOPPY_SLOT);
        if (environment != null) {
            final Node node = environment.getNode();
            if (node instanceof Component) {
                final Component component = (Component) node;
                component.setVisibility(Visibility.NETWORK);
            }
        }
//      ServerPacketSender.sendFloppyChange(this, stack)
        Sound.playDiskInsert(this);
    }

    @Override
    public void onItemChanged(final int slot, final ItemStack stack) {
        components.onItemChanged(slot, stack);
    }

    @Override
    public void onItemRemoved(final int slot, final ItemStack stack) {
        components.onItemRemoved(slot, stack);
//      ServerPacketSender.sendFloppyChange(this)
        Sound.playDiskEject(this);
    }

    // ----------------------------------------------------------------------- //

    private static final class EnvironmentDiskDrive extends AbstractEnvironment implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Disk);
            DEVICE_INFO.put(DeviceAttribute.Description, "Floppy disk drive");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor());
            DEVICE_INFO.put(DeviceAttribute.Product, "Spinner 520p1");
        }

        private final TileEntityDiskDrive diskDrive;

        EnvironmentDiskDrive(final TileEntityDiskDrive host) {
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

            final World world = getHost().getHostWorld();
            final EnumFacing facing = diskDrive.rotatable.getFacing();
            final Vec3d pos = getHost().getHostPosition().addVector(facing.getFrontOffsetX(), facing.getFrontOffsetY(), facing.getFrontOffsetZ());
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
        // Environment

        @Override
        public void onConnect(final Node node) {
            super.onConnect(node);
        }

        // ----------------------------------------------------------------------- //
        // AbstractEnvironment

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
        ItemHandlerDiskDrive(final ItemHandlerHost host, final int size) {
            super(host, size);
        }

        @Override
        public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
            final DriverItem driver = Driver.driverFor(stack, TileEntityDiskDrive.class);
            return driver != null && Objects.equals(driver.slot(stack), Slot.Floppy);
        }
    }
}
