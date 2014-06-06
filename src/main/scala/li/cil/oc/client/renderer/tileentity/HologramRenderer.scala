package li.cil.oc.client.renderer.tileentity

import com.google.common.cache.{RemovalNotification, RemovalListener, CacheBuilder}
import cpw.mods.fml.common.{TickType, ITickHandler}
import java.util
import java.util.concurrent.{Callable, TimeUnit}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.{GL15, GL11}
import scala.util.Random
import org.lwjgl.BufferUtils

object HologramRenderer extends TileEntitySpecialRenderer with Callable[Int] with RemovalListener[TileEntity, Int] with ITickHandler {
  val random = new Random()

  /** We cache the VBOs for the projectors we render for performance. */
  val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(10, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[Hologram, Int]].
    build[Hologram, Int]()

  /** Used to pass the current screen along to call(). */
  private var hologram: Hologram = null

  override def renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    hologram = te.asInstanceOf[Hologram]
    if (!hologram.hasPower) return

    GL11.glPushAttrib(0xFFFFFFFF)
    RenderState.makeItBlend()
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)

    val playerDistSq = x*x + y*y + z*z
    val maxDistSq = hologram.getMaxRenderDistanceSquared
    val fadeDistSq = hologram.getFadeStartDistanceSquared
    RenderState.setBlendAlpha(0.75f * (if (playerDistSq > fadeDistSq) math.max(0, 1 - ((playerDistSq - fadeDistSq) / (maxDistSq - fadeDistSq)).toFloat) else 1))

    GL11.glPushMatrix()
    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
    GL11.glScaled(1.001, 1.001, 1.001) // Avoid z-fighting with other blocks.
    GL11.glTranslated(-1.5 * hologram.scale, 0, -1.5 * hologram.scale)

    // Do a bit of flickering, because that's what holograms do!
    if (random.nextDouble() < 0.025) {
      GL11.glScaled(1 + random.nextGaussian() * 0.01, 1 + random.nextGaussian() * 0.001, 1 + random.nextGaussian() * 0.01)
      GL11.glTranslated(random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, random.nextGaussian() * 0.01)
    }

    // After the below scaling, hologram is drawn inside a [0..48]x[0..32]x[0..48] box
    GL11.glScaled(hologram.scale / 16f, hologram.scale / 16f, hologram.scale / 16f)

    // We do two passes here to avoid weird transparency effects: in the first
    // pass we find the front-most fragment, in the second we actually draw it.
    // TODO proper transparency shader? depth peeling e.g.
    // evg-zhabotinsky: I'd rather not do it. Anyway it won't work for multiple holograms.
    //  Also I commented out the first pass to see what it will look like and I prefer it the way it is now.
    GL11.glColorMask(false, false, false, false)
    GL11.glDepthMask(true)
    val privateBuf = cache.get(hologram, this)
    compileOrDraw(privateBuf)
    GL11.glColorMask(true, true, true, true)
    GL11.glDepthFunc(GL11.GL_EQUAL)
    compileOrDraw(privateBuf)

    GL11.glPopMatrix()
    GL11.glPopAttrib()
  }

  val compileOrDraw = {
    // WARNING works only if all the holograms have the same dimensions (in voxels)
    var commonBuffer = 0 // Common for all holograms (a-la static variable)
    (privateBuf: Int) => {
      // Save current state (don't forget to restore)
      GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS)
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

      if (commonBuffer == 0) { // First run only
        commonBuffer = GL15.glGenBuffers()
        var tmpBuf = BufferUtils.createFloatBuffer(hologram.width * hologram.width * hologram.height * 24 * (3 + 2))
        def newVert = (x: Int, y: Int, z: Int, u: Int, v: Int) => { // Dirty hack to avoid rewriting :)
          tmpBuf.put(u)
          tmpBuf.put(v)
          tmpBuf.put(x)
          tmpBuf.put(y)
          tmpBuf.put(z)
        }
        for (x <- 0 until hologram.width) {
          for (z <- 0 until hologram.width) {
            for (y <- 0 until hologram.height) {
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
                newVert(x + 1, y + 1, z + 1, 0, 0) // 5
                newVert(x + 0, y + 1, z + 1, 1, 0) // 4
                newVert(x + 0, y + 0, z + 1, 1, 1) // 7
                newVert(x + 1, y + 0, z + 1, 0, 1) // 6
              // North
                newVert(x + 1, y + 0, z + 0, 0, 0) // 3
                newVert(x + 0, y + 0, z + 0, 1, 0) // 2
                newVert(x + 0, y + 1, z + 0, 1, 1) // 1
                newVert(x + 1, y + 1, z + 0, 0, 1) // 0

              // East
                newVert(x + 1, y + 1, z + 1, 1, 0) // 5
                newVert(x + 1, y + 0, z + 1, 1, 1) // 6
                newVert(x + 1, y + 0, z + 0, 0, 1) // 3
                newVert(x + 1, y + 1, z + 0, 0, 0) // 0
              // West
                newVert(x + 0, y + 0, z + 1, 1, 0) // 7
                newVert(x + 0, y + 1, z + 1, 1, 1) // 4
                newVert(x + 0, y + 1, z + 0, 0, 1) // 1
                newVert(x + 0, y + 0, z + 0, 0, 0) // 2

              // Up
                newVert(x + 1, y + 1, z + 0, 0, 0) // 0
                newVert(x + 0, y + 1, z + 0, 1, 0) // 1
                newVert(x + 0, y + 1, z + 1, 1, 1) // 4
                newVert(x + 1, y + 1, z + 1, 0, 1) // 5
              // Down
                newVert(x + 1, y + 0, z + 1, 0, 0) // 6
                newVert(x + 0, y + 0, z + 1, 1, 0) // 7
                newVert(x + 0, y + 0, z + 0, 1, 1) // 2
                newVert(x + 1, y + 0, z + 0, 0, 1) // 3
            }
          }
        }
        tmpBuf.rewind() // Important!
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, commonBuffer)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, tmpBuf, GL15.GL_STATIC_DRAW)
      }

      if (hologram.dirty) { // Refresh hologram
        def value(hx: Int, hy: Int, hz: Int) = if (hx >= 0 && hy >= 0 && hz >= 0 && hx < hologram.width && hy < hologram.height && hz < hologram.width) hologram.getColor(hx, hy, hz) else 0

        def isSolid(hx: Int, hy: Int, hz: Int) = value(hx, hy, hz) != 0

        var tmpBuf = BufferUtils.createIntBuffer(hologram.width * hologram.width * hologram.height * 24 * 2)
        // Copy color information, identify which quads to render and prepare data for glDrawElements
        hologram.visibleQuads = 0;
        var c = 0
        tmpBuf.position(hologram.width * hologram.width * hologram.height * 24)
        for (hx <- 0 until hologram.width) {
          for (hz <- 0 until hologram.width) {
            for (hy <- 0 until hologram.height) {
              if (isSolid(hx, hy, hz)) {
                val v: Int = hologram.colors(value(hx, hy, hz) - 1)
                // South
                if (!isSolid(hx, hy, hz + 1)) {
                  tmpBuf.put(c)
                  tmpBuf.put(c + 1)
                  tmpBuf.put(c + 2)
                  tmpBuf.put(c + 3)
                  tmpBuf.put(c, v)
                  tmpBuf.put(c + 1, v)
                  tmpBuf.put(c + 2, v)
                  tmpBuf.put(c + 3, v)
                  hologram.visibleQuads += 1
                }
                c += 4
                // North
                if (!isSolid(hx, hy, hz - 1)) {
                  tmpBuf.put(c)
                  tmpBuf.put(c + 1)
                  tmpBuf.put(c + 2)
                  tmpBuf.put(c + 3)
                  tmpBuf.put(c, v)
                  tmpBuf.put(c + 1, v)
                  tmpBuf.put(c + 2, v)
                  tmpBuf.put(c + 3, v)
                  hologram.visibleQuads += 1
                }
                c += 4

                // East
                if (!isSolid(hx + 1, hy, hz)) {
                  tmpBuf.put(c)
                  tmpBuf.put(c + 1)
                  tmpBuf.put(c + 2)
                  tmpBuf.put(c + 3)
                  tmpBuf.put(c, v)
                  tmpBuf.put(c + 1, v)
                  tmpBuf.put(c + 2, v)
                  tmpBuf.put(c + 3, v)
                  hologram.visibleQuads += 1
                }
                c += 4
                // West
                if (!isSolid(hx - 1, hy, hz)) {
                  tmpBuf.put(c)
                  tmpBuf.put(c + 1)
                  tmpBuf.put(c + 2)
                  tmpBuf.put(c + 3)
                  tmpBuf.put(c, v)
                  tmpBuf.put(c + 1, v)
                  tmpBuf.put(c + 2, v)
                  tmpBuf.put(c + 3, v)
                  hologram.visibleQuads += 1
                }
                c += 4

                // Up
                if (!isSolid(hx, hy + 1, hz)) {
                  tmpBuf.put(c)
                  tmpBuf.put(c + 1)
                  tmpBuf.put(c + 2)
                  tmpBuf.put(c + 3)
                  tmpBuf.put(c, v)
                  tmpBuf.put(c + 1, v)
                  tmpBuf.put(c + 2, v)
                  tmpBuf.put(c + 3, v)
                  hologram.visibleQuads += 1
                }
                c += 4
                // Down
                if (!isSolid(hx, hy - 1, hz)) {
                  tmpBuf.put(c)
                  tmpBuf.put(c + 1)
                  tmpBuf.put(c + 2)
                  tmpBuf.put(c + 3)
                  tmpBuf.put(c, v)
                  tmpBuf.put(c + 1, v)
                  tmpBuf.put(c + 2, v)
                  tmpBuf.put(c + 3, v)
                  hologram.visibleQuads += 1
                }
                c += 4
              } else c += 24
            }
          }
        }
        tmpBuf.rewind() // Important!
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, privateBuf)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, tmpBuf, GL15.GL_STATIC_DRAW)

        hologram.dirty = false
      }

      GL11.glDisable(GL11.GL_LIGHTING) // Hologram that reflects light... It would be awesome! But...
      GL11.glEnable(GL11.GL_CULL_FACE)
      GL11.glCullFace(GL11.GL_BACK) // Because fragment processing started to slow things down
      bindTexture(Textures.blockHologram)
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, commonBuffer)
      GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY)
      GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
      GL11.glInterleavedArrays(GL11.GL_T2F_V3F, 0, 0)
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, privateBuf)
      GL11.glEnableClientState(GL11.GL_COLOR_ARRAY)
      GL11.glColorPointer(3, GL11.GL_UNSIGNED_BYTE, 4, 0)
      GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, privateBuf)

      GL11.glDrawElements(GL11.GL_QUADS, hologram.visibleQuads * 4, GL11.GL_UNSIGNED_INT, hologram.width * hologram.width * hologram.height * 24 * 4)

      // Restore original state
      GL11.glPopAttrib()
      GL11.glPopClientAttrib()
    }
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    val privateBuf = GL15.glGenBuffers()
    hologram.dirty = true // Force compilation.
    privateBuf
  }

  def onRemoval(e: RemovalNotification[TileEntity, Int]) {
    GL15.glDeleteBuffers(e.getValue)
  }

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  def getLabel = "OpenComputers.Hologram"

  def ticks() = util.EnumSet.of(TickType.CLIENT)

  def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) = cache.cleanUp()

  def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}
