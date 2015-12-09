package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11

object DisassemblerRenderer extends TileEntitySpecialRenderer[tileentity.Disassembler] {
  override def renderTileEntityAt(disassembler: tileentity.Disassembler, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

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
      r.begin(7, DefaultVertexFormats.POSITION_TEX)

      {
        val icon = Textures.getSprite(Textures.Block.DisassemblerTopOn)
        r.pos(0, 0, 1).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(1, 0, 1).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(1, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(0, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()
      }

      {
        val icon = Textures.getSprite(Textures.Block.DisassemblerSideOn)
        r.pos(1, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(0, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(0, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(1, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()

        r.pos(0, 1, 1).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(1, 1, 1).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(1, 0, 1).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(0, 0, 1).tex(icon.getMinU, icon.getMinV).endVertex()

        r.pos(1, 1, 1).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(1, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(1, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(1, 0, 1).tex(icon.getMinU, icon.getMinV).endVertex()

        r.pos(0, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(0, 1, 1).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(0, 0, 1).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(0, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()
      }

      t.draw()

      RenderState.enableEntityLighting()

      RenderState.popMatrix()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

}
