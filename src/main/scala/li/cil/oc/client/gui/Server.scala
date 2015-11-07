package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.tileentity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava.asJavaCollection

class Server(playerInventory: InventoryPlayer, serverInventory: ServerInventory, val rack: Option[tileentity.Rack] = None, val slot: Int = 0) extends DynamicGuiContainer(new container.Server(playerInventory, serverInventory)) with traits.LockedHotbar {
  protected var powerButton: ImageButton = _

  override def lockedStack = serverInventory.container

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0) {
      rack match {
        case Some(t) => ClientPacketSender.sendServerPower(t, slot, !inventoryContainer.isRunning)
        case _ =>
      }
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    // Close GUI if item is removed from rack.
    rack match {
      case Some(t) if t.getStackInSlot(slot) != serverInventory.container =>
        Minecraft.getMinecraft.displayGuiScreen(null)
        return
      case _ =>
    }

    powerButton.visible = !inventoryContainer.isItem
    powerButton.toggled = inventoryContainer.isRunning
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 48, guiTop + 33, 18, 18, Textures.guiButtonPower, canToggle = true)
    add(buttonList, powerButton)
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRendererObj.drawString(
      Localization.localizeImmediately(serverInventory.getInventoryName),
      8, 6, 0x404040)
    if (powerButton.func_146115_a) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(if (inventoryContainer.isRunning) Localization.Computer.TurnOff.lines.toIterable else Localization.Computer.TurnOn.lines.toIterable))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
  }

  override def drawSecondaryBackgroundLayer() {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiServer)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }
}