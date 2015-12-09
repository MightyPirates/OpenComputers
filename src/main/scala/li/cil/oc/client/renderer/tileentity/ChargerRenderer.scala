package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Charger
import li.cil.oc.util.RenderState
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
      RenderState.color(1, 1, 1, 1)

      RenderState.pushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

      charger.yaw match {
        case EnumFacing.WEST => GL11.glRotatef(-90, 0, 1, 0)
        case EnumFacing.NORTH => GL11.glRotatef(180, 0, 1, 0)
        case EnumFacing.EAST => GL11.glRotatef(90, 0, 1, 0)
        case _ => // No yaw.
      }

      GL11.glTranslatef(-0.5f, 0.5f, 0.5f)
      GL11.glScalef(1, -1, 1)

      val t = Tessellator.getInstance
      val r = t.getWorldRenderer

      Textures.Block.bind()
      r.begin(7, DefaultVertexFormats.POSITION_TEX)

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

      RenderState.enableEntityLighting()

      RenderState.popMatrix()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
