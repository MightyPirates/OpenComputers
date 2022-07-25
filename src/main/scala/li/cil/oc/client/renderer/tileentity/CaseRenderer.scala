package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Case
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.Direction
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.vector.Vector3f
import org.lwjgl.opengl.GL11

object CaseRenderer extends Function[TileEntityRendererDispatcher, CaseRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new CaseRenderer(dispatch)
}

class CaseRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[Case](dispatch) {
  override def render(computer: Case, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderState.pushAttrib()

    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderState.setBlendAlpha(1)

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)

    computer.yaw match {
      case Direction.WEST => stack.mulPose(Vector3f.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Vector3f.YP.rotationDegrees(180))
      case Direction.EAST => stack.mulPose(Vector3f.YP.rotationDegrees(90))
      case _ => // No yaw.
    }

    stack.translate(-0.5, 0.5, 0.505)
    stack.scale(1, -1, 1)

    if (computer.isRunning) {
      renderFrontOverlay(stack, Textures.Block.CaseFrontOn)
      if (System.currentTimeMillis() - computer.lastFileSystemAccess < 400 && computer.world.random.nextDouble() > 0.1) {
        renderFrontOverlay(stack, Textures.Block.CaseFrontActivity)
      }
    }
    else if (computer.hasErrored && RenderUtil.shouldShowErrorLight(computer.hashCode)) {
      renderFrontOverlay(stack, Textures.Block.CaseFrontError)
    }

    RenderState.disableBlend()
    RenderState.enableEntityLighting()

    stack.popPose()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def renderFrontOverlay(stack: MatrixStack, texture: ResourceLocation): Unit = {
    val t = Tessellator.getInstance
    val r = t.getBuilder

    Textures.Block.bind()
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

    val icon = Textures.getSprite(texture)
    r.vertex(stack.last.pose, 0, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
    r.vertex(stack.last.pose, 1, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
    r.vertex(stack.last.pose, 1, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
    r.vertex(stack.last.pose, 0, 0, 0).uv(icon.getU0, icon.getV0).endVertex()

    t.end()
  }
}
