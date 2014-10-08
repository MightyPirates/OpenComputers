package li.cil.occ.mods.vanilla;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public final class ModVanilla implements IMod {
    public static final String MOD_ID = "Minecraft";

    @Override
    public String getModId() {
        return MOD_ID;
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
