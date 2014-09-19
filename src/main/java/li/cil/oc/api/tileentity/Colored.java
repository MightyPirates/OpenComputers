package li.cil.oc.api.tileentity;

/**
 * This interface is implemented by tile entities that can be colored by
 * players, such as screens, computer cases and cables.
 * <p/>
 * Colors are in the common <tt>RRGGBB</tt> format.
 * <p/>
 * <em>This interface is not meant to be implemented, just used</em>
 */
public interface Colored {
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
     */
    void setColor(int value);
}
