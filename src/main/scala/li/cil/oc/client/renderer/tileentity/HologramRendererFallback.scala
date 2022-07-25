package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.common.tileentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer

object HologramRendererFallback {
  var text = "Requires OpenGL 1.5"

  def render(hologram: Hologram, f: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val fontRenderer = Minecraft.getInstance.font

    stack.pushPose()
    val pos = hologram.getBlockPos
    stack.translate(pos.getX + 0.5, pos.getY + 0.75, pos.getZ + 0.5)

    stack.scale(1 / 128f, -1 / 128f, 1 / 128f)
    RenderSystem.disableCull()
    fontRenderer.draw(stack, text, -fontRenderer.width(text) / 2, 0, 0xFFFFFFFF)

    stack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
