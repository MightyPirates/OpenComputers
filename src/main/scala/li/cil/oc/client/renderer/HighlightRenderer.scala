package li.cil.oc.client.renderer

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.{BlockPosition, RenderState}
import li.cil.oc.{Constants, Settings, api, common}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.vector.Vector3d
import net.minecraftforge.client.event.DrawHighlightEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.lwjgl.opengl.GL11

import scala.util.Random

object HighlightRenderer {
  private val random = new Random()

  lazy val tablet = api.Items.get(Constants.ItemName.Tablet)

  val TexHologram = RenderTypes.createTexturedQuad("hologram_effect", Textures.Model.HologramEffect, DefaultVertexFormats.POSITION_TEX_COLOR, true)

  @SubscribeEvent
  def onDrawBlockHighlight(e: DrawHighlightEvent.HighlightBlock): Unit = if (e.getTarget != null && e.getTarget.getBlockPos != null) {
    val hitInfo = e.getTarget
    val world = Minecraft.getInstance.level
    val blockPos = BlockPosition(hitInfo.getBlockPos, world)
    val stack = e.getMatrix
    if (api.Items.get(Minecraft.getInstance.player.getItemInHand(Hand.MAIN_HAND)) == tablet) {
      val isAir = world.isAirBlock(blockPos)
      if (!isAir) {
        val shape = world.getBlockState(hitInfo.getBlockPos).getShape(world, hitInfo.getBlockPos, ISelectionContext.of(e.getInfo.getEntity))
        val (minX, minY, minZ) = (shape.min(Direction.Axis.X).toFloat, shape.min(Direction.Axis.Y).toFloat, shape.min(Direction.Axis.Z).toFloat)
        val (maxX, maxY, maxZ) = (shape.max(Direction.Axis.X).toFloat, shape.max(Direction.Axis.Y).toFloat, shape.max(Direction.Axis.Z).toFloat)
        val sideHit = hitInfo.getDirection
        val view = e.getInfo.getPosition

        stack.pushPose()

        stack.translate(blockPos.x - view.x, blockPos.y - view.y, blockPos.z - view.z)
        stack.scale(1.002f, 1.002f, 1.002f)

        if (Settings.get.hologramFlickerFrequency > 0 && random.nextDouble() < Settings.get.hologramFlickerFrequency) {
          val (sx, sy, sz) = (1 - math.abs(sideHit.getStepX), 1 - math.abs(sideHit.getStepY), 1 - math.abs(sideHit.getStepZ))
          stack.scale(1f + (random.nextGaussian() * 0.01).toFloat, 1f + (random.nextGaussian() * 0.001).toFloat, 1f + (random.nextGaussian() * 0.01).toFloat)
          stack.translate((random.nextGaussian() * 0.01 * sx).toFloat, (random.nextGaussian() * 0.01 * sy).toFloat, (random.nextGaussian() * 0.01 * sz).toFloat)
        }

        val r = e.getBuffers.getBuffer(TexHologram)
        sideHit match {
          case Direction.UP =>
            r.vertex(stack.last.pose, maxX, maxY + 0.002f, maxZ).uv(maxZ * 16, maxX * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, maxX, maxY + 0.002f, minZ).uv(minZ * 16, maxX * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX, maxY + 0.002f, minZ).uv(minZ * 16, minX * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX, maxY + 0.002f, maxZ).uv(maxZ * 16, minX * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
          case Direction.DOWN =>
            r.vertex(stack.last.pose, maxX, minY - 0.002f, minZ).uv(minZ * 16, maxX * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, maxX, minY - 0.002f, maxZ).uv(maxZ * 16, maxX * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX, minY - 0.002f, maxZ).uv(maxZ * 16, minX * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX, minY - 0.002f, minZ).uv(minZ * 16, minX * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
          case Direction.EAST =>
            r.vertex(stack.last.pose, maxX + 0.002f, maxY, minZ).uv(minZ * 16, maxY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, maxX + 0.002f, maxY, maxZ).uv(maxZ * 16, maxY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, maxX + 0.002f, minY, maxZ).uv(maxZ * 16, minY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, maxX + 0.002f, minY, minZ).uv(minZ * 16, minY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
          case Direction.WEST =>
            r.vertex(stack.last.pose, minX - 0.002f, maxY, maxZ).uv(maxZ * 16, maxY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX - 0.002f, maxY, minZ).uv(minZ * 16, maxY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX - 0.002f, minY, minZ).uv(minZ * 16, minY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX - 0.002f, minY, maxZ).uv(maxZ * 16, minY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
          case Direction.SOUTH =>
            r.vertex(stack.last.pose, maxX, maxY, maxZ + 0.002f).uv(maxX * 16, maxY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX, maxY, maxZ + 0.002f).uv(minX * 16, maxY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX, minY, maxZ + 0.002f).uv(minX * 16, minY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, maxX, minY, maxZ + 0.002f).uv(maxX * 16, minY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
          case _ =>
            r.vertex(stack.last.pose, minX, maxY, minZ - 0.002f).uv(minX * 16, maxY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, maxX, maxY, minZ - 0.002f).uv(maxX * 16, maxY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, maxX, minY, minZ - 0.002f).uv(maxX * 16, minY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
            r.vertex(stack.last.pose, minX, minY, minZ - 0.002f).uv(minX * 16, minY * 16).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex()
        }

        stack.popPose()
      }
    }

    Minecraft.getInstance.level.getBlockEntity(hitInfo.getBlockPos) match {
      case print: common.tileentity.Print if print.shapes.nonEmpty =>
        val expansion = 0.002f

        // See WorldRenderer.renderHitOutline.
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 1)
        RenderSystem.color4f(0, 0, 0, 0.4f)
        RenderSystem.lineWidth(2)
        RenderSystem.disableTexture()
        RenderSystem.depthMask(false)

        val tesselator = Tessellator.getInstance
        val buffer = tesselator.getBuilder()
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)
        for (shape <- print.shapes) {
          val bounds = shape.bounds.rotateTowards(print.facing)
          WorldRenderer.renderLineBox(stack, buffer, bounds.inflate(expansion, expansion, expansion)
            .move(blockPos.x, blockPos.y, blockPos.z), 0, 0, 0, 0x66/0xFFf.toFloat)
        }
        tesselator.end()

        RenderSystem.depthMask(true)
        RenderSystem.enableTexture()
        RenderSystem.disableBlend()

        e.setCanceled(true)
      case cable: common.tileentity.Cable =>
        // See WorldRenderer.renderHitOutline.
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 1)
        RenderSystem.color4f(0, 0, 0, 0.4f)
        RenderSystem.lineWidth(2)
        RenderSystem.disableTexture()
        RenderSystem.depthMask(false)
        stack.pushPose()

        stack.translate(blockPos.x, blockPos.y, blockPos.z)

        val mask = common.block.Cable.neighbors(world, hitInfo.getBlockPos)
        val tesselator = Tessellator.getInstance
        val buffer = tesselator.getBuilder()

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)
        Cable.drawOverlay(stack, buffer, mask)
        tesselator.end()

        stack.popPose()
        RenderSystem.depthMask(true)
        RenderSystem.enableTexture()
        RenderSystem.disableBlend()

        e.setCanceled(true)
      case _ =>
    }
  }

  private object Cable {
    private final val EXPAND = 0.002f
    private final val MIN = (common.block.Cable.MIN - EXPAND).toFloat
    private final val MAX = (common.block.Cable.MAX + EXPAND).toFloat

    def drawOverlay(stack: MatrixStack, buffer: BufferBuilder, mask: Int): Unit = {
      // Draw the cable arms
      for (side <- Direction.values) {
        if (((1 << side.get3DDataValue) & mask) != 0) {
          val offset = if (side.getAxisDirection == Direction.AxisDirection.NEGATIVE) -EXPAND else 1 + EXPAND
          val centre = if (side.getAxisDirection == Direction.AxisDirection.NEGATIVE) MIN else MAX

          // Draw the arm end quad
          drawLineAdjacent(stack, buffer, side.getAxis, offset, MIN, MIN, MIN, MAX)
          drawLineAdjacent(stack, buffer, side.getAxis, offset, MIN, MAX, MAX, MAX)
          drawLineAdjacent(stack, buffer, side.getAxis, offset, MAX, MAX, MAX, MIN)
          drawLineAdjacent(stack, buffer, side.getAxis, offset, MAX, MIN, MIN, MIN)

          // Draw the connecting lines to the middle
          drawLineAlong(stack, buffer, side.getAxis, MIN, MIN, offset, centre)
          drawLineAlong(stack, buffer, side.getAxis, MAX, MIN, offset, centre)
          drawLineAlong(stack, buffer, side.getAxis, MAX, MAX, offset, centre)
          drawLineAlong(stack, buffer, side.getAxis, MIN, MAX, offset, centre)
        }
      }

      // Draw the cable core
      drawCore(stack, buffer, mask, Direction.WEST, Direction.DOWN, Direction.Axis.Z)
      drawCore(stack, buffer, mask, Direction.WEST, Direction.UP, Direction.Axis.Z)
      drawCore(stack, buffer, mask, Direction.EAST, Direction.DOWN, Direction.Axis.Z)
      drawCore(stack, buffer, mask, Direction.EAST, Direction.UP, Direction.Axis.Z)

      drawCore(stack, buffer, mask, Direction.WEST, Direction.NORTH, Direction.Axis.Y)
      drawCore(stack, buffer, mask, Direction.WEST, Direction.SOUTH, Direction.Axis.Y)
      drawCore(stack, buffer, mask, Direction.EAST, Direction.NORTH, Direction.Axis.Y)
      drawCore(stack, buffer, mask, Direction.EAST, Direction.SOUTH, Direction.Axis.Y)

      drawCore(stack, buffer, mask, Direction.DOWN, Direction.NORTH, Direction.Axis.X)
      drawCore(stack, buffer, mask, Direction.DOWN, Direction.SOUTH, Direction.Axis.X)
      drawCore(stack, buffer, mask, Direction.UP, Direction.NORTH, Direction.Axis.X)
      drawCore(stack, buffer, mask, Direction.UP, Direction.SOUTH, Direction.Axis.X)
    }

    /** Draw part of the core object */
    private def drawCore(stack: MatrixStack, buffer: BufferBuilder, mask: Int, a: Direction, b: Direction, other: Direction.Axis): Unit = {
      if (((mask >> a.ordinal) & 1) != ((mask >> b.ordinal) & 1)) return

      val offA = if (a.getAxisDirection == Direction.AxisDirection.NEGATIVE) MIN else MAX
      val offB = if (b.getAxisDirection == Direction.AxisDirection.NEGATIVE) MIN else MAX
      drawLineAlong(stack, buffer, other, offA, offB, MIN, MAX)
    }

    /** Draw a line parallel to an axis */
    private def drawLineAlong(stack: MatrixStack, buffer: BufferBuilder, axis: Direction.Axis, offA: Float, offB: Float, start: Float, end: Float): Unit = {
      axis match {
        case Direction.Axis.X =>
          buffer.vertex(stack.last.pose, start, offA, offB).endVertex()
          buffer.vertex(stack.last.pose, end, offA, offB).endVertex()
        case Direction.Axis.Y =>
          buffer.vertex(stack.last.pose, offA, start, offB).endVertex()
          buffer.vertex(stack.last.pose, offA, end, offB).endVertex()
        case Direction.Axis.Z =>
          buffer.vertex(stack.last.pose, offA, offB, start).endVertex()
          buffer.vertex(stack.last.pose, offA, offB, end).endVertex()
      }
    }

    /** Draw a line perpendicular to an axis */
    private def drawLineAdjacent(stack: MatrixStack, buffer: BufferBuilder, axis: Direction.Axis, offset: Float, startA: Float, startB: Float, endA: Float, endB: Float): Unit = {
      axis match {
        case Direction.Axis.X =>
          buffer.vertex(stack.last.pose, offset, startA, startB).endVertex()
          buffer.vertex(stack.last.pose, offset, endA, endB).endVertex()
        case Direction.Axis.Y =>
          buffer.vertex(stack.last.pose, startA, offset, startB).endVertex()
          buffer.vertex(stack.last.pose, endA, offset, endB).endVertex()
        case Direction.Axis.Z =>
          buffer.vertex(stack.last.pose, startA, startB, offset).endVertex()
          buffer.vertex(stack.last.pose, endA, endB, offset).endVertex()
      }
    }
  }

}
