package li.cil.oc.integration.jei

import com.mojang.blaze3d.matrix.MatrixStack
import mezz.jei.api.gui.ITickTimer
import mezz.jei.api.gui.drawable.IDrawableAnimated
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.AbstractGui
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
  * Used to simulate an animated texture.
  *
  * @author Vexatos
  */
class DrawableAnimatedIcon(resourceLocation: ResourceLocation, u: Int, v: Int, width: Int, height: Int, textureWidth: Int, textureHeight: Int,
                           tickTimer: ITickTimer, uOffset: Int, vOffset: Int,
                           paddingTop: Int = 0, paddingBottom: Int = 0, paddingLeft: Int = 0, paddingRight: Int = 0) extends IDrawableAnimated {

  override def getWidth: Int = width + paddingLeft + paddingRight

  override def getHeight: Int = height + paddingTop + paddingBottom

  @OnlyIn(Dist.CLIENT)
  override def draw(stack: MatrixStack, xOffset: Int, yOffset: Int) {
    val animationValue = tickTimer.getValue

    val uOffsetTotal = uOffset * animationValue
    val vOffsetTotal = vOffset * animationValue

    Minecraft.getInstance.getTextureManager.bind(resourceLocation)
    val x = xOffset + this.paddingLeft
    val y = yOffset + this.paddingTop
    val u = this.u + uOffsetTotal
    val v = this.v + vOffsetTotal
    AbstractGui.blit(stack, x, y, u, v, width, height, textureWidth, textureHeight)
  }
}
