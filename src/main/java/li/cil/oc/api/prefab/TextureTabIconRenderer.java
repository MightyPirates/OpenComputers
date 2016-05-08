package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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
        final VertexBuffer r = t.getBuffer();
        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        r.pos(0, 16, 0).tex(0, 1).endVertex();
        r.pos(16, 16, 0).tex(1, 1).endVertex();
        r.pos(16, 0, 0).tex(1, 0).endVertex();
        r.pos(0, 0, 0).tex(0, 0).endVertex();
        t.draw();
    }
}
