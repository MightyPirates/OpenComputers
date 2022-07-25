package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11

object TransposerRenderer extends Function[TileEntityRendererDispatcher, TransposerRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new TransposerRenderer(dispatch)
}

class TransposerRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[tileentity.Transposer](dispatch) {
  override def render(transposer: tileentity.Transposer, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val activity = math.max(0, 1 - (System.currentTimeMillis() - transposer.lastOperation) / 1000.0)
    if (activity > 0) {
      RenderState.pushAttrib()

      RenderState.disableEntityLighting()
      RenderState.makeItBlend()
      RenderState.setBlendAlpha(activity.toFloat)

      stack.pushPose()

      stack.translate(0.5, 0.5, 0.5)
      stack.scale(1.0025f, -1.0025f, 1.0025f)
      stack.translate(-0.5f, -0.5f, -0.5f)

      val t = Tessellator.getInstance
      val r = t.getBuilder

      Textures.Block.bind()
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

      val icon = Textures.getSprite(Textures.Block.TransposerOn)
      r.vertex(stack.last.pose, 0, 1, 0).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 1, 0).uv(icon.getU0, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 1, 1).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 1, 1).uv(icon.getU1, icon.getV1).endVertex()

      r.vertex(stack.last.pose, 0, 0, 0).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 0, 1).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 0, 1).uv(icon.getU0, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 0, 0).uv(icon.getU0, icon.getV1).endVertex()

      r.vertex(stack.last.pose, 1, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 0, 0).uv(icon.getU0, icon.getV0).endVertex()

      r.vertex(stack.last.pose, 0, 1, 1).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 1, 1, 1).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 1, 0, 1).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 0, 0, 1).uv(icon.getU0, icon.getV0).endVertex()

      r.vertex(stack.last.pose, 0, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 1, 1).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 0, 0, 1).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 0, 0, 0).uv(icon.getU0, icon.getV0).endVertex()

      r.vertex(stack.last.pose, 1, 1, 1).uv(icon.getU0, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 1, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
      r.vertex(stack.last.pose, 1, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
      r.vertex(stack.last.pose, 1, 0, 1).uv(icon.getU0, icon.getV0).endVertex()

      t.end()

      RenderState.disableBlend()
      RenderState.enableEntityLighting()

      stack.popPose()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
