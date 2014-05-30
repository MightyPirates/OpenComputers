package li.cil.occ.mods.forestry;

import net.minecraftforge.common.Configuration;
import li.cil.oc.api.Driver;
import li.cil.occ.OpenComponents;
import li.cil.occ.mods.IMod;

public class ModForestry implements IMod{

	@Override
	public String getModId() {
		return "Forestry";
	}

	@Override
	public void initialize() {
		Driver.add(new ConventerIAlleles());
		Driver.add(new ConverterIIndividual());
		Driver.add(new DriverBeeHouse());
	}

}
