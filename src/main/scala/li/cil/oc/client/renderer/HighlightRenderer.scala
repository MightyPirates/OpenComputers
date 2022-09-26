package li.cil.oc.client.renderer

import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.client.Textures
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.BlockPosition
import li.cil.oc.{Constants, Settings, api}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.Direction
import net.minecraft.util.Hand
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraftforge.client.event.DrawHighlightEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

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
  }
}
