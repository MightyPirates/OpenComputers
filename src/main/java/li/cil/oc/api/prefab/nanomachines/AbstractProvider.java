package li.cil.oc.api.prefab.nanomachines;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Example base implementation of nanomachine behavior provider.
 * <p/>
 * This class takes care of handling the unique identifier used to tell
 * if a behavior is its own when loading from NBT.
 */
public abstract class AbstractProvider implements BehaviorProvider {
    private static final String TAG_PROVIDER = "provider";

    /**
     * Unique identifier used to tell if a behavior is ours when asked to load it.
     */
    protected final String id;

    /**
     * For the ID passed in here, it is suggested to use a one-time generated
     * UUID that is hard-coded into your provider implementation. Take care to
     * use a different one for each provider you create!
     *
     * @param id the unique identifier for this provider.
     */
    protected AbstractProvider(final String id) {
        if (id == null) throw new NullPointerException("id must not be null");
        this.id = id;
    }

    /**
     * Called when saving a behavior created using this behavior to NBT.
     * <p/>
     * The ID will already have been written, don't overwrite it. Store
     * any additional data you need to restore the behavior here, if any.
     *
     * @param behavior the behavior to persist.
     * @param nbt      the NBT tag to persist it to.
     */
    protected void writeBehaviorToNBT(final Behavior behavior, final NBTTagCompound nbt) {
    }

    /**
     * Called when loading a behavior from NBT.
     * <p/>
     * Use the data written in {@link #writeBehaviorToNBT} to restore the behavior
     * to its previous state, then return it.
     *
     * @param player the player to restore the behavior for.
     * @param nbt    the NBT tag to load restore the behavior from.
     * @return the restored behavior.
     */
    protected abstract Behavior readBehaviorFromNBT(final EntityPlayer player, final NBTTagCompound nbt);

    // ----------------------------------------------------------------------- //

    @Override
    public NBTTagCompound writeToNBT(final Behavior behavior) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString(TAG_PROVIDER, id);
        writeBehaviorToNBT(behavior, nbt);
        return nbt;
    }

    @Override
    public Behavior readFromNBT(final EntityPlayer player, final NBTTagCompound nbt) {
        if (id.equals(nbt.getString(TAG_PROVIDER))) {
            return readBehaviorFromNBT(player, nbt);
        } else {
            return null;
        }
    }
}
