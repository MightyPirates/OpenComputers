package li.cil.occ.mods.mekanism;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;
import net.minecraft.item.ItemStack;

import java.util.Map;

public final class ModMekanism implements IMod {
    @Override
    public String getModId() {
        return "Mekanism";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverBasicMachine());
        Driver.add(new DriverDigitalMiner());
        Driver.add(new DriverElectrolyticSeperator());
        Driver.add(new DriverEnergyCube());
        Driver.add(new DriverFactory());
        Driver.add(new DriverGenerator());
        Driver.add(new DriverMetallurgicInfuser());
        Driver.add(new DriverTeleporter());

    }

    @Override
    public void populate(final Map<String, Object> map, final ItemStack stack) {
    }
}
