package li.cil.oc.client.renderer.tileentity

import com.google.common.cache.{CacheBuilder, RemovalNotification, RemovalListener}
import cpw.mods.fml.common.{TickType, ITickHandler}
import java.util
import java.util.concurrent.{TimeUnit, Callable}
import li.cil.oc.Config
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

object ScreenRenderer extends TileEntitySpecialRenderer with Callable[Int] with RemovalListener[TileEntity, Int] with ITickHandler {
  private val maxRenderDistanceSq = Config.maxScreenTextRenderDistance * Config.maxScreenTextRenderDistance

  private val fadeDistanceSq = Config.screenTextFadeStartDistance * Config.screenTextFadeStartDistance

  private val fadeRatio = 1.0 / (maxRenderDistanceSq - fadeDistanceSq)

  /** We cache the display lists for the screens we render for performance. */
  val cache = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(2, TimeUnit.SECONDS).
    removalListener(this).
    asInstanceOf[CacheBuilder[Screen, Int]].
    build[Screen, Int]()

  /** Used to pass the current screen along to call(). */
  private var screen: Screen = null

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def renderTileEntityAt(t: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    screen = t.asInstanceOf[Screen]
    if (!screen.isOrigin || !screen.hasPower) {
      return
    }

    val distance = playerDistanceSq()
    if (distance > maxRenderDistanceSq) {
      return
    }

    // Crude check whether screen text can be seen by the local player based
    // on the player's position -> angle relative to screen.
    val screenFacing = screen.facing.getOpposite
    if (screenFacing.offsetX * (x + 0.5) + screenFacing.offsetY * (y + 0.5) + screenFacing.offsetZ * (z + 0.5) < 0) {
      return
    }

    GL11.glPushAttrib(0xFFFFFF)

    RenderState.disableLighting()
    RenderState.makeItBlend()

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    if (distance > fadeDistanceSq) {
      RenderState.setBlendAlpha(0f max (1 - (distance - fadeDistanceSq) * fadeRatio).toFloat)
    }

    MonospaceFontRenderer.init(tileEntityRenderer.renderEngine)
    val list = cache.get(screen, this)
    compileOrDraw(list)

    GL11.glPopMatrix()
    GL11.glPopAttrib()
  }

  private def compileOrDraw(list: Int) = if (screen.hasChanged && !RenderState.compilingDisplayList) {
    screen.hasChanged = false
    val (sx, sy) = (screen.width, screen.height)
    val (tw, th) = (sx * 16f, sy * 16f)

    GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE)

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

    // Offset from border.
    GL11.glTranslatef(sx * 2.25f / tw, sy * 2.25f / th, 0)

    // Inner size (minus borders).
    val isx = sx - (4.5f / 16)
    val isy = sy - (4.5f / 16)

    // Scale based on actual buffer size.
    val (resX, resY) = screen.instance.resolution
    val sizeX = resX * MonospaceFontRenderer.fontWidth
    val sizeY = resY * MonospaceFontRenderer.fontHeight
    val scaleX = isx / sizeX
    val scaleY = isy / sizeY
    if (true) {
      if (scaleX > scaleY) {
        GL11.glTranslatef(sizeX * 0.5f * (scaleX - scaleY), 0, 0)
        GL11.glScalef(scaleY, scaleY, 1)
      }
      else {
        GL11.glTranslatef(0, sizeY * 0.5f * (scaleY - scaleX), 0)
        GL11.glScalef(scaleX, scaleX, 1)
      }
    }
    else {
      // Stretch to fit.
      GL11.glScalef(scaleX, scaleY, 1)
    }

    // Slightly offset the text so it doesn't clip into the screen.
    GL11.glTranslatef(0, 0, 0.01f)

    for (((line, color), i) <- screen.instance.lines.zip(screen.instance.color).zipWithIndex) {
      MonospaceFontRenderer.drawString(0, i * MonospaceFontRenderer.fontHeight, line, color, screen.instance.depth)
    }

    GL11.glEndList()

    true
  }
  else GL11.glCallList(list)

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
      val d = dx + ex
      d * d
    }
    else if (dx > ex) {
      val d = dx - ex
      d * d
    }
    else 0) + (if (dy < -ey) {
      val d = dy + ey
      d * d
    }
    else if (dy > ey) {
      val d = dy - ey
      d * d
    }
    else 0) + (if (dz < -ez) {
      val d = dz + ez
      d * d
    }
    else if (dz > ez) {
      val d = dz - ez
      d * d
    }
    else 0)
  }

  // ----------------------------------------------------------------------- //
  // Cache
  // ----------------------------------------------------------------------- //

  def call = {
    val list = GLAllocation.generateDisplayLists(1)
    screen.hasChanged = true // Force compilation.
    list
  }

  def onRemoval(e: RemovalNotification[TileEntity, Int]) {
    GLAllocation.deleteDisplayLists(e.getValue)
  }

  // ----------------------------------------------------------------------- //
  // ITickHandler
  // ----------------------------------------------------------------------- //

  def getLabel = "OpenComputers.Screen"

  def ticks() = util.EnumSet.of(TickType.CLIENT)

  def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) = cache.cleanUp()

  def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}