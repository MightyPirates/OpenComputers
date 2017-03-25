//package li.cil.oc.common.tileentity;
//
//import li.cil.oc.Constants;
//import li.cil.oc.Settings;
//import li.cil.oc.api.Network;
//import li.cil.oc.api.driver.DeviceInfo;
//import li.cil.oc.api.network.Node;
//import li.cil.oc.api.network.NodeContainer;
//import li.cil.oc.api.network.Visibility;
//import li.cil.oc.api.prefab.network.AbstractTileEntityNodeContainer;
//import net.minecraft.tileentity.TileEntity;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public final class TileEntityPowerConverter extends AbstractTileEntitySingleNodeContainer /* traits.PowerAcceptor with traits.NodeContainer with traits.NotAnalyzable with DeviceInfo*/ {
//    private final NodeContainerPowerConverter environment = new NodeContainerPowerConverter(this);
//
//    @Override
//    protected NodeContainer getNodeContainer() {
//        return environment;
//    }
//
//    //  @SideOnly(Side.CLIENT)
////  override protected def hasConnector(side: EnumFacing) = true
////
////  override protected def connector(side: EnumFacing) = Option(getNode)
////
////  override def energyThroughput = Settings.Power.Rate.powerConverter
//
//    private static final class NodeContainerPowerConverter extends AbstractTileEntityNodeContainer implements DeviceInfo {
//        private static final Map<String, String> DEVICE_INFO = new HashMap<>();
//
//        static {
//            DEVICE_INFO.put(DeviceAttribute.Class, DeviceClass.Power);
//            DEVICE_INFO.put(DeviceAttribute.Description, "Power converter");
//            DEVICE_INFO.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
//            DEVICE_INFO.put(DeviceAttribute.Product, "Transgizer-PX5");
//            DEVICE_INFO.put(DeviceAttribute.Capacity, energyThroughput.toString);
//        }
//
//        NodeContainerPowerConverter(final TileEntity host) {
//            super(host);
//        }
//
//        @Override
//        public Map<String, String> getDeviceInfo() {
//            return DEVICE_INFO;
//        }
//
//        @Override
//        protected Node createNode() {
//            return Network.newNode(this, Visibility.NONE).withConnector(Settings.get().bufferConverter).create();
//        }
//    }
//}
