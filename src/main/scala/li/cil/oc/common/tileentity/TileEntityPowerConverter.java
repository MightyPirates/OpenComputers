package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractEnvironment;

import java.util.HashMap;
import java.util.Map;

public final class TileEntityPowerConverter extends AbstractTileEntitySingleEnvironment /* traits.PowerAcceptor with traits.Environment with traits.NotAnalyzable with DeviceInfo*/ {
    private final EnvironmentPowerConverter environment = new EnvironmentPowerConverter(this);

    @Override
    protected Environment getEnvironment() {
        return environment;
    }

    //  @SideOnly(Side.CLIENT)
//  override protected def hasConnector(side: EnumFacing) = true
//
//  override protected def connector(side: EnumFacing) = Option(getNode)
//
//  override def energyThroughput = Settings.get.powerConverterRate

    private static final class EnvironmentPowerConverter extends AbstractEnvironment implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = new HashMap<>();

        static {
            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Power);
            DEVICE_INFO.put(DeviceAttribute.Description, "Power converter");
            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor());
            DEVICE_INFO.put(DeviceAttribute.Product, "Transgizer-PX5");
            DEVICE_INFO.put(DeviceAttribute.Capacity, energyThroughput.toString);
        }

        EnvironmentPowerConverter(final EnvironmentHost host) {
            super(host);
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NONE).withConnector(Settings.get().bufferConverter()).create();
        }
    }
}
