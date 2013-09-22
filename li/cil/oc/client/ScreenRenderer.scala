package li.cil.oc.client

import org.lwjgl.opengl.GL11
import li.cil.oc.client.gui.MonospaceFontRenderer
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection
import net.minecraft.client.renderer.OpenGlHelper

object ScreenRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(t: TileEntity, x: Double, y: Double, z: Double, f: Float) = {
    val tileEntity = t.asInstanceOf[TileEntityScreen]

    GL11.glPushAttrib(0xFFFFFF)
    GL11.glPushMatrix()
    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    tileEntity.pitch match {
      case ForgeDirection.DOWN => GL11.glRotatef(-90, 1, 0, 0)
      case ForgeDirection.UP => GL11.glRotatef(90, 1, 0, 0)
      case _ => // No pitch.
    }
    tileEntity.yaw match {
      case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    // Fit area to screen (top left = top left).
    GL11.glTranslatef(-0.5f, 0.5f, 0.501f)

    // Scale to inner screen size and offset it.
    GL11.glTranslatef(2.25f / 16f, -2.25f / 16f, 0)
    GL11.glScalef(11.5f / 16f, 11.5f / 16f, 1)

    // Scale based on actual buffer size.
    val (w, h) = tileEntity.screen.resolution
    val scale = 1f / ((w * MonospaceFontRenderer.fontWidth) max (h * MonospaceFontRenderer.fontHeight))
    GL11.glScalef(scale, scale, 1)

    // Flip text upside down.
    GL11.glScalef(1, -1, 1)

    GL11.glDepthMask(false)
    GL11.glDisable(GL11.GL_LIGHTING)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR)
    GL11.glDepthFunc(GL11.GL_LEQUAL)

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 200, 200)

    MonospaceFontRenderer.init(tileEntityRenderer.renderEngine)
    for ((line, i) <- tileEntity.screen.lines.zipWithIndex) {
      MonospaceFontRenderer.drawString(line, 0, i * MonospaceFontRenderer.fontHeight)
    }

    GL11.glPopMatrix()
    GL11.glPopAttrib()
  }
}