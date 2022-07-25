package li.cil.oc.client.gui.traits

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screen.Screen

trait DisplayBuffer extends Screen {
  protected def bufferX: Int

  protected def bufferY: Int

  protected def bufferColumns: Int

  protected def bufferRows: Int

  protected var guiSizeChanged = false

  protected var currentWidth, currentHeight = -1

  protected var scale = 0.0

  override protected def init() = {
    super.init()
    BufferRenderer.init(Minecraft.getInstance.textureManager)
    guiSizeChanged = true
  }

  protected def drawBufferLayer(stack: MatrixStack) {
    val oldWidth = currentWidth
    val oldHeight = currentHeight
    currentWidth = bufferColumns
    currentHeight = bufferRows
    scale = changeSize(currentWidth, currentHeight, guiSizeChanged || oldWidth != currentWidth || oldHeight != currentHeight)

    RenderState.checkError(getClass.getName + ".drawBufferLayer: entering (aka: wasntme)")

    stack.pushPose()
    RenderState.disableEntityLighting()
    drawBuffer(stack)
    stack.popPose()

    RenderState.checkError(getClass.getName + ".drawBufferLayer: buffer layer")
  }

  protected def drawBuffer(stack: MatrixStack): Unit

  protected def changeSize(w: Double, h: Double, recompile: Boolean): Double
}
