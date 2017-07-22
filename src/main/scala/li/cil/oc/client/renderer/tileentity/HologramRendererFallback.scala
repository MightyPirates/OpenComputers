package li.cil.oc.client.renderer.tileentity

import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object HologramRendererFallback extends TileEntitySpecialRenderer {
  var text = "Requires OpenGL 1.5"

  override def renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val fontRenderer = Minecraft.getMinecraft.fontRenderer

    GL11.glPushMatrix()
    GL11.glTranslated(x + 0.5, y + 0.75, z + 0.5)

    GL11.glScalef(1 / 128f, -1 / 128f, 1 / 128f)
    GL11.glDisable(GL11.GL_CULL_FACE)
    fontRenderer.drawString(text, -fontRenderer.getStringWidth(text) / 2, 0, 0xFFFFFFFF)

    GL11.glPopMatrix()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
