package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.OpenComputers$;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractNodeContainer;
import li.cil.oc.common.GuiType;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.inventory.ItemHandlerComponents;
import li.cil.oc.common.inventory.ItemHandlerHosted;
import li.cil.oc.common.tileentity.traits.*;
import li.cil.oc.server.PacketSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;

import javax.annotation.Nullable;
import java.util.Objects;

public final class TileEntityNetworkBridge extends AbstractTileEntityMultiNodeContainer implements Analyzable, BlockActivationListener, ItemHandlerHostTileEntityProxy, LocationTileEntityProxy, NetworkBridge.NetworkBridgeHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainer[] nodeContainers = new NodeContainer[EnumFacing.VALUES.length];
    private final ItemHandlerNetworkBridge inventory = new ItemHandlerNetworkBridge(this, InventorySlots.relay().length);
    private final NetworkBridge bridge = new NetworkBridge(this);
    private final NetworkBridgeAdapterWired wired = new NetworkBridgeAdapterWired(this, nodeContainers.length);
    private final NetworkBridgeAdapterWireless wireless = new NetworkBridgeAdapterWireless(this);
    private final NetworkBridgeAdapterQuantum quantum = new NetworkBridgeAdapterQuantum();

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_BRIDGE = "bridge";
    private static final String TAG_WIRED = "wired";
    private static final String TAG_WIRELESS = "wireless";

    private int queueSize;
    private int interval;
    private int bandwidth;
    private long lastMessage = 0L;

    // ----------------------------------------------------------------------- //

    public TileEntityNetworkBridge() {
        for (int index = 0; index < nodeContainers.length; index++) {
            nodeContainers[index] = new NodeContainerNetworkBridge(this, index);
        }
        bridge.addAdapter(wired);
        bridge.addAdapter(wireless);
        bridge.addAdapter(quantum);
    }

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        updateConfiguration();
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        inventory.deserializeNBT((NBTTagList) nbt.getTag(TAG_INVENTORY));
        bridge.deserializeNBT((NBTTagCompound) nbt.getTag(TAG_BRIDGE));
        wired.deserializeNBT((NBTTagList) nbt.getTag(TAG_WIRED));
        wireless.deserializeNBT((NBTTagCompound) nbt.getTag(TAG_WIRELESS));
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        nbt.setTag(TAG_INVENTORY, inventory.serializeNBT());
        nbt.setTag(TAG_BRIDGE, bridge.serializeNBT());
        nbt.setTag(TAG_WIRED, wired.serializeNBT());
        nbt.setTag(TAG_WIRELESS, wireless.serializeNBT());
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntityMultiNodeContainer

    @Override
    protected NodeContainer[] getEnvironments() {
        return nodeContainers;
    }

    // ----------------------------------------------------------------------- //
    // Analyzable

    @Nullable
    @Override
    public Node[] onAnalyze(final EntityPlayer player, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        player.sendMessage(Localization.Analyzer.WirelessStrength(wireless.getStrength()));
        if (wireless.isRepeater()) {
            player.sendMessage(Localization.Analyzer.IsRepeater());
        }

        return new Node[]{nodeContainers[side.ordinal()].getNode()};
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (isServer()) {
            player.openGui(OpenComputers$.MODULE$, GuiType.Relay().id(), world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    // ----------------------------------------------------------------------- //
    // ItemHandlerHost

    @Override
    public void onItemAdded(final int slot, final ItemStack stack) {
        updateConfiguration();
    }

    @Override
    public void onItemRemoved(final int slot, final ItemStack stack) {
        updateConfiguration();
    }

    // ----------------------------------------------------------------------- //
    // NetworkBridgeHost

    @Override
    public int getNetworkBridgeMaxQueueSize() {
        return queueSize;
    }

    @Override
    public int getNetworkBridgeInterval() {
        return interval;
    }

    @Override
    public int getNetworkBridgeBandwidth() {
        return bandwidth;
    }

    @Override
    public void onNetworkMessageProcessed() {
        final long now = System.currentTimeMillis();
        if (now - lastMessage >= getNetworkBridgeInterval() * 50) {
            lastMessage = now;
            PacketSender.sendSwitchActivity(this);
        }
    }

    @Override
    public Node getPacketHopNode() {
        final Node node = nodeContainers[0].getNode();
        assert node != null : "getPacketHopNode called on client side? Don't.";
        return node;
    }

    // ----------------------------------------------------------------------- //

    private void updateConfiguration() {
        queueSize = Settings.get().switchDefaultMaxQueueSize;
        interval = Settings.get().switchDefaultRelayDelay;
        bandwidth = Settings.get().switchDefaultRelayAmount;

        boolean wirelessEnabled = false;
        boolean quantumEnabled = false;

        final InventorySlots.InventorySlot[] slots = InventorySlots.relay();
        for (int slot = 0; slot < slots.length; slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            final DriverItem driver = Driver.driverFor(stack);
            final int multiplier;
            if (driver != null) {
                multiplier = driver.tier(stack) + 1;
            } else {
                multiplier = 1;
            }

            final InventorySlots.InventorySlot slotInfo = slots[slot];

            if (Objects.equals(slotInfo.slot(), Slot.CPU)) {
                interval -= multiplier * Settings.get().switchRelayDelayUpgrade;
            }

            if (Objects.equals(slotInfo.slot(), Slot.Memory)) {
                bandwidth += multiplier * Settings.get().switchRelayAmountUpgrade;
            }

            if (Objects.equals(slotInfo.slot(), Slot.HDD)) {
                queueSize += multiplier * Settings.get().switchQueueSizeUpgrade;
            }

            if (Objects.equals(slotInfo.slot(), Slot.Card)) {
                final ItemInfo info = Items.get(stack);

                if (info == WIRELESS_NETWORK_CARD) {
                    wirelessEnabled = true;
                }

                if (info == LINKED_CARD) {
                    quantumEnabled = true;
                }
            }
        }

        queueSize = Math.max(queueSize, 1);
        interval = Math.max(interval, 1);
        bandwidth = Math.max(bandwidth, 1);

        wireless.setEnabled(wirelessEnabled);
        quantum.setEnabled(quantumEnabled);
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerNetworkBridge extends AbstractNodeContainer {
        // ----------------------------------------------------------------------- //
        // Computed data.

        private static final String NAME_RELAY = "relay";

        private final TileEntityNetworkBridge tileEntity;
        private final int index;

        // ----------------------------------------------------------------------- //

        NodeContainerNetworkBridge(final TileEntityNetworkBridge tileEntity, final int index) {
            super(tileEntity);
            this.tileEntity = tileEntity;
            this.index = index;
        }

        // ----------------------------------------------------------------------- //

        @Callback(direct = true, doc = "function():boolean -- Checks if wireless communication is available.")
        public Object[] isWireless(final Context context, final Arguments args) {
            return new Object[]{tileEntity.wireless.isEnabled()};
        }

        @Callback(direct = true, doc = "function():boolean -- Checks if linked communication is available.")
        public Object[] isLinked(final Context context, final Arguments args) {
            return new Object[]{tileEntity.quantum.isEnabled()};
        }

        @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when relaying messages.")
        public Object[] getStrength(final Context context, final Arguments args) {
            return new Object[]{tileEntity.wireless.getStrength()};
        }

        @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when relaying messages.")
        public Object[] setStrength(final Context context, final Arguments args) {
            final double oldValue = tileEntity.wireless.getStrength();
            tileEntity.wireless.setStrength(args.checkDouble(0));
            return new Object[]{oldValue};
        }

        @Callback(direct = true, doc = "function():boolean -- Get whether the access point currently acts as a repeater (resend received wireless packets wirelessly).")
        public Object[] isRepeater(final Context context, final Arguments args) {
            return new Object[]{tileEntity.wireless.isRepeater()};
        }

        @Callback(doc = "function(enabled:boolean):boolean -- Set whether the access point should act as a repeater.")
        public Object[] setRepeater(final Context context, final Arguments args) {
            final boolean oldValue = tileEntity.wireless.isRepeater();
            tileEntity.wireless.setRepeater(args.checkBoolean(0));
            return new Object[]{oldValue};
        }

        // ----------------------------------------------------------------------- //
        // NodeContainer

        @Override
        public void onConnect(final Node node) {
            super.onConnect(node);

            if (node != getNode()) {
                return;
            }

            final NodeContainer adapter = tileEntity.wired.getContainer(index);
            final Node adapterNode = adapter.getNode();
            if (adapterNode == null) {
                return;
            }

            node.connect(adapterNode);
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NETWORK).
                    withComponent(NAME_RELAY).
                    create();
        }
    }

    private static final ItemInfo WIRELESS_NETWORK_CARD = Items.get(Constants.ItemName.WirelessNetworkCard());
    private static final ItemInfo LINKED_CARD = Items.get(Constants.ItemName.LinkedCard());

    private static final class ItemHandlerNetworkBridge extends ItemHandlerComponents {
        ItemHandlerNetworkBridge(final ItemHandlerHost host, final int size) {
            super(host, size);
        }

        @Override
        public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
            if (!InventorySlots.isValidForSlot(InventorySlots.relay(), slot, stack, TileEntityNetworkBridge.class)) {
                return false;
            }

            return !Objects.equals(InventorySlots.relay()[slot].slot(), Slot.Card) || Items.get(stack) == WIRELESS_NETWORK_CARD || Items.get(stack) == LINKED_CARD;
        }
    }
}
