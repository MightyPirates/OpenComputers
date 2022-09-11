package li.cil.oc.client.renderer
;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.BufferBuilder.DrawState;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.system.MemoryUtil;

public class RenderCache implements IRenderTypeBuffer {
    public static class DrawEntry {
        private final DrawState state;
        private final ByteBuffer data;

        public DrawEntry(DrawState state, ByteBuffer data, boolean copy) {
            this.state = state;
            if (copy)
            {
                int bufferCap = state.format().getVertexSize() * state.vertexCount();
                ByteBuffer temp = GLAllocation.createByteBuffer(bufferCap);
                temp.put(data).flip();
                data = temp;
            }
            this.data = data;
        }

        public DrawState state() {
            return state;
        }

        public ByteBuffer data() {
            return data;
        }
    }

    private final Map<RenderType, List<DrawEntry>> cached;
    private RenderType activeType;
    private BufferBuilder activeBuilder;

    public RenderCache() {
        cached = new HashMap<>();
    }

    public boolean isEmpty() {
        for (List<DrawEntry> frames : cached.values()) {
            if (!frames.isEmpty()) return false;
        }
        return true;
    }

    public void clear() {
        cached.forEach((k, v) -> v.clear());
    }

    private void flush(RenderType type) {
        if (type == activeType) {
            activeBuilder.end();
            List<DrawEntry> frames = cached.computeIfAbsent(type, k -> new ArrayList<>());
            Pair<DrawState, ByteBuffer> rendered = activeBuilder.popNextBuffer();
            if (rendered.getSecond().hasRemaining()) {
                frames.add(new DrawEntry(rendered.getFirst(), rendered.getSecond(), true));
            }
            activeType = null;
        }
    }

    @Override
    public IVertexBuilder getBuffer(RenderType type) {
        if (type == null) throw new NullPointerException(); // Same as vanilla.
        if (activeType != null) {
            if (activeType == type) return activeBuilder;
            flush(activeType);
        }
        activeType = type;
        activeBuilder = Tessellator.getInstance().getBuilder();
        activeBuilder.clear();
        activeBuilder.begin(type.mode(), type.format());
        return activeBuilder;
    }

    public void finish() {
        // Flush the last active type (if any) so it gets rendered too.
        if (activeType != null) flush(activeType);
    }

    public void render(MatrixStack stack) {
        // Apply transform globally so we don't have to update stored vertices.
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(stack.last().pose());

        cached.forEach((type, frames) -> {
            if (!frames.isEmpty()) {
                type.setupRenderState();
                frames.forEach(frame -> {
                    DrawState state = frame.state();
                    state.format().setupBufferState(MemoryUtil.memAddress(frame.data()));
                    RenderSystem.drawArrays(state.mode(), 0, state.vertexCount());
                    state.format().clearBufferState();
                });
                type.clearRenderState();
            }
        });

        RenderSystem.popMatrix();
    }
}