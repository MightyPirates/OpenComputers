package li.cil.occ.mods.forestry;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IMutation;
import li.cil.oc.api.driver.Converter;
import li.cil.occ.mods.Registry;

public class ConventerIAlleles implements Converter{

	@Override
	public void convert(Object value, Map<Object, Object> output) {
			if(value instanceof IMutation){
				IMutation mutation = (IMutation) value;
				
				IAllele allele1 = mutation.getAllele0();
				if (allele1 instanceof IAlleleSpecies){
					Map<Object, Object> allelMap1 = Maps.newHashMap();
					convert(allele1, allelMap1);
					output.put("allele1", allelMap1);
				}
				IAllele allele2 = mutation.getAllele1();
				if (allele2 instanceof IAlleleSpecies){
					Map<Object, Object> allelMap2 = Maps.newHashMap();
					convert(allele2, allelMap2);
					output.put("allele2", allelMap2);
				}
				output.put("chance", mutation.getBaseChance());
				output.put("specialConditions", mutation.getSpecialConditions().toArray());
			}
			
			if(value instanceof IAlleleSpecies){
				convertAlleleSpecies((IAlleleSpecies) value,output);	
			}
	}
	
	private void convertAlleleSpecies(IAlleleSpecies value,Map<Object, Object> output){
			IAlleleSpecies species = (IAlleleSpecies) value;
			output.put("name", species.getName());
			output.put("uid", species.getUID());
	}

}
