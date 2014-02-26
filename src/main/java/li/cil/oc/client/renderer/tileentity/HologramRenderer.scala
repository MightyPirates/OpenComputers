package li.cil.oc.client.renderer.tileentity

import com.google.common.cache.{RemovalNotification, RemovalListener, CacheBuilder}
import cpw.mods.fml.common.{TickType, ITickHandler}
import java.util
import java.util.concurrent.{Callable, TimeUnit}
import li.cil.oc.client.TexturePreloader
import li.cil.oc.common.tileentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.{GLAllocation, Tessellator}
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11
import scala.util.Random

object HologramRenderer extends TileEntitySpecialRenderer with Callable[Int] with RemovalListener[TileEntity, Int] with ITickHandler {
  val random = new Random()

  /** We cache the display lists for the projectors we render for performance. */
  val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(2, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[Hologram, Int]].
    build[Hologram, Int]()

  /** Used to pass the current screen along to call(). */
  private var hologram: Hologram = null

  override def renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    hologram = te.asInstanceOf[Hologram]

    GL11.glPushAttrib(0xFFFFFFFF)
    RenderState.makeItBlend()

    GL11.glPushMatrix()

    GL11.glScaled(1.01, 1.01, 1.01) // Avoid z-fighting with other blocks.
    GL11.glTranslated(x - 1, y + 0.5, z - 1)

    if (random.nextDouble() < 0.025) {
      GL11.glScaled(1 + random.nextGaussian() * 0.01, 1 + random.nextGaussian() * 0.001, 1 + random.nextGaussian() * 0.01)
      GL11.glTranslated(random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, random.nextGaussian() * 0.01)
    }

    GL11.glColorMask(false, false, false, false)
    val list = cache.get(hologram, this)
    compileOrDraw(list)
    GL11.glColorMask(true, true, true, true)
    GL11.glDepthFunc(GL11.GL_EQUAL)
    compileOrDraw(list)

    GL11.glPopMatrix()
    GL11.glPopAttrib()
  }

  def compileOrDraw(list: Int) = if (hologram.dirty) {
    val doCompile = !RenderState.compilingDisplayList
    if (doCompile) {
      hologram.dirty = false
      GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE)
    }

    def isSolid(hx: Int, hy: Int, hz: Int) = {
      hx >= 0 && hy >= 0 && hz >= 0 && hx < hologram.width && hy < hologram.height && hz < hologram.width &&
        (hologram.volume(hx + hz * hologram.width) & (1 << hy)) != 0
    }

    bindTexture(TexturePreloader.blockHologram)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.setColorRGBA_F(1, 1, 1, 0.7f)

    val s = 1f / 16f
    for (hx <- 0 until hologram.width) {
      val wx = hx * s
      for (hz <- 0 until hologram.width) {
        val wz = hz * s
        for (hy <- 0 until hologram.height) {
          val wy = hy * s

          if (isSolid(hx, hy, hz)) {
            /*
                  0---1
                  | N |
              0---3---2---1---0
              | W | U | E | D |
              5---6---7---4---5
                  | S |
                  5---4
             */

            // South
            if (!isSolid(hx, hy, hz + 1)) {
              t.setNormal(0, 0, 1)
              t.addVertexWithUV(wx + s, wy + s, wz + s, 0, 0) // 5
              t.addVertexWithUV(wx + 0, wy + s, wz + s, 1, 0) // 4
              t.addVertexWithUV(wx + 0, wy + 0, wz + s, 1, 1) // 7
              t.addVertexWithUV(wx + s, wy + 0, wz + s, 0, 1) // 6
            }
            // North
            if (!isSolid(hx, hy, hz - 1)) {
              t.setNormal(0, 0, -1)
              t.addVertexWithUV(wx + s, wy + 0, wz + 0, 0, 0) // 3
              t.addVertexWithUV(wx + 0, wy + 0, wz + 0, 1, 0) // 2
              t.addVertexWithUV(wx + 0, wy + s, wz + 0, 1, 1) // 1
              t.addVertexWithUV(wx + s, wy + s, wz + 0, 0, 1) // 0
            }

            // East
            if (!isSolid(hx + 1, hy, hz)) {
              t.setNormal(1, 0, 0)
              t.addVertexWithUV(wx + s, wy + s, wz + s, 1, 0) // 5
              t.addVertexWithUV(wx + s, wy + 0, wz + s, 1, 1) // 6
              t.addVertexWithUV(wx + s, wy + 0, wz + 0, 0, 1) // 3
              t.addVertexWithUV(wx + s, wy + s, wz + 0, 0, 0) // 0
            }
            // West
            if (!isSolid(hx - 1, hy, hz)) {
              t.setNormal(-1, 0, 0)
              t.addVertexWithUV(wx + 0, wy + 0, wz + s, 1, 0) // 7
              t.addVertexWithUV(wx + 0, wy + s, wz + s, 1, 1) // 4
              t.addVertexWithUV(wx + 0, wy + s, wz + 0, 0, 1) // 1
              t.addVertexWithUV(wx + 0, wy + 0, wz + 0, 0, 0) // 2
            }

            // Up
            if (!isSolid(hx, hy + 1, hz)) {
              t.setNormal(0, 1, 0)
              t.addVertexWithUV(wx + s, wy + s, wz + 0, 0, 0) // 0
              t.addVertexWithUV(wx + 0, wy + s, wz + 0, 1, 0) // 1
              t.addVertexWithUV(wx + 0, wy + s, wz + s, 1, 1) // 4
              t.addVertexWithUV(wx + s, wy + s, wz + s, 0, 1) // 5
            }
            // Down
            if (!isSolid(hx, hy - 1, hz)) {
              t.setNormal(0, -1, 0)
              t.addVertexWithUV(wx + s, wy + 0, wz + s, 0, 0) // 6
              t.addVertexWithUV(wx + 0, wy + 0, wz + s, 1, 0) // 7
              t.addVertexWithUV(wx + 0, wy + 0, wz + 0, 1, 1) // 2
              t.addVertexWithUV(wx + s, wy + 0, wz + 0, 0, 1) // 3
            }
          }
        }
      }
    }

    t.draw()

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
    hologram.dirty = true // Force compilation.
    list
  }

  def onRemoval(e: RemovalNotification[TileEntity, Int]) {
    GLAllocation.deleteDisplayLists(e.getValue)
  }

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  def getLabel = "OpenComputers.Hologram"

  def ticks() = util.EnumSet.of(TickType.CLIENT)

  def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) = cache.cleanUp()

  def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}
