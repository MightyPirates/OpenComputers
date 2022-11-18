package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.tileentity.Assembler
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.util.math.vector.Vector3f

object AssemblerRenderer extends Function[TileEntityRendererDispatcher, AssemblerRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new AssemblerRenderer(dispatch)
}

class AssemblerRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[Assembler](dispatch) {
  override def render(assembler: Assembler, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderSystem.color4f(1, 1, 1, 1)

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)

    val r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY)

    {
      val icon = Textures.getSprite(Textures.Block.AssemblerTopOn)
      r.vertex(stack.last.pose, -0.5f, 0.55f, 0.5f).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0.5f, 0.55f, 0.5f).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0.5f, 0.55f, -0.5f).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, -0.5f, 0.55f, -0.5f).uv(icon.getU0, icon.getV0).endVertex()
    }

    // TODO Unroll loop to draw all at once?
    val indent = 6 / 16f + 0.005f
    for (i <- 0 until 4) {
      if (assembler.isAssembling) {
        val icon = Textures.getSprite(Textures.Block.AssemblerSideAssembling)
        r.vertex(stack.last.pose, indent, 0.5f, -indent).uv(icon.getU((0.5f - indent) * 16), icon.getV1).endVertex()
        r.vertex(stack.last.pose, indent, 0.5f, indent).uv(icon.getU((0.5f + indent) * 16), icon.getV1).endVertex()
        r.vertex(stack.last.pose, indent, -0.5f, indent).uv(icon.getU((0.5f + indent) * 16), icon.getV0).endVertex()
        r.vertex(stack.last.pose, indent, -0.5f, -indent).uv(icon.getU((0.5f - indent) * 16), icon.getV0).endVertex()
      }

      {
        val icon = Textures.getSprite(Textures.Block.AssemblerSideOn)
        r.vertex(stack.last.pose, 0.5005f, 0.5f, -0.5f).uv(icon.getU0, icon.getV1).endVertex()
        r.vertex(stack.last.pose, 0.5005f, 0.5f, 0.5f).uv(icon.getU1, icon.getV1).endVertex()
        r.vertex(stack.last.pose, 0.5005f, -0.5f, 0.5f).uv(icon.getU1, icon.getV0).endVertex()
        r.vertex(stack.last.pose, 0.5005f, -0.5f, -0.5f).uv(icon.getU0, icon.getV0).endVertex()
      }

      stack.mulPose(Vector3f.YP.rotationDegrees(90))
    }

    stack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
