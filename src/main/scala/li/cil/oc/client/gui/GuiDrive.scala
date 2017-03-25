package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.item.data.DriveData
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack

class GuiDrive(playerInventory: InventoryPlayer, val driveStack: () => ItemStack) extends GuiScreen with traits.Window {
  override val windowHeight = 85

  override def backgroundImage = Textures.GUI.Drive

  protected var managedButton: ImageButton = _
  protected var unmanagedButton: ImageButton = _

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0) {
      ClientPacketSender.sendDriveMode(false)
    }
    if (button.id == 1) {
      ClientPacketSender.sendDriveMode(true)
    }
  }

  override def initGui(): Unit = {
    super.initGui()
    managedButton = new ImageButton(0, guiLeft + 11, guiTop + 11, 74, 18, Textures.GUI.ButtonDriveMode, text = Localization.Drive.Managed, textColor = 0x608060, canToggle = true)
    unmanagedButton = new ImageButton(1, guiLeft + 91, guiTop + 11, 74, 18, Textures.GUI.ButtonDriveMode, text = Localization.Drive.Unmanaged, textColor = 0x608060, canToggle = true)
    add(buttonList, managedButton)
    add(buttonList, unmanagedButton)
  }

  override def updateScreen(): Unit = {
    unmanagedButton.toggled = new DriveData(driveStack()).isUnmanaged
    managedButton.toggled = !unmanagedButton.toggled
    super.updateScreen()
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.drawScreen(mouseX, mouseY, dt)
    fontRenderer.drawSplitString(Localization.Drive.Warning, guiLeft + 7, guiTop + 37, xSize - 16, 0x404040)
  }
}
