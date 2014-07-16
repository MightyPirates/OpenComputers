package li.cil.oc.client.renderer.tileentity

import li.cil.oc.common.tileentity.DiskDrive
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.entity.{RenderItem, RenderManager}
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.entity.item.EntityItem
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

object DiskDriveRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val drive = tileEntity.asInstanceOf[DiskDrive]
    drive.items(0) match {
      case Some(stack) =>
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

        val brightness = drive.world.getLightBrightnessForSkyBlocks(drive.x + drive.facing.offsetX, drive.y + drive.facing.offsetY, drive.z + drive.facing.offsetZ, 0)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

        GL11.glPushMatrix()

        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

        drive.yaw match {
          case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
          case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
          case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
          case _ => // No yaw.
        }

        GL11.glTranslatef(0, 3.5f / 16, 9 / 16f)
        GL11.glRotatef(90, -1, 0, 0)

        // This is very 'meh', but item frames do it like this, too!
        val entity = new EntityItem(drive.world, 0, 0, 0, stack)
        entity.hoverStart = 0
        RenderItem.renderInFrame = true
        RenderManager.instance.renderEntityWithPosYaw(entity, 0, 0, 0, 0, 0)
        RenderItem.renderInFrame = false

        GL11.glPopMatrix()
        GL11.glPopAttrib()
      case _ =>
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}