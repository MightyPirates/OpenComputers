package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object PowerDistributorRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val distributor = tileEntity.asInstanceOf[tileentity.PowerDistributor]
    distributor.globalBuffer = 0.5
    if (distributor.globalBuffer > 0) {
      RenderState.pushAttrib()

      RenderState.disableEntityLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha((distributor.globalBuffer / distributor.globalBufferSize).toFloat)

      RenderState.pushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
      GL11.glScaled(1.0025, -1.0025, 1.0025)
      GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

      val t = Tessellator.getInstance
      val r = t.getWorldRenderer

      Textures.Block.bind()
      r.startDrawingQuads()

      {
        val icon = Textures.getSprite(Textures.Block.PowerDistributorTopOn)
        r.addVertexWithUV(0, 0, 1, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 1, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)
      }

      {
        val icon = Textures.getSprite(Textures.Block.PowerDistributorSideOn)
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

      RenderState.enableEntityLighting()

      RenderState.popMatrix()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

}
