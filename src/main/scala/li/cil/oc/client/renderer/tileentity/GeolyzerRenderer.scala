package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object GeolyzerRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    RenderState.disableLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
    GL11.glScalef(1.002f, -1.002f, 1.002f)
    GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

    bindTexture(TextureMap.locationBlocksTexture)
    val t = Tessellator.instance
    t.startDrawingQuads()

    val topOn = Textures.Geolyzer.iconTopOn
    t.addVertexWithUV(0, 0, 1, topOn.getMinU, topOn.getMaxV)
    t.addVertexWithUV(1, 0, 1, topOn.getMaxU, topOn.getMaxV)
    t.addVertexWithUV(1, 0, 0, topOn.getMaxU, topOn.getMinV)
    t.addVertexWithUV(0, 0, 0, topOn.getMinU, topOn.getMinV)

    t.draw()

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

}
