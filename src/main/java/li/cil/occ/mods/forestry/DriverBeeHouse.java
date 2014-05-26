package li.cil.occ.mods.forestry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.genetics.*;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidTank;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import li.cil.occ.mods.vanilla.DriverFluidTank.Environment;

public class DriverBeeHouse extends DriverTileEntity{
	
	@Override
	public ManagedEnvironment createEnvironment(World world, int x, int y, int z) {
		 return new Environment((IBeeHousing)world.getBlockTileEntity(x, y, z));
	}

	@Override
	public Class<?> getTileEntityClass() {
		return IBeeHousing.class;
	}	
	
	public static final class Environment extends ManagedTileEntityEnvironment<IBeeHousing> {
			ConverterIIndividual converterIIndividual = new ConverterIIndividual();
	        public Environment(final IBeeHousing tileEntity) {
	            super(tileEntity, "bee_housing");
	        }

	        @Callback
	        public Object[] canBreed(final Context context, final Arguments args) {
	        	return new Object[]{tileEntity.canBreed()};
	        }

	        @Callback
	        public Object[] getDrone(final Context context, final Arguments args) {
	        	ItemStack drone = tileEntity.getDrone();
	        	if (drone != null) {
	        		return (Object[]) converterIIndividual.toLua(AlleleManager.alleleRegistry.getIndividual(drone));
	        	}
	        	return null;
	        }

	        @Callback
	        public Object[] getQueen(final Context context, final Arguments args) {
	        	ItemStack queen = tileEntity.getQueen();
	        	if (queen != null) {
	        		return (Object[]) converterIIndividual.toLua(AlleleManager.alleleRegistry.getIndividual(queen));
	        	}
	        	return null;
	        }
	        
	        @Callback
	        public Object[] getBeeBreedingData(final Context context, final Arguments args) {
	        	ISpeciesRoot beeRoot = AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
	        	if (beeRoot == null) {
	        		return null;
	        	}
	        	Map<Integer, Map<String, Object>> result = Maps.newHashMap();
	        	int j = 1;
	        	for (IMutation mutation : beeRoot.getMutations(false)) {
	        		HashMap<String, Object> mutationMap = new HashMap<String, Object>();
	        		IAllele allele1 = mutation.getAllele0();
	        		if (allele1 != null) {
	        			mutationMap.put("allele1", allele1.getName());
	        		}
	        		IAllele allele2 = mutation.getAllele1();
	        		if (allele2 != null) {
	        			mutationMap.put("allele2", allele2.getName());
	        		}
	        		mutationMap.put("chance", mutation.getBaseChance());
	        		mutationMap.put("specialConditions", mutation
					.getSpecialConditions().toArray());
	        		IAllele[] template = mutation.getTemplate();
	        		if (template != null && template.length > 0) {
	        			mutationMap.put("result", template[0].getName());
	        		}
	        		result.put(j++, mutationMap);
	        	}
	        	return new Object[]{result};
	        }

	    @Callback
		public Object[] listAllSpecies(final Context context, final Arguments args) {
	    	ISpeciesRoot beeRoot = AlleleManager.alleleRegistry
				.getSpeciesRoot("rootBees");
	    	if (beeRoot == null)
	    		return null;
	    	List<Map<String, String>> result = Lists.newArrayList();

	    	for (IMutation mutation : beeRoot.getMutations(false)) {
	    		IAllele[] template = mutation.getTemplate();
	    		if (template != null && template.length > 0) {
	    			IAllele allele = template[0];
				if (allele instanceof IAlleleSpecies)
					result.add(serializeSpecies((IAlleleSpecies) allele));
	    		}
	    	}
	    	return new Object[]{result};
	    }

	    @Callback
		public Object[] getBeeParents(final Context context, final Arguments args) {
	    	ISpeciesRoot beeRoot = AlleleManager.alleleRegistry
				.getSpeciesRoot("rootBees");
	    	if (beeRoot == null)
	    		return null;
	    	List<Map<String, Object>> result = Lists.newArrayList();
	    	String childType = args.checkString(0).toLowerCase();

	    	for (IMutation mutation : beeRoot.getMutations(false)) {
	    		IAllele[] template = mutation.getTemplate();
	    		if (template == null || template.length < 1)
	    			continue;

	    		IAllele allele = template[0];

	    		if (!(allele instanceof IAlleleSpecies))
	    			continue;
	    		IAlleleSpecies species = (IAlleleSpecies) allele;
	    		final String uid = species.getUID().toLowerCase();
	    		final String localizedName = species.getName().toLowerCase();
	    		if (localizedName.equals(childType) || uid.equals(childType)) {
	    			Map<String, Object> parentMap = serializeMutation(mutation);
					result.add(parentMap);
	    		}
	    	}
	    	return new Object[]{result};
	    }

	    private static Map<String, String> serializeSpecies(IAlleleSpecies species) {
	    	Map<String, String> result = Maps.newHashMap();
	    	result.put("name", species.getName());
	    	result.put("uid", species.getUID());
	    	return result;
	    }

	    private static Map<String, Object> serializeMutation(IMutation mutation) {
	    	Map<String, Object> parentMap = Maps.newHashMap();

	    	IAllele allele1 = mutation.getAllele0();
	    	if (allele1 instanceof IAlleleSpecies)
	    		parentMap
					.put("allele1", serializeSpecies((IAlleleSpecies) allele1));

	    	IAllele allele2 = mutation.getAllele1();
	    	if (allele2 instanceof IAlleleSpecies)
	    		parentMap
					.put("allele2", serializeSpecies((IAlleleSpecies) allele2));

	    	parentMap.put("chance", mutation.getBaseChance());
	    	parentMap.put("specialConditions", mutation.getSpecialConditions());
	    	return parentMap;
	    }
	}
}