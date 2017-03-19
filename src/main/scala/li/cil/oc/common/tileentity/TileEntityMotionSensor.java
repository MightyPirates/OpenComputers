package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractTileEntityNodeContainer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TileEntityMotionSensor extends AbstractTileEntitySingleNodeContainer implements ITickable {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainerMotionSensor environment = new NodeContainerMotionSensor(this);

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return environment;
    }

    // ----------------------------------------------------------------------- //
    // ITickable

    @Override
    public void update() {
        if (isServer()) {
            environment.update();
        }
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerMotionSensor extends AbstractTileEntityNodeContainer implements DeviceInfo {
        // ----------------------------------------------------------------------- //
        // Persisted data.

        private float sensitivity = 0.4f;

        // ----------------------------------------------------------------------- //
        // Computed data.

        // NBT tag names.
        private static final String TAG_SENSITIVITY = "sensitivity";

        private static final float RADIUS = 8;
        private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(-RADIUS, -RADIUS, -RADIUS, RADIUS, RADIUS, RADIUS);
        private static final String MOTION_SENSOR_NAME = "motion_sensor";
        private static final String COMPUTER_SIGNAL_NAME = "computer.signal";
        private static final String MOTION_SIGNAL_NAME = "motion";
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Generic);
            DEVICE_INFO.put(DeviceAttribute.Description, "Motion sensor");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            DEVICE_INFO.put(DeviceAttribute.Product, "Blinker M1K0");
            DEVICE_INFO.put(DeviceAttribute.Capacity, String.valueOf(RADIUS));
        }

        private final Map<EntityLivingBase, Vec3d> trackedEntities = new HashMap<>();

        // ----------------------------------------------------------------------- //

        NodeContainerMotionSensor(final TileEntity host) {
            super(host);
        }

        // ----------------------------------------------------------------------- //

        @Callback(direct = true, doc = "function():number -- Gets the current sensor sensitivity.")
        public Object[] getSensitivity(final Context context, final Arguments args) {
            return new Object[]{sensitivity};
        }

        @Callback(direct = true, doc = "function(value:number):number -- Sets the sensor's sensitivity. Returns the old value.")
        public Object[] setSensitivity(final Context context, final Arguments args) {
            final float oldValue = sensitivity;
            sensitivity = (float) Math.max(0.2, args.checkDouble(0));
            return new Object[]{oldValue};
        }

        // ----------------------------------------------------------------------- //
        // DeviceInfo

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        // ----------------------------------------------------------------------- //
        // INBTSerializable

        @Override
        public NBTTagCompound serializeNBT() {
            final NBTTagCompound nbt = super.serializeNBT();
            nbt.setFloat(TAG_SENSITIVITY, sensitivity);
            return nbt;
        }

        @Override
        public void deserializeNBT(final NBTTagCompound nbt) {
            super.deserializeNBT(nbt);
            sensitivity = nbt.getFloat(TAG_SENSITIVITY);
        }

        // ----------------------------------------------------------------------- //
        // AbstractNodeContainer

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NETWORK).
                    withComponent(MOTION_SENSOR_NAME).
                    withConnector().
                    create();
        }

        // ----------------------------------------------------------------------- //

        public void update() {
            final World world = getLocation().getHostWorld();
            if (world.getTotalWorldTime() % 10 == 0) {
                // Get a list of all living entities we could possibly detect, using a rough
                // bounding box check, then refining it using the actual distance and an
                // actual visibility check.
                final Set<EntityLivingBase> entities = world.
                        getEntitiesWithinAABB(EntityLivingBase.class, sensorBounds()).stream().
                        filter(entity -> entity.isEntityAlive() && isInRange(entity) && isVisible(entity))
                        .collect(Collectors.toSet());
                // Get rid of all tracked entities that are no longer visible.
                final Iterator<EntityLivingBase> iterator = trackedEntities.keySet().iterator();
                while (iterator.hasNext()) {
                    if (!entities.contains(iterator.next())) {
                        iterator.remove();
                    }
                }
                // Check for which entities we should generate a signal.
                for (final EntityLivingBase entity : entities) {
                    final Vec3d lastPos = trackedEntities.get(entity);
                    if (lastPos == null || entity.getPositionVector().distanceTo(lastPos) > sensitivity * sensitivity * 2) {
                        sendSignal(entity);
                    }
                    // Update tracked position.
                    trackedEntities.replace(entity, entity.getPositionVector());
                }
            }
        }

        private AxisAlignedBB sensorBounds() {
            return BOUNDS.offset(getLocation().getHostBlockPosition());
        }

        private boolean isInRange(final EntityLivingBase entity) {
            return entity.getPositionVector().distanceTo(getLocation().getHostPosition()) <= RADIUS * RADIUS;
        }

        private boolean isVisible(final EntityLivingBase entity) {
            final Potion potion = Potion.getPotionFromResourceLocation("invisibility");
            if (potion == null) {
                OpenComputers.log().warn("Failed looking up invisibility potion.");
                return false;
            }
            final boolean isInvisible = entity.getActivePotionEffect(potion) != null;
            if (isInvisible) {
                return false;
            }

            final Vec3d sensorPos = getLocation().getHostPosition();
            final Vec3d entityPos = entity.getPositionVector();

            // Start trace outside of this block.
            final Vec3d origin = sensorPos.add(entityPos.subtract(sensorPos).normalize());

            return getLocation().getHostWorld().rayTraceBlocks(origin, entityPos) == null;
        }

        private void sendSignal(final EntityLivingBase entity) {
            final Node node = getNode();
            assert node != null : "sendSignal called on client side? Don't.";

            final Vec3d relativePos = entity.getPositionVector().subtract(getLocation().getHostPosition());
            if (Settings.get().inputUsername) {
                node.sendToReachable(COMPUTER_SIGNAL_NAME, MOTION_SIGNAL_NAME, relativePos.xCoord, relativePos.yCoord, relativePos.zCoord, entity.getName());
            } else {
                node.sendToReachable(COMPUTER_SIGNAL_NAME, MOTION_SIGNAL_NAME, relativePos.xCoord, relativePos.yCoord, relativePos.zCoord);
            }
        }
    }
}
