package li.cil.occ.mods.mekanism;

import li.cil.occ.mods.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverDigitalMiner extends DriverPeripheral {
    private static final Class<?> TileEntityDigitalMiner = Reflection.getClass("mekanism.common.tileentity.TileEntityDigitalMiner");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityDigitalMiner;
    }
}
