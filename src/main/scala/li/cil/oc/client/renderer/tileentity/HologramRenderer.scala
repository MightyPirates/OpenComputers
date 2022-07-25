package li.cil.oc.client.renderer.tileentity

import java.nio.IntBuffer
import java.util.function.Function
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalListener
import com.google.common.cache.RemovalNotification
import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3f
import net.minecraftforge.event.TickEvent.ClientTickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15

import scala.util.Random

object HologramRenderer extends Function[TileEntityRendererDispatcher, HologramRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new HologramRenderer(dispatch)
}

class HologramRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[Hologram](dispatch)
  with Callable[Int] with RemovalListener[TileEntity, Int] {

  private val random = new Random()

  /** We cache the VBOs for the projectors we render for performance. */
  private val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(5, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[Hologram, Int]].
    build[Hologram, Int]()

  /**
   * Common for all holograms. Holds the vertex positions, texture
   * coordinates and normals information. Layout is: u v nx ny nz x y z
   *
   * WARNING: this optimization only works if all the holograms have the
   * same dimensions (in voxels). If we ever need holograms of different
   * sizes we could probably just fake that by making the outer layers
   * immutable (i.e. always empty).
   *
   * NOTE: It already takes up 47.25 MiB of video memory and increasing
   * hologram size to, for example, 64*64*64 will result in 168 MiB.
   */
  private var commonBuffer = 0

  /**
   * Also common for all holograms. Temporary buffer used to upload
   * hologram data to GPU. First half stores colors for each vertex
   * (0xAABBGGRR Int, alpha is used for alignment only) and second
   * half stores (Int) indices of vertices that should be drawn.
   */
  private var dataBuffer: IntBuffer = null

  /** Used to pass the current screen along to call(). */
  private var hologram: Hologram = null

  /**
   * Whether initialization failed (e.g. due to an out of memory error) and we
   * should render using the fallback renderer instead.
   */
  private var failed = false

  override def render(hologram: Hologram, f: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    if (failed) {
      HologramRendererFallback.render(hologram, f, stack, buffer, light, overlay)
      return
    }

    this.hologram = hologram
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (!hologram.hasPower) return

    GL11.glPushClientAttrib(GL11.GL_CLIENT_ALL_ATTRIB_BITS)
    RenderState.pushAttrib()
    RenderState.makeItBlend()
    RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)

    val pos = hologram.getBlockPos
    val playerDistSq = Minecraft.getInstance.player.getEyePosition(f)
      .distanceToSqr(pos.getX + 0.5, pos.getY + 0.5, pos.getZ + 0.5)
    val maxDistSq = hologram.getViewDistance * hologram.getViewDistance
    val fadeDistSq = hologram.getFadeStartDistanceSquared
    RenderState.setBlendAlpha(0.75f * (if (playerDistSq > fadeDistSq) math.max(0, 1 - ((playerDistSq - fadeDistSq) / (maxDistSq - fadeDistSq)).toFloat) else 1))

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)

    hologram.yaw match {
      case Direction.WEST => stack.mulPose(Vector3f.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Vector3f.YP.rotationDegrees(180))
      case Direction.EAST => stack.mulPose(Vector3f.YP.rotationDegrees(90))
      case _ => // No yaw.
    }
    hologram.pitch match {
      case Direction.DOWN => stack.mulPose(Vector3f.XP.rotationDegrees(90))
      case Direction.UP => stack.mulPose(Vector3f.XP.rotationDegrees(-90))
      case _ => // No pitch.
    }

    stack.mulPose(new Vector3f(hologram.rotationX, hologram.rotationY, hologram.rotationZ).rotationDegrees(hologram.rotationAngle))
    stack.mulPose(new Vector3f(hologram.rotationSpeedX, hologram.rotationSpeedY, hologram.rotationSpeedZ)
      .rotationDegrees(hologram.rotationSpeed * (hologram.getLevel.getGameTime % (360 * 20 - 1) + f) / 20f))

    stack.scale(1.001f, 1.001f, 1.001f) // Avoid z-fighting with other blocks.
    stack.translate(
      (hologram.translation.x * hologram.width / 16 - 1.5) * hologram.scale,
      hologram.translation.y * hologram.height / 16 * hologram.scale,
      (hologram.translation.z * hologram.width / 16 - 1.5) * hologram.scale)

    // Do a bit of flickering, because that's what holograms do!
    if (Settings.get.hologramFlickerFrequency > 0 && random.nextDouble() < Settings.get.hologramFlickerFrequency) {
      stack.scale(1 + random.nextGaussian().toFloat * 0.01f, 1 + random.nextGaussian().toFloat * 0.001f, 1 + random.nextGaussian().toFloat * 0.01f)
      stack.translate(random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, random.nextGaussian() * 0.01)
    }

    // After the below scaling, hologram is drawn inside a [0..48]x[0..32]x[0..48] box
    stack.scale(hologram.scale.toFloat / 16f, hologram.scale.toFloat / 16f, hologram.scale.toFloat / 16f)

    Textures.bind(Textures.Model.HologramEffect)

    val sx = (pos.getX + 0.5) * hologram.scale
    val sy = -(pos.getY + 0.5) * hologram.scale
    val sz = (pos.getZ + 0.5) * hologram.scale
    if (sx >= -1.5 && sx <= 1.5 && sz >= -1.5 && sz <= 1.5 && sy >= 0 && sy <= 2) {
      // Camera is inside the hologram.
      RenderSystem.disableCull()
    }
    else {
      // Camera is outside the hologram.
      RenderSystem.enableCull()
    }

    // We do two passes here to avoid weird transparency effects: in the first
    // pass we find the front-most fragment, in the second we actually draw it.
    // When we don't do this the hologram will look different from different
    // angles (because some faces will shine through sometimes and sometimes
    // they won't), so a more... consistent look is desirable.
    val glBuffer = cache.get(hologram, this)
    RenderSystem.colorMask(false, false, false, false)
    RenderSystem.depthMask(true)
    draw(glBuffer)
    RenderSystem.colorMask(true, true, true, true)
    RenderSystem.depthFunc(GL11.GL_EQUAL)
    draw(glBuffer)
    RenderSystem.depthFunc(GL11.GL_LEQUAL)

    stack.popPose()

    RenderState.disableBlend()
    RenderState.popAttrib()
    GL11.glPopClientAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  def draw(glBuffer: Int) {
    if (initialize()) {
      validate(glBuffer)
      publish(glBuffer)
    }
  }

  private def initialize(): Boolean = !failed && (try {
    // First run only, create structure information.
    if (commonBuffer == 0) {
      dataBuffer = BufferUtils.createIntBuffer(hologram.width * hologram.width * hologram.height * 6 * 4 * 2)

      commonBuffer = GL15.glGenBuffers()

      val data = BufferUtils.createFloatBuffer(hologram.width * hologram.width * hologram.height * 24 * (2 + 3 + 3))
      def addVertex(x: Int, y: Int, z: Int, u: Int, v: Int, nx: Int, ny: Int, nz: Int) {
        data.put(u)
        data.put(v)
        data.put(nx)
        data.put(ny)
        data.put(nz)
        data.put(x)
        data.put(y)
        data.put(z)
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
            addVertex(x + 1, y + 1, z + 1, 0, 0, 0, 0, 1) // 5
            addVertex(x + 0, y + 1, z + 1, 1, 0, 0, 0, 1) // 4
            addVertex(x + 0, y + 0, z + 1, 1, 1, 0, 0, 1) // 7
            addVertex(x + 1, y + 0, z + 1, 0, 1, 0, 0, 1) // 6
            // North
            addVertex(x + 1, y + 0, z + 0, 0, 0, 0, 0, -1) // 3
            addVertex(x + 0, y + 0, z + 0, 1, 0, 0, 0, -1) // 2
            addVertex(x + 0, y + 1, z + 0, 1, 1, 0, 0, -1) // 1
            addVertex(x + 1, y + 1, z + 0, 0, 1, 0, 0, -1) // 0

            // East
            addVertex(x + 1, y + 1, z + 1, 1, 0, 1, 0, 0) // 5
            addVertex(x + 1, y + 0, z + 1, 1, 1, 1, 0, 0) // 6
            addVertex(x + 1, y + 0, z + 0, 0, 1, 1, 0, 0) // 3
            addVertex(x + 1, y + 1, z + 0, 0, 0, 1, 0, 0) // 0
            // West
            addVertex(x + 0, y + 0, z + 1, 1, 0, -1, 0, 0) // 7
            addVertex(x + 0, y + 1, z + 1, 1, 1, -1, 0, 0) // 4
            addVertex(x + 0, y + 1, z + 0, 0, 1, -1, 0, 0) // 1
            addVertex(x + 0, y + 0, z + 0, 0, 0, -1, 0, 0) // 2

            // Up
            addVertex(x + 1, y + 1, z + 0, 0, 0, 0, 1, 0) // 0
            addVertex(x + 0, y + 1, z + 0, 1, 0, 0, 1, 0) // 1
            addVertex(x + 0, y + 1, z + 1, 1, 1, 0, 1, 0) // 4
            addVertex(x + 1, y + 1, z + 1, 0, 1, 0, 1, 0) // 5
            // Down
            addVertex(x + 1, y + 0, z + 1, 0, 0, 0, -1, 0) // 6
            addVertex(x + 0, y + 0, z + 1, 1, 0, 0, -1, 0) // 7
            addVertex(x + 0, y + 0, z + 0, 1, 1, 0, -1, 0) // 2
            addVertex(x + 1, y + 0, z + 0, 0, 1, 0, -1, 0) // 3
          }
        }
      }

      // Important! OpenGL will start reading from the current buffer position.
      data.rewind()

      // This buffer never ever changes, so static is the way to go.
      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, commonBuffer)
      GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW)
    }
    true
  }
  catch {
    case oom: OutOfMemoryError =>
      HologramRendererFallback.text = "Not enough memory"
      failed = true
      false
  })

  private def validate(glBuffer: Int) {
    // Refresh indexes when the hologram's data changed.
    if (hologram.needsRendering) {
      def value(hx: Int, hy: Int, hz: Int) = if (hx >= 0 && hy >= 0 && hz >= 0 && hx < hologram.width && hy < hologram.height && hz < hologram.width) hologram.getColor(hx, hy, hz) else 0

      def isSolid(hx: Int, hy: Int, hz: Int) = value(hx, hy, hz) != 0

      def addFace(index: Int, color: Int) {
        dataBuffer.put(index)
        dataBuffer.put(index + 1)
        dataBuffer.put(index + 2)
        dataBuffer.put(index + 3)

        dataBuffer.put(index, color)
        dataBuffer.put(index + 1, color)
        dataBuffer.put(index + 2, color)
        dataBuffer.put(index + 3, color)

        hologram.visibleQuads += 1
      }

      // Copy color information, identify which quads to render and prepare data for glDrawElements
      hologram.visibleQuads = 0
      var index = 0
      dataBuffer.position(hologram.width * hologram.width * hologram.height * 6 * 4)
      for (hx <- 0 until hologram.width) {
        for (hz <- 0 until hologram.width) {
          for (hy <- 0 until hologram.height) {
            // Do we need to draw at least one face?
            if (isSolid(hx, hy, hz)) {
              // Yes, get the color of the voxel.
              val color = hologram.colors(value(hx, hy, hz) - 1)

              // South
              if (!isSolid(hx, hy, hz + 1)) {
                addFace(index, color)
              }
              index += 4
              // North
              if (!isSolid(hx, hy, hz - 1)) {
                addFace(index, color)
              }
              index += 4

              // East
              if (!isSolid(hx + 1, hy, hz)) {
                addFace(index, color)
              }
              index += 4
              // West
              if (!isSolid(hx - 1, hy, hz)) {
                addFace(index, color)
              }
              index += 4

              // Up
              if (!isSolid(hx, hy + 1, hz)) {
                addFace(index, color)
              }
              index += 4
              // Down
              if (!isSolid(hx, hy - 1, hz)) {
                addFace(index, color)
              }
              index += 4
            }
            else {
              // No, skip all associated indices.
              index += 6 * 4
            }
          }
        }
      }

      GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBuffer)
      if (hologram.visibleQuads > 0) {
        // Flip the buffer to only fill in as much data as necessary.
        dataBuffer.flip()

        // This buffer can be updated quite frequently, so dynamic seems sensible.
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataBuffer, GL15.GL_DYNAMIC_DRAW)
      }
      else {
        // Empty hologram.
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 0L, GL15.GL_DYNAMIC_DRAW)
      }

      // Reset for the next operation.
      dataBuffer.clear()

      hologram.needsRendering = false
    }
  }

  private def publish(glBuffer: Int) {
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, commonBuffer)
    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY)
    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
    GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY)
    GL11.glInterleavedArrays(GL11.GL_T2F_N3F_V3F, 0, 0)

    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBuffer)
    GL11.glEnableClientState(GL11.GL_COLOR_ARRAY)
    GL11.glColorPointer(3, GL11.GL_UNSIGNED_BYTE, 4, 0)

    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glBuffer)
    GL11.glDrawElements(GL11.GL_QUADS, hologram.visibleQuads * 4, GL11.GL_UNSIGNED_INT, hologram.width * hologram.width * hologram.height * 6 * 4 * 4)
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    val glBuffer = GL15.glGenBuffers()

    // Force re-indexing.
    hologram.needsRendering = true

    glBuffer
  }

  def onRemoval(e: RemovalNotification[TileEntity, Int]) {
    val glBuffer = e.getValue
    GL15.glDeleteBuffers(glBuffer)
    dataBuffer.clear()
  }

  @SubscribeEvent
  def onTick(e: ClientTickEvent) = cache.cleanUp()
}
