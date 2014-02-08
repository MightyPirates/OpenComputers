package li.cil.occ.handler.mekanism;

import li.cil.occ.handler.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public final class DriverDigitalMiner extends DriverPeripheral {
    private static final Class<?> TileEntityDigitalMiner = Reflection.getClass("mekanism.common.tileentity.TileEntityDigitalMiner");

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntityDigitalMiner;
    }
}
