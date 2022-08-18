package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

object Disassembler {
  def of(id: Int, playerInventory: PlayerInventory, disassembler: tileentity.Disassembler) =
    new Disassembler(new container.Disassembler(container.ContainerTypes.DISASSEMBLER, id, playerInventory, disassembler), playerInventory, disassembler.getName)
}

class Disassembler(state: container.Disassembler, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name) {

  val progress = addCustomWidget(new ProgressBar(18, 65))

  override def renderBg(stack: MatrixStack, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.color3f(1, 1, 1)
    Textures.bind(Textures.GUI.Disassembler)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    progress.level = inventoryContainer.disassemblyProgress / 100.0
    drawWidgets(stack)
  }
}
