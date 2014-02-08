package li.cil.occ.handler.mekanism;

import li.cil.occ.handler.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverMetallurgicInfuser extends DriverPeripheral {
    private static final Class<?> TileEntityMetallurgicInfuser = Reflection.getClass("mekanism.common.tileentity.TileEntityMetallurgicInfuser");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityMetallurgicInfuser;
    }
}
