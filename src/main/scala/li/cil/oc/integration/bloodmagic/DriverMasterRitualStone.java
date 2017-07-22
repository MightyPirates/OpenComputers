package li.cil.oc.integration.bloodmagic;

import WayofTime.alchemicalWizardry.api.rituals.IMasterRitualStone;
import WayofTime.alchemicalWizardry.common.tileEntity.TEMasterStone;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class DriverMasterRitualStone extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IMasterRitualStone.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((IMasterRitualStone) world.getTileEntity(x, y, z));
    }

    public static class Environment extends ManagedTileEntityEnvironment<IMasterRitualStone> implements NamedBlock {
        public Environment(IMasterRitualStone tileEntity) {
            super(tileEntity, "master_ritual_stone");
        }

        @Override
        public String preferredName() {
            return "master_ritual_stone";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():string -- Get the name of the player owning this master ritual stone.")
        public Object[] getOwner(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getOwner()};
        }

        @Callback(doc = "function():string -- Get the current ritual.")
        public Object[] getCurrentRitual(final Context context, final Arguments arguments) {
            if (tileEntity instanceof TEMasterStone) {
                TEMasterStone masterStone = (TEMasterStone) tileEntity;
                return new Object[]{masterStone.getCurrentRitual()};
            }
            return new Object[]{"internal error"};
        }

        @Callback(doc = "function():number -- Get the remaining cooldown.")
        public Object[] getCooldown(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getCooldown()};
        }

        @Callback(doc = "function():number -- Get the running time.")
        public Object[] getRunningTime(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.getRunningTime()};
        }

        @Callback(doc = "function():boolean -- Get whether the tanks are empty.")
        public Object[] areTanksEmpty(final Context context, final Arguments arguments) {
            return new Object[]{tileEntity.areTanksEmpty()};
        }
    }
}
