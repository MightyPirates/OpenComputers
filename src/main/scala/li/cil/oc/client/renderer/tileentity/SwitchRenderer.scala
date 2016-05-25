package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11

class SwitchRenderer[T <: tileentity.traits.SwitchLike] extends TileEntitySpecialRenderer[T] {
  override def renderTileEntityAt(switch: T, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val activity = math.max(0, 1 - (System.currentTimeMillis() - switch.lastMessage) / 1000.0)
    if (activity > 0) {
      //GlStateManager.pushAttrib()

      RenderState.disableEntityLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha(activity.toFloat)

      GlStateManager.pushMatrix()

      GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)
      GlStateManager.scale(1.0025, -1.0025, 1.0025)
      GlStateManager.translate(-0.5f, -0.5f, -0.5f)

      val t = Tessellator.getInstance
      val r = t.getBuffer

      Textures.Block.bind()
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

      val icon = Textures.getSprite(Textures.Block.SwitchSideOn)
      r.pos(1, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
      r.pos(0, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
      r.pos(0, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
      r.pos(1, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()

      r.pos(0, 1, 1).tex(icon.getMinU, icon.getMaxV).endVertex()
      r.pos(1, 1, 1).tex(icon.getMaxU, icon.getMaxV).endVertex()
      r.pos(1, 0, 1).tex(icon.getMaxU, icon.getMinV).endVertex()
      r.pos(0, 0, 1).tex(icon.getMinU, icon.getMinV).endVertex()

      r.pos(1, 1, 1).tex(icon.getMinU, icon.getMaxV).endVertex()
      r.pos(1, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
      r.pos(1, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
      r.pos(1, 0, 1).tex(icon.getMinU, icon.getMinV).endVertex()

      r.pos(0, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
      r.pos(0, 1, 1).tex(icon.getMaxU, icon.getMaxV).endVertex()
      r.pos(0, 0, 1).tex(icon.getMaxU, icon.getMinV).endVertex()
      r.pos(0, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()

      t.draw()

      RenderState.enableEntityLighting()

      GlStateManager.popMatrix()
      //GlStateManager.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
