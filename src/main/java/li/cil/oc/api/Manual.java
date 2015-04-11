package li.cil.oc.api;

import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * This API allows interfacing with the in-game manual of OpenComputers.
 * <p/>
 * It allows opening the manual at a desired specific page, as well as
 * registering custom tabs and content callback handlers.
 * <p/>
 * Note: this is a <em>client side only</em> API. It will do nothing on
 * dedicated servers (i.e. <tt>API.manual</tt> will be <tt>null</tt>).
 */
public class Manual {
    /**
     * Register a tab to be displayed next to the manual.
     * <p/>
     * These are intended to link to index pages, and for the time being there
     * a relatively low number of tabs that can be displayed, so I'd ask you to
     * only register as many tabs as actually, technically *needed*. Which will
     * usually be one, for your main index page.
     *
     * @param renderer the renderer used to render the icon on your tab.
     * @param tooltip  the unlocalized tooltip of the tab, or <tt>null</tt>.
     * @param path     the path to the page to open when the tab is clicked.
     */
    public static void addTab(TabIconRenderer renderer, String tooltip, String path) {
        if (API.manual != null)
            API.manual.addTab(renderer, tooltip, path);
    }

    /**
     * Register a path provider.
     * <p/>
     * Path providers are used to find documentation entries for item stacks
     * and blocks in the world.
     *
     * @param provider the provider to register.
     */
    public static void addProvider(PathProvider provider) {
        if (API.manual != null)
            API.manual.addProvider(provider);
    }

    /**
     * Register a content provider.
     * <p/>
     * Content providers are used to resolve paths to page content, if the
     * standard system (using Minecraft's resource loading facilities) fails.
     * <p/>
     * This can be useful for providing dynamic content, for example.
     *
     * @param provider the provider to register.
     */
    public static void addProvider(ContentProvider provider) {
        if (API.manual != null)
            API.manual.addProvider(provider);
    }

    /**
     * Register an image provider.
     * <p/>
     * Image providers are used to render custom content in a page. These are
     * selected via the standard image tag of Markdown, based on the prefix of
     * the image URL, i.e. <tt>![tooltip](prefix:data)</tt> will select the
     * image provider registered for the prefix <tt>prefix</tt>, and pass to
     * it the argument <tt>data</tt>, then use the returned renderer to draw
     * an element in the place of the tag. The provided prefix is expected to
     * be <em>without</em> the colon (<tt>:</tt>).
     * <p/>
     * Custom providers are only selected if a prefix is matched, otherwise
     * it'll treat it as a relative path to an image to load via Minecraft's
     * resource providing facilities, and display that.
     *
     * @param prefix   the prefix on which to use the provider.
     * @param provider the provider to register.
     */
    public static void addProvider(String prefix, ImageProvider provider) {
        if (API.manual != null)
            API.manual.addProvider(prefix, provider);
    }

    /**
     * Get the image renderer for the specified image path.
     * <p/>
     * This will look for {@link ImageProvider}s registered for a prefix in the
     * specified path. If there is no match, or the matched content provider
     * does not provide a renderer, this will return <tt>null</tt>.
     *
     * @param path the path to the image to get the renderer for.
     * @return the custom renderer for that path.
     */
    public static ImageRenderer imageFor(String path) {
        if (API.manual != null)
            return API.manual.imageFor(path);
        return null;
    }

    // ----------------------------------------------------------------------- //

    /**
     * Look up the documentation path for the specified item stack.
     *
     * @param stack the stack to find the documentation path for.
     * @return the path to the page, <tt>null</tt> if none is known.
     */
    public static String pathFor(ItemStack stack) {
        if (API.manual != null)
            return API.manual.pathFor(stack);
        return null;
    }

    /**
     * Look up the documentation for the specified block in the world.
     *
     * @param world the world containing the block.
     * @param x     the X coordinate of the block.
     * @param y     the Y coordinate of the block.
     * @param z     the Z coordinate of the block.
     * @return the path to the page, <tt>null</tt> if none is known.
     */
    public static String pathFor(World world, int x, int y, int z) {
        if (API.manual != null)
            return API.manual.pathFor(world, x, y, z);
        return null;
    }

    /**
     * Get the content of the documentation page at the specified location.
     *
     * @param path the path of the page to get the content of.
     * @return the content of the page, or <tt>null</tt> if none exists.
     */
    public static Iterable<String> contentFor(String path) {
        if (API.manual != null)
            return API.manual.contentFor(path);
        return null;
    }

    // ----------------------------------------------------------------------- //

    /**
     * Open the manual for the specified player.
     * <p/>
     * If you wish to display a specific page, call {@link #navigate(String)}
     * after this function returns, with the path to the page to show.
     *
     * @param player the player to open the manual for.
     */
    public static void openFor(EntityPlayer player) {
        if (API.manual != null)
            API.manual.openFor(player);
    }

    /**
     * Reset the history of the manual.
     */
    public static void reset() {
        if (API.manual != null)
            API.manual.reset();
    }

    /**
     * Navigate to a page in the manual.
     *
     * @param path the path to navigate to.
     */
    public static void navigate(String path) {
        if (API.manual != null)
            API.manual.navigate(path);
    }

    // ----------------------------------------------------------------------- //

    private Manual() {
    }
}
