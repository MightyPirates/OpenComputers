package li.cil.oc.api.prefab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

/**
 * Simple implementation of a tab icon renderer using a full texture as its graphic.
 */
@SuppressWarnings("UnusedDeclaration")
public class TextureTabIconRenderer implements TabIconRenderer {
    private final ResourceLocation location;

    public TextureTabIconRenderer(ResourceLocation location) {
        this.location = location;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void render() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(location);
        final Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.addVertexWithUV(0, 16, 0, 0, 1);
        t.addVertexWithUV(16, 16, 0, 1, 1);
        t.addVertexWithUV(16, 0, 0, 1, 0);
        t.addVertexWithUV(0, 0, 0, 0, 0);
        t.draw();
    }
}
