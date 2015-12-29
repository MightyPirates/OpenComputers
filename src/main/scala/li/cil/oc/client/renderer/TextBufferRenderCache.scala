package li.cil.oc.client.renderer

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener
import com.google.common.cache.RemovalNotification
import li.cil.oc.Settings
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.opengl.GL11

object TextBufferRenderCache extends Callable[Int] with RemovalListener[TileEntity, Int] {
  val renderer =
    if (Settings.get.fontRenderer == "texture") new font.StaticFontRenderer()
    else new font.DynamicFontRenderer()

  private val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(2, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[TextBufferRenderData, Int]].
    build[TextBufferRenderData, Int]()

  // To allow access in cache entry init.
  private var currentBuffer: TextBufferRenderData = _

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  def render(buffer: TextBufferRenderData) {
    currentBuffer = buffer
    compileOrDraw(cache.get(currentBuffer, this))
  }

  private def compileOrDraw(list: Int) = {
    if (currentBuffer.dirty) {
      RenderState.checkError(getClass.getName + ".compileOrDraw: entering (aka: wasntme)")

      for (line <- currentBuffer.data.buffer) {
        renderer.generateChars(line)
      }

      val doCompile = !RenderState.compilingDisplayList
      if (doCompile) {
        currentBuffer.dirty = false
        GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE)

        RenderState.checkError(getClass.getName + ".compileOrDraw: glNewList")
      }

      renderer.drawBuffer(currentBuffer.data)

      RenderState.checkError(getClass.getName + ".compileOrDraw: drawString")

      if (doCompile) {
        GL11.glEndList()

        RenderState.checkError(getClass.getName + ".compileOrDraw: glEndList")
      }

      RenderState.checkError(getClass.getName + ".compileOrDraw: leaving")

      true
    }
    else {
      GL11.glCallList(list)
      RenderState.bindTexture(0)
      GlStateManager.depthMask(true)
      GlStateManager.color(1, 1, 1)

      RenderState.checkError(getClass.getName + ".compileOrDraw: glCallList")
    }
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    RenderState.checkError(getClass.getName + ".call: entering (aka: wasntme)")

    val list = GLAllocation.generateDisplayLists(1)
    currentBuffer.dirty = true // Force compilation.

    RenderState.checkError(getClass.getName + ".call: leaving")

    list
  }

  def onRemoval(e: RemovalNotification[TileEntity, Int]) {
    RenderState.checkError(getClass.getName + ".onRemoval: entering (aka: wasntme)")

    GLAllocation.deleteDisplayLists(e.getValue)

    RenderState.checkError(getClass.getName + ".onRemoval: leaving")
  }

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onTick(e: ClientTickEvent) = cache.cleanUp()
}
