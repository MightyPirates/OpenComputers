package li.cil.oc.api.nanomachines;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Implemented by providers for behaviors.
 * <p/>
 * You may implement one provider for each of your behaviors, or one provider
 * for all of your behaviors; it really doesn't matter. This just allows for
 * some logical grouping of behaviors, where desired.
 * <p/>
 * Each behavior provider must be capable or serializing the behaviors it
 * creates, and re-create the behavior from its serialized form. It will
 * not be given any hints as to whether a provided tag was originally
 * produced by it, so you should add a sufficiently unique marker to the
 * output NBT to allow identification later on. I recommend generating a
 * UUID once, and using that. This is necessary to both save and restore
 * neural connection state between saves without breaking the state when
 * new behaviors are added, as well as to send states to the client.
 */
public interface BehaviorProvider {
    /**
     * Create all behaviors valid for the specified player.
     * <p/>
     * Note that this is only called on the server side when reconfiguring
     * nanomachines. If you have a behavior that actually acts client-only,
     * you still need to return it here, as it will be synchronized to the
     * client using {@link #writeToNBT} and {@link #readFromNBT}.
     *
     * @param player the player the behaviors should be created for.
     * @return list of new behaviors, may be <tt>null</tt>.
     */
    Iterable<Behavior> createBehaviors(EntityPlayer player);

    /**
     * Write a behavior to NBT.
     * <p/>
     * This will only be called for behaviors originally created by this provider.
     * <p/>
     * This will only be called on the server. All behaviors not saved will be
     * lost when loading again, they will <em>not</em> be regenerated using
     * {@link #createBehaviors}, so make sure to save all your behaviors.
     *
     * @param behavior the behavior to serialize.
     * @return the serialized representation of the specified behavior.
     */
    NBTTagCompound writeToNBT(Behavior behavior);

    /**
     * Restore a behavior from NBT.
     * <p/>
     * You are <em>not</em> guaranteed that his nbt belongs to a behavior
     * created by this provider! If the NBT cannot be handled, return
     * <tt>null</tt>.
     * <p/>
     * This is called both on the server and the client; on the server it
     * is called when restoring a saved player, on the client when
     * synchronizing a configuration.
     *
     * @param player the player the behaviors should be created for.
     * @param nbt    the tag to restore the behavior from.
     * @return the restored behavior, or <tt>null</tt> if unhandled.
     */
    Behavior readFromNBT(EntityPlayer player, NBTTagCompound nbt);
}
