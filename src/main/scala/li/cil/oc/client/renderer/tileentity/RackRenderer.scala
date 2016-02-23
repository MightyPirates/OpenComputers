package li.cil.oc.client.renderer.tileentity

import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.common.tileentity.Rack
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.opengl.GL11

object RackRenderer extends TileEntitySpecialRenderer[Rack] {
  private final val vOffset = 2 / 16f
  private final val vSize = 3 / 16f

  override def renderTileEntityAt(rack: Rack, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int): Unit = {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    rack.yaw match {
      case EnumFacing.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case EnumFacing.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case EnumFacing.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GL11.glTranslated(-0.5, 0.5, 0.505 - 1 / 16f)
    GL11.glScalef(1, -1, 1)

    // Note: we manually sync the rack inventory for this to work.
    for (i <- 0 until rack.getSizeInventory) {
      if (rack.getStackInSlot(i) != null) {
        GL11.glPushMatrix()
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

        val v0 = vOffset + i * vSize
        val v1 = vOffset + (i + 1) * vSize
        val event = new RackMountableRenderEvent.TileEntity(rack, i, rack.lastData(i), v0, v1)
        MinecraftForge.EVENT_BUS.post(event)

        GL11.glPopAttrib()
        GL11.glPopMatrix()
      }
    }

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
