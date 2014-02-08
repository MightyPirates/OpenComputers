package li.cil.occ.mods.mekanism;

import li.cil.occ.mods.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverGenerator extends DriverPeripheral {
    private static final Class<?> TileGenerator = Reflection.getClass("mekanism.generators.common.tileentity.TileEntityGenerator");

    @Override
    public Class<?> getTileEntityClass() {
        return TileGenerator;
    }
}
