package li.cil.oc.driver.vanilla;

import li.cil.oc.api.Driver;

public final class ComponentsVanilla {
    private ComponentsVanilla() {
    }

    public static void register() {
        Driver.add(new DriverBeacon());
        Driver.add(new DriverBrewingStand());
        Driver.add(new DriverComparator());
        Driver.add(new DriverFluidHandler());
        Driver.add(new DriverFluidTank());
        Driver.add(new DriverFurnace());
        Driver.add(new DriverInventory());
        Driver.add(new DriverMobSpawner());
        Driver.add(new DriverRecordPlayer());
    }
}
