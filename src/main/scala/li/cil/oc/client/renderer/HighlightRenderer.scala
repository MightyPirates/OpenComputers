package li.cil.oc.client.renderer

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
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
          val (sx, sy, sz) = (1 - math.abs(sideHit.getFrontOffsetX), 1 - math.abs(sideHit.getFrontOffsetY), 1 - math.abs(sideHit.getFrontOffsetZ))
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
        GL11.glLineWidth(2)
        GlStateManager.disableTexture2D()
        GlStateManager.depthMask(false)

        for (shape <- print.shapes) {
          val bounds = shape.bounds.rotateTowards(print.facing)
          RenderGlobal.drawSelectionBoundingBox(bounds.expand(expansion, expansion, expansion)
            .offset(blockPos.x, blockPos.y, blockPos.z)
            .offset(-pos.x, -pos.y, -pos.z), 0, 0, 0, 0x66/0xFFf.toFloat)
        }

        GlStateManager.depthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()

        e.setCanceled(true)
      case _ =>
    }
  }
}
