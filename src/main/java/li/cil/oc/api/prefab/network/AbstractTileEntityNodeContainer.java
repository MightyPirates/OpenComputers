package li.cil.oc.api.prefab.network;

import li.cil.oc.api.util.Location;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@SuppressWarnings("UnusedDeclaration")
public abstract class AbstractTileEntityNodeContainer extends AbstractNodeContainer {
    public AbstractTileEntityNodeContainer(final TileEntity tileEntity) {
        super(new LocationTileEntity(tileEntity));
    }

    // ----------------------------------------------------------------------- //

    private static final class LocationTileEntity implements Location {
        // ----------------------------------------------------------------------- //
        // Computed data.

        private final TileEntity tileEntity;

        // ----------------------------------------------------------------------- //

        LocationTileEntity(final TileEntity tileEntity) {
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
            return new Vec3d(tileEntity.getPos()).addVector(0.5, 0.5, 0.5);
        }

        @Override
        public BlockPos getHostBlockPosition() {
            return tileEntity.getPos();
        }
    }
}
