package li.cil.oc.integration.jei

import mezz.jei.api.gui.IDrawableAnimated
import mezz.jei.api.gui.ITickTimer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

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

  @SideOnly(Side.CLIENT)
  override def draw(minecraft: Minecraft): Unit = draw(minecraft, 0, 0)

  @SideOnly(Side.CLIENT)
  override def draw(minecraft: Minecraft, xOffset: Int, yOffset: Int) {
    val animationValue = tickTimer.getValue

    val uOffsetTotal = uOffset * animationValue
    val vOffsetTotal = vOffset * animationValue

    minecraft.getTextureManager.bindTexture(resourceLocation)
    val x = xOffset + this.paddingLeft
    val y = yOffset + this.paddingTop
    val u = this.u + uOffsetTotal
    val v = this.v + vOffsetTotal
    Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight)
  }
}
