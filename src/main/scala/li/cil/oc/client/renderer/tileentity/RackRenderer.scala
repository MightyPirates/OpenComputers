package li.cil.oc.client.renderer.tileentity

import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.common.tileentity.Rack
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.opengl.GL11

object RackRenderer extends TileEntitySpecialRenderer[Rack] {
  private final val vOffset = 2 / 16f
  private final val vSize = 3 / 16f

  override def renderTileEntityAt(rack: Rack, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int): Unit = {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    //GlStateManager.popAttrib()

    GlStateManager.pushMatrix()

    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    rack.yaw match {
      case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
      case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
      case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GlStateManager.translate(-0.5, 0.5, 0.505 - 0.5f / 16f)
    GlStateManager.scale(1, -1, 1)

    // Note: we manually sync the rack inventory for this to work.
    for (i <- 0 until rack.getSizeInventory) {
      if (rack.getStackInSlot(i) != null) {
        GlStateManager.pushMatrix()
        //GlStateManager.pushAttrib()

        val v0 = vOffset + i * vSize
        val v1 = vOffset + (i + 1) * vSize
        val event = new RackMountableRenderEvent.TileEntity(rack, i, rack.lastData(i), v0, v1)
        MinecraftForge.EVENT_BUS.post(event)

        //GlStateManager.popAttrib()
        GlStateManager.popMatrix()
      }
    }

    GlStateManager.popMatrix()
    //GlStateManager.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
