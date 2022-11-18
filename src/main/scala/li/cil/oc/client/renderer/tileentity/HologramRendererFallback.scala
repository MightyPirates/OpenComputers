package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.common.tileentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.util.math.vector.Vector3f

object HologramRendererFallback {
  var text = "Requires OpenGL 1.5"

  def render(hologram: Hologram, f: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    RenderSystem.color4f(1, 1, 1, 1)

    val fontRenderer = Minecraft.getInstance.font

    stack.pushPose()
    stack.translate(0.5, 0.75, 0.5)
    stack.scale(1 / 128f, -1 / 128f, 1 / 128f)

    fontRenderer.drawInBatch(text, -fontRenderer.width(text) / 2, 0, 0xFFFFFFFF,
      false, stack.last.pose, buffer, false, 0, light)
    stack.mulPose(Vector3f.YP.rotationDegrees(180))
    fontRenderer.drawInBatch(text, -fontRenderer.width(text) / 2, 0, 0xFFFFFFFF,
      false, stack.last.pose, buffer, false, 0, light)

    stack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
