package li.cil.oc.client.renderer.tileentity

import com.google.common.cache.{CacheBuilder, RemovalNotification, RemovalListener}
import cpw.mods.fml.common.{TickType, ITickHandler}
import java.util
import java.util.concurrent.{TimeUnit, Callable}
import li.cil.oc.Config
import li.cil.oc.client.gui.MonospaceFontRenderer
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.{GLContext, GL14, GL11}

object ScreenRenderer extends TileEntitySpecialRenderer with Callable[Int] with RemovalListener[TileEntity, Int] with ITickHandler {
  private val maxRenderDistanceSq = Config.maxScreenTextRenderDistance * Config.maxScreenTextRenderDistance

  private val fadeDistanceSq = Config.screenTextFadeStartDistance * Config.screenTextFadeStartDistance

  private val fadeRatio = 1.0 / (maxRenderDistanceSq - fadeDistanceSq)

  private val canFade = GLContext.getCapabilities.OpenGL14

  /** We cache the display lists for the screens we render for performance. */
  val cache = com.google.common.cache.CacheBuilder.newBuilder().
    weakKeys().
    expireAfterAccess(5, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[Screen, Int]].build[Screen, Int]()

  /** Used to pass the current screen along to call(). */
  private var screen: Screen = null

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def renderTileEntityAt(t: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    screen = t.asInstanceOf[Screen]
    if (!screen.isOrigin)
      return

    val distance = playerDistanceSq()
    if (distance > maxRenderDistanceSq)
      return

    // Crude check whether screen text can be seen by the local player based
    // on the player's position -> angle relative to screen.
    val screenFacing = screen.facing.getOpposite
    if (screenFacing.offsetX * (x + 0.5) + screenFacing.offsetY * (y + 0.5) + screenFacing.offsetZ * (z + 0.5) < 0)
      return

    GL11.glPushAttrib(0xFFFFFF)

    RenderState.disableLighting()
    RenderState.makeItBlend()

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    if (canFade && distance > fadeDistanceSq) {
      val fade = 1.0 min ((distance - fadeDistanceSq) * fadeRatio)
      GL14.glBlendColor(0, 0, 0, 1 - fade.toFloat)
      GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE)
    }

    MonospaceFontRenderer.init(tileEntityRenderer.renderEngine)
    val list = cache.get(screen, this)
    compile(list)
    GL11.glCallList(list)

    GL11.glPopMatrix()
    GL11.glPopAttrib()
  }

  private def compile(list: Int) = if (screen.hasChanged) {
    screen.hasChanged = false
    val (sx, sy) = (screen.width, screen.height)
    val (tw, th) = (sx * 16f, sy * 16f)

    GL11.glNewList(list, GL11.GL_COMPILE)

    screen.yaw match {
      case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }
    screen.pitch match {
      case ForgeDirection.DOWN => GL11.glRotatef(90, 1, 0, 0)
      case ForgeDirection.UP => GL11.glRotatef(-90, 1, 0, 0)
      case _ => // No pitch.
    }

    // Fit area to screen (bottom left = bottom left).
    GL11.glTranslatef(-0.5f, -0.5f, 0.5f)
    GL11.glTranslatef(0, sy, 0)

    // Flip text upside down.
    GL11.glScalef(1, -1, 1)

    // Scale to multi-block size.
    GL11.glScalef(sx, sy, 1)

    // Scale to inner screen size and offset it.
    GL11.glTranslatef(2.25f / tw, 2.25f / th, 0)
    GL11.glScalef((tw - 4.5f) / tw, (th - 4.5f) / th, 1)

    // Slightly offset the text so it doesn't clip into the screen.
    GL11.glTranslatef(0, 0, 0.01f)

    // Scale based on actual buffer size.
    val (w, h) = screen.instance.resolution
    val scaleX = sx.toFloat / (w * MonospaceFontRenderer.fontWidth)
    val scaleY = sy.toFloat / (h * MonospaceFontRenderer.fontHeight)
    val scale = scaleX min scaleY
    GL11.glScalef(scale / sx.toFloat, scale / sy.toFloat, 1)

    for ((line, i) <- screen.instance.lines.zipWithIndex) {
      MonospaceFontRenderer.drawString(line, 0, i * MonospaceFontRenderer.fontHeight)
    }

    GL11.glEndList()
  }

  private def playerDistanceSq() = {
    val player = Minecraft.getMinecraft.thePlayer
    val bounds = screen.getRenderBoundingBox

    val px = player.posX
    val py = player.posY
    val pz = player.posZ

    val ex = bounds.maxX - bounds.minX
    val ey = bounds.maxY - bounds.minY
    val ez = bounds.maxZ - bounds.minZ
    val cx = bounds.minX + ex * 0.5
    val cy = bounds.minY + ey * 0.5
    val cz = bounds.minZ + ez * 0.5
    val dx = px - cx
    val dy = py - cy
    val dz = pz - cz

    (if (dx < -ex) {
      val d = dx + ex; d * d
    }
    else if (dx > ex) {
      val d = dx - ex; d * d
    }
    else 0) + (if (dy < -ey) {
      val d = dy + ey; d * d
    }
    else if (dy > ey) {
      val d = dy - ey; d * d
    }
    else 0) + (if (dz < -ez) {
      val d = dz + ez; d * d
    }
    else if (dz > ez) {
      val d = dz - ez; d * d
    }
    else 0)
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    val list = GLAllocation.generateDisplayLists(1)
    screen.hasChanged = true // Force compilation.
    compile(list)
    list
  }

  def onRemoval(e: RemovalNotification[TileEntity, Int]) =
    GLAllocation.deleteDisplayLists(e.getValue)

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  def getLabel = "OpenComputers.Screen"

  def ticks() = util.EnumSet.of(TickType.CLIENT)

  def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) = cache.cleanUp()

  def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}