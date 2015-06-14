package li.cil.oc.client.renderer

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.common.util.ForgeDirection
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
      val blockPos = BlockPosition(hitInfo.blockX, hitInfo.blockY, hitInfo.blockZ, world)
      val isAir = world.isAirBlock(blockPos)
      if (!isAir) {
        val block = world.getBlock(blockPos)
        block.setBlockBoundsBasedOnState(blockPos)
        val bounds = block.getSelectedBoundingBoxFromPool(blockPos).getOffsetBoundingBox(-blockPos.x, -blockPos.y, -blockPos.z)
        val sideHit = ForgeDirection.getOrientation(hitInfo.sideHit)
        val playerPos = e.player.getPosition(e.partialTicks)
        val renderPos = blockPos.offset(-playerPos.xCoord, -playerPos.yCoord, -playerPos.zCoord)

        GL11.glPushMatrix()
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        RenderState.makeItBlend()
        Minecraft.getMinecraft.renderEngine.bindTexture(Textures.blockHologram)

        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0)
        GL11.glColor4f(0.0F, 1.0F, 0.0F, 0.4F)

        GL11.glTranslated(renderPos.xCoord, renderPos.yCoord, renderPos.zCoord)
        GL11.glScaled(1.002, 1.002, 1.002)

        if (Settings.get.hologramFlickerFrequency > 0 && random.nextDouble() < Settings.get.hologramFlickerFrequency) {
          val (sx, sy, sz) = (1 - math.abs(sideHit.offsetX), 1 - math.abs(sideHit.offsetY), 1 - math.abs(sideHit.offsetZ))
          GL11.glScaled(1 + random.nextGaussian() * 0.01, 1 + random.nextGaussian() * 0.001, 1 + random.nextGaussian() * 0.01)
          GL11.glTranslated(random.nextGaussian() * 0.01 * sx, random.nextGaussian() * 0.01 * sy, random.nextGaussian() * 0.01 * sz)
        }

        val t = Tessellator.instance
        t.startDrawingQuads()
        sideHit match {
          case ForgeDirection.UP =>
            t.addVertexWithUV(bounds.maxX, bounds.maxY + 0.002, bounds.maxZ, bounds.maxZ * 16, bounds.maxX * 16)
            t.addVertexWithUV(bounds.maxX, bounds.maxY + 0.002, bounds.minZ, bounds.minZ * 16, bounds.maxX * 16)
            t.addVertexWithUV(bounds.minX, bounds.maxY + 0.002, bounds.minZ, bounds.minZ * 16, bounds.minX * 16)
            t.addVertexWithUV(bounds.minX, bounds.maxY + 0.002, bounds.maxZ, bounds.maxZ * 16, bounds.minX * 16)
          case ForgeDirection.DOWN =>
            t.addVertexWithUV(bounds.maxX, bounds.minY - 0.002, bounds.minZ, bounds.minZ * 16, bounds.maxX * 16)
            t.addVertexWithUV(bounds.maxX, bounds.minY - 0.002, bounds.maxZ, bounds.maxZ * 16, bounds.maxX * 16)
            t.addVertexWithUV(bounds.minX, bounds.minY - 0.002, bounds.maxZ, bounds.maxZ * 16, bounds.minX * 16)
            t.addVertexWithUV(bounds.minX, bounds.minY - 0.002, bounds.minZ, bounds.minZ * 16, bounds.minX * 16)
          case ForgeDirection.EAST =>
            t.addVertexWithUV(bounds.maxX + 0.002, bounds.maxY, bounds.minZ, bounds.minZ * 16, bounds.maxY * 16)
            t.addVertexWithUV(bounds.maxX + 0.002, bounds.maxY, bounds.maxZ, bounds.maxZ * 16, bounds.maxY * 16)
            t.addVertexWithUV(bounds.maxX + 0.002, bounds.minY, bounds.maxZ, bounds.maxZ * 16, bounds.minY * 16)
            t.addVertexWithUV(bounds.maxX + 0.002, bounds.minY, bounds.minZ, bounds.minZ * 16, bounds.minY * 16)
          case ForgeDirection.WEST =>
            t.addVertexWithUV(bounds.minX - 0.002, bounds.maxY, bounds.maxZ, bounds.maxZ * 16, bounds.maxY * 16)
            t.addVertexWithUV(bounds.minX - 0.002, bounds.maxY, bounds.minZ, bounds.minZ * 16, bounds.maxY * 16)
            t.addVertexWithUV(bounds.minX - 0.002, bounds.minY, bounds.minZ, bounds.minZ * 16, bounds.minY * 16)
            t.addVertexWithUV(bounds.minX - 0.002, bounds.minY, bounds.maxZ, bounds.maxZ * 16, bounds.minY * 16)
          case ForgeDirection.SOUTH =>
            t.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.maxZ + 0.002, bounds.maxX * 16, bounds.maxY * 16)
            t.addVertexWithUV(bounds.minX, bounds.maxY, bounds.maxZ + 0.002, bounds.minX * 16, bounds.maxY * 16)
            t.addVertexWithUV(bounds.minX, bounds.minY, bounds.maxZ + 0.002, bounds.minX * 16, bounds.minY * 16)
            t.addVertexWithUV(bounds.maxX, bounds.minY, bounds.maxZ + 0.002, bounds.maxX * 16, bounds.minY * 16)
          case _ =>
            t.addVertexWithUV(bounds.minX, bounds.maxY, bounds.minZ - 0.002, bounds.minX * 16, bounds.maxY * 16)
            t.addVertexWithUV(bounds.maxX, bounds.maxY, bounds.minZ - 0.002, bounds.maxX * 16, bounds.maxY * 16)
            t.addVertexWithUV(bounds.maxX, bounds.minY, bounds.minZ - 0.002, bounds.maxX * 16, bounds.minY * 16)
            t.addVertexWithUV(bounds.minX, bounds.minY, bounds.minZ - 0.002, bounds.minX * 16, bounds.minY * 16)
        }
        t.draw()

        GL11.glPopAttrib()
        GL11.glPopMatrix()
      }
    }

    if (hitInfo.typeOfHit == MovingObjectType.BLOCK) e.player.getEntityWorld.getTileEntity(hitInfo.blockX, hitInfo.blockY, hitInfo.blockZ) match {
      case print: common.tileentity.Print =>
        val pos = e.player.getPosition(e.partialTicks)
        val expansion = 0.002f

        // See RenderGlobal.drawSelectionBox.
        GL11.glEnable(GL11.GL_BLEND)
        OpenGlHelper.glBlendFunc(770, 771, 1, 0)
        GL11.glColor4f(0, 0, 0, 0.4f)
        GL11.glLineWidth(2)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDepthMask(false)

        for (shape <- if (print.state) print.data.stateOn else print.data.stateOff) {
          val bounds = shape.bounds.rotateTowards(print.facing)
          RenderGlobal.drawOutlinedBoundingBox(bounds.copy().expand(expansion, expansion, expansion)
            .offset(e.target.blockX, e.target.blockY, e.target.blockZ)
            .offset(-pos.xCoord, -pos.yCoord, -pos.zCoord), -1)
        }

        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)

        e.setCanceled(true)
      case _ =>
    }
  }
}
