package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Assembler
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11

object AssemblerRenderer extends TileEntitySpecialRenderer[Assembler] {

  override def render(assembler: Assembler, x: Double, y: Double, z: Double, f: Float, damage: Int, alpha: Float) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderState.pushAttrib()

    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)

    GlStateManager.pushMatrix()
    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    val t = Tessellator.getInstance
    val r = t.getBuffer

    Textures.Block.bind()
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

    {
      val icon = Textures.getSprite(Textures.Block.AssemblerTopOn)
      r.pos(-0.5, 0.55, 0.5).tex(icon.getMinU, icon.getMaxV).endVertex()
      r.pos(0.5, 0.55, 0.5).tex(icon.getMaxU, icon.getMaxV).endVertex()
      r.pos(0.5, 0.55, -0.5).tex(icon.getMaxU, icon.getMinV).endVertex()
      r.pos(-0.5, 0.55, -0.5).tex(icon.getMinU, icon.getMinV).endVertex()
    }

    t.draw()

    // TODO Unroll loop to draw all at once?
    val indent = 6 / 16f + 0.005
    for (i <- 0 until 4) {
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

      if (assembler.isAssembling) {
        val icon = Textures.getSprite(Textures.Block.AssemblerSideAssembling)
        r.pos(indent, 0.5, -indent).tex(icon.getInterpolatedU((0.5 - indent) * 16), icon.getMaxV).endVertex()
        r.pos(indent, 0.5, indent).tex(icon.getInterpolatedU((0.5 + indent) * 16), icon.getMaxV).endVertex()
        r.pos(indent, -0.5, indent).tex(icon.getInterpolatedU((0.5 + indent) * 16), icon.getMinV).endVertex()
        r.pos(indent, -0.5, -indent).tex(icon.getInterpolatedU((0.5 - indent) * 16), icon.getMinV).endVertex()
      }

      {
        val icon = Textures.getSprite(Textures.Block.AssemblerSideOn)
        r.pos(0.5005, 0.5, -0.5).tex(icon.getMinU, icon.getMaxV).endVertex()
        r.pos(0.5005, 0.5, 0.5).tex(icon.getMaxU, icon.getMaxV).endVertex()
        r.pos(0.5005, -0.5, 0.5).tex(icon.getMaxU, icon.getMinV).endVertex()
        r.pos(0.5005, -0.5, -0.5).tex(icon.getMinU, icon.getMinV).endVertex()
      }

      t.draw()

      GlStateManager.rotate(90, 0, 1, 0)
    }

    RenderState.disableBlend()
    RenderState.enableEntityLighting()

    GlStateManager.popMatrix()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
