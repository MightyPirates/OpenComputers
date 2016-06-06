package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Charger
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object ChargerRenderer extends TileEntitySpecialRenderer[Charger] {
  override def renderTileEntityAt(charger: Charger, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    if (charger.chargeSpeed > 0) {
      RenderState.pushAttrib()

      RenderState.disableEntityLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha(1)
      GlStateManager.color(1, 1, 1, 1)

      GlStateManager.pushMatrix()

      GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

      charger.yaw match {
        case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
        case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
        case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
        case _ => // No yaw.
      }

      GlStateManager.translate(-0.5f, 0.5f, 0.5f)
      GlStateManager.scale(1, -1, 1)

      val t = Tessellator.getInstance
      val r = t.getWorldRenderer

      Textures.Block.bind()
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

      {
        val inverse = 1 - charger.chargeSpeed
        val icon = Textures.getSprite(Textures.Block.ChargerFrontOn)
        r.pos(0, 1, 0.005).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(1, 1, 0.005).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(1, inverse, 0.005).tex(icon.getMaxU, icon.getInterpolatedV(inverse * 16)).endVertex()
        r.pos(0, inverse, 0.005).tex(icon.getMinU, icon.getInterpolatedV(inverse * 16)).endVertex()
      }

      if (charger.hasPower) {
        val icon = Textures.getSprite(Textures.Block.ChargerSideOn)

        r.pos(-0.005, 1, -1).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(-0.005, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(-0.005, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(-0.005, 0, -1).tex(icon.getMinU, icon.getMinV).endVertex()

        r.pos(1, 1, -1.005).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(0, 1, -1.005).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(0, 0, -1.005).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(1, 0, -1.005).tex(icon.getMinU, icon.getMinV).endVertex()

        r.pos(1.005, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(1.005, 1, -1).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(1.005, 0, -1).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(1.005, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()
      }

      t.draw()

      RenderState.disableBlend()
      RenderState.enableEntityLighting()

      GlStateManager.popMatrix()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
