package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Charger
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object ChargerRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val charger = tileEntity.asInstanceOf[Charger]
    if (charger.chargeSpeed > 0) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

      RenderState.disableLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha(1)
      GL11.glColor4f(1, 1, 1, 1)

      GL11.glPushMatrix()

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
      r.startDrawingQuads()

      {
        val inverse = 1 - charger.chargeSpeed
        val icon = Textures.Block.getSprite(Textures.Block.ChargerFrontOn)
        r.addVertexWithUV(0, 1, 0.005, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1, 1, 0.005, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1, inverse, 0.005, icon.getMaxU, icon.getInterpolatedV(inverse * 16))
        r.addVertexWithUV(0, inverse, 0.005, icon.getMinU, icon.getInterpolatedV(inverse * 16))
      }

      if (charger.hasPower) {
        val icon = Textures.Block.getSprite(Textures.Block.ChargerSideOn)

        r.addVertexWithUV(-0.005, 1, -1, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(-0.005, 1, 0, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(-0.005, 0, 0, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(-0.005, 0, -1, icon.getMinU, icon.getMinV)

        r.addVertexWithUV(1, 1, -1.005, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(0, 1, -1.005, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(0, 0, -1.005, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(1, 0, -1.005, icon.getMinU, icon.getMinV)

        r.addVertexWithUV(1.005, 1, 0, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1.005, 1, -1, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1.005, 0, -1, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(1.005, 0, 0, icon.getMinU, icon.getMinV)
      }

      t.draw()
      Textures.Block.unbind()

      GL11.glPopMatrix()
      GL11.glPopAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
