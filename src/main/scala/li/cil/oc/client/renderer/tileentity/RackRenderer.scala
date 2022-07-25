package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.common.tileentity.Rack
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3f
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.opengl.GL11

object RackRenderer extends Function[TileEntityRendererDispatcher, RackRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new RackRenderer(dispatch)
}

class RackRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[Rack](dispatch) {
  private final val vOffset = 2 / 16f
  private final val vSize = 3 / 16f

  override def render(rack: Rack, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderState.pushAttrib()

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)

    rack.yaw match {
      case Direction.WEST => stack.mulPose(Vector3f.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Vector3f.YP.rotationDegrees(180))
      case Direction.EAST => stack.mulPose(Vector3f.YP.rotationDegrees(90))
      case _ => // No yaw.
    }

    stack.translate(-0.5, 0.5, 0.505 - 0.5f / 16f)
    stack.scale(1, -1, 1)

    // Note: we manually sync the rack inventory for this to work.
    for (i <- 0 until rack.getContainerSize) {
      if (!rack.getItem(i).isEmpty) {
        stack.pushPose()
        RenderState.pushAttrib()

        val v0 = vOffset + i * vSize
        val v1 = vOffset + (i + 1) * vSize
        val event = new RackMountableRenderEvent.TileEntity(rack, i, rack.lastData(i), stack, buffer, light, overlay, v0, v1)
        MinecraftForge.EVENT_BUS.post(event)

        RenderState.popAttrib()
        stack.popPose()
      }
    }

    stack.popPose()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
