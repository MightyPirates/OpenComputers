package li.cil.oc.driver.mekanism;


import li.cil.oc.driver.computercraft.DriverPeripheral;
import li.cil.oc.util.Reflection;

public class DriverEnergyCube extends DriverPeripheral {
    private static final Class<?> TileentityEnergyCube = Reflection.getClass("mekanism.common.tileentity.TileEntityEnergyCube");

    @Override
    public Class<?> getTileEntityClass() {
        return TileentityEnergyCube;
    }
}
