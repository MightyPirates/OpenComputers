package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

/**
 * Simple implementation of a tab icon renderer using an icon as its graphic.
 */
@SuppressWarnings("UnusedDeclaration")
public class IconTabIconRenderer implements TabIconRenderer {
    private final IIcon icon;

    public IconTabIconRenderer(IIcon icon) {
        this.icon = icon;
    }

    @Override
    public void render() {
        final Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(0, 16, 0, icon.getMinU(), icon.getMaxV());
        t.addVertexWithUV(16, 16, 0, icon.getMaxU(), icon.getMaxV());
        t.addVertexWithUV(16, 0, 0, icon.getMaxU(), icon.getMinV());
        t.addVertexWithUV(0, 0, 0, icon.getMinU(), icon.getMinV());
        t.draw();
    }
}
