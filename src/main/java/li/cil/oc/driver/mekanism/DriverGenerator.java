package li.cil.oc.driver.mekanism;


import li.cil.oc.driver.computercraft.DriverPeripheral;
import li.cil.oc.util.Reflection;

public class DriverGenerator extends DriverPeripheral {
    private static final Class<?> TileGenerator = Reflection.getClass("mekanism.generators.common.tileentity.TileEntityGenerator");

    @Override
    public Class<?> getTileEntityClass() {
        return TileGenerator;
    }
}
