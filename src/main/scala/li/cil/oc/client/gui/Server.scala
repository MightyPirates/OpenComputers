package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.tileentity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.widget.button.Button
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.text.ITextComponent

import scala.collection.JavaConverters.asJavaCollection

object Server {
  def of(id: Int, playerInventory: PlayerInventory, serverInventory: ServerInventory, slot: Int = -1)
    = new Server(new container.Server(container.ContainerTypes.SERVER, id, playerInventory, serverInventory, slot), playerInventory, serverInventory.getName)
}

class Server(state: container.Server, playerInventory: PlayerInventory, name: ITextComponent)
  extends DynamicGuiContainer(state, playerInventory, name)
  with traits.LockedHotbar[container.Server] {

  protected var powerButton: ImageButton = _

  override def lockedStack = inventoryContainer.stack

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.visible = !inventoryContainer.isItem
    powerButton.toggled = inventoryContainer.isRunning
    super.render(stack, mouseX, mouseY, dt)
  }

  override protected def init() {
    super.init()
    powerButton = new ImageButton(leftPos + 48, topPos + 33, 18, 18, new Button.IPressable {
      override def onPress(b: Button) = if (inventoryContainer.rackSlot >= 0) {
        ClientPacketSender.sendServerPower(inventoryContainer, inventoryContainer.rackSlot, !inventoryContainer.isRunning)
      }
    }, Textures.GUI.ButtonPower, canToggle = true)
    addButton(powerButton)
  }

  override def drawSecondaryForegroundLayer(stack: MatrixStack, mouseX: Int, mouseY: Int) {
    super.drawSecondaryForegroundLayer(stack, mouseX, mouseY)
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(if (inventoryContainer.isRunning) Localization.Computer.TurnOff.lines.toIterable else Localization.Computer.TurnOn.lines.toIterable))
      copiedDrawHoveringText(stack, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
  }

  override def drawSecondaryBackgroundLayer(stack: MatrixStack) {
    RenderSystem.color3f(1, 1, 1)
    Textures.bind(Textures.GUI.Server)
    blit(stack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
