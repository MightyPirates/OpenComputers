package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object GeolyzerRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    GL11.glPushClientAttrib(GL11.GL_ALL_CLIENT_ATTRIB_BITS)

    RenderState.disableLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)
    GL11.glColor4f(1, 1, 1, 1)

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
    GL11.glScaled(1.0025, -1.0025, 1.0025)
    GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

    val t = Tessellator.getInstance
    val r = t.getWorldRenderer

    Textures.Block.bind()
    r.startDrawingQuads()

    val icon = Textures.Block.getSprite(Textures.Block.GeolyzerTopOn)
    r.addVertexWithUV(0, 0, 1, icon.getMinU, icon.getMaxV)
    r.addVertexWithUV(1, 0, 1, icon.getMaxU, icon.getMaxV)
    r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
    r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)

    t.draw()

    RenderState.enableLighting()

    GL11.glPopMatrix()
    GL11.glPopAttrib()
    GL11.glPopClientAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

}
