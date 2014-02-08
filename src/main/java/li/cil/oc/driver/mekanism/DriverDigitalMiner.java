package li.cil.oc.driver.mekanism;


import li.cil.oc.driver.computercraft.DriverPeripheral;
import li.cil.oc.util.Reflection;

public class DriverDigitalMiner extends DriverPeripheral {
    private static final Class<?> TileEntityDigitalMiner = Reflection.getClass("mekanism.common.tileentity.TileEntityDigitalMiner");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityDigitalMiner;
    }
}
