package li.cil.oc.api.manual;

/**
 * Allows implementing advanced image renderers that react to mouse input and
 * specify customized tooltips.
 * <p/>
 * This way you can e.g. disable the default tooltip and render a more advanced
 * one, or render a small GUI on a page.
 */
public interface InteractiveImageRenderer extends ImageRenderer {
    /**
     * Get a custom tooltip for this image renderer.
     * <p/>
     * This can be used to override the original tooltip of an image.
     *
     * @param tooltip the original tooltip of the element.
     * @return the tooltip to use for the element.
     */
    String getTooltip(String tooltip);

    /**
     * Called when the mouse is clicked while over this image renderer.
     * <p/>
     * This only fires for left-clicks, because right-clicks are reserved for
     * navigating back in the manual.
     * <p/>
     * If this returns <tt>false</tt> and the element is a link, the link will
     * be followed. If it returns <tt>true</tt>, it will not.
     *
     * @param mouseX the X coordinate of the mouse, relative to the element.
     * @param mouseY the Y coordinate of the mouse, relative to the element.
     * @return whether the click was handled.
     */
    boolean onMouseClick(int mouseX, int mouseY);
}
