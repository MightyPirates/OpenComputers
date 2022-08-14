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

import scala.collection.JavaConverters.asJavaCollection

class Server(id: Int, playerInventory: PlayerInventory, serverInventory: ServerInventory, val rack: Option[tileentity.Rack] = None, val slot: Int = 0)
  extends DynamicGuiContainer(new container.Server(container.ContainerTypes.SERVER, id, playerInventory, serverInventory),
    playerInventory, serverInventory.getName)
  with traits.LockedHotbar[container.Server] {

  protected var powerButton: ImageButton = _

  override def lockedStack = serverInventory.container

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float) {
    // Close GUI if item is removed from rack.
    rack match {
      case Some(t) if t.getItem(slot) != serverInventory.container =>
        onClose()
        return
      case _ =>
    }

    powerButton.visible = !inventoryContainer.isItem
    powerButton.toggled = inventoryContainer.isRunning
    super.render(stack, mouseX, mouseY, dt)
  }

  override protected def init() {
    super.init()
    powerButton = new ImageButton(leftPos + 48, topPos + 33, 18, 18, new Button.IPressable {
      override def onPress(b: Button) = rack match {
        case Some(t) => ClientPacketSender.sendServerPower(t, slot, !inventoryContainer.isRunning)
        case _ =>
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
