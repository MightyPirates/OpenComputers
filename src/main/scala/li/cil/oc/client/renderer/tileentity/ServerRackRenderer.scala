package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.ServerRack
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

object ServerRackRenderer extends TileEntitySpecialRenderer[ServerRack] {
  private final val v1 = 2 / 16f
  private final val fs = 3 / 16f

  override def renderTileEntityAt(rack: ServerRack, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    GlStateManager.pushAttrib()

    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    GlStateManager.color(1, 1, 1, 1)

    GlStateManager.pushMatrix()

    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    rack.yaw match {
      case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
      case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
      case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GlStateManager.translate(-0.5, 0.5, 0.505 - 0.5f / 16f)
    GlStateManager.scale(1, -1, 1)

    if (rack.anyRunning) {
      for (i <- 0 until rack.getSizeInventory if rack.isRunning(i)) {
        renderFrontOverlay(Textures.Block.RackFrontOn, i)
      }
      for (i <- 0 until rack.getSizeInventory if System.currentTimeMillis() - rack.lastAccess(i) < 400 && rack.world.rand.nextDouble() > 0.1) {
        renderFrontOverlay(Textures.Block.RackFrontActivity, i)
      }
    }
    if (rack.anyErrored) {
      for (i <- 0 until rack.getSizeInventory if rack.hasErrored(i) && RenderUtil.shouldShowErrorLight(rack.hashCode * (i + 1))) {
        renderFrontOverlay(Textures.Block.RackFrontError, i)
      }
    }

    RenderState.enableEntityLighting()

    GlStateManager.popMatrix()
    GlStateManager.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

  private def renderFrontOverlay(texture: ResourceLocation, i: Int): Unit = {
    val t = Tessellator.getInstance
    val r = t.getWorldRenderer

    Textures.Block.bind()
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

    val l = v1 + i * fs
    val h = v1 + (i + 1) * fs

    val icon = Textures.getSprite(texture)
    r.pos(0, h, 0).tex(icon.getMinU, icon.getInterpolatedV(h * 16)).endVertex()
    r.pos(1, h, 0).tex(icon.getMaxU, icon.getInterpolatedV(h * 16)).endVertex()
    r.pos(1, l, 0).tex(icon.getMaxU, icon.getInterpolatedV(l * 16)).endVertex()
    r.pos(0, l, 0).tex(icon.getMinU, icon.getInterpolatedV(l * 16)).endVertex()

    t.draw()
  }
}