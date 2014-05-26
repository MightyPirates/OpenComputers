package li.cil.occ.mods.forestry;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public class ModForestry implements IMod{

	@Override
	public String getModId() {
		return "Forestry";
	}

	@Override
	public void initialize() {
		Driver.add(new DriverBeeHouse());
	}

}
