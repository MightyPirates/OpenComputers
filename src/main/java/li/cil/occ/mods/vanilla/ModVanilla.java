package li.cil.occ.mods.vanilla;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModVanilla implements IMod {
    @Override
    public String getModId() {
        return "Minecraft";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverBeacon());
        Driver.add(new DriverBrewingStand());
        Driver.add(new DriverCommandBlock());
        Driver.add(new DriverComparator());
        Driver.add(new DriverFluidHandler());
        Driver.add(new DriverFluidTank());
        Driver.add(new DriverFurnace());
        Driver.add(new DriverInventory());
        Driver.add(new DriverMobSpawner());
        Driver.add(new DriverNoteBlock());
        Driver.add(new DriverRecordPlayer());
        Driver.add(new DriverSign());
    }
}
