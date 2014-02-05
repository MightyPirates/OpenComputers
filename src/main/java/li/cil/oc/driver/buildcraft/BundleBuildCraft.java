package li.cil.oc.driver.buildcraft;

import li.cil.oc.api.Driver;
import li.cil.oc.driver.IDriverBundle;

public final class BundleBuildCraft implements IDriverBundle {
    @Override
    public String getModId() {
        return "BuildCraft|Core";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverPipe());
        Driver.add(new DriverPowerReceptor());
    }
}
