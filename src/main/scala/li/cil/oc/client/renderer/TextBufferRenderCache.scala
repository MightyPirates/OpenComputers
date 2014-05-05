package li.cil.oc.client.renderer

import com.google.common.cache.{RemovalListener, RemovalNotification, CacheBuilder}
import cpw.mods.fml.common.{ITickHandler, TickType}
import java.util
import java.util.concurrent.{Callable, TimeUnit}
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11
import net.minecraft.client.Minecraft

object TextBufferRenderCache extends Callable[Int] with RemovalListener[TileEntity, Int] with ITickHandler {
  val cache = com.google.common.cache.CacheBuilder.newBuilder().
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
    MonospaceFontRenderer.init(Minecraft.getMinecraft.getTextureManager)
    currentBuffer = buffer
    compileOrDraw(cache.get(currentBuffer, this))
  }

  private def compileOrDraw(list: Int) = if (currentBuffer.proxy.dirty) {
    val doCompile = !RenderState.compilingDisplayList
    if (doCompile) {
      currentBuffer.proxy.dirty = false
      GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE)
    }

    for (((line, color), i) <- currentBuffer.data.buffer.zip(currentBuffer.data.color).zipWithIndex) {
      MonospaceFontRenderer.drawString(0, i * MonospaceFontRenderer.fontHeight, line, color, currentBuffer.data.format)
    }

    if (doCompile) {
      GL11.glEndList()
    }

    true
  }
  else GL11.glCallList(list)

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    val list = GLAllocation.generateDisplayLists(1)
    currentBuffer.proxy.dirty = true // Force compilation.
    list
  }

  def onRemoval(e: RemovalNotification[TileEntity, Int]) {
    GLAllocation.deleteDisplayLists(e.getValue)
  }

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  def getLabel = "OpenComputers.TextBufferRenderer"

  def ticks() = util.EnumSet.of(TickType.CLIENT)

  def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) = cache.cleanUp()

  def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}
