package li.cil.oc.api.tileentity;

import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * This interface is implemented by tile entities that can be colored by
 * players, such as screens, computer cases and cables.
 * <p/>
 * Colors are in the common <tt>RRGGBB</tt> format.
 * <p/>
 * This interface is <em>not meant to be implemented</em>, just used.
 */
public interface Colored extends INBTSerializable<NBTTagInt> {
    /**
     * Get the current color value.
     *
     * @return the current color value.
     */
    int getColor();

    /**
     * Set the color value.
     *
     * @param value the new color value.
     * @return <code>true</code> if the value changed; <code>false</code> otherwise.
     */
    boolean setColor(int value);

    boolean consumesDye();

    /**
     * Whether the color of the implementing object controls how it can connect
     * to other objects. If this is <tt>true</tt> for <em>both</em> involved
     * objects, silver/light gray objects connect to any other object, but
     * objects of otherwise different color do not connect to each other. If
     * this is <tt>false</tt> for <em>either</em> of the two objects, they may
     * always connect to each other.
     *
     * @return whether the color influences this object's connectivity.
     */
    boolean controlsConnectivity();
}
