package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractNodeContainer;
import li.cil.oc.common.GuiType;
import li.cil.oc.common.inventory.ItemHandlerHosted;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.common.tileentity.traits.BlockActivationListener;
import li.cil.oc.common.tileentity.traits.ComparatorOutputOverride;
import li.cil.oc.common.tileentity.traits.ItemHandlerHostTileEntityProxy;
import li.cil.oc.common.tileentity.traits.LocationTileEntityProxy;
import li.cil.oc.util.GUIUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;

import java.util.HashMap;
import java.util.Map;

public final class TileEntityAssembler extends AbstractTileEntitySingleNodeContainer implements BlockActivationListener, ComparatorOutputOverride, LocationTileEntityProxy, ItemHandlerHostTileEntityProxy, ITickable {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final ItemHandlerAssembler inventory = new ItemHandlerAssembler(this);
    private final NodeContainerAssembler nodeContainer = new NodeContainerAssembler(this);

    private double requiredEnergyTotal;
    private double requiredEnergyRemaining;
    private ItemStack output = ItemStack.EMPTY;

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_ENERGY_TOTAL = "energyTotal";
    private static final String TAG_ENERGY_REMAINING = "energyRemaining";
    private static final String TAG_OUTPUT = "output";

    private static final int INVENTORY_SIZE = 22; // 1 casing + 20 ingredients + 1 output.
    private static final int CASING_SLOT = 0;
    private static final int CONTAINER_SLOTS_OFFSET = 1;
    private static final int UPGRADE_SLOTS_OFFSET = 4;
    private static final int COMPONENT_SLOTS_OFFSET = 13;
    private static final int OUTPUT_SLOT = 21;

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        inventory.deserializeNBT((NBTTagList) nbt.getTag(TAG_INVENTORY));
        requiredEnergyTotal = nbt.getDouble(TAG_ENERGY_TOTAL);
        requiredEnergyRemaining = nbt.getDouble(TAG_ENERGY_REMAINING);
        output = new ItemStack((NBTTagCompound) nbt.getTag(TAG_OUTPUT));
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        nbt.setTag(TAG_INVENTORY, inventory.serializeNBT());
        nbt.setDouble(TAG_ENERGY_TOTAL, requiredEnergyTotal);
        nbt.setDouble(TAG_ENERGY_REMAINING, requiredEnergyRemaining);
        nbt.setTag(TAG_OUTPUT, output.serializeNBT());
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return nodeContainer;
    }

    public boolean isAssembling() {
        return requiredEnergyRemaining > 0;
    }

    public double getProgress() {
        if (requiredEnergyTotal > 0) {
            return (1 - requiredEnergyRemaining / requiredEnergyTotal);
        } else {
            return 0;
        }
    }

    //  def timeRemaining = (requiredEnergy / Settings.get.assemblerTickAmount / 20).toInt

    public boolean canAssemble() {
        if (isAssembling()) {
            return false;
        }

        if (!output.isEmpty()) {
            return false;
        }

        final AssemblerTemplates.Template template = AssemblerTemplates.select(inventory.getStackInSlot(CASING_SLOT));
        if (template == null) {
            return false;
        }

        final AssemblerTemplates.ValidationResult validationResult = template.validate(inventory);
        return validationResult.isValid();
    }

    public boolean assemble(final boolean instant) {
        if (!canAssemble()) {
            return false;
        }

        final AssemblerTemplates.Template template = AssemblerTemplates.select(inventory.getStackInSlot(CASING_SLOT));
        assert template != null : "canAssemble lied.";

        final AssemblerTemplates.AssemblyResult assemblyResult = template.assemble(inventory);
        output = assemblyResult.getOutput();
        if (instant) {
            requiredEnergyTotal = 0;
        } else {
            requiredEnergyTotal = Math.max(0, assemblyResult.getRequiredEnergy());
        }
        requiredEnergyRemaining = requiredEnergyTotal;

//        ServerPacketSender.sendRobotAssembling(this, assembling = true)

        for (int slot = 0; slot < OUTPUT_SLOT; slot++) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }

        return true;
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (player.isSneaking()) {
            return false;
        }

        GUIUtils.openGUI(this, GuiType.Assembler());
        return true;
    }

    // ----------------------------------------------------------------------- //
    // ComparatorOutputOverride

    @Override
    public int getComparatorValue() {
        return isAssembling() ? 15 : 0;
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        if (isClient()) {
            return;
        }

        if (output.isEmpty()) {
            return;
        }

        if (!mayTickEnergy()) {
            return;
        }

        final Node node = nodeContainer.getNode();
        if (node == null) {
            return;
        }
        final Network network = node.getNetwork();
        if (network == null) {
            return;
        }

        final double energyAvailable = network.getEnergyStored();
        final double energyAllowed = Settings.get().assemblerTickAmount * Settings.get().tickFrequency;
        final double energyProvided = Math.min(energyAvailable, energyAllowed);
        if (energyProvided > 0) {
            if (energyProvided < requiredEnergyRemaining) {
                network.changeEnergy(-energyProvided);
                requiredEnergyRemaining -= energyProvided;
            } else {
                network.changeEnergy(-requiredEnergyRemaining);
                requiredEnergyRemaining = 0;

                inventory.setStackInSlot(OUTPUT_SLOT, output);
                output = ItemStack.EMPTY;
            }
        }

//      ServerPacketSender.sendRobotAssembling(this, energyProvided > 0 && output.isDefined)
    }

    // ----------------------------------------------------------------------- //

    private static final class ItemHandlerAssembler extends ItemHandlerHosted {
        private final TileEntityAssembler assembler;

        ItemHandlerAssembler(final TileEntityAssembler assembler) {
            super(assembler, INVENTORY_SIZE);
            this.assembler = assembler;
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 1;
        }

        @Override
        public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
            if (slot == CASING_SLOT) {
                return !assembler.isAssembling() && AssemblerTemplates.select(stack) != null;
            }

            final AssemblerTemplates.Template template = AssemblerTemplates.select(assembler.inventory.getStackInSlot(CASING_SLOT));
            if (template == null) {
                return false;
            }

            final AssemblerTemplates.SlotInfo templateSlot;
            if (slot < UPGRADE_SLOTS_OFFSET) {
                final int containerSlot = slot - CONTAINER_SLOTS_OFFSET;
                templateSlot = template.containerSlots[containerSlot];
            } else if (slot < COMPONENT_SLOTS_OFFSET) {
                final int upgradeSlot = slot - UPGRADE_SLOTS_OFFSET;
                templateSlot = template.upgradeSlots[upgradeSlot];
            } else if (slot < OUTPUT_SLOT) {
                final int componentSlot = slot - COMPONENT_SLOTS_OFFSET;
                templateSlot = template.componentSlots[componentSlot];
            } else {
                templateSlot = AssemblerTemplates.NoSlot;
            }

            return templateSlot.validate(assembler.inventory, slot, stack);
        }
    }

    private static final class NodeContainerAssembler extends AbstractNodeContainer implements DeviceInfo {
        // ----------------------------------------------------------------------- //
        // Computed data.

        private static final String NAME_ASSEMBLER = "assembler";
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Generic);
            DEVICE_INFO.put(DeviceAttribute.Description, "Assembler");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            DEVICE_INFO.put(DeviceAttribute.Product, "Factorizer R1D1");
        }

        private final TileEntityAssembler assembler;

        // ----------------------------------------------------------------------- //

        NodeContainerAssembler(final TileEntityAssembler assembler) {
            super(assembler);
            this.assembler = assembler;
        }

        // ----------------------------------------------------------------------- //

        @Callback(doc = "function(): string, number or boolean -- The current state of the assembler, `busy' or `idle', followed by the progress or template validity, respectively.")
        public Object[] status(final Context context, final Arguments args) {
            if (assembler.isAssembling()) {
                return new Object[]{"busy", assembler.getProgress()};
            }

            final AssemblerTemplates.Template template = AssemblerTemplates.select(assembler.inventory.getStackInSlot(CASING_SLOT));
            if (template == null) {
                return new Object[]{"idle", false};
            }

            final AssemblerTemplates.ValidationResult validationResult = template.validate(assembler.inventory);
            return new Object[]{"idle", validationResult.isValid()};
        }

        @Callback(doc = "function():boolean -- Start assembling, if possible. Returns whether assembly was started or not.")
        public Object[] start(final Context context, final Arguments args) {
            return new Object[]{assembler.assemble(false)};
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return li.cil.oc.api.Network.newNode(this, Visibility.NETWORK).
                    withComponent(NAME_ASSEMBLER).
                    withConnector(Settings.get().bufferConverter).
                    create();
        }

        // ----------------------------------------------------------------------- //
        // DeviceInfo

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }
    }
}
