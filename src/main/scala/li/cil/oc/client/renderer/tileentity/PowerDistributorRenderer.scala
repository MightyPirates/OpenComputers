package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object PowerDistributorRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val distributor = tileEntity.asInstanceOf[tileentity.PowerDistributor]
    if (distributor.globalBuffer > 0) {
      GlStateManager.pushAttrib()

      RenderState.disableLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha((distributor.globalBuffer / distributor.globalBufferSize).toFloat)
      GL11.glColor4f(1, 1, 1, 1)

      GL11.glPushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
      GL11.glScaled(1.0025, -1.0025, 1.0025)
      GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

      val t = Tessellator.getInstance
      val r = t.getWorldRenderer

      Textures.Block.bind()
      r.startDrawingQuads()

      {
        val icon = Textures.Block.getSprite(Textures.Block.PowerDistributorTopOn)
        r.addVertexWithUV(0, 0, 1, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 1, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)
      }

      {
        val icon = Textures.Block.getSprite(Textures.Block.PowerDistributorSideOn)
        r.startDrawingQuads()
        r.addVertexWithUV(1, 1, 0, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(0, 1, 0, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(0, 0, 0, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(1, 0, 0, icon.getMinU, icon.getMinV)

        r.addVertexWithUV(0, 1, 1, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1, 1, 1, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 1, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(0, 0, 1, icon.getMinU, icon.getMinV)

        r.addVertexWithUV(1, 1, 1, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1, 1, 0, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(1, 0, 1, icon.getMinU, icon.getMinV)

        r.addVertexWithUV(0, 1, 0, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(0, 1, 1, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(0, 0, 1, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)
      }

      t.draw()

      RenderState.enableLighting()

      GL11.glPopMatrix()
      GlStateManager.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

}
