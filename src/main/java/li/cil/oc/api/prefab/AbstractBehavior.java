package li.cil.oc.api.prefab;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Base class for behaviors, mostly useful to have less cluttered classes when
 * you only need one or two of the methods in the interface.
 * <p/>
 * This implementation will also store the player the behavior was created for.
 */
public abstract class AbstractBehavior implements Behavior {
    /**
     * The player this behavior was created for.
     */
    public final EntityPlayer player;

    /**
     * Pass along the player the behavior was created for here to have it stored
     * for later use.
     *
     * @param player the player the behavior was created for.
     */
    protected AbstractBehavior(EntityPlayer player) {
        this.player = player;
    }

    /**
     * Use this if you do not need the player reference in your implementation.
     */
    protected AbstractBehavior() {
        this(null);
    }

    @Override
    public String getNameHint() {
        return null;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable(DisableReason reason) {
    }

    @Override
    public void update() {
    }
}
