package li.cil.oc.client.renderer

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.Settings
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.util.RenderState
import net.minecraftforge.event.TickEvent.ClientTickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object TextBufferRenderCache {
  val renderer =
    if (Settings.get.fontRenderer == "texture") new font.StaticFontRenderer()
    else new font.DynamicFontRenderer()

  private val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(2, TimeUnit.SECONDS).
    build[TextBufferRenderData, RenderCache]()

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  def render(stack: MatrixStack, buffer: TextBufferRenderData) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val cached = cache.get(buffer, () => new RenderCache)
    if (buffer.dirty || cached.isEmpty) {
      for (line <- buffer.data.buffer) {
        renderer.generateChars(line)
      }

      buffer.dirty = false

      cached.clear()
      renderer.drawBuffer(new MatrixStack(), cached, buffer.data, buffer.viewport._1, buffer.viewport._2)
      cached.finish()

      RenderState.checkError(getClass.getName + ".render: compiled buffer")
    }

    cached.render(stack)

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onTick(e: ClientTickEvent) = cache.cleanUp()
}
