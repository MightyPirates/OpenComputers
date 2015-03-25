package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Printer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OpenGlHelper
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

      RenderState.pushAttrib(GL11.GL_ALL_ATTRIB_BITS)
      RenderState.pushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

      GL11.glRotated((System.currentTimeMillis() % 20000) / 20000.0 * 360, 0, 1, 0)

      val brightness = printer.world.getCombinedLight(printer.getPos, 0)
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

      // This is very 'meh', but item frames do it like this, too!
      val entity = new EntityItem(printer.world, 0, 0, 0, stack)
      entity.hoverStart = 0
      Textures.Block.bind()
      Minecraft.getMinecraft.getRenderItem.renderItemModel(entity.getEntityItem)

      RenderState.popMatrix()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}