package li.cil.oc.client.renderer.textbuffer;

import li.cil.oc.client.renderer.font.FontTextureProvider;
import li.cil.oc.client.renderer.font.TextBufferRenderData;

public interface TextBufferRenderer {
    boolean render(FontTextureProvider fontTextureProvider, TextBufferRenderData data);
    boolean destroy();
}
