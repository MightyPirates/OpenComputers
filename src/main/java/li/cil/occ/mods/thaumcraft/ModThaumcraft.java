package li.cil.occ.mods.thaumcraft;

import li.cil.oc.api.Driver;
import li.cil.occ.mods.IMod;

public class ModThaumcraft implements IMod{

	@Override
	public String getModId() {
		return "Thaumcraft";
	}

	@Override
	public void initialize() {
		Driver.add(new ConverterIAspectContainer());
		Driver.add(new DriverAspectContainer());
	}
}
