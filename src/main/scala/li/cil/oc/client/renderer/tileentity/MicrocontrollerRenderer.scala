package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Microcontroller
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.VertexBuffer
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

object MicrocontrollerRenderer extends TileEntitySpecialRenderer[Microcontroller] {
  override def renderTileEntityAt(mcu: Microcontroller, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    //GlStateManager.pushAttrib()

    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)
    RenderState.color(1, 1, 1, 1)

    GlStateManager.pushMatrix()

    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    mcu.yaw match {
      case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
      case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
      case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GlStateManager.translate(-0.5, 0.5, 0.505)
    GlStateManager.scale(1, -1, 1)

    val t = Tessellator.getInstance
    val r = t.getBuffer

    Textures.Block.bind()
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

    renderFrontOverlay(Textures.Block.MicrocontrollerFrontLight, r)

    if (mcu.isRunning) {
      renderFrontOverlay(Textures.Block.MicrocontrollerFrontOn, r)
    }
    else if (mcu.hasErrored && RenderUtil.shouldShowErrorLight(mcu.hashCode)) {
      renderFrontOverlay(Textures.Block.MicrocontrollerFrontError, r)
    }

    t.draw()

    RenderState.enableEntityLighting()

    GlStateManager.popMatrix()
    //GlStateManager.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

  private def renderFrontOverlay(texture: ResourceLocation, r: VertexBuffer): Unit = {
    val icon = Textures.getSprite(texture)
    r.pos(0, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
    r.pos(1, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
    r.pos(1, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
    r.pos(0, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()
  }
}