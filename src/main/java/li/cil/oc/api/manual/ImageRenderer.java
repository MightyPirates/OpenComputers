package li.cil.oc.api.manual;

/**
 * This allows implementing custom image renderers.
 * <p/>
 * Image renderers are used to draw custom areas in a manual page, defined as
 * an image with a special URL, matching the prefix of a registered image
 * provider. A renderer will then be used to draw something at the position
 * of the image tag.
 * <p/>
 * Built-in image renderers are <tt>item</tt>, <tt>block</tt> and <tt>oredict</tt>.
 */
public interface ImageRenderer {
    /**
     * The width of the area this renderer uses.
     * <p/>
     * This is used to offset the OpenGL state properly before calling
     * {@link #render(int, int)}, to correctly align the image horizontally.
     *
     * @return the width of the rendered image.
     */
    int getWidth();

    /**
     * The height of the area this renderer uses.
     * <p/>
     * This is used to offset the OpenGL state properly before calling
     * {@link #render(int, int)}, as well as to know where to resume rendering
     * other content below the image.
     *
     * @return the height of the rendered image.
     */
    int getHeight();

    /**
     * Render the image, with specified maximum width.
     * <p/>
     * This should render the image as is, the OpenGL state will be set up
     * such that you can start drawing at (0,0,*), and render up to
     * (getWidth,getHeight,*), i.e. translation and scaling are taken care
     * of for you.
     *
     * @param mouseX the X position of the mouse relative to the element.
     * @param mouseY the Y position of the mouse relative to the element.
     */
    void render(int mouseX, int mouseY);
}
