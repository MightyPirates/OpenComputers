package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object NetSplitterRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val splitter = tileEntity.asInstanceOf[tileentity.NetSplitter]
    if (splitter.openSides.contains(!splitter.isInverted)) {
      RenderState.pushAttrib()
      RenderState.disableEntityLighting()
      RenderState.makeItBlend()

      RenderState.pushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
      GL11.glScaled(1.0025, -1.0025, 1.0025)
      GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

      bindTexture(TextureMap.locationBlocksTexture)

      val t = Tessellator.getInstance
      val r = t.getWorldRenderer

      Textures.Block.bind()
      r.startDrawingQuads()

      val sideActivity = Textures.getSprite(Textures.Block.NetSplitterOn)

      if (splitter.isSideOpen(EnumFacing.DOWN)) {
        r.addVertexWithUV(0, 1, 0, sideActivity.getMaxU, sideActivity.getMinV)
        r.addVertexWithUV(1, 1, 0, sideActivity.getMinU, sideActivity.getMinV)
        r.addVertexWithUV(1, 1, 1, sideActivity.getMinU, sideActivity.getMaxV)
        r.addVertexWithUV(0, 1, 1, sideActivity.getMaxU, sideActivity.getMaxV)
      }

      if (splitter.isSideOpen(EnumFacing.UP)) {
        r.addVertexWithUV(0, 0, 0, sideActivity.getMaxU, sideActivity.getMaxV)
        r.addVertexWithUV(0, 0, 1, sideActivity.getMaxU, sideActivity.getMinV)
        r.addVertexWithUV(1, 0, 1, sideActivity.getMinU, sideActivity.getMinV)
        r.addVertexWithUV(1, 0, 0, sideActivity.getMinU, sideActivity.getMaxV)
      }

      if (splitter.isSideOpen(EnumFacing.NORTH)) {
        r.addVertexWithUV(1, 1, 0, sideActivity.getMinU, sideActivity.getMaxV)
        r.addVertexWithUV(0, 1, 0, sideActivity.getMaxU, sideActivity.getMaxV)
        r.addVertexWithUV(0, 0, 0, sideActivity.getMaxU, sideActivity.getMinV)
        r.addVertexWithUV(1, 0, 0, sideActivity.getMinU, sideActivity.getMinV)
      }

      if (splitter.isSideOpen(EnumFacing.SOUTH)) {
        r.addVertexWithUV(0, 1, 1, sideActivity.getMinU, sideActivity.getMaxV)
        r.addVertexWithUV(1, 1, 1, sideActivity.getMaxU, sideActivity.getMaxV)
        r.addVertexWithUV(1, 0, 1, sideActivity.getMaxU, sideActivity.getMinV)
        r.addVertexWithUV(0, 0, 1, sideActivity.getMinU, sideActivity.getMinV)
      }

      if (splitter.isSideOpen(EnumFacing.WEST)) {
        r.addVertexWithUV(0, 1, 0, sideActivity.getMinU, sideActivity.getMaxV)
        r.addVertexWithUV(0, 1, 1, sideActivity.getMaxU, sideActivity.getMaxV)
        r.addVertexWithUV(0, 0, 1, sideActivity.getMaxU, sideActivity.getMinV)
        r.addVertexWithUV(0, 0, 0, sideActivity.getMinU, sideActivity.getMinV)
      }

      if (splitter.isSideOpen(EnumFacing.EAST)) {
        r.addVertexWithUV(1, 1, 1, sideActivity.getMinU, sideActivity.getMaxV)
        r.addVertexWithUV(1, 1, 0, sideActivity.getMaxU, sideActivity.getMaxV)
        r.addVertexWithUV(1, 0, 0, sideActivity.getMaxU, sideActivity.getMinV)
        r.addVertexWithUV(1, 0, 1, sideActivity.getMinU, sideActivity.getMinV)
      }

      t.draw()

      RenderState.enableEntityLighting()

      RenderState.popMatrix()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
