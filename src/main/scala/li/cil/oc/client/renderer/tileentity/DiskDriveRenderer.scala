package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.tileentity.DiskDrive
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.model.ItemCameraTransforms
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3f

object DiskDriveRenderer extends Function[TileEntityRendererDispatcher, DiskDriveRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new DiskDriveRenderer(dispatch)
}

class DiskDriveRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[DiskDrive](dispatch) {
  override def render(drive: DiskDrive, dt: Float, matrix: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderSystem.color4f(1, 1, 1, 1)

    matrix.pushPose()

    matrix.translate(0.5, 0.5, 0.5)

    drive.yaw match {
      case Direction.WEST => matrix.mulPose(Vector3f.YP.rotationDegrees(-90))
      case Direction.NORTH => matrix.mulPose(Vector3f.YP.rotationDegrees(180))
      case Direction.EAST => matrix.mulPose(Vector3f.YP.rotationDegrees(90))
      case _ => // No yaw.
    }

    drive.items(0) match {
      case stack if !stack.isEmpty =>
        matrix.pushPose()
        matrix.translate(0, 3.5f / 16, 6 / 16f)
        matrix.mulPose(Vector3f.XN.rotationDegrees(90))
        matrix.scale(0.5f, 0.5f, 0.5f)

        Minecraft.getInstance.getItemRenderer.renderStatic(stack, ItemCameraTransforms.TransformType.FIXED, light, overlay, matrix, buffer)
        matrix.popPose()
      case _ =>
    }

    if (System.currentTimeMillis() - drive.lastAccess < 400 && drive.world.random.nextDouble() > 0.1) {
      matrix.translate(-0.5, 0.5, 0.505)
      matrix.scale(1, -1, 1)

      val r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY)

      val icon = Textures.getSprite(Textures.Block.DiskDriveFrontActivity)
      r.vertex(matrix.last.pose, 0, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(matrix.last.pose, 1, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(matrix.last.pose, 1, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(matrix.last.pose, 0, 0, 0).uv(icon.getU0, icon.getV0).endVertex()
    }

    matrix.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
