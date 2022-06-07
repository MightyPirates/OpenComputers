package li.cil.oc.client.renderer

import li.cil.oc.client.Textures
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.{BlockPosition, RenderState}
import li.cil.oc.{Constants, Settings, api, common}
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.{RayTraceResult, Vec3d}
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

import scala.util.Random

object HighlightRenderer {
  private val random = new Random()

  lazy val tablet = api.Items.get(Constants.ItemName.Tablet)

  @SubscribeEvent
  def onDrawBlockHighlight(e: DrawBlockHighlightEvent): Unit = if (e.getTarget != null && e.getTarget.getBlockPos != null) {
    val hitInfo = e.getTarget
    val world = e.getPlayer.getEntityWorld
    val blockPos = BlockPosition(hitInfo.getBlockPos, world)
    if (hitInfo.typeOfHit == RayTraceResult.Type.BLOCK && api.Items.get(e.getPlayer.getHeldItemMainhand) == tablet) {
      val isAir = world.isAirBlock(blockPos)
      if (!isAir) {
        val block = world.getBlock(blockPos)
        val bounds = block.getSelectedBoundingBox(world.getBlockState(hitInfo.getBlockPos), world, hitInfo.getBlockPos).offset(-blockPos.x, -blockPos.y, -blockPos.z)
        val sideHit = hitInfo.sideHit
        val playerPos = new Vec3d(
          e.getPlayer.prevPosX + (e.getPlayer.posX - e.getPlayer.prevPosX) * e.getPartialTicks,
          e.getPlayer.prevPosY + (e.getPlayer.posY - e.getPlayer.prevPosY) * e.getPartialTicks,
          e.getPlayer.prevPosZ + (e.getPlayer.posZ - e.getPlayer.prevPosZ) * e.getPartialTicks)
        val renderPos = blockPos.offset(-playerPos.x, -playerPos.y, -playerPos.z)

        GlStateManager.pushMatrix()
        RenderState.pushAttrib()
        RenderState.makeItBlend()
        Textures.bind(Textures.Model.HologramEffect)

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
        GlStateManager.color(0.0F, 1.0F, 0.0F, 0.4F)

        GlStateManager.translate(renderPos.x, renderPos.y, renderPos.z)
        GlStateManager.scale(1.002, 1.002, 1.002)

        if (Settings.get.hologramFlickerFrequency > 0 && random.nextDouble() < Settings.get.hologramFlickerFrequency) {
          val (sx, sy, sz) = (1 - math.abs(sideHit.getXOffset), 1 - math.abs(sideHit.getYOffset), 1 - math.abs(sideHit.getZOffset))
          GlStateManager.scale(1 + random.nextGaussian() * 0.01, 1 + random.nextGaussian() * 0.001, 1 + random.nextGaussian() * 0.01)
          GlStateManager.translate(random.nextGaussian() * 0.01 * sx, random.nextGaussian() * 0.01 * sy, random.nextGaussian() * 0.01 * sz)
        }

        val t = Tessellator.getInstance()
        val r = t.getBuffer
        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        sideHit match {
          case EnumFacing.UP =>
            r.pos(bounds.maxX, bounds.maxY + 0.002, bounds.maxZ).tex(bounds.maxZ * 16, bounds.maxX * 16).endVertex()
            r.pos(bounds.maxX, bounds.maxY + 0.002, bounds.minZ).tex(bounds.minZ * 16, bounds.maxX * 16).endVertex()
            r.pos(bounds.minX, bounds.maxY + 0.002, bounds.minZ).tex(bounds.minZ * 16, bounds.minX * 16).endVertex()
            r.pos(bounds.minX, bounds.maxY + 0.002, bounds.maxZ).tex(bounds.maxZ * 16, bounds.minX * 16).endVertex()
          case EnumFacing.DOWN =>
            r.pos(bounds.maxX, bounds.minY - 0.002, bounds.minZ).tex(bounds.minZ * 16, bounds.maxX * 16).endVertex()
            r.pos(bounds.maxX, bounds.minY - 0.002, bounds.maxZ).tex(bounds.maxZ * 16, bounds.maxX * 16).endVertex()
            r.pos(bounds.minX, bounds.minY - 0.002, bounds.maxZ).tex(bounds.maxZ * 16, bounds.minX * 16).endVertex()
            r.pos(bounds.minX, bounds.minY - 0.002, bounds.minZ).tex(bounds.minZ * 16, bounds.minX * 16).endVertex()
          case EnumFacing.EAST =>
            r.pos(bounds.maxX + 0.002, bounds.maxY, bounds.minZ).tex(bounds.minZ * 16, bounds.maxY * 16).endVertex()
            r.pos(bounds.maxX + 0.002, bounds.maxY, bounds.maxZ).tex(bounds.maxZ * 16, bounds.maxY * 16).endVertex()
            r.pos(bounds.maxX + 0.002, bounds.minY, bounds.maxZ).tex(bounds.maxZ * 16, bounds.minY * 16).endVertex()
            r.pos(bounds.maxX + 0.002, bounds.minY, bounds.minZ).tex(bounds.minZ * 16, bounds.minY * 16).endVertex()
          case EnumFacing.WEST =>
            r.pos(bounds.minX - 0.002, bounds.maxY, bounds.maxZ).tex(bounds.maxZ * 16, bounds.maxY * 16).endVertex()
            r.pos(bounds.minX - 0.002, bounds.maxY, bounds.minZ).tex(bounds.minZ * 16, bounds.maxY * 16).endVertex()
            r.pos(bounds.minX - 0.002, bounds.minY, bounds.minZ).tex(bounds.minZ * 16, bounds.minY * 16).endVertex()
            r.pos(bounds.minX - 0.002, bounds.minY, bounds.maxZ).tex(bounds.maxZ * 16, bounds.minY * 16).endVertex()
          case EnumFacing.SOUTH =>
            r.pos(bounds.maxX, bounds.maxY, bounds.maxZ + 0.002).tex(bounds.maxX * 16, bounds.maxY * 16).endVertex()
            r.pos(bounds.minX, bounds.maxY, bounds.maxZ + 0.002).tex(bounds.minX * 16, bounds.maxY * 16).endVertex()
            r.pos(bounds.minX, bounds.minY, bounds.maxZ + 0.002).tex(bounds.minX * 16, bounds.minY * 16).endVertex()
            r.pos(bounds.maxX, bounds.minY, bounds.maxZ + 0.002).tex(bounds.maxX * 16, bounds.minY * 16).endVertex()
          case _ =>
            r.pos(bounds.minX, bounds.maxY, bounds.minZ - 0.002).tex(bounds.minX * 16, bounds.maxY * 16).endVertex()
            r.pos(bounds.maxX, bounds.maxY, bounds.minZ - 0.002).tex(bounds.maxX * 16, bounds.maxY * 16).endVertex()
            r.pos(bounds.maxX, bounds.minY, bounds.minZ - 0.002).tex(bounds.maxX * 16, bounds.minY * 16).endVertex()
            r.pos(bounds.minX, bounds.minY, bounds.minZ - 0.002).tex(bounds.minX * 16, bounds.minY * 16).endVertex()
        }
        t.draw()

        RenderState.disableBlend()
        RenderState.popAttrib()
        GlStateManager.popMatrix()
      }
    }

    if (hitInfo.typeOfHit == RayTraceResult.Type.BLOCK) e.getPlayer.getEntityWorld.getTileEntity(hitInfo.getBlockPos) match {
      case print: common.tileentity.Print if print.shapes.nonEmpty =>
        val pos = new Vec3d(
          e.getPlayer.prevPosX + (e.getPlayer.posX - e.getPlayer.prevPosX) * e.getPartialTicks,
          e.getPlayer.prevPosY + (e.getPlayer.posY - e.getPlayer.prevPosY) * e.getPartialTicks,
          e.getPlayer.prevPosZ + (e.getPlayer.posZ - e.getPlayer.prevPosZ) * e.getPartialTicks)
        val expansion = 0.002f

        // See RenderGlobal.drawSelectionBox.
        GlStateManager.enableBlend()
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 1)
        GlStateManager.color(0, 0, 0, 0.4f)
        GlStateManager.glLineWidth(2)
        GlStateManager.disableTexture2D()
        GlStateManager.depthMask(false)

        for (shape <- print.shapes) {
          val bounds = shape.bounds.rotateTowards(print.facing)
          RenderGlobal.drawSelectionBoundingBox(bounds.grow(expansion, expansion, expansion)
            .offset(blockPos.x, blockPos.y, blockPos.z)
            .offset(-pos.x, -pos.y, -pos.z), 0, 0, 0, 0x66/0xFFf.toFloat)
        }

        GlStateManager.depthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()

        e.setCanceled(true)
      case cable: common.tileentity.Cable =>
        // See RenderGlobal.drawSelectionBox.
        GlStateManager.enableBlend()
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 1)
        GlStateManager.color(0, 0, 0, 0.4f)
        GlStateManager.glLineWidth(2)
        GlStateManager.disableTexture2D()
        GlStateManager.depthMask(false)
        GlStateManager.pushMatrix()

        val player = e.getPlayer
        GlStateManager.translate(
          blockPos.x - (player.lastTickPosX + (player.posX - player.lastTickPosX) * e.getPartialTicks),
          blockPos.y - (player.lastTickPosY + (player.posY - player.lastTickPosY) * e.getPartialTicks),
          blockPos.z - (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * e.getPartialTicks)
        )

        val mask = common.block.Cable.neighbors(world, hitInfo.getBlockPos)
        val tesselator = Tessellator.getInstance
        val buffer = tesselator.getBuffer

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)
        Cable.drawOverlay(buffer, mask)
        tesselator.draw()

        GlStateManager.popMatrix()
        GlStateManager.depthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()

        e.setCanceled(true)
      case _ =>
    }
  }

  private object Cable {
    private final val EXPAND = 0.002f
    private final val MIN = common.block.Cable.MIN - EXPAND
    private final val MAX = common.block.Cable.MAX + EXPAND

    def drawOverlay(buffer: BufferBuilder, mask: Int): Unit = {
      // Draw the cable arms
      for (side <- EnumFacing.values) {
        if (((1 << side.getIndex) & mask) != 0) {
          val offset = if (side.getAxisDirection == EnumFacing.AxisDirection.NEGATIVE) -EXPAND else 1 + EXPAND
          val centre = if (side.getAxisDirection == EnumFacing.AxisDirection.NEGATIVE) MIN else MAX

          // Draw the arm end quad
          drawLineAdjacent(buffer, side.getAxis, offset, MIN, MIN, MIN, MAX)
          drawLineAdjacent(buffer, side.getAxis, offset, MIN, MAX, MAX, MAX)
          drawLineAdjacent(buffer, side.getAxis, offset, MAX, MAX, MAX, MIN)
          drawLineAdjacent(buffer, side.getAxis, offset, MAX, MIN, MIN, MIN)

          // Draw the connecting lines to the middle
          drawLineAlong(buffer, side.getAxis, MIN, MIN, offset, centre)
          drawLineAlong(buffer, side.getAxis, MAX, MIN, offset, centre)
          drawLineAlong(buffer, side.getAxis, MAX, MAX, offset, centre)
          drawLineAlong(buffer, side.getAxis, MIN, MAX, offset, centre)
        }
      }

      // Draw the cable core
      drawCore(buffer, mask, EnumFacing.WEST, EnumFacing.DOWN, EnumFacing.Axis.Z)
      drawCore(buffer, mask, EnumFacing.WEST, EnumFacing.UP, EnumFacing.Axis.Z)
      drawCore(buffer, mask, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.Axis.Z)
      drawCore(buffer, mask, EnumFacing.EAST, EnumFacing.UP, EnumFacing.Axis.Z)

      drawCore(buffer, mask, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.Axis.Y)
      drawCore(buffer, mask, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.Axis.Y)
      drawCore(buffer, mask, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.Axis.Y)
      drawCore(buffer, mask, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.Axis.Y)

      drawCore(buffer, mask, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.Axis.X)
      drawCore(buffer, mask, EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.Axis.X)
      drawCore(buffer, mask, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.Axis.X)
      drawCore(buffer, mask, EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.Axis.X)
    }

    /** Draw part of the core object */
    private def drawCore(buffer: BufferBuilder, mask: Int, a: EnumFacing, b: EnumFacing, other: EnumFacing.Axis): Unit = {
      if (((mask >> a.ordinal) & 1) != ((mask >> b.ordinal) & 1)) return

      val offA = if (a.getAxisDirection == EnumFacing.AxisDirection.NEGATIVE) MIN else MAX
      val offB = if (b.getAxisDirection == EnumFacing.AxisDirection.NEGATIVE) MIN else MAX
      drawLineAlong(buffer, other, offA, offB, MIN, MAX)
    }

    /** Draw a line parallel to an axis */
    private def drawLineAlong(buffer: BufferBuilder, axis: EnumFacing.Axis, offA: Double, offB: Double, start: Double, end: Double): Unit = {
      axis match {
        case EnumFacing.Axis.X =>
          buffer.pos(start, offA, offB).endVertex()
          buffer.pos(end, offA, offB).endVertex()
        case EnumFacing.Axis.Y =>
          buffer.pos(offA, start, offB).endVertex()
          buffer.pos(offA, end, offB).endVertex()
        case EnumFacing.Axis.Z =>
          buffer.pos(offA, offB, start).endVertex()
          buffer.pos(offA, offB, end).endVertex()
      }
    }

    /** Draw a line perpendicular to an axis */
    private def drawLineAdjacent(buffer: BufferBuilder, axis: EnumFacing.Axis, offset: Double, startA: Double, startB: Double, endA: Double, endB: Double): Unit = {
      axis match {
        case EnumFacing.Axis.X =>
          buffer.pos(offset, startA, startB).endVertex()
          buffer.pos(offset, endA, endB).endVertex()
        case EnumFacing.Axis.Y =>
          buffer.pos(startA, offset, startB).endVertex()
          buffer.pos(endA, offset, endB).endVertex()
        case EnumFacing.Axis.Z =>
          buffer.pos(startA, startB, offset).endVertex()
          buffer.pos(endA, endB, offset).endVertex()
      }
    }
  }

}
