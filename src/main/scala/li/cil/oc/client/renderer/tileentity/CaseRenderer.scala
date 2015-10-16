package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Case
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

object CaseRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val computer = tileEntity.asInstanceOf[Case]
    RenderState.pushAttrib()

    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)

    RenderState.pushMatrix()

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
      renderFrontOverlay(Textures.Block.CaseFrontOn)
      if (System.currentTimeMillis() - computer.lastAccess < 400 && computer.world.rand.nextDouble() > 0.1) {
        renderFrontOverlay(Textures.Block.CaseFrontActivity)
      }
    }
    else if (computer.hasErrored && RenderUtil.shouldShowErrorLight(computer.hashCode)) {
      renderFrontOverlay(Textures.Block.CaseFrontError)
    }

    RenderState.enableEntityLighting()

    RenderState.popMatrix()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

  private def renderFrontOverlay(texture: ResourceLocation): Unit = {
    val t = Tessellator.getInstance
    val r = t.getWorldRenderer

    Textures.Block.bind()
    r.startDrawingQuads()

    val icon = Textures.getSprite(texture)
    r.addVertexWithUV(0, 1, 0, icon.getMinU, icon.getMaxV)
    r.addVertexWithUV(1, 1, 0, icon.getMaxU, icon.getMaxV)
    r.addVertexWithUV(1, 0, 0, icon.getMaxU, icon.getMinV)
    r.addVertexWithUV(0, 0, 0, icon.getMinU, icon.getMinV)

    t.draw()
  }
}