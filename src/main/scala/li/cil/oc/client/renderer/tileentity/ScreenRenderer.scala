package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.vector.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14

object ScreenRenderer extends Function[TileEntityRendererDispatcher, ScreenRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new ScreenRenderer(dispatch)
}

class ScreenRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[Screen](dispatch) {
  private val maxRenderDistanceSq = Settings.get.maxScreenTextRenderDistance * Settings.get.maxScreenTextRenderDistance

  private val fadeDistanceSq = Settings.get.screenTextFadeStartDistance * Settings.get.screenTextFadeStartDistance

  private val fadeRatio = 1.0 / (maxRenderDistanceSq - fadeDistanceSq)

  private var screen: Screen = null

  private lazy val screens = Set(
    api.Items.get(Constants.BlockName.ScreenTier1),
    api.Items.get(Constants.BlockName.ScreenTier2),
    api.Items.get(Constants.BlockName.ScreenTier3))

  private val canUseBlendColor = true // Minecraft 1.16 already requires OpenGL 2.0 or above.

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def render(screen: Screen, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    this.screen = screen
    if (!screen.isOrigin) {
      return
    }

    val distance = playerDistanceSq() / math.min(screen.width, screen.height)
    if (distance > maxRenderDistanceSq) {
      return
    }

    val eye_pos = Minecraft.getInstance.player.getEyePosition(dt)
    val eye_delta: Double = screen.getBlockPos.getY - eye_pos.y

    // Crude check whether screen text can be seen by the local player based
    // on the player's position -> angle relative to screen.
    val screenFacing = screen.facing.getOpposite
    val x = screen.getBlockPos.getX - eye_pos.x
    val z = screen.getBlockPos.getZ - eye_pos.z
    if (screenFacing.getStepX * (x + 0.5) + screenFacing.getStepY * (eye_delta + 0.5) + screenFacing.getStepZ * (z + 0.5) < 0) {
      return
    }

    RenderState.checkError(getClass.getName + ".render: checks")

    RenderState.pushAttrib()

    RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 0xFF, 0xFF)
    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderSystem.color4f(1, 1, 1, 1)

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)

    RenderState.checkError(getClass.getName + ".render: setup")

    drawOverlay(stack)

    RenderState.checkError(getClass.getName + ".render: overlay")

    if (distance > fadeDistanceSq) {
      val alpha = math.max(0, 1 - ((distance - fadeDistanceSq) * fadeRatio).toFloat)
      if (canUseBlendColor) {
        GL14.glBlendColor(0, 0, 0, alpha)
        RenderSystem.blendFunc(GL14.GL_CONSTANT_ALPHA, GL11.GL_ONE)
      }
    }

    RenderState.checkError(getClass.getName + ".render: fade")

    if (screen.buffer.isRenderingEnabled) {
      draw(stack)
    }

    RenderState.disableBlend()
    RenderState.enableEntityLighting()

    stack.popPose()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def transform(stack: MatrixStack) {
    screen.yaw match {
      case Direction.WEST => stack.mulPose(Vector3f.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Vector3f.YP.rotationDegrees(180))
      case Direction.EAST => stack.mulPose(Vector3f.YP.rotationDegrees(90))
      case _ => // No yaw.
    }
    screen.pitch match {
      case Direction.DOWN => stack.mulPose(Vector3f.XP.rotationDegrees(90))
      case Direction.UP => stack.mulPose(Vector3f.XP.rotationDegrees(-90))
      case _ => // No pitch.
    }

    // Fit area to screen (bottom left = bottom left).
    stack.translate(-0.5f, -0.5f, 0.5f)
    stack.translate(0, screen.height, 0)

    // Flip text upside down.
    stack.scale(1, -1, 1)
  }

  private def drawOverlay(matrix: MatrixStack) = if (screen.facing == Direction.UP || screen.facing == Direction.DOWN) {
    // Show up vector overlay when holding same screen block.
    val stack = Minecraft.getInstance.player.getItemInHand(Hand.MAIN_HAND)
    if (!stack.isEmpty) {
      if (Wrench.holdsApplicableWrench(Minecraft.getInstance.player, screen.getBlockPos) || screens.contains(api.Items.get(stack))) {
        matrix.pushPose()
        transform(matrix)
        RenderSystem.depthMask(false)
        matrix.translate(screen.width / 2f - 0.5f, screen.height / 2f - 0.5f, 0.05f)

        val t = Tessellator.getInstance
        val r = t.getBuilder

        Textures.Block.bind()
        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

        val icon = Textures.getSprite(Textures.Block.ScreenUpIndicator)
        r.vertex(matrix.last.pose, 0, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
        r.vertex(matrix.last.pose, 1, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
        r.vertex(matrix.last.pose, 1, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
        r.vertex(matrix.last.pose, 0, 0, 0).uv(icon.getU0, icon.getV0).endVertex()

        t.end()

        RenderSystem.depthMask(true)
        matrix.popPose()
      }
    }
  }

  private def draw(stack: MatrixStack) {
    RenderState.checkError(getClass.getName + ".draw: entering (aka: wasntme)")

    val sx = screen.width
    val sy = screen.height
    val tw = sx * 16f
    val th = sy * 16f

    transform(stack)

    // Offset from border.
    stack.translate(sx * 2.25f / tw, sy * 2.25f / th, 0)

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
        stack.translate(sizeX * 0.5f * (scaleX - scaleY), 0, 0)
        stack.scale(scaleY, scaleY, 1)
      }
      else {
        stack.translate(0, sizeY * 0.5f * (scaleY - scaleX), 0)
        stack.scale(scaleX, scaleX, 1)
      }
    }
    else {
      // Stretch to fit.
      stack.scale(scaleX, scaleY, 1)
    }

    // Slightly offset the text so it doesn't clip into the screen.
    stack.translate(0, 0, 0.01)

    RenderState.checkError(getClass.getName + ".draw: setup")

    // Render the actual text.
    screen.buffer.renderText(stack)

    RenderState.checkError(getClass.getName + ".draw: text")
  }

  private def playerDistanceSq() = {
    val player = Minecraft.getInstance.player
    val bounds = screen.getRenderBoundingBox

    val px = player.getX
    val py = player.getY
    val pz = player.getZ

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
