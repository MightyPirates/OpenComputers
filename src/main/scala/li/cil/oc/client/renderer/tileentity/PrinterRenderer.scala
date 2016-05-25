package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Printer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.entity.item.EntityItem

object PrinterRenderer extends TileEntitySpecialRenderer[Printer] {
  override def renderTileEntityAt(printer: Printer, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    if (printer.data.stateOff.nonEmpty) {
      val stack = printer.data.createItemStack()

      //GlStateManager.pushAttrib()
      GlStateManager.pushMatrix()

      GlStateManager.translate(x + 0.5, y + 0.5 + 0.3, z + 0.5)

      GlStateManager.rotate((System.currentTimeMillis() % 20000) / 20000f * 360, 0, 1, 0)
      GlStateManager.scale(0.75, 0.75, 0.75)

      val brightness = printer.world.getCombinedLight(printer.getPos, 0)
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

      // This is very 'meh', but item frames do it like this, too!
      val entity = new EntityItem(printer.world, 0, 0, 0, stack)
      entity.hoverStart = 0
      Textures.Block.bind()
      Minecraft.getMinecraft.getRenderItem.renderItem(entity.getEntityItem, ItemCameraTransforms.TransformType.FIXED)

      GlStateManager.popMatrix()
      //GlStateManager.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}