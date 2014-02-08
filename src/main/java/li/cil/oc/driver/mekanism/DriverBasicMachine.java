package li.cil.oc.driver.mekanism;


import li.cil.oc.driver.computercraft.DriverPeripheral;
import li.cil.oc.util.Reflection;

public class DriverBasicMachine extends DriverPeripheral {
    private static final Class<?> TileEntityBasicMachine = Reflection.getClass("mekanism.common.tileentity.TileEntityBasicMachine");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityBasicMachine;
    }
}
