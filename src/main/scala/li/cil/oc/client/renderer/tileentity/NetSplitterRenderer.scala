package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.AtlasTexture
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.Direction
import org.lwjgl.opengl.GL11

object NetSplitterRenderer extends Function[TileEntityRendererDispatcher, NetSplitterRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new NetSplitterRenderer(dispatch)
}

class NetSplitterRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[tileentity.NetSplitter](dispatch) {
  override def render(splitter: tileentity.NetSplitter, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (splitter.openSides.contains(!splitter.isInverted)) {
      RenderState.pushAttrib()
      RenderState.disableEntityLighting()
      RenderState.makeItBlend()

      stack.pushPose()

      stack.translate(0.5, 0.5, 0.5)
      stack.scale(1.0025f, -1.0025f, 1.0025f)
      stack.translate(-0.5f, -0.5f, -0.5f)

      Minecraft.getInstance().getModelManager().getAtlas(AtlasTexture.LOCATION_BLOCKS).bind()

      val t = Tessellator.getInstance
      val r = t.getBuilder

      Textures.Block.bind()
      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)

      val sideActivity = Textures.getSprite(Textures.Block.NetSplitterOn)

      if (splitter.isSideOpen(Direction.DOWN)) {
        r.vertex(stack.last.pose, 0, 1, 0).uv(sideActivity.getU1, sideActivity.getV0).endVertex()
        r.vertex(stack.last.pose, 1, 1, 0).uv(sideActivity.getU0, sideActivity.getV0).endVertex()
        r.vertex(stack.last.pose, 1, 1, 1).uv(sideActivity.getU0, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 0, 1, 1).uv(sideActivity.getU1, sideActivity.getV1).endVertex()
      }

      if (splitter.isSideOpen(Direction.UP)) {
        r.vertex(stack.last.pose, 0, 0, 0).uv(sideActivity.getU1, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 0, 0, 1).uv(sideActivity.getU1, sideActivity.getV0).endVertex()
        r.vertex(stack.last.pose, 1, 0, 1).uv(sideActivity.getU0, sideActivity.getV0).endVertex()
        r.vertex(stack.last.pose, 1, 0, 0).uv(sideActivity.getU0, sideActivity.getV1).endVertex()
      }

      if (splitter.isSideOpen(Direction.NORTH)) {
        r.vertex(stack.last.pose, 1, 1, 0).uv(sideActivity.getU0, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 0, 1, 0).uv(sideActivity.getU1, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 0, 0, 0).uv(sideActivity.getU1, sideActivity.getV0).endVertex()
        r.vertex(stack.last.pose, 1, 0, 0).uv(sideActivity.getU0, sideActivity.getV0).endVertex()
      }

      if (splitter.isSideOpen(Direction.SOUTH)) {
        r.vertex(stack.last.pose, 0, 1, 1).uv(sideActivity.getU0, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 1, 1, 1).uv(sideActivity.getU1, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 1, 0, 1).uv(sideActivity.getU1, sideActivity.getV0).endVertex()
        r.vertex(stack.last.pose, 0, 0, 1).uv(sideActivity.getU0, sideActivity.getV0).endVertex()
      }

      if (splitter.isSideOpen(Direction.WEST)) {
        r.vertex(stack.last.pose, 0, 1, 0).uv(sideActivity.getU0, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 0, 1, 1).uv(sideActivity.getU1, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 0, 0, 1).uv(sideActivity.getU1, sideActivity.getV0).endVertex()
        r.vertex(stack.last.pose, 0, 0, 0).uv(sideActivity.getU0, sideActivity.getV0).endVertex()
      }

      if (splitter.isSideOpen(Direction.EAST)) {
        r.vertex(stack.last.pose, 1, 1, 1).uv(sideActivity.getU0, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 1, 1, 0).uv(sideActivity.getU1, sideActivity.getV1).endVertex()
        r.vertex(stack.last.pose, 1, 0, 0).uv(sideActivity.getU1, sideActivity.getV0).endVertex()
        r.vertex(stack.last.pose, 1, 0, 1).uv(sideActivity.getU0, sideActivity.getV0).endVertex()
      }

      t.end()

      RenderState.disableBlend()
      RenderState.enableEntityLighting()

      stack.popPose()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
