package li.cil.oc.driver.mekanism;


import li.cil.oc.driver.computercraft.DriverPeripheral;
import li.cil.oc.util.Reflection;

public class DriverMetallurgicInfuser extends DriverPeripheral {
    private static final Class<?> TileEntityMetallurgicInfuser = Reflection.getClass("mekanism.common.tileentity.TileEntityMetallurgicInfuser");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityMetallurgicInfuser;
    }
}
