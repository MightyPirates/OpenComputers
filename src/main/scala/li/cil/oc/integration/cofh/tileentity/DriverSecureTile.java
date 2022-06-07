package li.cil.oc.integration.cofh.tileentity;

import cofh.api.tileentity.ISecurable;
import com.mojang.authlib.GameProfile;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.text.WordUtils;

public final class DriverSecureTile extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ISecurable.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return new Environment((ISecurable) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ISecurable> {
        public Environment(final ISecurable tileEntity) {
            super(tileEntity, "secure_tile");
        }

        @Callback(doc = "function(name:string):boolean --  Returns whether the player with the given name can access the component")
        public Object[] canPlayerAccess(final Context context, final Arguments args) {
            return new Object[]{tileEntity.canPlayerAccess(MinecraftServer.getServer().getConfigurationManager().func_152612_a(args.checkString(0)))};
        }

        @Callback(doc = "function():string --  Returns the type of the access.")
        public Object[] getAccess(final Context context, final Arguments args) {
            return new Object[]{WordUtils.capitalize(tileEntity.getAccess().name())};
        }

        @Callback(doc = "function():string --  Returns the name of the owner.")
        public Object[] getOwnerName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getOwnerName()};
        }
    }
}
