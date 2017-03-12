package li.cil.oc.common.tileentity.traits;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.NodeContainerHost;
import li.cil.oc.util.BlockPosUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class NodeContainerHostTileEntity implements NodeContainerHost {
    // ----------------------------------------------------------------------- //
    // Computed data.

    private final TileEntity tileEntity;
    private final Object lock = new Object();
    private boolean isMarkDirtyScheduled;

    public NodeContainerHostTileEntity(final TileEntity tileEntity) {
        this.tileEntity = tileEntity;
    }

    // ----------------------------------------------------------------------- //
    // Location

    @Override
    public World getHostWorld() {
        return tileEntity.getWorld();
    }

    @Override
    public Vec3d getHostPosition() {
        return BlockPosUtils.getCenter(tileEntity.getPos());
    }

    @Override
    public BlockPos getHostBlockPosition() {
        return tileEntity.getPos();
    }

    // ----------------------------------------------------------------------- //
    // NodeContainerHost

    @Override
    public void markHostChanged() {
        final IThreadListener thread = getHostWorld().getMinecraftServer();
        if (thread != null) {
            synchronized (lock) {
                if (!isMarkDirtyScheduled) {
                    thread.addScheduledTask(this::markDirty);
                    isMarkDirtyScheduled = true;
                }
            }
        } else {
            OpenComputers.log().warn("markHostChanged called on client side? Don't.");
        }
    }

    // ----------------------------------------------------------------------- //

    public void markDirty() {
        isMarkDirtyScheduled = false;
        tileEntity.markDirty();
    }
}
