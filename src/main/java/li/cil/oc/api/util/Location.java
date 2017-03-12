package li.cil.oc.api.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface Location {
    /**
     * The world the host lives in.
     */
    World getHostWorld();

    /**
     * The host's position in the world.
     * <p/>
     * For tile entities this is the <em>centered</em> position. For example,
     * if the tile entity is located at (0, -2, 3) this will be (0.5, -1.5, 3.5).
     */
    Vec3d getHostPosition();

    /**
     * The host's block position in the world.
     * <p/>
     * For entities this is the <em>containing</em> block position. For example,
     * if the entity is located at (0.3, -2.5, 3.3) this will be (0, -3, 3).
     */
    BlockPos getHostBlockPosition();
}
