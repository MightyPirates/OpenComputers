package li.cil.oc.driver.mekanism;


import li.cil.oc.driver.computercraft.DriverPeripheral;
import li.cil.oc.util.Reflection;

public class DriverFactory extends DriverPeripheral {
    private static final Class<?> TileEntityFactory = Reflection.getClass("mekanism.common.tileentity.TileEntityFactory");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityFactory;
    }
}
