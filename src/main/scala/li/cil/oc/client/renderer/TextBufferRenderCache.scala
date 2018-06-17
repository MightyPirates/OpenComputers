package li.cil.oc.client.renderer

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener
import com.google.common.cache.RemovalNotification
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent
import li.cil.oc.Settings
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object TextBufferRenderCache extends Callable[Int] with RemovalListener[TileEntity, Int] {
	
  final val renderer = new font.TextureFontRenderer()

  private val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(2, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[TextBufferRenderData, Int]].
    build[TextBufferRenderData, Int]()

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  def render(buffer: TextBufferRenderData) {
    var reRender = cache.getIfPresent(buffer) == null
    renderer.drawBuffer(buffer.data, buffer.viewport._1, buffer.viewport._2, cache.get(buffer, this), reRender)
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    RenderState.checkError(getClass.getName + ".call: entering (aka: wasntme)")
    
    var texID = font.TextureFontRenderer.createTexture

    RenderState.checkError(getClass.getName + ".call: leaving")
    
    texID
  }


  def onRemoval(e: RemovalNotification[TileEntity, Int]) {
    RenderState.checkError(getClass.getName + ".onRemoval: entering (aka: wasntme)")

    GL11.glDeleteTextures(e.getValue)

    RenderState.checkError(getClass.getName + ".onRemoval: leaving")
  }

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onTick(e: ClientTickEvent) = cache.cleanUp()
}
