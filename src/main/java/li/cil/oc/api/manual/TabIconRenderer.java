package li.cil.oc.api.manual;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Allows defining a renderer for a manual tab.
 * <p/>
 * Each renderer instance represents the single graphic it is drawing. To
 * provide different graphics for different tabs you'll need to create
 * multiple tab renderer instances.
 * <p/>
 *
 * @see li.cil.oc.api.prefab.ItemStackTabIconRenderer
 * @see li.cil.oc.api.prefab.TextureTabIconRenderer
 */
public interface TabIconRenderer {
    /**
     * Called when icon of a tab should be rendered.
     * <p/>
     * This should render something in a 16x16 area. The OpenGL state has been
     * adjusted so that drawing starts at (0,0,0), and should go to (16,16,0).
     */
    @SideOnly(Side.CLIENT)
    void render();
}
