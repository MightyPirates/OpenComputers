package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Raid
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.VertexBuffer
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object RaidRenderer extends TileEntitySpecialRenderer[Raid] {
  override def renderTileEntityAt(raid: Raid, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    //GlStateManager.pushAttrib()

    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderState.color(1, 1, 1, 1)

    GlStateManager.pushMatrix()

    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    raid.yaw match {
      case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
      case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
      case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GlStateManager.translate(-0.5, 0.5, 0.505)
    GlStateManager.scale(1, -1, 1)

    val t = Tessellator.getInstance
    val r = t.getBuffer

    Textures.Block.bind()
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

    {
      val icon = Textures.getSprite(Textures.Block.RaidFrontError)
      for (slot <- 0 until raid.getSizeInventory) {
        if (!raid.presence(slot)) {
          renderSlot(r, slot, icon)
        }
      }
    }

    {
      val icon = Textures.getSprite(Textures.Block.RaidFrontActivity)
      for (slot <- 0 until raid.getSizeInventory) {
        if (System.currentTimeMillis() - raid.lastAccess < 400 && raid.world.rand.nextDouble() > 0.1 && slot == raid.lastAccess % raid.getSizeInventory) {
          renderSlot(r, slot, icon)
        }
      }
    }

    t.draw()

    RenderState.enableEntityLighting()

    GlStateManager.popMatrix()
    //GlStateManager.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

  private val u1 = 2 / 16f
  private val fs = 4 / 16f

  private def renderSlot(r: VertexBuffer, slot: Int, icon: TextureAtlasSprite) {
    val l = u1 + slot * fs
    val h = u1 + (slot + 1) * fs
    r.pos(l, 1, 0).tex(icon.getInterpolatedU(l * 16), icon.getMaxV).endVertex()
    r.pos(h, 1, 0).tex(icon.getInterpolatedU(h * 16), icon.getMaxV).endVertex()
    r.pos(h, 0, 0).tex(icon.getInterpolatedU(h * 16), icon.getMinV).endVertex()
    r.pos(l, 0, 0).tex(icon.getInterpolatedU(l * 16), icon.getMinV).endVertex()
  }
}
