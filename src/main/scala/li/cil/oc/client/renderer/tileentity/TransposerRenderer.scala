package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object TransposerRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val transposer = tileEntity.asInstanceOf[tileentity.Transposer]
    val activity = math.max(0, 1 - (System.currentTimeMillis() - transposer.lastOperation) / 1000.0)
    if (activity > 0) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

      RenderState.disableLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha(activity.toFloat)

      GL11.glPushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
      GL11.glScaled(1.0025, -1.0025, 1.0025)
      GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

      bindTexture(TextureMap.locationBlocksTexture)
      val t = Tessellator.instance
      t.startDrawingQuads()

      val icon = Textures.Transposer.iconOn

      t.addVertexWithUV(0, 1, 0, icon.getMaxU, icon.getMinV)
      t.addVertexWithUV(1, 1, 0, icon.getMinU, icon.getMinV)
      t.addVertexWithUV(1, 1, 1, icon.getMinU, icon.getMaxV)
      t.addVertexWithUV(0, 1, 1, icon.getMaxU, icon.getMaxV)

      t.addVertexWithUV(0, 0, 0, icon.getMaxU, icon.getMaxV)
      t.addVertexWithUV(0, 0, 1, icon.getMaxU, icon.getMinV)
      t.addVertexWithUV(1, 0, 1, icon.getMinU, icon.getMinV)
      t.addVertexWithUV(1, 0, 0, icon.getMinU, icon.getMaxV)

      t.addVertexWithUV(1, 1, 0, icon.getMinU, icon.getMaxV)
      t.addVertexWithUV(0, 1, 0, icon.getMaxU, icon.getMaxV)
      t.addVertexWithUV(0, 0, 0, icon.getMaxU, icon.getMinV)
      t.addVertexWithUV(1, 0, 0, icon.getMinU, icon.getMinV)

      t.addVertexWithUV(0, 1, 1, icon.getMinU, icon.getMaxV)
      t.addVertexWithUV(1, 1, 1, icon.getMaxU, icon.getMaxV)
      t.addVertexWithUV(1, 0, 1, icon.getMaxU, icon.getMinV)
      t.addVertexWithUV(0, 0, 1, icon.getMinU, icon.getMinV)

      t.addVertexWithUV(0, 1, 0, icon.getMinU, icon.getMaxV)
      t.addVertexWithUV(0, 1, 1, icon.getMaxU, icon.getMaxV)
      t.addVertexWithUV(0, 0, 1, icon.getMaxU, icon.getMinV)
      t.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)

      t.addVertexWithUV(1, 1, 1, icon.getMinU, icon.getMaxV)
      t.addVertexWithUV(1, 1, 0, icon.getMaxU, icon.getMaxV)
      t.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
      t.addVertexWithUV(1, 0, 1, icon.getMinU, icon.getMinV)

      t.draw()

      RenderState.enableLighting()

      GL11.glPopMatrix()
      GL11.glPopAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
