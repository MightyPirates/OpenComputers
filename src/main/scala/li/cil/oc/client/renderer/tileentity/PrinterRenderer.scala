package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Printer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.entity.item.EntityItem
import org.lwjgl.opengl.GL11

object PrinterRenderer extends TileEntitySpecialRenderer[Printer] {
  override def renderTileEntityAt(printer: Printer, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    if (printer.data.stateOff.nonEmpty) {
      val stack = printer.data.createItemStack()

      RenderState.pushAttrib(GL11.GL_ALL_ATTRIB_BITS)
      RenderState.pushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5 + 0.3, z + 0.5)

      GL11.glRotated((System.currentTimeMillis() % 20000) / 20000.0 * 360, 0, 1, 0)

      val brightness = printer.world.getCombinedLight(printer.getPos, 0)
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

      // This is very 'meh', but item frames do it like this, too!
      val entity = new EntityItem(printer.world, 0, 0, 0, stack)
      entity.hoverStart = 0
      Textures.Block.bind()
      Minecraft.getMinecraft.getRenderItem.renderItem(entity.getEntityItem, ItemCameraTransforms.TransformType.FIXED)

      RenderState.popMatrix()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}