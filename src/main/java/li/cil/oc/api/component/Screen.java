package li.cil.oc.api.component;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.entity.player.EntityPlayer;

/**
 * This interface is implemented by screens' environments.
 * <p/>
 * This allows re-using the built-in screens in third-party code without
 * access to the internals of OC.
 * <p/>
 * To get an instance of the screen component, use its item driver, e.g.:
 * <pre>
 *     final ItemStack stack = li.cil.oc.api.Items.get("screen1").createItemStack(1);
 *     final Screen screen = (Screen) li.cil.oc.api.Driver.driverFor(stack).createEnvironment(stack, this);
 * </pre>
 */
public interface Screen extends ManagedEnvironment, Persistable {
    void setEnergyCostPerTick(double value);

    double getEnergyCostPerTick();

    void setPowerState(boolean value);

    boolean getPowerState();

    /**
     * Sets the maximum resolution supported by this screen.
     *
     * @param width  the maximum horizontal resolution, in characters.
     * @param height the maximum vertical resolution, in characters.
     */
    void setMaximumResolution(int width, int height);

    int getMaximumWidth();

    int getMaximumHeight();

    void setAspectRatio(double width, double height);

    double getAspectRatio();

    boolean setResolution(int width, int height);

    int getWidth();

    int getHeight();

    /**
     * Sets the maximum color depth supported by this screen.
     * <p/>
     * Note that this is the <em>maximum</em> supported depth, lower depths
     * will be supported, too. So when setting this to four bit, one bit will
     * be supported, too. When setting this to eight bit, four and one bit
     * will be supported, also.
     *
     * @param depth the maximum color depth of the screen.
     */
    void setMaximumColorDepth(ColorDepth depth);

    ColorDepth getMaximumColorDepth();

    boolean setColorDepth(ColorDepth depth);

    ColorDepth getColorDepth();

    void setPaletteColor(int index, int color);

    int getPaletteColor(int index);

    void setForegroundColor(int color);

    void setForegroundColor(int color, boolean isFromPalette);

    int getForegroundColor();

    boolean isForegroundFromPalette();

    void setBackgroundColor(int color);

    void setBackgroundColor(int color, boolean isFromPalette);

    int getBackgroundColor();

    boolean isBackgroundFromPalette();

    void copy(int column, int row, int width, int height, int horizontalTranslation, int verticalTranslation);

    void fill(int column, int row, int width, int height, char value);

    void set(int column, int row, String value);

    char get(int column, int row);

    /**
     * Renders the <em>text</em> displayed on the screen.
     * <p/>
     * You are responsible for setting up the actual context and applying any
     * transformations necessary to properly position and scale the text before
     * calling this. The text should be rendered on a black background.
     * <p/>
     * You can use this to either render the text in a GUI or in the world.
     */
    @SideOnly(Side.CLIENT)
    void renderText();

    /**
     * The natural width of the rendered text.
     * <p/>
     * This is the width of the complete text buffer, in pixels. In other
     * words, this is the width of the buffer in chars times the actual width
     * of a single char in pixels.
     *
     * @return the total width of the rendered buffer, in pixels.
     */
    @SideOnly(Side.CLIENT)
    int renderWidth();

    /**
     * The natural height of the rendered text.
     * <p/>
     * This is the height of the complete text buffer, in pixels. In other
     * words, this is the height of the buffer in chars times the actual height
     * of a single char in pixels.
     *
     * @return the total height of the rendered buffer, in pixels.
     */
    @SideOnly(Side.CLIENT)
    int renderHeight();

    @SideOnly(Side.CLIENT)
    void setRenderingEnabled(boolean enabled);

    @SideOnly(Side.CLIENT)
    boolean isRenderingEnabled();

    void keyDown(char character, int code, EntityPlayer player);

    void keyUp(char character, int code, EntityPlayer player);

    void clipboard(String value, EntityPlayer player);

    void mouseDown(int x, int y, int button, EntityPlayer player);

    void mouseDrag(int x, int y, int button, EntityPlayer player);

    void mouseUp(int x, int y, int button, EntityPlayer player);

    void mouseScroll(int x, int y, int delta, EntityPlayer player);

    /**
     * Used when setting a screens maximum color depth.
     */
    public static enum ColorDepth {
        /**
         * Monochrome color, black and white.
         */
        OneBit,

        /**
         * 16 color palette, defaults to Minecraft colors.
         */
        FourBit,

        /**
         * 240 colors, 16 color palette, defaults to grayscale.
         */
        EightBit
    }
}
