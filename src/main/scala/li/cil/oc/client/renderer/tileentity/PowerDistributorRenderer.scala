package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object PowerDistributorRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) = {
    val distributor = tileEntity.asInstanceOf[tileentity.PowerDistributor]
    if (distributor.globalBuffer > 0) {
      GL11.glPushAttrib(0xFFFFFF)

      RenderState.disableLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha((distributor.globalBuffer / distributor.globalBufferSize).toFloat)

      GL11.glPushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
      GL11.glScalef(1.002f, -1.002f, 1.002f)
      GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

      bindTexture(Textures.blockPowerDistributorOn)

      val t = Tessellator.instance
      t.startDrawingQuads()

      t.addVertexWithUV(1, 1, 0, 0, 1)
      t.addVertexWithUV(0, 1, 0, 1, 1)
      t.addVertexWithUV(0, 0, 0, 1, 0)
      t.addVertexWithUV(1, 0, 0, 0, 0)

      t.addVertexWithUV(0, 1, 1, 0, 1)
      t.addVertexWithUV(1, 1, 1, 1, 1)
      t.addVertexWithUV(1, 0, 1, 1, 0)
      t.addVertexWithUV(0, 0, 1, 0, 0)

      t.addVertexWithUV(1, 1, 1, 0, 1)
      t.addVertexWithUV(1, 1, 0, 1, 1)
      t.addVertexWithUV(1, 0, 0, 1, 0)
      t.addVertexWithUV(1, 0, 1, 0, 0)

      t.addVertexWithUV(0, 1, 0, 0, 1)
      t.addVertexWithUV(0, 1, 1, 1, 1)
      t.addVertexWithUV(0, 0, 1, 1, 0)
      t.addVertexWithUV(0, 0, 0, 0, 0)

      t.draw()

      GL11.glPopMatrix()
      GL11.glPopAttrib()
    }
  }

}
