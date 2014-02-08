package li.cil.occ.handler.mekanism;


import li.cil.occ.handler.computercraft.DriverPeripheral;
import li.cil.occ.util.Reflection;

public class DriverEnergyCube extends DriverPeripheral {
    private static final Class<?> TileentityEnergyCube = Reflection.getClass("mekanism.common.tileentity.TileEntityEnergyCube");

    @Override
    public Class<?> getTileEntityClass() {
        return TileentityEnergyCube;
    }
}
