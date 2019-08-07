package li.cil.oc.client.renderer.textbuffer;

import li.cil.oc.client.renderer.font.FontTextureProvider;
import li.cil.oc.client.renderer.font.TextBufferRenderData;
import net.minecraft.profiler.Profiler;

public interface TextBufferRenderer {
    boolean render(Profiler profiler, FontTextureProvider fontTextureProvider, TextBufferRenderData data);
    boolean destroy();
}
