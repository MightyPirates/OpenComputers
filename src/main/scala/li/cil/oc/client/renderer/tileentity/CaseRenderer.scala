package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Case
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

object CaseRenderer extends TileEntitySpecialRenderer[Case] {
  override def render(computer: Case, x: Double, y: Double, z: Double, f: Float, damage: Int, alpha: Float) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderState.pushAttrib()

    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)

    GlStateManager.pushMatrix()

    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    computer.yaw match {
      case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
      case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
      case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GlStateManager.translate(-0.5, 0.5, 0.505)
    GlStateManager.scale(1, -1, 1)

    if (computer.isRunning) {
      renderFrontOverlay(Textures.Block.CaseFrontOn)
      if (System.currentTimeMillis() - computer.lastFileSystemAccess < 400 && computer.world.rand.nextDouble() > 0.1) {
        renderFrontOverlay(Textures.Block.CaseFrontActivity)
      }
    }
    else if (computer.hasErrored && RenderUtil.shouldShowErrorLight(computer.hashCode)) {
      renderFrontOverlay(Textures.Block.CaseFrontError)
    }

    RenderState.disableBlend()
    RenderState.enableEntityLighting()

    GlStateManager.popMatrix()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def renderFrontOverlay(texture: ResourceLocation): Unit = {
    val t = Tessellator.getInstance
    val r = t.getBuffer

    Textures.Block.bind()
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

    val icon = Textures.getSprite(texture)
    r.pos(0, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
    r.pos(1, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
    r.pos(1, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
    r.pos(0, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()

    t.draw()
  }
}
