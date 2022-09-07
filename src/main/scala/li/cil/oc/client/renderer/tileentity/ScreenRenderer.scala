package li.cil.oc.client.renderer.tileentity

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GLContext

object ScreenRenderer extends TileEntitySpecialRenderer {
  private val maxRenderDistanceSq = Settings.get.maxScreenTextRenderDistance * Settings.get.maxScreenTextRenderDistance

  private val fadeDistanceSq = Settings.get.screenTextFadeStartDistance * Settings.get.screenTextFadeStartDistance

  private val fadeRatio = 1.0 / (maxRenderDistanceSq - fadeDistanceSq)

  private var screen: Screen = null

  private lazy val screens = Set(
    api.Items.get(Constants.BlockName.ScreenTier1),
    api.Items.get(Constants.BlockName.ScreenTier2),
    api.Items.get(Constants.BlockName.ScreenTier3))

  private val canUseBlendColor = GLContext.getCapabilities.OpenGL14

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def renderTileEntityAt(t: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

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

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: checks")

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 0xFF, 0xFF)
    RenderState.disableLighting()
    RenderState.makeItBlend()

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: setup")

    drawOverlay()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: overlay")

    if (distance > fadeDistanceSq) {
      val alpha = math.max(0, 1 - ((distance - fadeDistanceSq) * fadeRatio).toFloat)
      if (canUseBlendColor) {
        GL14.glBlendColor(0, 0, 0, alpha)
        GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE)
      }
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: fade")

    if (screen.buffer.isRenderingEnabled) {
      val profiler = Minecraft.getMinecraft.mcProfiler
      profiler.startSection("opencomputers:screen_text")
      draw()
      profiler.endSection()
    }

    RenderState.enableLighting()

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
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
      if (Wrench.holdsApplicableWrench(Minecraft.getMinecraft.thePlayer, screen.position) || screens.contains(api.Items.get(stack))) {
        GL11.glPushMatrix()
        transform()
        bindTexture(Textures.blockScreenUpIndicator)
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

  private def draw() {
    RenderState.checkError(getClass.getName + ".draw: entering (aka: wasntme)")

    val sx = screen.width
    val sy = screen.height
    val tw = sx * 16f
    val th = sy * 16f

    transform()

    // Offset from border.
    GL11.glTranslatef(sx * 2.25f / tw, sy * 2.25f / th, 0)

    // Inner size (minus borders).
    val isx = sx - (4.5f / 16)
    val isy = sy - (4.5f / 16)

    // Scale based on actual buffer size.
    val sizeX = screen.buffer.renderWidth
    val sizeY = screen.buffer.renderHeight
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
    GL11.glTranslated(0, 0, 0.01)

    RenderState.checkError(getClass.getName + ".draw: setup")

    // Render the actual text.
    screen.buffer.renderText()

    RenderState.checkError(getClass.getName + ".draw: text")
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
}