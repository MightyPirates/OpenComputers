package li.cil.oc.client.gui.traits

import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager

trait DisplayBuffer extends GuiScreen {
  protected def bufferX: Int

  protected def bufferY: Int

  protected def bufferColumns: Int

  protected def bufferRows: Int

  protected var guiSizeChanged = false

  protected var currentWidth, currentHeight = -1

  protected var scale = 0.0

  override def initGui() = {
    super.initGui()
    BufferRenderer.init(Minecraft.getMinecraft.renderEngine)
    guiSizeChanged = true
  }

  protected def drawBufferLayer() {
    val oldWidth = currentWidth
    val oldHeight = currentHeight
    currentWidth = bufferColumns
    currentHeight = bufferRows
    scale = changeSize(currentWidth, currentHeight, guiSizeChanged || oldWidth != currentWidth || oldHeight != currentHeight)

    RenderState.checkError(getClass.getName + ".drawBufferLayer: entering (aka: wasntme)")

    GlStateManager.pushMatrix()
    RenderState.disableEntityLighting()
    drawBuffer()
    GlStateManager.popMatrix()

    RenderState.checkError(getClass.getName + ".drawBufferLayer: buffer layer")
  }

  protected def drawBuffer(): Unit

  protected def changeSize(w: Double, h: Double, recompile: Boolean): Double
}
