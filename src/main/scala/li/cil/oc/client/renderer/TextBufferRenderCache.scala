package li.cil.oc.client.renderer

import java.util
import java.util.concurrent.{Callable, TimeUnit}

import com.google.common.cache.{CacheBuilder, RemovalListener, RemovalNotification}
import cpw.mods.fml.common.{ITickHandler, TickType}
import li.cil.oc.Settings
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object TextBufferRenderCache extends Callable[Int] with RemovalListener[TileEntity, Int] with ITickHandler {
  val renderer =
    if (Settings.get.fontRenderer == "texture") new font.StaticFontRenderer()
    else new font.DynamicFontRenderer()

  private val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(2, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[TextBuffer, Int]].
    build[TextBuffer, Int]()

  // To allow access in cache entry init.
  private var currentBuffer: TextBuffer = _

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  def render(buffer: TextBuffer) {
    currentBuffer = buffer
    compileOrDraw(cache.get(currentBuffer, this))
  }

  private def compileOrDraw(list: Int) = {
    if (currentBuffer.proxy.dirty) {
      RenderState.checkError(getClass.getName + ".compileOrDraw: entering (aka: wasntme)")

      for (line <- currentBuffer.data.buffer) {
        renderer.generateChars(line)
      }

      val doCompile = !RenderState.compilingDisplayList
      if (doCompile) {
        currentBuffer.proxy.dirty = false
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

      RenderState.checkError(getClass.getName + ".compileOrDraw: glCallList")
    }
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    RenderState.checkError(getClass.getName + ".call: entering (aka: wasntme)")

    val list = GLAllocation.generateDisplayLists(1)
    currentBuffer.proxy.dirty = true // Force compilation.

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

  def getLabel = "OpenComputers.TextBufferRenderer"

  def ticks = util.EnumSet.of(TickType.CLIENT)

  def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) = cache.cleanUp()

  def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}
