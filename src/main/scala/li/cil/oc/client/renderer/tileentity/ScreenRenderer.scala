package li.cil.oc.client.renderer.tileentity

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GLContext

object ScreenRenderer extends TileEntitySpecialRenderer[Screen] {
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

  override def render(screen: Screen, x: Double, y: Double, z: Double, f: Float, damage: Int, alpha: Float) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    this.screen = screen
    if (!screen.isOrigin) {
      return
    }

    val distance = playerDistanceSq() / math.min(screen.width, screen.height)
    if (distance > maxRenderDistanceSq) {
      return
    }

    // y = block.bottom - player.feet
    // eye is higher, so the y delta should be more negative
    val eye_delta: Double = y - Minecraft.getMinecraft.player.getEyeHeight

    // Crude check whether screen text can be seen by the local player based
    // on the player's position -> angle relative to screen.
    val screenFacing = screen.facing.getOpposite
    if (screenFacing.getFrontOffsetX * (x + 0.5) + screenFacing.getFrontOffsetY * (eye_delta + 0.5) + screenFacing.getFrontOffsetZ * (z + 0.5) < 0) {
      return
    }

    RenderState.checkError(getClass.getName + ".render: checks")

    RenderState.pushAttrib()

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 0xFF, 0xFF)
    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    GlStateManager.color(1, 1, 1, 1)

    GlStateManager.pushMatrix()

    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    RenderState.checkError(getClass.getName + ".render: setup")

    drawOverlay()

    RenderState.checkError(getClass.getName + ".render: overlay")

    if (distance > fadeDistanceSq) {
      val alpha = math.max(0, 1 - ((distance - fadeDistanceSq) * fadeRatio).toFloat)
      if (canUseBlendColor) {
        GL14.glBlendColor(0, 0, 0, alpha)
        GlStateManager.blendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE)
      }
    }

    RenderState.checkError(getClass.getName + ".render: fade")

    if (screen.buffer.isRenderingEnabled) {
      draw()
    }

    RenderState.disableBlend()
    RenderState.enableEntityLighting()

    GlStateManager.popMatrix()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def transform() {
    screen.yaw match {
      case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
      case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
      case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
      case _ => // No yaw.
    }
    screen.pitch match {
      case EnumFacing.DOWN => GlStateManager.rotate(90, 1, 0, 0)
      case EnumFacing.UP => GlStateManager.rotate(-90, 1, 0, 0)
      case _ => // No pitch.
    }

    // Fit area to screen (bottom left = bottom left).
    GlStateManager.translate(-0.5f, -0.5f, 0.5f)
    GlStateManager.translate(0, screen.height, 0)

    // Flip text upside down.
    GlStateManager.scale(1, -1, 1)
  }

  private def drawOverlay() = if (screen.facing == EnumFacing.UP || screen.facing == EnumFacing.DOWN) {
    // Show up vector overlay when holding same screen block.
    val stack = Minecraft.getMinecraft.player.getHeldItemMainhand
    if (!stack.isEmpty) {
      if (Wrench.holdsApplicableWrench(Minecraft.getMinecraft.player, screen.getPos) || screens.contains(api.Items.get(stack))) {
        GlStateManager.pushMatrix()
        transform()
        GlStateManager.depthMask(false)
        GlStateManager.translate(screen.width / 2f - 0.5f, screen.height / 2f - 0.5f, 0.05f)

        val t = Tessellator.getInstance
        val r = t.getBuffer

        Textures.Block.bind()
        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

        val icon = Textures.getSprite(Textures.Block.ScreenUpIndicator)
        r.pos(0, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(1, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(1, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(0, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()

        t.draw()

        GlStateManager.depthMask(true)
        GlStateManager.popMatrix()
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
    GlStateManager.translate(sx * 2.25f / tw, sy * 2.25f / th, 0)

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
        GlStateManager.translate(sizeX * 0.5f * (scaleX - scaleY), 0, 0)
        GlStateManager.scale(scaleY, scaleY, 1)
      }
      else {
        GlStateManager.translate(0, sizeY * 0.5f * (scaleY - scaleX), 0)
        GlStateManager.scale(scaleX, scaleX, 1)
      }
    }
    else {
      // Stretch to fit.
      GlStateManager.scale(scaleX, scaleY, 1)
    }

    // Slightly offset the text so it doesn't clip into the screen.
    GlStateManager.translate(0, 0, 0.01)

    RenderState.checkError(getClass.getName + ".draw: setup")

    // Render the actual text.
    screen.buffer.renderText()

    RenderState.checkError(getClass.getName + ".draw: text")
  }

  private def playerDistanceSq() = {
    val player = Minecraft.getMinecraft.player
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
