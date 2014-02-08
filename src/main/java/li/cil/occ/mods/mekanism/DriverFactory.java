package li.cil.occ.mods.mekanism;

import li.cil.occ.mods.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverFactory extends DriverPeripheral {
    private static final Class<?> TileEntityFactory = Reflection.getClass("mekanism.common.tileentity.TileEntityFactory");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityFactory;
    }
}
