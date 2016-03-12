package li.cil.oc.api.internal;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.util.Vec3;

/**
 * This interface is implemented as a marker by drones.
 * <p/>
 * This is implemented by drones entities. That means you can use this to check
 * for drones by using:
 * <pre>
 *     if (entity instanceof Drone) {
 * </pre>
 * <p/>
 * The only purpose is to allow identifying entities as drones via the API,
 * i.e. without having to link against internal classes. This also means
 * that <em>you should not implement this</em>.
 */
public interface Drone extends Agent, EnvironmentHost, Rotatable, Tiered {
    /**
     * Get the current target coordinates of the drone.
     */
    Vec3 getTarget();

    /**
     * Set the new target coordinates of the drone.
     * <p/>
     * Note that the actual value used will use a reduced accuracy. This is
     * to avoid jitter on the client and floating point inaccuracies to
     * accumulate.
     */
    void setTarget(Vec3 value);

    /**
     * Get the drones velocity vector.
     * <p/>
     * Note that this is really just the underlying entity's <tt>motionX/Y/Z</tt>,
     * so you can cast this to {@link net.minecraft.entity.Entity} and use that
     * instead, if you'd like.
     */
    Vec3 getVelocity();
}
