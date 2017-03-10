package li.cil.oc.integration.forestry;

import com.google.common.collect.Sets;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IMutation;
import forestry.api.genetics.ISpeciesRoot;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentItem;
import li.cil.oc.api.prefab.driver.AbstractDriverTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DriverBeeHouse extends AbstractDriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IBeeHousing.class;
    }

    @Override
    public EnvironmentItem createEnvironment(World world, BlockPos pos, EnumFacing side) {
        return new Environment((IBeeHousing) world.getTileEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IBeeHousing> implements NamedBlock {
        public Environment(final IBeeHousing tileEntity) {
            super(tileEntity, "bee_housing");
        }

        @Override
        public String preferredName() {
            return "bee_housing";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():boolean -- Can the bees breed?")
        public Object[] canBreed(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getBeekeepingLogic().canWork()};
        }

        @Callback(doc = "function():table -- Get the drone")
        public Object[] getDrone(final Context context, final Arguments args) {
            final ItemStack drone = tileEntity.getBeeInventory().getDrone();
            if (drone != null) {
                return new Object[]{AlleleManager.alleleRegistry.getIndividual(drone)};
            }
            return null;
        }

        @Callback(doc = "function():table -- Get the queen")
        public Object[] getQueen(final Context context, final Arguments args) {
            final ItemStack queen = tileEntity.getBeeInventory().getQueen();
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

            final Set<Map<String, Object>> result = Sets.newHashSet();
            for (IMutation mutation : beeRoot.getMutations(false)) {
                final HashMap<String, Object> mutationMap = new HashMap<String, Object>();

                final IAllele allele1 = mutation.getAllele0();
                if (allele1 != null) {
                    mutationMap.put("allele1", allele1.getName());
                }

                final IAllele allele2 = mutation.getAllele1();
                if (allele2 != null) {
                    mutationMap.put("allele2", allele2.getName());
                }

                mutationMap.put("chance", mutation.getBaseChance());
                mutationMap.put("specialConditions", mutation
                        .getSpecialConditions().toArray());

                final IAllele[] template = mutation.getTemplate();
                if (template != null && template.length > 0) {
                    mutationMap.put("result", template[0].getName());
                }
                result.add(mutationMap);
            }
            return new Object[]{result};
        }

        @Callback(doc = "function():table -- Get all known bees mutations")
        public Object[] listAllSpecies(final Context context, final Arguments args) {
            final ISpeciesRoot beeRoot = AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
            if (beeRoot == null) {
                return null;
            }

            final Set<IAlleleSpecies> result = Sets.newHashSet();
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
            return new Object[]{result};
        }

        @Callback(doc = "function(beeName:string):table -- Get the parents for a particular mutation")
        public Object[] getBeeParents(final Context context, final Arguments args) {
            final ISpeciesRoot beeRoot = AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
            if (beeRoot == null) {
                return null;
            }

            final Set<IMutation> result = Sets.newHashSet();
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
            return new Object[]{result};
        }
    }
}