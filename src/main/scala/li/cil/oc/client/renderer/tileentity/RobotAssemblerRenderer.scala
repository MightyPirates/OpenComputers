package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.RobotAssembler
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object RobotAssemblerRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val assembler = tileEntity.asInstanceOf[RobotAssembler]

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    RenderState.disableLighting()
    RenderState.makeItBlend()
    bindTexture(TextureMap.locationBlocksTexture)

    GL11.glPushMatrix()
    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    val t = Tessellator.instance
    t.startDrawingQuads()

    {
      val icon = Textures.RobotAssembler.iconTopOn
      t.addVertexWithUV(-0.5, 0.55, 0.5, icon.getMinU, icon.getMaxV)
      t.addVertexWithUV(0.5, 0.55, 0.5, icon.getMaxU, icon.getMaxV)
      t.addVertexWithUV(0.5, 0.55, -0.5, icon.getMaxU, icon.getMinV)
      t.addVertexWithUV(-0.5, 0.55, -0.5, icon.getMinU, icon.getMinV)
    }

    t.draw()

    val indent = 6 / 16f + 0.005
    for (i <- 0 until 4) {
      t.startDrawingQuads()

      if (assembler.isAssembling) {
        val icon = Textures.RobotAssembler.iconSideAssembling
        t.addVertexWithUV(indent, 0.5, -indent, icon.getInterpolatedU((0.5 - indent) * 16), icon.getMaxV)
        t.addVertexWithUV(indent, 0.5, indent, icon.getInterpolatedU((0.5 + indent) * 16), icon.getMaxV)
        t.addVertexWithUV(indent, -0.5, indent, icon.getInterpolatedU((0.5 + indent) * 16), icon.getMinV)
        t.addVertexWithUV(indent, -0.5, -indent, icon.getInterpolatedU((0.5 - indent) * 16), icon.getMinV)
      }

      {
        val icon = Textures.RobotAssembler.iconSideOn
        t.addVertexWithUV(0.5005, 0.5, -0.5, icon.getMinU, icon.getMaxV)
        t.addVertexWithUV(0.5005, 0.5, 0.5, icon.getMaxU, icon.getMaxV)
        t.addVertexWithUV(0.5005, -0.5, 0.5, icon.getMaxU, icon.getMinV)
        t.addVertexWithUV(0.5005, -0.5, -0.5, icon.getMinU, icon.getMinV)
      }

      t.draw()

      GL11.glRotatef(90, 0, 1, 0)
    }

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
