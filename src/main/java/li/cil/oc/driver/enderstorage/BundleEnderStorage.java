package li.cil.oc.driver.enderstorage;

import li.cil.oc.driver.IDriverBundle;
import li.cil.oc.api.Driver;

public final class BundleEnderStorage implements IDriverBundle {
    @Override
    public String getModId() {
        return "EnderStorage";
    }

    @Override
    public void initialize() {
        Driver.add(new DriverFrequencyOwner());
    }
}
