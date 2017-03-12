package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.PowerNode;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractNodeContainer;
import li.cil.oc.api.util.Location;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.common.inventory.ItemHandlerComponents;
import li.cil.oc.common.inventory.ItemHandlerHosted;
import li.cil.oc.common.tileentity.capabilities.RedstoneAwareImpl;
import li.cil.oc.common.tileentity.capabilities.RotatableImpl;
import li.cil.oc.common.tileentity.traits.BlockActivationListener;
import li.cil.oc.common.tileentity.traits.LocationTileEntityProxy;
import li.cil.oc.integration.util.Wrench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TileEntityCharger extends AbstractTileEntitySingleNodeContainer implements BlockActivationListener, ITickable, LocationTileEntityProxy, ItemHandlerHosted.ItemHandlerHost, RedstoneAwareImpl.RedstoneAwareHost, RotatableImpl.RotatableHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final ItemHandlerCharger inventory = new ItemHandlerCharger(this);
    private final NodeContainerCharger nodeContainer = new NodeContainerCharger(this);
    private final RedstoneAwareImpl redstone = new RedstoneAwareImpl(this);
    private final RotatableImpl rotatable = new RotatableImpl(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_INVENTORY = "inventory";
    private static final String TAG_REDSTONE = "redstone";
    private static final String TAG_ROTATABLE = "rotatable";

    private final Set<Chargeable> chargeables = new HashSet<>();
    private boolean invertSignal;
    private double chargeSpeed;

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        redstone.scheduleInputUpdate();
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return nodeContainer;
    }

//  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
//    player.sendMessage(Localization.Analyzer.ChargerSpeed(chargeSpeed))
//    null
//  }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        if (Wrench.holdsApplicableWrench(player, getPos())) {
            if (isServer()) {
                invertSignal = !invertSignal;
                updateConfiguration();
                Wrench.wrenchUsed(player, getPos());
            }
            return true;
        }
        return false;
    }

//  override def getCurrentState = {
//    // TODO Refine to only report working if present robots/drones actually *need* power.
//    if (connectors.nonEmpty) {
//      if (hasPower) util.EnumSet.of(api.util.StateAware.State.IsWorking)
//      else util.EnumSet.of(api.util.StateAware.State.CanWork)
//    }
//    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
//  }

    // ----------------------------------------------------------------------- //
    // ItemHandlerHost

    @Override
    public void markHostChanged() {
        markDirty();
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
    // Offset by hashcode to avoid all chargers ticking at the same time.
    if ((getWorld().getTotalWorldTime() + Math.abs(hashCode())) % 20 == 0) {
      updateConnectors();
    }

//    if (isServer && getWorld.getWorldInfo.getWorldTotalTime % Settings.get.tickFrequency == 0) {
//      var canCharge = Settings.get.ignorePower
//
//      // Charging of external devices.
//      {
//        val charge = Settings.get.chargeRateExternal * chargeSpeed * Settings.get.tickFrequency
//        canCharge ||= charge > 0 && getNode.getGlobalBuffer >= charge * 0.5
//        if (canCharge) {
//          connectors.foreach(connector => getNode.changeBuffer(connector.changeBuffer(charge + getNode.changeBuffer(-charge))))
//        }
//      }
//
//      // Charging of internal devices.
//      {
//        val charge = Settings.get.chargeRateTablet * chargeSpeed * Settings.get.tickFrequency
//        canCharge ||= charge > 0 && getNode.getGlobalBuffer >= charge * 0.5
//        if (canCharge) {
//          (0 until getSizeInventory).map(getStackInSlot).foreach(stack => if (stack != null) {
//            val offered = charge + getNode.changeBuffer(-charge)
//            val surplus = ItemCharge.charge(stack, offered)
//            getNode.changeBuffer(surplus)
//          })
//        }
//      }
//
//      if (hasPower && !canCharge) {
//        hasPower = false
//        ServerPacketSender.sendChargerState(this)
//      }
//      if (!hasPower && canCharge) {
//        hasPower = true
//        ServerPacketSender.sendChargerState(this)
//      }
//    }
//
//    if (isClient && chargeSpeed > 0 && hasPower && getWorld.getWorldInfo.getWorldTotalTime % 10 == 0) {
//      connectors.foreach(connector => {
//        val position = connector.pos
//        val theta = getWorld.rand.nextDouble * Math.PI
//        val phi = getWorld.rand.nextDouble * Math.PI * 2
//        val dx = 0.45 * Math.sin(theta) * Math.cos(phi)
//        val dy = 0.45 * Math.sin(theta) * Math.sin(phi)
//        val dz = 0.45 * Math.cos(theta)
//        getWorld.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, position.xCoord + dx, position.yCoord + dz, position.zCoord + dy, 0, 0, 0)
//      })
//    }
    }

    // ----------------------------------------------------------------------- //
    // LocationTileEntityProxy

    @Override
    public TileEntity getTileEntity() {
        return this;
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
//    val robots = EnumFacing.values.map(side => {
//      val blockPos = BlockPosition(this).offset(side)
//      if (getWorld.blockExists(blockPos)) Option(getWorld.getTileEntity(blockPos))
//      else None
//    }).collect {
//      case Some(t: RobotProxy) => new RobotChargeable(t.robot)
//    }
//    val bounds = BlockPosition(this).bounds.expand(1, 1, 1)
//    val drones = getWorld.getEntitiesWithinAABB(classOf[Drone], bounds).collect {
//      case drone: Drone => new DroneChargeable(drone)
//    }
//
//    val players = getWorld.getEntitiesWithinAABB(classOf[EntityPlayer], bounds).collect {
//      case player: EntityPlayer if api.Nanomachines.hasController(player) => new PlayerChargeable(player)
//    }
//
//    // Only update list when we have to, keeps pointless block updates to a minimum.
//
//    val newConnectors = robots ++ drones ++ players
//    if (connectors.size != newConnectors.length || (connectors.nonEmpty && (connectors -- newConnectors).nonEmpty)) {
//      connectors.clear()
//      connectors ++= newConnectors
//      getWorld.notifyNeighborsOfStateChange(getPos, getBlockType, false)
//    }
  }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerCharger extends AbstractNodeContainer implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Generic);
            DEVICE_INFO.put(DeviceAttribute.Description, "Charger");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor());
            DEVICE_INFO.put(DeviceAttribute.Product, "PowerUpper");
        }

        NodeContainerCharger(final Location location) {
            super(location);
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NONE).withConnector(Settings.get().bufferConverter).create();
        }

        // ----------------------------------------------------------------------- //
        // DeviceInfo

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }
    }

    private static final class ItemHandlerCharger extends ItemHandlerComponents {
        ItemHandlerCharger(final ItemHandlerHost host) {
            super(host, 1);
        }

        @Override
        public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
            return InventorySlots.isValidForSlot(InventorySlots.charger(), slot, stack, TileEntityCharger.class);
        }
    }

    private interface Chargeable {
        Vec3d getPosition();

        double changeBuffer(final double delta);
    }

    private static abstract class AbstractChargeablePowerNode implements Chargeable {
        @Override
        public double changeBuffer(final double delta) {
            return getPowerNode().changeBuffer(delta);
        }

        abstract protected PowerNode getPowerNode();
    }

    private static final class ChargeableRobot extends AbstractChargeablePowerNode {
        private final Robot robot;

        // ----------------------------------------------------------------------- //

        public ChargeableRobot(final Robot robot) {
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

        @Override
        protected PowerNode getPowerNode() {
            return (PowerNode) robot.getNode();
        }

        // ----------------------------------------------------------------------- //
        // Chargeable

        @Override
        public Vec3d getPosition() {
            return robot.getHostPosition();
        }
    }

    private static final class ChargeableDrone extends AbstractChargeablePowerNode {
        private final Drone drone;

        // ----------------------------------------------------------------------- //

        public ChargeableDrone(final Drone drone) {
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

        @Override
        protected PowerNode getPowerNode() {
            return (PowerNode) drone.node();
        }

        // ----------------------------------------------------------------------- //
        // Chargeable

        @Override
        public Vec3d getPosition() {
            return drone.getPositionVector();
        }
    }

    private static final class ChargeablePlayer implements Chargeable {
        private final EntityPlayer player;

        // ----------------------------------------------------------------------- //

        public ChargeablePlayer(final EntityPlayer player) {
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
        public double changeBuffer(final double delta) {
            final Controller controller = Nanomachines.getController(player);
            if (controller == null) {
                return delta;
            }
            return controller.changeBuffer(delta);
        }
    }
}
