package li.cil.occ.mods.mekanism;

import li.cil.occ.mods.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverMetallurgicInfuser extends DriverPeripheral {
    private static final Class<?> TileEntityMetallurgicInfuser = Reflection.getClass("mekanism.common.tileentity.TileEntityMetallurgicInfuser");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityMetallurgicInfuser;
    }
}
