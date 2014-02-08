package li.cil.occ.handler.mekanism;

import li.cil.occ.handler.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverTeleporter extends DriverPeripheral {
    private static final Class<?> TileEntityTeleporter = Reflection.getClass("mekanism.common.tileentity.TileEntityTeleporter");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityTeleporter;
    }
}
