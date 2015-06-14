package li.cil.oc.api.prefab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Simple implementation of a tab icon renderer using an item stack as its graphic.
 */
@SuppressWarnings("UnusedDeclaration")
public class ItemStackTabIconRenderer implements TabIconRenderer {
    private final ItemStack stack;

    public ItemStackTabIconRenderer(ItemStack stack) {
        this.stack = stack;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void render() {
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        RenderItem.getInstance().renderItemAndEffectIntoGUI(Minecraft.getMinecraft().fontRenderer, Minecraft.getMinecraft().getTextureManager(), stack, 0, 0);
        RenderHelper.disableStandardItemLighting();
    }
}
