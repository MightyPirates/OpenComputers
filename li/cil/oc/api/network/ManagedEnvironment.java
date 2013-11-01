package li.cil.oc.api.network;

import li.cil.oc.api.Persistable;

/**
 * This kind of environment is managed by either a compatible inventory, such
 * as a computer or floppy drive, or by an adapter block or similar.
 * <p/>
 * This means its update and save/load methods will be called by their logical
 * container. This is required for item environments, and for block
 * environments that cannot be directly integrated into a block's tile entity,
 * for example because you have no direct control over the block (e.g. what we
 * do with the command block).
 * <p/>
 * You should <em>not</em> implement this interface in your tile entities, or
 * weird things may happen (e.g. update and save/load being called multiple
 * times).
 */
public interface ManagedEnvironment extends Environment, Persistable {
    /**
     * This is called by the host of this managed environment once per tick.
     */
    void update();
}
