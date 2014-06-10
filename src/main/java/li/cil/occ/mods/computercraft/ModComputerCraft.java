package li.cil.occ.mods.computercraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModComputerCraft implements IMod {
    @Override
    public String getModId() {
        return "ComputerCraft";
    }

    @Override
    public void initialize() {
        try {
            Driver.add(new DriverPeripheral16());
        } catch (Throwable ignored) {
        }
    }
}
