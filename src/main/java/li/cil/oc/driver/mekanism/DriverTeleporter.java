package li.cil.oc.driver.mekanism;


import li.cil.oc.driver.computercraft.DriverPeripheral;
import li.cil.oc.util.Reflection;

public class DriverTeleporter extends DriverPeripheral {
    private static final Class<?> TileEntityTeleporter = Reflection.getClass("mekanism.common.tileentity.TileEntityTeleporter");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityTeleporter;
    }
}
