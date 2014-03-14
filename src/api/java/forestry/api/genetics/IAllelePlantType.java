package forestry.api.genetics;

import java.util.EnumSet;

import net.minecraftforge.common.EnumPlantType;

public interface IAllelePlantType extends IAllele {

	public EnumSet<EnumPlantType> getPlantTypes();

}
