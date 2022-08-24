package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

class Raid(state: container.Raid, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {

  override def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color3f(1, 1, 1) // Required under Linux.
    Textures.bind(Textures.GUI.Raid)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
