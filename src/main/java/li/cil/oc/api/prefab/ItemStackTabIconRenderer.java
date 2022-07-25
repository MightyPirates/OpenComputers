package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL13;

/**
 * Simple implementation of a tab icon renderer using an item stack as its graphic.
 */
@SuppressWarnings("UnusedDeclaration")
public class ItemStackTabIconRenderer implements TabIconRenderer {
    private final ItemStack stack;

    public ItemStackTabIconRenderer(ItemStack stack) {
        this.stack = stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render() {
        RenderSystem.enableRescaleNormal();
        RenderHelper.turnOff();
        RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240, 240);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, 0, 0);
        RenderHelper.turnOff();
    }
}
