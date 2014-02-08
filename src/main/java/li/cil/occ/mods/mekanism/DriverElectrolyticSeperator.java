package li.cil.occ.mods.mekanism;

import li.cil.occ.mods.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverElectrolyticSeperator extends DriverPeripheral {
    private static final Class<?> TileEntityElectrolyticSeparator = Reflection.getClass("mekanism.generators.common.tileentity.TileEntityElectrolyticSeparator");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityElectrolyticSeparator;
    }
}
