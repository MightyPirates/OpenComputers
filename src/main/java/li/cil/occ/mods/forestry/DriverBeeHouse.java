package li.cil.occ.mods.forestry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.genetics.*;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverBeeHouse extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IBeeHousing.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IBeeHousing) world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IBeeHousing> {
        public Environment(final IBeeHousing tileEntity) {
            super(tileEntity, "bee_housing");
        }

        @Callback(doc = "function():boolean -- Can the bees breed?")
        public Object[] canBreed(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canBreed()};
        }

        @Callback(doc = "function():table -- Get the drone")
        public Object[] getDrone(final Context context, final Arguments args) {
            final ItemStack drone = tileEntity.getDrone();
            if (drone != null) {
                return new Object[]{AlleleManager.alleleRegistry.getIndividual(drone)};
            }
            return null;
        }

        @Callback(doc = "function():table -- Get the queen")
        public Object[] getQueen(final Context context, final Arguments args) {
            final ItemStack queen = tileEntity.getQueen();
            if (queen != null) {
                return new Object[]{AlleleManager.alleleRegistry.getIndividual(queen)};
            }
            return null;
        }

        @Callback(doc = "function():table -- Get the full breeding list thingy.")
        public Object[] getBeeBreedingData(final Context context, final Arguments args) {
            final ISpeciesRoot beeRoot = AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
            if (beeRoot == null) {
                return null;
            }

            final Map<Integer, Map<String, Object>> result = Maps.newHashMap();
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

        @Callback(doc = "function():table -- Get all known bees mutations")
        public Object[] listAllSpecies(final Context context, final Arguments args) {
            final ISpeciesRoot beeRoot = AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
            if (beeRoot == null) {
                return null;
            }

            final List<IAlleleSpecies> result = Lists.newArrayList();
            for (IMutation mutation : beeRoot.getMutations(false)) {
                final IAllele[] template = mutation.getTemplate();
                if (template == null || template.length <= 0) {
                    continue;
                }

                final IAllele allele = template[0];
                if (!(allele instanceof IAlleleSpecies)) {
                    continue;
                }

                result.add((IAlleleSpecies) allele);
            }
            return new Object[]{result.toArray(new IAlleleSpecies[result.size()])};
        }

        @Callback(doc = "function(beeName:string):table -- Get the parents for a particular mutation")
        public Object[] getBeeParents(final Context context, final Arguments args) {
            final ISpeciesRoot beeRoot = AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
            if (beeRoot == null) {
                return null;
            }

            List<IMutation> result = Lists.newArrayList();
            final String childType = args.checkString(0).toLowerCase();
            for (IMutation mutation : beeRoot.getMutations(false)) {
                final IAllele[] template = mutation.getTemplate();
                if (template == null || template.length < 1) {
                    continue;
                }

                final IAllele allele = template[0];
                if (!(allele instanceof IAlleleSpecies)) {
                    continue;
                }

                final IAlleleSpecies species = (IAlleleSpecies) allele;
                final String uid = species.getUID().toLowerCase();
                final String localizedName = species.getName().toLowerCase();
                if (localizedName.equals(childType) || uid.equals(childType)) {
                    result.add(mutation);
                }
            }
            return new Object[]{result.toArray(new IMutation[result.size()])};
        }
    }
}