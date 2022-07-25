package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.item.data.DriveData
import net.minecraft.client.gui.widget.button.Button
import net.minecraft.client.gui.screen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.text.StringTextComponent

class Drive(playerInventory: PlayerInventory, val driveStack: () => ItemStack) extends screen.Screen(StringTextComponent.EMPTY) with traits.Window {
  override val windowHeight = 120

  override def backgroundImage = Textures.GUI.Drive

  protected var managedButton: ImageButton = _
  protected var unmanagedButton: ImageButton = _
  protected var lockedButton: ImageButton = _

  def updateButtonStates(): Unit = {
    val data = new DriveData(driveStack())
    unmanagedButton.toggled = data.isUnmanaged
    managedButton.toggled = !unmanagedButton.toggled
    lockedButton.toggled = data.isLocked
    lockedButton.active = !data.isLocked
  }

  override protected def init(): Unit = {
    super.init()
    managedButton = new ImageButton(leftPos + 11, topPos + 11, 74, 18, new Button.IPressable {
      override def onPress(b: Button) = {
        ClientPacketSender.sendDriveMode(unmanaged = false)
        DriveData.setUnmanaged(driveStack(), unmanaged = false)
      }
    }, Textures.GUI.ButtonDriveMode, text = new StringTextComponent(Localization.Drive.Managed), textColor = 0x608060, canToggle = true)
    unmanagedButton = new ImageButton(leftPos + 91, topPos + 11, 74, 18, new Button.IPressable {
      override def onPress(b: Button) = {
        ClientPacketSender.sendDriveMode(unmanaged = true)
        DriveData.setUnmanaged(driveStack(), unmanaged = true)
      }
    }, Textures.GUI.ButtonDriveMode, text = new StringTextComponent(Localization.Drive.Unmanaged), textColor = 0x608060, canToggle = true)
    lockedButton = new ImageButton(leftPos + 11, topPos + windowHeight - 42, 44, 18, new Button.IPressable {
      override def onPress(b: Button) = {
        ClientPacketSender.sendDriveLock()
        DriveData.lock(driveStack(), playerInventory.player)
      }
    }, Textures.GUI.ButtonDriveMode, text = new StringTextComponent(Localization.Drive.ReadOnlyLock), textColor = 0x608060, canToggle = true)
    addButton(managedButton)
    addButton(unmanagedButton)
    addButton(lockedButton)
    updateButtonStates()
  }

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.render(stack, mouseX, mouseY, dt)
    font.drawWordWrap(new StringTextComponent(Localization.Drive.Warning), leftPos + 11, topPos + 37, imageWidth - 20, 0x404040)
    font.drawWordWrap(new StringTextComponent(Localization.Drive.LockWarning), leftPos + 61, topPos + windowHeight - 48, imageWidth - 68, 0x404040)
  }
}
