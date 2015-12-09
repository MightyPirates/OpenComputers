package li.cil.oc.client.gui.widget

import li.cil.oc.client.Textures
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats

class ProgressBar(val x: Int, val y: Int) extends Widget {
  override def width = 140

  override def height = 12

  def barTexture = Textures.GUI.Bar

  var level = 0.0

  def draw() {
    if (level > 0) {
      val u0 = 0
      val u1 = level
      val v0 = 0
      val v1 = 1
      val tx = owner.windowX + x
      val ty = owner.windowY + y
      val w = width * level

      Textures.bind(barTexture)
      val t = Tessellator.getInstance
      val r = t.getWorldRenderer
      r.begin(7, DefaultVertexFormats.POSITION_TEX)
      r.pos(tx, ty, owner.windowZ).tex(u0, v0).endVertex()
      r.pos(tx, ty + height, owner.windowZ).tex(u0, v1).endVertex()
      r.pos(tx + w, ty + height, owner.windowZ).tex(u1, v1).endVertex()
      r.pos(tx + w, ty, owner.windowZ).tex(u1, v0).endVertex()
      t.draw()
    }
  }
}
