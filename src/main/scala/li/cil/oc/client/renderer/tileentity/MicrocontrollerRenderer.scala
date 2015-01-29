package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Microcontroller
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object MicrocontrollerRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val mcu = tileEntity.asInstanceOf[Microcontroller]
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    RenderState.disableLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)
    GL11.glColor4f(1, 1, 1, 1)

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    mcu.yaw match {
      case EnumFacing.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case EnumFacing.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case EnumFacing.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GL11.glTranslated(-0.5, 0.5, 0.505)
    GL11.glScalef(1, -1, 1)

    val t = Tessellator.getInstance
    val r = t.getWorldRenderer

    Textures.Block.bind()
    r.startDrawingQuads()

    {
      val icon = Textures.Block.getSprite(Textures.Block.MicrocontrollerFrontLight)
      r.addVertexWithUV(0, 1, 0, icon.getMinU, icon.getMaxV)
      r.addVertexWithUV(1, 1, 0, icon.getMaxU, icon.getMaxV)
      r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
      r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)
    }

    if (mcu.isRunning) {
      val icon = Textures.Block.getSprite(Textures.Block.MicrocontrollerFrontOn)
      r.addVertexWithUV(0, 1, 0, icon.getMinU, icon.getMaxV)
      r.addVertexWithUV(1, 1, 0, icon.getMaxU, icon.getMaxV)
      r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
      r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)
    }

    t.draw()

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}