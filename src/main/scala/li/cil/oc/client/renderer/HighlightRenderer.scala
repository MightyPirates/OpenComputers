package li.cil.oc.client.renderer

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

import scala.util.Random

object HighlightRenderer {
  private val random = new Random()

  lazy val tablet = api.Items.get(Constants.ItemName.Tablet)

  @SubscribeEvent
  def onDrawBlockHighlight(e: DrawBlockHighlightEvent): Unit = {
    val hitInfo = e.target
    if (hitInfo.typeOfHit == MovingObjectType.BLOCK && api.Items.get(e.currentItem) == tablet) {
      val world = e.player.getEntityWorld
      val blockPos = BlockPosition(hitInfo.getBlockPos, world)
      val isAir = world.isAirBlock(blockPos)
      if (!isAir) {
        val block = world.getBlock(blockPos)
        block.setBlockBoundsBasedOnState(blockPos)
        val bounds = block.getSelectedBoundingBoxFromPool(blockPos).offset(-blockPos.x, -blockPos.y, -blockPos.z)
        val sideHit = hitInfo.sideHit
        val playerPos = e.player.getPositionEyes(e.partialTicks)
        val renderPos = blockPos.offset(-playerPos.xCoord, -playerPos.yCoord, -playerPos.zCoord)

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()
        RenderState.makeItBlend()
        Textures.bind(Textures.Model.HologramEffect)

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
        GlStateManager.color(0.0F, 1.0F, 0.0F, 0.4F)

        GL11.glTranslated(renderPos.xCoord, renderPos.yCoord, renderPos.zCoord)
        GL11.glScaled(1.002, 1.002, 1.002)

        if (Settings.get.hologramFlickerFrequency > 0 && random.nextDouble() < Settings.get.hologramFlickerFrequency) {
          val (sx, sy, sz) = (1 - math.abs(sideHit.getFrontOffsetX), 1 - math.abs(sideHit.getFrontOffsetY), 1 - math.abs(sideHit.getFrontOffsetZ))
          GL11.glScaled(1 + random.nextGaussian() * 0.01, 1 + random.nextGaussian() * 0.001, 1 + random.nextGaussian() * 0.01)
          GL11.glTranslated(random.nextGaussian() * 0.01 * sx, random.nextGaussian() * 0.01 * sy, random.nextGaussian() * 0.01 * sz)
        }

        val t = Tessellator.getInstance()
        val r = t.getWorldRenderer
        r.startDrawingQuads()
        sideHit match {
          case EnumFacing.UP =>
            r.addVertexWithUV(bounds.maxX, bounds.maxY + 0.002, bounds.maxZ, bounds.maxZ * 16, bounds.maxX * 16)
            r.addVertexWithUV(bounds.maxX, bounds.maxY + 0.002, bounds.minZ, bounds.minZ * 16, bounds.maxX * 16)
            r.addVertexWithUV(bounds.minX, bounds.maxY + 0.002, bounds.minZ, bounds.minZ * 16, bounds.minX * 16)
            r.addVertexWithUV(bounds.minX, bounds.maxY + 0.002, bounds.maxZ, bounds.maxZ * 16, bounds.minX * 16)
          case EnumFacing.DOWN =>
            r.addVertexWithUV(bounds.maxX, bounds.minY - 0.002, bounds.minZ, bounds.minZ * 16, bounds.maxX * 16)
            r.addVertexWithUV(bounds.maxX, bounds.minY - 0.002, bounds.maxZ, bounds.maxZ * 16, bounds.maxX * 16)
            r.addVertexWithUV(bounds.minX, bounds.minY - 0.002, bounds.maxZ, bounds.maxZ * 16, bounds.minX * 16)
            r.addVertexWithUV(bounds.minX, bounds.minY - 0.002, bounds.minZ, bounds.minZ * 16, bounds.minX * 16)
          case EnumFacing.EAST =>
            r.addVertexWithUV(bounds.maxX + 0.002, bounds.maxY, bounds.minZ, bounds.minZ * 16, bounds.maxY * 16)
            r.addVertexWithUV(bounds.maxX + 0.002, bounds.maxY, bounds.maxZ, bounds.maxZ * 16, bounds.maxY * 16)
            r.addVertexWithUV(bounds.maxX + 0.002, bounds.minY, bounds.maxZ, bounds.maxZ * 16, bounds.minY * 16)
            r.addVertexWithUV(bounds.maxX + 0.002, bounds.minY, bounds.minZ, bounds.minZ * 16, bounds.minY * 16)
          case EnumFacing.WEST =>
            r.addVertexWithUV(bounds.minX - 0.002, bounds.maxY, bounds.maxZ, bounds.maxZ * 16, bounds.maxY * 16)
            r.addVertexWithUV(bounds.minX - 0.002, bounds.maxY, bounds.minZ, bounds.minZ * 16, bounds.maxY * 16)
            r.addVertexWithUV(bounds.minX - 0.002, bounds.minY, bounds.minZ, bounds.minZ * 16, bounds.minY * 16)
            r.addVertexWithUV(bounds.minX - 0.002, bounds.minY, bounds.maxZ, bounds.maxZ * 16, bounds.minY * 16)
          case EnumFacing.SOUTH =>
            r.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.maxZ + 0.002, bounds.maxX * 16, bounds.maxY * 16)
            r.addVertexWithUV(bounds.minX, bounds.maxY, bounds.maxZ + 0.002, bounds.minX * 16, bounds.maxY * 16)
            r.addVertexWithUV(bounds.minX, bounds.minY, bounds.maxZ + 0.002, bounds.minX * 16, bounds.minY * 16)
            r.addVertexWithUV(bounds.maxX, bounds.minY, bounds.maxZ + 0.002, bounds.maxX * 16, bounds.minY * 16)
          case _ =>
            r.addVertexWithUV(bounds.minX, bounds.maxY, bounds.minZ - 0.002, bounds.minX * 16, bounds.maxY * 16)
            r.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.minZ - 0.002, bounds.maxX * 16, bounds.maxY * 16)
            r.addVertexWithUV(bounds.maxX, bounds.minY, bounds.minZ - 0.002, bounds.maxX * 16, bounds.minY * 16)
            r.addVertexWithUV(bounds.minX, bounds.minY, bounds.minZ - 0.002, bounds.minX * 16, bounds.minY * 16)
        }
        t.draw()

        GlStateManager.popAttrib()
        GlStateManager.popMatrix()
      }
    }
  }
}
