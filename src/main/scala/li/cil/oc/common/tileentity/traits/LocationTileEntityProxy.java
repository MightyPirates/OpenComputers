package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.util.Location;
import li.cil.oc.util.BlockPosUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface LocationTileEntityProxy extends Location, TileEntityAccess {
    @Override
    default World getHostWorld() {
        return getTileEntity().getWorld();
    }

    @Override
    default Vec3d getHostPosition() {
        return BlockPosUtils.getCenter(getTileEntity().getPos());
    }

    @Override
    default BlockPos getHostBlockPosition() {
        return getTileEntity().getPos();
    }
}
