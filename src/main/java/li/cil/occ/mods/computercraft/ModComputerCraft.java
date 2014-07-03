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
            final DriverPeripheral15 driver = new DriverPeripheral15();
            if (driver.isValid()) {
                Driver.add(new ConverterLuaObject15());
                Driver.add(driver);
            }
        } catch (Throwable ignored) {
        }
        try {
            final DriverPeripheral16 driver = new DriverPeripheral16();
            if (driver.isValid()) {
                Driver.add(new ConverterLuaObject16());
                Driver.add(driver);
            }
        } catch (Throwable ignored) {
        }
    }
}
