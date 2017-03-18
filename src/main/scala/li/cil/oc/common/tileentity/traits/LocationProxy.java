package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.util.Location;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface LocationProxy extends Location {
    Location getLocation();

    @Override
    default World getHostWorld() {
        return getLocation().getHostWorld();
    }

    @Override
    default Vec3d getHostPosition() {
        return getLocation().getHostPosition();
    }

    @Override
    default BlockPos getHostBlockPosition() {
        return getLocation().getHostBlockPosition();
    }
}
