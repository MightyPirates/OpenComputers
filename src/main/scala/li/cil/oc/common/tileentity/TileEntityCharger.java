package li.cil.oc.common.tileentity;

import com.google.common.collect.Sets;
import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.network.AbstractNodeContainer;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.common.inventory.ComponentManager;
import li.cil.oc.common.inventory.ItemHandlerComponents;
import li.cil.oc.common.tileentity.capabilities.RedstoneAwareImpl;
import li.cil.oc.common.tileentity.capabilities.RotatableImpl;
import li.cil.oc.common.tileentity.traits.*;
import li.cil.oc.integration.util.ItemCharge;
import li.cil.oc.integration.util.Wrench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.*;

public final class TileEntityCharger extends AbstractTileEntitySingleNodeContainer implements Analyzable, BlockActivationListener, ComparatorOutputOverride, ITickable, LocationTileEntityProxy, ItemHandlerHostTileEntityProxy, RedstoneAwareImpl.RedstoneAwareHost, RotatableImpl.RotatableHost, ComponentManager.ComponentInventoryHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainerCharger nodeContainer = new NodeContainerCharger(this);
    private final ItemHandlerCharger inventory = new ItemHandlerCharger(this);
    private final ComponentManager components = new ComponentManager(this, new NodeContainerHostTileEntity(this));
    private final RedstoneAwareImpl redstone = new RedstoneAwareImpl(this);
    private final RotatableImpl rotatable = new RotatableImpl(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_COMPONENTS = "components";
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_REDSTONE = "redstone";
    private static final String TAG_ROTATABLE = "rotatable";

    private static final int TABLET_SLOT = 0;

    private final Set<Chargeable> chargeables = new HashSet<>();
    private boolean invertSignal;
    private double chargeSpeed;
    private boolean hasEnergy;

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        components.initialize();
        redstone.scheduleInputUpdate();
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
        redstone.deserializeNBT((NBTTagCompound) nbt.getTag(TAG_REDSTONE));
        rotatable.deserializeNBT((NBTTagByte) nbt.getTag(TAG_ROTATABLE));
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        nbt.setTag(TAG_INVENTORY, inventory.serializeNBT());
        nbt.setTag(TAG_COMPONENTS, components.serializeNBT());
        nbt.setTag(TAG_REDSTONE, redstone.serializeNBT());
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
        player.sendMessage(Localization.Analyzer.ChargerSpeed(chargeSpeed));
        return null;
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (Wrench.holdsApplicableWrench(player, hand, getPos())) {
            if (isServer()) {
                invertSignal = !invertSignal;
                updateConfiguration();
                Wrench.wrenchUsed(player, hand, getPos());
            }
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //
    // ComparatorOutputOverride

    @Override
    public int getComparatorValue() {
        return chargeables.isEmpty() ? 0 : 15;
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
    }

    @Override
    public void onItemRemoved(final int slot, final ItemStack stack) {
        components.disposeComponent(slot, stack);
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        // Offset by hashcode to avoid all chargers ticking at the same time.
        if ((getWorld().getTotalWorldTime() + Math.abs(hashCode())) % 20 == 0) {
            updateConnectors();
        }

        if (isServer()) {
            components.update();

            if (!mayTickEnergy()) {
                return;
            }

            final Node node = getNodeContainer().getNode();
            assert node != null : "Charger node is null. Wat.";
            final Network network = node.getNetwork();
            if (network == null) {
                return;
            }

            final boolean newHasEnergy = network.getEnergyStored() > 0;
            if (!newHasEnergy) {
                if (hasEnergy) {
                    hasEnergy = false;
//                    ServerPacketSender.sendChargerState(this);
                }
                return;
            } else {
                if (!hasEnergy) {
                    hasEnergy = true;
//                    ServerPacketSender.sendChargerState(this);
                }
            }

            chargeExternal(network);

            chargeInternal(network);
        }

        if (isClient()) {
            if (chargeSpeed <= 0) {
                return;
            }

            if (!hasEnergy) {
                return;
            }

            final boolean shouldEmitParticle = getWorld().getTotalWorldTime() % 10 == 0;
            if (!shouldEmitParticle) {
                return;
            }

            for (final Chargeable chargeable : chargeables) {
                final Random rng = getWorld().rand;
                final Vec3d position = chargeable.getPosition();
                final double radius = chargeable.getRadius();
                final double theta = rng.nextDouble() * Math.PI;
                final double phi = rng.nextDouble() * Math.PI * 2;
                final double dx = radius * Math.sin(theta) * Math.cos(phi);
                final double dy = radius * Math.sin(theta) * Math.sin(phi);
                final double dz = radius * Math.cos(theta);
                getWorld().spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, position.xCoord + dx, position.yCoord + dz, position.zCoord + dy, 0, 0, 0);
            }
        }
    }

    private void chargeExternal(final Network network) {
        if (chargeables.isEmpty()) {
            return;
        }

        final double energyAvailable = network.getEnergyStored();
        final double energyAllowed = Settings.get().chargeRateExternal * chargeSpeed * Settings.Power.tickFrequency;
        final double energyProvided = Math.min(energyAvailable, energyAllowed);
        if (energyProvided > 0) {
            final double energyPerChargeable = energyAvailable / chargeables.size();
            for (final Chargeable chargeable : chargeables) {
                final double remainder = chargeable.changeEnergy(energyPerChargeable);
                network.changeEnergy(-(energyPerChargeable - remainder));
            }
        }
    }

    private void chargeInternal(final Network network) {
        if (inventory.getStackInSlot(TABLET_SLOT).isEmpty()) {
            return;
        }

        final ItemStack itemStack = inventory.getStackInSlot(TABLET_SLOT);
        if (!ItemCharge.canCharge(itemStack)) {
            return;
        }

        final double energyAvailable = network.getEnergyStored();
        final double energyAllowed = Settings.Power.chargeRateTablet * chargeSpeed * Settings.Power.tickFrequency;
        final double energyProvided = Math.min(energyAvailable, energyAllowed);
        if (energyProvided > 0) {
            final double remainder = ItemCharge.charge(itemStack, energyProvided);
            network.changeEnergy(-(energyProvided - remainder));
        }
    }

    // ----------------------------------------------------------------------- //
    // RedstoneAwareHost

    @Override
    public void onRedstoneInputChanged(final EnumFacing side, final int oldValue, final int newValue) {
        updateConfiguration();
    }

    // ----------------------------------------------------------------------- //

    private void updateConfiguration() {
        chargeSpeed = redstone.getMaxInput() / 15f;
        if (invertSignal) {
            chargeSpeed = 1 - chargeSpeed;
        }

//      ServerPacketSender.sendChargerState(this)
    }

    private void updateConnectors() {
        final Set<Chargeable> newChargeables = new HashSet<>();

        for (final EnumFacing side : EnumFacing.VALUES) {
            final BlockPos blockPos = getPos().offset(side);
            if (getWorld().isBlockLoaded(blockPos)) {
                final TileEntity tileEntity = getWorld().getTileEntity(blockPos);
                if (tileEntity instanceof TileEntityRobot) {
                    final TileEntityRobot robot = (TileEntityRobot) tileEntity;
                    newChargeables.add(new ChargeableRobot(robot));
                }
            }
        }

        final AxisAlignedBB bounds = new AxisAlignedBB(getPos()).expand(1, 1, 1);
        final List<Drone> drones = getWorld().getEntitiesWithinAABB(Drone.class, bounds);
        for (final Drone drone : drones) {
            newChargeables.add(new ChargeableDrone(drone));
        }

        final List<EntityPlayer> players = getWorld().getEntitiesWithinAABB(EntityPlayer.class, bounds);
        for (final EntityPlayer player : players) {
            newChargeables.add(new ChargeablePlayer(player));
        }

        // Only update list when we have to, keeps pointless block updates for comparators to a minimum.
        if (chargeables.isEmpty() && newChargeables.isEmpty()) {
            return;
        }

        if (chargeables.size() != newChargeables.size() || !Sets.difference(chargeables, newChargeables).isEmpty()) {
            chargeables.clear();
            chargeables.addAll(newChargeables);
            getWorld().notifyNeighborsOfStateChange(getPos(), getBlockType(), false);
        }
    }

    // ----------------------------------------------------------------------- //

    private static final class ItemHandlerCharger extends ItemHandlerComponents {
        ItemHandlerCharger(final ItemHandlerHost host) {
            super(host, InventorySlots.charger().length);
        }

        @Override
        public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
            return InventorySlots.isValidForSlot(InventorySlots.charger(), slot, stack, TileEntityCharger.class);
        }
    }

    private static final class NodeContainerCharger extends AbstractNodeContainer implements DeviceInfo {
        // ----------------------------------------------------------------------- //
        // Computed data.

        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Generic);
            DEVICE_INFO.put(DeviceAttribute.Description, "Charger");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            DEVICE_INFO.put(DeviceAttribute.Product, "PowerUpper");
        }

        private final TileEntityCharger charger;

        // ----------------------------------------------------------------------- //

        NodeContainerCharger(final TileEntityCharger charger) {
            super(charger);
            this.charger = charger;
        }

        // ----------------------------------------------------------------------- //
        // NodeContainer

        @Override
        public void onConnect(final Node node) {
            super.onConnect(node);
            if (node == getNode()) {
                charger.components.connect();
            }
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return li.cil.oc.api.Network.newNode(this, Visibility.NONE).withConnector(Settings.get().bufferConverter).create();
        }

        // ----------------------------------------------------------------------- //
        // DeviceInfo

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }
    }

    private interface Chargeable {
        Vec3d getPosition();

        double getRadius();

        double changeEnergy(final double delta);
    }

    private static abstract class AbstractChargeablePowerNode implements Chargeable {
        @Override
        public double changeEnergy(final double delta) {
            final Network network = getNetwork();
            if (network == null) {
                return delta;
            }
            return getNetwork().changeEnergy(delta);
        }

        @Nullable
        abstract protected Network getNetwork();
    }

    private static final class ChargeableRobot extends AbstractChargeablePowerNode {
        private final TileEntityRobot robot;

        // ----------------------------------------------------------------------- //

        ChargeableRobot(final TileEntityRobot robot) {
            this.robot = robot;
        }

        // ----------------------------------------------------------------------- //
        // Object

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ChargeableRobot that = (ChargeableRobot) o;

            return robot.equals(that.robot);
        }

        @Override
        public int hashCode() {
            return robot.hashCode();
        }

        // ----------------------------------------------------------------------- //
        // AbstractChargeablePowerNode

        @Nullable
        @Override
        protected Network getNetwork() {
            final Node node = robot.getNode();
            if (node == null) {
                return null;
            }
            return node.getNetwork();
        }

        // ----------------------------------------------------------------------- //
        // Chargeable

        @Override
        public Vec3d getPosition() {
            return robot.getHostPosition();
        }

        @Override
        public double getRadius() {
            return 0.45;
        }
    }

    private static final class ChargeableDrone extends AbstractChargeablePowerNode {
        private final Drone drone;

        // ----------------------------------------------------------------------- //

        ChargeableDrone(final Drone drone) {
            this.drone = drone;
        }

        // ----------------------------------------------------------------------- //
        // Object

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ChargeableDrone that = (ChargeableDrone) o;

            return drone.equals(that.drone);
        }

        @Override
        public int hashCode() {
            return drone.hashCode();
        }

        // ----------------------------------------------------------------------- //
        // AbstractChargeablePowerNode

        @Nullable
        @Override
        protected Network getNetwork() {
            final Node node = drone.node();
            if (node == null) {
                return null;
            }
            return node.getNetwork();
        }

        // ----------------------------------------------------------------------- //
        // Chargeable

        @Override
        public Vec3d getPosition() {
            return drone.getPositionVector();
        }

        @Override
        public double getRadius() {
            return 0.25;
        }
    }

    private static final class ChargeablePlayer implements Chargeable {
        private final EntityPlayer player;

        // ----------------------------------------------------------------------- //

        ChargeablePlayer(final EntityPlayer player) {
            this.player = player;
        }

        // ----------------------------------------------------------------------- //
        // Object

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ChargeablePlayer that = (ChargeablePlayer) o;

            return player.equals(that.player);
        }

        @Override
        public int hashCode() {
            return player.hashCode();
        }

        // ----------------------------------------------------------------------- //
        // Chargeable

        @Override
        public Vec3d getPosition() {
            return player.getPositionVector();
        }

        @Override
        public double getRadius() {
            return 1;
        }

        @Override
        public double changeEnergy(final double delta) {
            final Controller controller = Nanomachines.getController(player);
            if (controller == null) {
                return delta;
            }
            return controller.changeBuffer(delta);
        }
    }
}
