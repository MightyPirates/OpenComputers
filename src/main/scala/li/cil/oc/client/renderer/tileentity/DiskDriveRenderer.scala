package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.DiskDrive
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.entity.RenderItem
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.entity.item.EntityItem
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

object DiskDriveRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val drive = tileEntity.asInstanceOf[DiskDrive]
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    drive.yaw match {
      case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    drive.items(0) match {
      case Some(stack) =>
        GL11.glPushMatrix()
        GL11.glTranslatef(0, 3.5f / 16, 9 / 16f)
        GL11.glRotatef(90, -1, 0, 0)

        val brightness = drive.world.getLightBrightnessForSkyBlocks(drive.x + drive.facing.offsetX, drive.y + drive.facing.offsetY, drive.z + drive.facing.offsetZ, 0)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightness % 65536, brightness / 65536)

        // This is very 'meh', but item frames do it like this, too!
        val entity = new EntityItem(drive.world, 0, 0, 0, stack)
        entity.hoverStart = 0
        RenderItem.renderInFrame = true
        RenderManager.instance.renderEntityWithPosYaw(entity, 0, 0, 0, 0, 0)
        RenderItem.renderInFrame = false
        GL11.glPopMatrix()
      case _ =>
    }

    if (System.currentTimeMillis() - drive.lastAccess < 400 && drive.world.rand.nextDouble() > 0.1) {
      GL11.glTranslated(-0.5, 0.5, 0.505)
      GL11.glScalef(1, -1, 1)

      RenderState.disableLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha(1)

      bindTexture(Textures.blockDiskDriveFrontActivity)
      val t = Tessellator.instance
      t.startDrawingQuads()
      t.addVertexWithUV(0, 1, 0, 0, 1)
      t.addVertexWithUV(1, 1, 0, 1, 1)
      t.addVertexWithUV(1, 0, 0, 1, 0)
      t.addVertexWithUV(0, 0, 0, 0, 0)
      t.draw()

      RenderState.enableLighting()
    }

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}