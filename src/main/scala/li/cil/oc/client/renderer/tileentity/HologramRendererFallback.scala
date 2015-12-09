package li.cil.oc.client.renderer.tileentity

import li.cil.oc.common.tileentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import org.lwjgl.opengl.GL11

object HologramRendererFallback extends TileEntitySpecialRenderer[Hologram] {
  var text = "Requires OpenGL 1.5"

  override def renderTileEntityAt(hologram: Hologram, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val fontRenderer = Minecraft.getMinecraft.fontRendererObj

    RenderState.pushMatrix()
    GL11.glTranslated(x + 0.5, y + 0.75, z + 0.5)

    GL11.glScalef(1 / 128f, -1 / 128f, 1 / 128f)
    RenderState.disableCullFace()
    fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2, 0, 0xFFFFFFFF)

    RenderState.popMatrix()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
