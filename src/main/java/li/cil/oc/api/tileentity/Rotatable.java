package li.cil.oc.api.tileentity;

import li.cil.oc.api.util.Pitch;
import li.cil.oc.api.util.Yaw;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * This interface is provided as a capability by some blocks.
 * <p/>
 * This interface is <em>not meant to be implemented</em>, just used.
 */
public interface Rotatable extends INBTSerializable<NBTTagByte> {
    Pitch getPitch();

    boolean setPitch(final Pitch value);

    Yaw getYaw();

    boolean setYaw(final Yaw value);

    boolean rotate(final EnumFacing.Axis around);

    EnumFacing getFacing();

    EnumFacing[] getValidRotations();

    /**
     * Converts a facing relative to the block's <em>local</em> coordinate
     * system to a <tt>global orientation</tt>, using south as the standard
     * orientation.
     * <p/>
     * For example, if the block is facing east, calling this with south will
     * return east, calling it with west will return south and so on.
     *
     * @param value the value to translate.
     * @return the translated orientation.
     */
    EnumFacing toGlobal(final EnumFacing value);

    /**
     * Converts a <tt>global</tt> orientation to a facing relative to the
     * block's <em>local</em> coordinate system, using south as the standard
     * orientation.
     * <p/>
     * For example, if the block is facing east, calling this with south will
     * return east, calling it with west will return north and so on.
     *
     * @param value the value to translate.
     * @return the translated orientation.
     */
    EnumFacing toLocal(final EnumFacing value);
}
