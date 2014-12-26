package li.cil.oc.api.internal;

import net.minecraft.item.EnumDyeColor;

/**
 * This interface is implemented by tile entities that can be colored by
 * players, such as screens, computer cases and cables.
 * <p/>
 * Colors are in the common <tt>RRGGBB</tt> format.
 * <p/>
 * This interface is <em>not meant to be implemented</em>, just used.
 */
public interface Colored {
    /**
     * Get the current color value.
     *
     * @return the current color value.
     */
    EnumDyeColor getColor();

    /**
     * Set the color value.
     *
     * @param value the new color value.
     */
    void setColor(EnumDyeColor value);
}
