package li.cil.oc.integration.cofh.tileentity;

import cofh.api.core.ISecurable;
import com.mojang.authlib.GameProfile;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.integration.ManagedTileEntityEnvironment;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.text.WordUtils;

public final class DriverSecureTile extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ISecurable.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final EnumFacing side) {
        return new Environment((ISecurable) world.getTileEntity(pos));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ISecurable> {
        public Environment(final ISecurable tileEntity) {
            super(tileEntity, "secure_tile");
        }

        @Callback(doc = "function(name:string):boolean --  Returns whether the player with the given name can access the component")
        public Object[] canPlayerAccess(final Context context, final Arguments args) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(args.checkString(0));
            return new Object[]{player != null && tileEntity.canPlayerAccess(player)};
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
