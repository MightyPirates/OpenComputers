package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

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
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, Minecraft.getMinecraft().getTextureManager().getTexture(location).getGlTextureId());
        final Tessellator t = Tessellator.getInstance();
        final WorldRenderer r = t.getWorldRenderer();
        r.startDrawingQuads();
        r.addVertexWithUV(0, 16, 0, 0, 1);
        r.addVertexWithUV(16, 16, 0, 1, 1);
        r.addVertexWithUV(16, 0, 0, 1, 0);
        r.addVertexWithUV(0, 0, 0, 0, 0);
        t.draw();
    }
}
