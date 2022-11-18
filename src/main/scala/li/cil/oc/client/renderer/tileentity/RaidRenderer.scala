package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.IVertexBuilder
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.RenderTypes
import li.cil.oc.common.tileentity.Raid
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3f

object RaidRenderer extends Function[TileEntityRendererDispatcher, RaidRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new RaidRenderer(dispatch)
}

class RaidRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[Raid](dispatch) {
  override def render(raid: Raid, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderSystem.color4f(1, 1, 1, 1)

    stack.pushPose()

    stack.translate(0.5, 0.5, 0.5)

    raid.yaw match {
      case Direction.WEST => stack.mulPose(Vector3f.YP.rotationDegrees(-90))
      case Direction.NORTH => stack.mulPose(Vector3f.YP.rotationDegrees(180))
      case Direction.EAST => stack.mulPose(Vector3f.YP.rotationDegrees(90))
      case _ => // No yaw.
    }

    stack.translate(-0.5, 0.5, 0.505)
    stack.scale(1, -1, 1)

    val r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY)

    {
      val icon = Textures.getSprite(Textures.Block.RaidFrontError)
      for (slot <- 0 until raid.getContainerSize) {
        if (!raid.presence(slot)) {
          renderSlot(stack, r, slot, icon)
        }
      }
    }

    {
      val icon = Textures.getSprite(Textures.Block.RaidFrontActivity)
      for (slot <- 0 until raid.getContainerSize) {
        if (System.currentTimeMillis() - raid.lastAccess < 400 && raid.world.random.nextDouble() > 0.1 && slot == raid.lastAccess % raid.getContainerSize) {
          renderSlot(stack, r, slot, icon)
        }
      }
    }

    stack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private val u1 = 2 / 16f
  private val fs = 4 / 16f

  private def renderSlot(stack: MatrixStack, r: IVertexBuilder, slot: Int, icon: TextureAtlasSprite) {
    val l = u1 + slot * fs
    val h = u1 + (slot + 1) * fs
    r.vertex(stack.last.pose, l, 1, 0).uv(icon.getU(l * 16), icon.getV1).endVertex()
    r.vertex(stack.last.pose, h, 1, 0).uv(icon.getU(h * 16), icon.getV1).endVertex()
    r.vertex(stack.last.pose, h, 0, 0).uv(icon.getU(h * 16), icon.getV0).endVertex()
    r.vertex(stack.last.pose, l, 0, 0).uv(icon.getU(l * 16), icon.getV0).endVertex()
  }
}
