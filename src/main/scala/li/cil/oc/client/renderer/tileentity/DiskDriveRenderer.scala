package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.DiskDrive
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.item.EntityItem
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object DiskDriveRenderer extends TileEntitySpecialRenderer[DiskDrive] {
  override def renderTileEntityAt(drive: DiskDrive, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    RenderState.pushAttrib()
    GlStateManager.color(1, 1, 1, 1)

    GlStateManager.pushMatrix()

    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    drive.yaw match {
      case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
      case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
      case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
      case _ => // No yaw.
    }

    drive.items(0) match {
      case Some(stack) =>
        GlStateManager.pushMatrix()
        GlStateManager.translate(0, 3.5f / 16, 6 / 16f)
        GlStateManager.rotate(90, -1, 0, 0)
        GlStateManager.scale(0.5f, 0.5f, 0.5f)

        val brightness = drive.world.getCombinedLight(drive.getPos.offset(drive.facing), 0)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

        // This is very 'meh', but item frames do it like this, too!
        val entity = new EntityItem(drive.world, 0, 0, 0, stack)
        entity.hoverStart = 0
        Textures.Block.bind()
        Minecraft.getMinecraft.getRenderItem.renderItem(entity.getEntityItem, ItemCameraTransforms.TransformType.FIXED)
        GlStateManager.popMatrix()
      case _ =>
    }

    if (System.currentTimeMillis() - drive.lastAccess < 400 && drive.world.rand.nextDouble() > 0.1) {
      GlStateManager.translate(-0.5, 0.5, 0.505)
      GlStateManager.scale(1, -1, 1)

      RenderState.disableEntityLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha(1)

      val t = Tessellator.getInstance
      val r = t.getWorldRenderer

      Textures.Block.bind()
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

      val icon = Textures.getSprite(Textures.Block.DiskDriveFrontActivity)
      r.pos(0, 1, 0).tex(icon.getMinU, icon.getMaxV).endVertex()
      r.pos(1, 1, 0).tex(icon.getMaxU, icon.getMaxV).endVertex()
      r.pos(1, 0, 0).tex(icon.getMaxU, icon.getMinV).endVertex()
      r.pos(0, 0, 0).tex(icon.getMinU, icon.getMinV).endVertex()

      t.draw()

      RenderState.disableBlend()
      RenderState.enableEntityLighting()
    }

    GlStateManager.popMatrix()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}