package li.cil.oc.client.renderer.tileentity

import li.cil.oc.common.tileentity.Printer
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.entity.item.EntityItem
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

object PrinterRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val printer = tileEntity.asInstanceOf[Printer]
    if (printer.data.stateOff.size > 0) {
      val stack = printer.data.createItemStack()

      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
      GL11.glPushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

      GL11.glRotated((System.currentTimeMillis() % 20000) / 20000.0 * 360, 0, 1, 0)

      val brightness = printer.world.getLightBrightnessForSkyBlocks(printer.x, printer.y, printer.z, 0)
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

      // This is very 'meh', but item frames do it like this, too!
      val entity = new EntityItem(printer.world, 0, 0, 0, stack)
      entity.hoverStart = 0
      RenderItem.renderInFrame = true
      RenderManager.instance.renderEntityWithPosYaw(entity, 0, -0.1, 0, 0, 0)
      RenderItem.renderInFrame = false

      GL11.glPopMatrix()
      GL11.glPopAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}