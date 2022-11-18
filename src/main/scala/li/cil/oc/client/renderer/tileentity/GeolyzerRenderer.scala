package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.tileentity.Geolyzer
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher

object GeolyzerRenderer extends Function[TileEntityRendererDispatcher, GeolyzerRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new GeolyzerRenderer(dispatch)
}

class GeolyzerRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[Geolyzer](dispatch) {
  override def render(geolyzer: Geolyzer, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderSystem.color4f(1, 1, 1, 1)

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)
    stack.scale(1.0025f, -1.0025f, 1.0025f)
    stack.translate(-0.5f, -0.5f, -0.5f)

    val r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY)

    val icon = Textures.getSprite(Textures.Block.GeolyzerTopOn)
    r.vertex(stack.last.pose, 0, 0, 1).uv(icon.getU0, icon.getV1).endVertex()
    r.vertex(stack.last.pose, 1, 0, 1).uv(icon.getU1, icon.getV1).endVertex()
    r.vertex(stack.last.pose, 1, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
    r.vertex(stack.last.pose, 0, 0, 0).uv(icon.getU0, icon.getV0).endVertex()

    stack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

}
