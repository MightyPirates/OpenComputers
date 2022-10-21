package li.cil.oc.client.renderer;

import java.util.OptionalDouble;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.Textures;
import net.minecraft.client.renderer.RenderState.LineState;
import net.minecraft.client.renderer.RenderState.TextureState;
import net.minecraft.client.renderer.RenderState.TexturingState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.State;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderTypes extends RenderType {
    public static final VertexFormat POSITION_TEX_NORMAL = new VertexFormat(new ImmutableList.Builder<VertexFormatElement>()
        .add(DefaultVertexFormats.ELEMENT_POSITION)
        .add(DefaultVertexFormats.ELEMENT_UV0)
        .add(DefaultVertexFormats.ELEMENT_NORMAL)
        .add(DefaultVertexFormats.ELEMENT_PADDING)
        .build());

    public static final TextureState ROBOT_CHASSIS_TEXTURE = new TextureState(Textures.Model$.MODULE$.Robot(), false, false);

    public static final RenderType ROBOT_CHASSIS = create(OpenComputers.ID() + ":robot_chassis",
        DefaultVertexFormats.BLOCK, GL11.GL_TRIANGLES, 1024, State.builder()
            .setTextureState(ROBOT_CHASSIS_TEXTURE)
            .setDiffuseLightingState(DIFFUSE_LIGHTING)
            .setLightmapState(LIGHTMAP)
            .createCompositeState(true));

    public static final RenderType ROBOT_LIGHT = create(OpenComputers.ID() + ":robot_light",
        DefaultVertexFormats.POSITION_COLOR_TEX, GL11.GL_QUADS, 256, State.builder()
            .setTextureState(ROBOT_CHASSIS_TEXTURE)
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .createCompositeState(true));

    private static final RenderType createUpgrade(String name, ResourceLocation texture) {
        return create(OpenComputers.ID() + ":upgrade_" + name,
            POSITION_TEX_NORMAL, GL11.GL_QUADS, 1024, State.builder()
                .setTextureState(new TextureState(texture, false, false))
                .createCompositeState(true));
    }

    public static final RenderType UPGRADE_CRAFTING = createUpgrade("crafting", Textures.Model$.MODULE$.UpgradeCrafting());

    public static final RenderType UPGRADE_GENERATOR = createUpgrade("generator", Textures.Model$.MODULE$.UpgradeGenerator());

    public static final RenderType UPGRADE_INVENTORY = createUpgrade("inventory", Textures.Model$.MODULE$.UpgradeInventory());

    public static final RenderType MFU_LINES = create(OpenComputers.ID() + ":mfu_lines",
            DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 1024, State.builder()
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setOutputState(TRANSLUCENT_TARGET)
                .setLineState(new LineState(OptionalDouble.of(2.0)))
                .createCompositeState(false));

    public static final RenderType MFU_QUADS = create(OpenComputers.ID() + ":mfu_quads",
            DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 256, State.builder()
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setOutputState(TRANSLUCENT_TARGET)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false));

    public static final RenderType BLOCK_OVERLAY = create(OpenComputers.ID() + ":overlay_block",
            DefaultVertexFormats.POSITION_TEX, GL11.GL_QUADS, 1024, State.builder()
                .setTextureState(BLOCK_SHEET_MIPPED)
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setAlphaState(DEFAULT_ALPHA)
                .createCompositeState(false));

    public static final RenderType BLOCK_OVERLAY_COLOR = create(OpenComputers.ID() + ":overlay_block",
            DefaultVertexFormats.POSITION_COLOR_TEX, GL11.GL_QUADS, 1024, State.builder()
                .setTextureState(BLOCK_SHEET_MIPPED)
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setAlphaState(DEFAULT_ALPHA)
                .createCompositeState(false));

    public static final RenderType FONT_QUAD = create(OpenComputers.ID() + ":font_quad",
            DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 1024, State.builder()
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false));

    private static class CustomTextureState extends TexturingState {
        public CustomTextureState(int id) {
            super("custom_tex_" + id, () -> {
                // Should already be enabled, but vanilla does it too.
                RenderSystem.enableTexture();
                RenderSystem.bindTexture(id);
            }, () -> {});
        }
    }

    private static class LinearTexturingState extends TexturingState {
        public LinearTexturingState(boolean linear) {
            super(linear ? "lin_font_texturing" : "near_font_texturing", () -> {
                // Texture is already bound, only have to make set minify filter.
                if (linear) GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                else GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            }, () -> {
                // Nothing to do, the texture was already unbound.
            });
        }
    }

    private static final LinearTexturingState NEAR = new LinearTexturingState(false);
    private static final LinearTexturingState LINEAR = new LinearTexturingState(true);

    public static final RenderType createFontTex(String name, ResourceLocation texture, boolean linear) {
        return create(OpenComputers.ID() + ":font_stat_" + name,
            DefaultVertexFormats.POSITION_COLOR_TEX, GL11.GL_QUADS, 1024, State.builder()
                // First parameter is blur (i.e. linear filter).
                // We can't use it because it's also MAG_FILTER.
                .setTextureState(new TextureState(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setAlphaState(DEFAULT_ALPHA)
                .setTexturingState(linear ? LINEAR : NEAR)
                .createCompositeState(false));
    }

    public static final RenderType createFontTex(int id) {
        return create(OpenComputers.ID() + ":font_dyn_" + id,
            DefaultVertexFormats.POSITION_COLOR_TEX, GL11.GL_QUADS, 1024, State.builder()
                .setTexturingState(new CustomTextureState(id))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setAlphaState(DEFAULT_ALPHA)
                .createCompositeState(false));
    }

    public static final RenderType createTexturedQuad(String name, ResourceLocation texture, VertexFormat format, boolean additive) {
        return create(OpenComputers.ID() + ":tex_quad_" + name,
            format, GL11.GL_QUADS, 1024, State.builder()
                .setTextureState(new TextureState(texture, false, false))
                .setTransparencyState(additive ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
                .setAlphaState(DEFAULT_ALPHA)
                .createCompositeState(false));
    }

    private RenderTypes() {
        super(null, null, 0, 0, false, false, null, null);
        throw new Error();
    }
}
