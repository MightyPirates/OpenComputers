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
      RenderState.pushAttrib()

      RenderState.disableEntityLighting()
      RenderState.makeItBlend()
      RenderState.color(1, 1, 1, 1)

      RenderState.pushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
      GL11.glScaled(1.0025, -1.0025, 1.0025)
      GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

      val t = Tessellator.getInstance
      val r = t.getWorldRenderer
      Textures.Block.bind()
      r.startDrawingQuads()

      {
        val icon = Textures.getSprite(Textures.Block.DisassemblerTopOn)
        r.addVertexWithUV(0, 0, 1, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 1, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)
      }

      {
        val icon = Textures.getSprite(Textures.Block.DisassemblerSideOn)
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
