package li.cil.oc.client.renderer.textbuffer

import java.util.concurrent.{Callable, TimeUnit}

import com.google.common.cache.{CacheBuilder, RemovalListener, RemovalNotification}
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.client.renderer.font.{CouldNotFitTextureException, FontParserHex, FontTextureProvider, MultiDynamicFontTextureProvider, SingleDynamicFontTextureProvider, StaticFontTextureProvider, TextBufferRenderData}
import li.cil.oc.util.RenderState
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.opengl.GLContext

object TextBufferRenderCache extends Callable[TextBufferRenderer] with RemovalListener[TextBufferRenderData, TextBufferRenderer] {
  private val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(2, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[TextBufferRenderData, TextBufferRenderer]].
    build[TextBufferRenderData, TextBufferRenderer]()

  val fontTextureProvider: FontTextureProvider = {
    if (Settings.get.fontRenderer == "texture") {
      new StaticFontTextureProvider()
    }
    else {
      val glyphProvider = new FontParserHex()
      try {
        new SingleDynamicFontTextureProvider(glyphProvider)
      } catch {
        case _: CouldNotFitTextureException => {
          OpenComputers.log.warn("Could not fit font into maximum texture; using slow fallback renderer.")
          new MultiDynamicFontTextureProvider(glyphProvider)
        }
      }
    }
  }

  // To allow access in cache entry init.
  private var currentBuffer: TextBufferRenderData = _

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  def render(buffer: TextBufferRenderData) {
    currentBuffer = buffer
    cache.get(currentBuffer, this).render(fontTextureProvider, buffer)
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    RenderState.checkError(getClass.getName + ".call: entering (aka: wasntme)")

    val renderer = {
      if (GLContext.getCapabilities.OpenGL15 && fontTextureProvider.getTextureCount == 1) {
        new TextBufferRendererVBO()
      } else {
        new TextBufferRendererDisplayList()
      }
    }
    currentBuffer.dirty = true // Force compilation.

    RenderState.checkError(getClass.getName + ".call: leaving")

    renderer.asInstanceOf[TextBufferRenderer]
  }

  def onRemoval(e: RemovalNotification[TextBufferRenderData, TextBufferRenderer]) {
    RenderState.checkError(getClass.getName + ".onRemoval: entering (aka: wasntme)")

    e.getValue.destroy()

    RenderState.checkError(getClass.getName + ".onRemoval: leaving")
  }

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onTick(e: ClientTickEvent) = cache.cleanUp()
}
