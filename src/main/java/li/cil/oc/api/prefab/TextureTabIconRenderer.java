package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
    @OnlyIn(Dist.CLIENT)
    public void render() {
        Minecraft.getInstance().getTextureManager().bind(location);
        final Tessellator t = Tessellator.getInstance();
        final BufferBuilder r = t.getBuilder();
        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        r.vertex(0, 16, 0).uv(0, 1).endVertex();
        r.vertex(16, 16, 0).uv(1, 1).endVertex();
        r.vertex(16, 0, 0).uv(1, 0).endVertex();
        r.vertex(0, 0, 0).uv(0, 0).endVertex();
        t.end();
    }
}
