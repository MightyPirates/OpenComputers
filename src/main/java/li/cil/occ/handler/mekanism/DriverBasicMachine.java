package li.cil.occ.handler.mekanism;

import li.cil.occ.handler.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverBasicMachine extends DriverPeripheral {
    private static final Class<?> TileEntityBasicMachine = Reflection.getClass("mekanism.common.tileentity.TileEntityBasicMachine");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityBasicMachine;
    }
}
