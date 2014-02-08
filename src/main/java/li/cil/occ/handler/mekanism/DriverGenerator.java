package li.cil.occ.handler.mekanism;


import li.cil.occ.handler.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public class DriverGenerator extends DriverPeripheral {
    private static final Class<?> TileGenerator = Reflection.getClass("mekanism.generators.common.tileentity.TileEntityGenerator");

    @Override
    public Class<?> getTileEntityClass() {
        return TileGenerator;
    }
}
