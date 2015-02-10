package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Case
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object CaseRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val computer = tileEntity.asInstanceOf[Case]
    GlStateManager.pushAttrib()

    RenderState.disableLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)
    GL11.glColor4f(1, 1, 1, 1)

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    computer.yaw match {
      case EnumFacing.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case EnumFacing.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case EnumFacing.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GL11.glTranslated(-0.5, 0.5, 0.505)
    GL11.glScalef(1, -1, 1)

    if (computer.isRunning) {
      val t = Tessellator.getInstance
      val r = t.getWorldRenderer

      Textures.Block.bind()
      r.startDrawingQuads()

      {
        val icon = Textures.Block.getSprite(Textures.Block.CaseFrontOn)
        r.addVertexWithUV(0, 1, 0, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1, 1, 0, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)
      }

      if (System.currentTimeMillis() - computer.lastAccess < 400 && computer.world.rand.nextDouble() > 0.1) {
        val icon = Textures.Block.getSprite(Textures.Block.CaseFrontActivity)
        r.addVertexWithUV(0, 1, 0, icon.getMinU, icon.getMaxV)
        r.addVertexWithUV(1, 1, 0, icon.getMaxU, icon.getMaxV)
        r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
        r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)
      }

      t.draw()
    }

    RenderState.enableLighting()

    GL11.glPopMatrix()
    GlStateManager.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}