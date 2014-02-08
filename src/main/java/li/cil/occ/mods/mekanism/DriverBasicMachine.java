package li.cil.occ.mods.mekanism;

import li.cil.occ.mods.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverBasicMachine extends DriverPeripheral {
    private static final Class<?> TileEntityBasicMachine = Reflection.getClass("mekanism.common.tileentity.TileEntityBasicMachine");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityBasicMachine;
    }
}
