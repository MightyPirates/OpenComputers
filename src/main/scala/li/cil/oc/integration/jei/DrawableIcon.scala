package li.cil.oc.integration.jei

import mezz.jei.api.gui.IDrawableAnimated
import mezz.jei.api.gui.IDrawableStatic
import mezz.jei.api.gui.ITickTimer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

/**
  * Taken from The 1.11 version of JEI to support different texture sizes
  *
  * @author mezz, Vexatos
  */
class DrawableIcon(resourceLocation: ResourceLocation, u: Int, v: Int, width: Int, height: Int, textureWidth: Int, textureHeight: Int,
                   paddingTop: Int = 0, paddingBottom: Int = 0, paddingLeft: Int = 0, paddingRight: Int = 0) extends IDrawableStatic {
  // TODO Remove entire class in 1.11, replace with createDrawable

  override def getWidth: Int = width + paddingLeft + paddingRight

  override def getHeight: Int = height + paddingTop + paddingBottom

  @SideOnly(Side.CLIENT)
  override def draw(minecraft: Minecraft): Unit = draw(minecraft, 0, 0)

  @SideOnly(Side.CLIENT)
  override def draw(minecraft: Minecraft, xOffset: Int, yOffset: Int): Unit = draw(minecraft, xOffset, yOffset, 0, 0, 0, 0)

  @SideOnly(Side.CLIENT)
  override def draw(minecraft: Minecraft, xOffset: Int, yOffset: Int, maskTop: Int, maskBottom: Int, maskLeft: Int, maskRight: Int) {
    minecraft.getTextureManager.bindTexture(this.resourceLocation)
    val x = xOffset + this.paddingLeft + maskLeft
    val y = yOffset + this.paddingTop + maskTop
    val u = this.u + maskLeft
    val v = this.v + maskTop
    val width = this.width - maskRight - maskLeft
    val height = this.height - maskBottom - maskTop
    Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight)
  }
}

/**
  * Used to simulate an animated texture.
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

/**
  * Fixed implementation of TickTimer.
  * @author mezz, Vexatos
  */
class TickTimer(ticksPerCycle: Int, maxValue: Int, countDown: Boolean = false) extends ITickTimer {
  // TODO Replace with createTickTimer with JEI 3.14.6

  private var lastUpdateWorldTime: Long = 0
  private var tickCount: Int = 0

  def getValue: Int = {
    val worldTime = Minecraft.getMinecraft.theWorld.getTotalWorldTime
    val ticksPassed = worldTime - lastUpdateWorldTime
    lastUpdateWorldTime = worldTime
    tickCount += ticksPassed.toInt
    if (tickCount >= ticksPerCycle) tickCount = 0
    val value = Math.round((tickCount * maxValue).toFloat / ticksPerCycle)
    if(countDown) maxValue - value
    else value
  }

  def getMaxValue: Int = maxValue
}
