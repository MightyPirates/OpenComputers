package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object DisassemblerRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val disassembler = tileEntity.asInstanceOf[tileentity.Disassembler]
    if (disassembler.isActive) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

      RenderState.disableLighting()
      RenderState.makeItBlend()

      GL11.glPushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
      GL11.glScaled(1.0025, -1.0025, 1.0025)
      GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

      val t = Tessellator.getInstance
      val r = t.getWorldRenderer
      Textures.Block.bind()
      r.startDrawingQuads()

      {
        val topOn = Textures.Block.getSprite(Textures.Block.DisassemblerTopOn)
        r.addVertexWithUV(0, 0, 1, topOn.getMinU, topOn.getMaxV)
        r.addVertexWithUV(1, 0, 1, topOn.getMaxU, topOn.getMaxV)
        r.addVertexWithUV(1, 0, 0, topOn.getMaxU, topOn.getMinV)
        r.addVertexWithUV(0, 0, 0, topOn.getMinU, topOn.getMinV)
      }

      val sideOn = Textures.Block.getSprite(Textures.Block.DisassemblerSideOn)
      r.addVertexWithUV(1, 1, 0, sideOn.getMinU, sideOn.getMaxV)
      r.addVertexWithUV(0, 1, 0, sideOn.getMaxU, sideOn.getMaxV)
      r.addVertexWithUV(0, 0, 0, sideOn.getMaxU, sideOn.getMinV)
      r.addVertexWithUV(1, 0, 0, sideOn.getMinU, sideOn.getMinV)

      r.addVertexWithUV(0, 1, 1, sideOn.getMinU, sideOn.getMaxV)
      r.addVertexWithUV(1, 1, 1, sideOn.getMaxU, sideOn.getMaxV)
      r.addVertexWithUV(1, 0, 1, sideOn.getMaxU, sideOn.getMinV)
      r.addVertexWithUV(0, 0, 1, sideOn.getMinU, sideOn.getMinV)

      r.addVertexWithUV(1, 1, 1, sideOn.getMinU, sideOn.getMaxV)
      r.addVertexWithUV(1, 1, 0, sideOn.getMaxU, sideOn.getMaxV)
      r.addVertexWithUV(1, 0, 0, sideOn.getMaxU, sideOn.getMinV)
      r.addVertexWithUV(1, 0, 1, sideOn.getMinU, sideOn.getMinV)

      r.addVertexWithUV(0, 1, 0, sideOn.getMinU, sideOn.getMaxV)
      r.addVertexWithUV(0, 1, 1, sideOn.getMaxU, sideOn.getMaxV)
      r.addVertexWithUV(0, 0, 1, sideOn.getMaxU, sideOn.getMinV)
      r.addVertexWithUV(0, 0, 0, sideOn.getMinU, sideOn.getMinV)

      t.draw()
      Textures.Block.unbind()

      GL11.glPopMatrix()
      GL11.glPopAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

}
