package mods.railcraft.api.tracks;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Used by rails that modify the bounding boxes.
 *
 * For example, the Gated Rails.
 *
 * Not very useful since there is no system in place to insert custom render code.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface ITrackCustomShape extends ITrackInstance
{

    public AxisAlignedBB getCollisionBoundingBoxFromPool();

    public AxisAlignedBB getSelectedBoundingBoxFromPool();

    public MovingObjectPosition collisionRayTrace(Vec3 vec3d, Vec3 vec3d1);
}
