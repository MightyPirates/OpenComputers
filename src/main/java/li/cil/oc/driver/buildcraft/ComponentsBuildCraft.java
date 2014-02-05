package li.cil.oc.driver.buildcraft;

import li.cil.oc.api.Driver;

public final class ComponentsBuildCraft {
    private ComponentsBuildCraft() {
    }

    public static void register() {
        Driver.add(new DriverPipe());
        Driver.add(new DriverPowerReceptor());
    }
}
