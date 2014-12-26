package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Assembler
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object AssemblerRenderer extends TileEntitySpecialRenderer {

  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val assembler = tileEntity.asInstanceOf[Assembler]

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    RenderState.disableLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)

    GL11.glPushMatrix()
    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    val t = Tessellator.getInstance
    val r = t.getWorldRenderer

    Textures.Block.bind()
    r.startDrawingQuads()

    {
      val icon = Textures.Block.getSprite(Textures.Block.AssemblerTopOn)
      r.addVertexWithUV(-0.5, 0.55, 0.5, icon.getMinU, icon.getMaxV)
      r.addVertexWithUV(0.5, 0.55, 0.5, icon.getMaxU, icon.getMaxV)
      r.addVertexWithUV(0.5, 0.55, -0.5, icon.getMaxU, icon.getMinV)
      r.addVertexWithUV(-0.5, 0.55, -0.5, icon.getMinU, icon.getMinV)
    }

    val indent = 6 / 16f + 0.005
    for (i <- 0 until 4) {

      if (assembler.isAssembling) {
        val icon = Textures.Block.getSprite(Textures.Block.AssemblerSideAssembling)
        r.addVertexWithUV(indent, 0.5, -indent, icon.getInterpolatedU((0.5 - indent) * 16), icon.getMaxV)
        r.addVertexWithUV(indent, 0.5, indent, icon.getInterpolatedU((0.5 + indent) * 16), icon.getMaxV)
        r.addVertexWithUV(indent, -0.5, indent, icon.getInterpolatedU((0.5 + indent) * 16), icon.getMinV)
        r.addVertexWithUV(indent, -0.5, -indent, icon.getInterpolatedU((0.5 - indent) * 16), icon.getMinV)
      }

      {
        val icon = Textures.Block.getSprite(Textures.Block.AssemblerSideOn)
        r.addVertexWithUV(0.5005, 0.5, -0.5, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(0.5005, 0.5, 0.5, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(0.5005, -0.5, 0.5, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(0.5005, -0.5, -0.5, icon.getMinU, icon.getMinV)
      }

      GL11.glRotatef(90, 0, 1, 0)
    }

    t.draw()
    Textures.Block.unbind()

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
