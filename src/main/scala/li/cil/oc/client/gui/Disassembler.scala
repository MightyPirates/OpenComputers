package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

class Disassembler(state: container.Disassembler, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {

  val progress = addCustomWidget(new ProgressBar(18, 65))

  override protected def renderLabels(stack: MatrixStack, mouseX: Int, mouseY: Int) {
    font.draw(stack, title, titleLabelX, titleLabelY, 0x404040);
  }

  override def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color3f(1, 1, 1)
    Textures.bind(Textures.GUI.Disassembler)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    progress.level = inventoryContainer.disassemblyProgress / 100.0
    drawWidgets(stack)
  }
}
