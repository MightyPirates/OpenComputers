package li.cil.oc.client.gui.widget

import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.client.Textures
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11

class ProgressBar(val x: Int, val y: Int) extends Widget {
  override def width = 140

  override def height = 12

  def barTexture = Textures.GUI.Bar

  var level = 0.0

  def draw(stack: MatrixStack) {
    if (level > 0) {
      val u0 = 0
      val u1 = level.toFloat
      val v0 = 0
      val v1 = 1
      val tx = owner.windowX + x
      val ty = owner.windowY + y
      val w = (width * level).toFloat

      Textures.bind(barTexture)
      val t = Tessellator.getInstance
      val r = t.getBuilder
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
      r.vertex(stack.last.pose, tx, ty, owner.windowZ).uv(u0, v0).endVertex()
      r.vertex(stack.last.pose, tx, ty + height, owner.windowZ).uv(u0, v1).endVertex()
      r.vertex(stack.last.pose, tx + w, ty + height, owner.windowZ).uv(u1, v1).endVertex()
      r.vertex(stack.last.pose, tx + w, ty, owner.windowZ).uv(u1, v0).endVertex()
      t.end()
    }
  }
}
