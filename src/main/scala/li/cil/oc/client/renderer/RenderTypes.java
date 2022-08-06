package li.cil.oc.client.renderer;

import com.google.common.collect.ImmutableList;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.Textures;
import net.minecraft.client.renderer.RenderState.TextureState;
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

    public static final VertexFormat POSITION_TEX_UV2_NORMAL = new VertexFormat(new ImmutableList.Builder<VertexFormatElement>()
        .add(DefaultVertexFormats.ELEMENT_POSITION)
        .add(DefaultVertexFormats.ELEMENT_UV0)
        .add(DefaultVertexFormats.ELEMENT_UV2)
        .add(DefaultVertexFormats.ELEMENT_NORMAL)
        .add(DefaultVertexFormats.ELEMENT_PADDING)
        .build());

    public static final TextureState ROBOT_CHASSIS_TEXTURE = new TextureState(Textures.Model$.MODULE$.Robot(), false, false);

    public static final RenderType ROBOT_CHASSIS = create(OpenComputers.ID() + ":robot_chassis",
        POSITION_TEX_UV2_NORMAL, GL11.GL_TRIANGLES, 1024, State.builder()
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

    private RenderTypes() {
        super(null, null, 0, 0, false, false, null, null);
        throw new Error();
    }
}
