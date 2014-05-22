package li.cil.oc.client.renderer.tileentity

import com.google.common.cache.{CacheBuilder, RemovalNotification, RemovalListener}
import cpw.mods.fml.common.{TickType, ITickHandler}
import java.util
import java.util.concurrent.{TimeUnit, Callable}
import li.cil.oc.Settings
import li.cil.oc.client.TexturePreloader
import li.cil.oc.client.renderer.MonospaceFontRenderer
import li.cil.oc.common.block
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.util.RenderState
import li.cil.oc.util.mods.BuildCraft
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.{Tessellator, GLAllocation}
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

object ScreenRenderer extends TileEntitySpecialRenderer with Callable[Int] with RemovalListener[TileEntity, Int] with ITickHandler {
  private val maxRenderDistanceSq = Settings.get.maxScreenTextRenderDistance * Settings.get.maxScreenTextRenderDistance

  private val fadeDistanceSq = Settings.get.screenTextFadeStartDistance * Settings.get.screenTextFadeStartDistance

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
    if (!screen.isOrigin) {
      return
    }

    val distance = playerDistanceSq() / math.min(screen.width, screen.height)
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

    drawOverlay()

    if (distance > fadeDistanceSq) {
      RenderState.setBlendAlpha(math.max(0, 1 - ((distance - fadeDistanceSq) * fadeRatio).toFloat))
    }

    if (screen.hasPower && screen.relativeLitArea != 0) {
      MonospaceFontRenderer.init(tileEntityRenderer.renderEngine)
      compileOrDraw(cache.get(screen, this))
    }

    GL11.glPopMatrix()
    GL11.glPopAttrib()
  }

  private def transform() {
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
    GL11.glTranslatef(0, screen.height, 0)

    // Flip text upside down.
    GL11.glScalef(1, -1, 1)
  }

  private def drawOverlay() = if (screen.facing == ForgeDirection.UP || screen.facing == ForgeDirection.DOWN) {
    // Show up vector overlay when holding same screen block.
    val stack = Minecraft.getMinecraft.thePlayer.getHeldItem
    if (stack != null) {
      if (BuildCraft.holdsApplicableWrench(Minecraft.getMinecraft.thePlayer, screen.x, screen.y, screen.z) ||
        (stack.getItem match {
          case block: block.Item => block.getMetadata(stack.getItemDamage) == screen.getBlockMetadata
          case _ => false
        })) {
        GL11.glPushMatrix()
        transform()
        bindTexture(TexturePreloader.blockScreenUpIndicator)
        GL11.glDepthMask(false)
        GL11.glTranslatef(screen.width / 2f - 0.5f, screen.height / 2f - 0.5f, 0.05f)
        val t = Tessellator.instance
        t.startDrawingQuads()
        t.addVertexWithUV(0, 1, 0, 0, 1)
        t.addVertexWithUV(1, 1, 0, 1, 1)
        t.addVertexWithUV(1, 0, 0, 1, 0)
        t.addVertexWithUV(0, 0, 0, 0, 0)
        t.draw()
        GL11.glDepthMask(true)
        GL11.glPopMatrix()
      }
    }
  }

  private def compileOrDraw(list: Int) = if (screen.bufferIsDirty) {
    val sx = screen.width
    val sy = screen.height
    val tw = sx * 16f
    val th = sy * 16f

    val doCompile = !RenderState.compilingDisplayList
    if (doCompile) {
      screen.bufferIsDirty = false
      GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE)
    }

    transform()

    // Offset from border.
    GL11.glTranslatef(sx * 2.25f / tw, sy * 2.25f / th, 0)

    // Inner size (minus borders).
    val isx = sx - (4.5f / 16)
    val isy = sy - (4.5f / 16)

    // Scale based on actual buffer size.
    val (resX, resY) = screen.buffer.resolution
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

    for (((line, color), i) <- screen.buffer.lines.zip(screen.buffer.color).zipWithIndex) {
      MonospaceFontRenderer.drawString(0, i * MonospaceFontRenderer.fontHeight, line, color, screen.buffer.depth)
    }

    if (doCompile) {
      GL11.glEndList()
    }

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
    screen.bufferIsDirty = true // Force compilation.
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