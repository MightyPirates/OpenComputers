package li.cil.oc.client.renderer.tileentity

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import li.cil.oc.common.tileentity.Hologram
import net.minecraft.client.renderer.Tessellator
import org.lwjgl.opengl.GL11
import li.cil.oc.util.RenderState
import li.cil.oc.client.TexturePreloader
import net.minecraftforge.client.MinecraftForgeClient

object HologramRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    if (MinecraftForgeClient.getRenderPass != 0) return

    val hologram = te.asInstanceOf[Hologram]

    GL11.glPushAttrib(0xFFFFFFFF)
    GL11.glDisable(GL11.GL_CULL_FACE)

//    RenderState.disableLighting()
    RenderState.makeItBlend()

    GL11.glPushMatrix()
    GL11.glTranslated(x - 1, y + 0.5, z - 1)

    def isSolid(hx: Int, hy: Int, hz: Int) = {
      hx >= 0 && hy >= 0 && hz >= 0 && hx < hologram.width && hy < hologram.height && hz < hologram.width &&
        (hologram.volume(hx + hz * hologram.width) & (1 << hy)) != 0
    }

    bindTexture(TexturePreloader.blockHologram)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.setColorRGBA_F(1, 1, 1, 0.7f)

    val s = 1f / 16f
    for (hx <- 0 until hologram.width) {
      val wx = hx * s
      for (hz <- 0 until hologram.width) {
        val wz = hz * s
        for (hy <- 0 until hologram.height) {
          val wy = hy * s

          if (isSolid(hx, hy, hz)) {
            /*
                  0---1
                  | N |
              0---3---2---1---0
              | W | U | E | D |
              5---6---7---4---5
                  | S |
                  5---4
             */

            // South
            if (!isSolid(hx, hy, hz + 1)) {
              t.setNormal(0, 0, 1)
              t.addVertex(wx + s, wy + s, wz + s) // 5
              t.addVertex(wx + 0, wy + s, wz + s) // 4
              t.addVertex(wx + 0, wy + 0, wz + s) // 7
              t.addVertex(wx + s, wy + 0, wz + s) // 6
            }
            // North
            if (!isSolid(hx, hy, hz - 1)) {
              t.setNormal(0, 0, -1)
              t.addVertex(wx + s, wy + 0, wz + 0) // 3
              t.addVertex(wx + 0, wy + 0, wz + 0) // 2
              t.addVertex(wx + 0, wy + s, wz + 0) // 1
              t.addVertex(wx + s, wy + s, wz + 0) // 0
            }

            // East
            if (!isSolid(hx + 1, hy, hz)) {
              t.setNormal(1, 0, 0)
              t.addVertex(wx + s, wy + s, wz + s) // 5
              t.addVertex(wx + s, wy + 0, wz + s) // 6
              t.addVertex(wx + s, wy + 0, wz + 0) // 3
              t.addVertex(wx + s, wy + s, wz + 0) // 0
            }
            // West
            if (!isSolid(hx - 1, hy, hz)) {
              t.setNormal(-1, 0, 0)
              t.addVertex(wx + 0, wy + 0, wz + s) // 7
              t.addVertex(wx + 0, wy + s, wz + s) // 4
              t.addVertex(wx + 0, wy + s, wz + 0) // 1
              t.addVertex(wx + 0, wy + 0, wz + 0) // 2
            }

            // Up
            if (!isSolid(hx, hy + 1, hz)) {
              t.setNormal(0, 1, 0)
              t.addVertex(wx + s, wy + s, wz + 0) // 0
              t.addVertex(wx + 0, wy + s, wz + 0) // 1
              t.addVertex(wx + 0, wy + s, wz + s) // 4
              t.addVertex(wx + s, wy + s, wz + s) // 5
            }
            // Down
            if (!isSolid(hx, hy - 1, hz)) {
              t.setNormal(0, -1, 0)
              t.addVertex(wx + s, wy + 0, wz + s) // 6
              t.addVertex(wx + 0, wy + 0, wz + s) // 7
              t.addVertex(wx + 0, wy + 0, wz + 0) // 2
              t.addVertex(wx + s, wy + 0, wz + 0) // 3
            }

          }
        }
      }
    }

    t.draw()

    GL11.glPopMatrix()
    GL11.glPopAttrib()
  }
}
