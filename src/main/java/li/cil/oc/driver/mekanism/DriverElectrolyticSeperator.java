package li.cil.oc.driver.mekanism;


import li.cil.oc.driver.computercraft.DriverPeripheral;
import li.cil.oc.util.Reflection;

public class DriverElectrolyticSeperator extends DriverPeripheral {
    private static final Class<?> TileEntityElectrolyticSeparator = Reflection.getClass("mekanism.generators.common.tileentity.TileEntityElectrolyticSeparator");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityElectrolyticSeparator;
    }
}
