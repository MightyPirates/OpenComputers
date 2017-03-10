package li.cil.oc.common.tileentity;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class AbstractTileEntityEnvironmentHost extends AbstractTileEntity implements EnvironmentHost {
    // ----------------------------------------------------------------------- //
    // EnvironmentHost

    @Override
    public World getHostWorld() {
        return getWorld();
    }

    @Override
    public Vec3d getHostPosition() {
        return new Vec3d(getPos()).addVector(0.5, 0.5, 0.5);
    }

    @Override
    public BlockPos getHostBlockPosition() {
        return getPos();
    }

    @Override
    public void markHostChanged() {
        final IThreadListener thread = getWorld().getMinecraftServer();
        if (thread != null) {
            thread.addScheduledTask(this::markDirty);
        } else {
            OpenComputers.log().warn("markHostChanged called on client side? Don't.");
        }
    }
}
