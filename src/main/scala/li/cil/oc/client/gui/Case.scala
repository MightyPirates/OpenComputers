package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.widget.button.Button
import net.minecraft.entity.player.PlayerInventory

import scala.collection.JavaConverters.asJavaCollection
import scala.collection.convert.ImplicitConversionsToJava._

class Case(id: Int, playerInventory: PlayerInventory, val computer: tileentity.Case)
  extends DynamicGuiContainer(new container.Case(id, playerInventory, computer),
    playerInventory, computer.getName) {

  protected var powerButton: ImageButton = _

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = computer.isRunning
    super.render(stack, mouseX, mouseY, dt)
  }

  override protected def init() {
    super.init()
    powerButton = new ImageButton(leftPos + 70, topPos + 33, 18, 18, new Button.IPressable {
      override def onPress(b: Button) = ClientPacketSender.sendComputerPower(computer, !computer.isRunning)
    }, Textures.GUI.ButtonPower, canToggle = true)
    addButton(powerButton)
  }

  override protected def drawSecondaryForegroundLayer(stack: MatrixStack, mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(stack, mouseX, mouseY)
    font.draw(stack, computer.getName, 8, 6, 0x404040)
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(if (computer.isRunning) Localization.Computer.TurnOff.lines.toIterable else Localization.Computer.TurnOn.lines.toIterable))
      copiedDrawHoveringText(stack, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
  }

  override def drawSecondaryBackgroundLayer(stack: MatrixStack) {
    RenderSystem.color3f(1, 1, 1)
    Textures.bind(Textures.GUI.Computer)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
