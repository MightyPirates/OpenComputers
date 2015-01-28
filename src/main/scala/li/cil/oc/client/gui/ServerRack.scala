package li.cil.oc.client.gui

import java.util

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

class ServerRack(playerInventory: InventoryPlayer, val rack: tileentity.ServerRack) extends DynamicGuiContainer(new container.ServerRack(playerInventory, rack)) {
  protected var switchButton: ImageButton = _

  protected var powerButtons = new Array[ImageButton](4)

  protected var sideButtons = new Array[GuiButton](4)

  def sideName(number: Int) = rack.sides(number) match {
    case Some(EnumFacing.UP) => Localization.ServerRack.Top
    case Some(EnumFacing.DOWN) => Localization.ServerRack.Bottom
    case Some(EnumFacing.EAST) => Localization.ServerRack.Left
    case Some(EnumFacing.WEST) => Localization.ServerRack.Right
    case Some(EnumFacing.NORTH) => Localization.ServerRack.Back
    case _ => Localization.ServerRack.None
  }

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id >= 0 && button.id <= 3) {
      ClientPacketSender.sendServerPower(rack, button.id, !rack.isRunning(button.id))
    }
    if (button.id >= 4 && button.id <= 7) {
      val number = button.id - 4
      val sides = EnumFacing.values.map(Option(_)) ++ Seq(None)
      val currentSide = sides.indexOf(rack.sides(number))
      val searchSides = sides.drop(currentSide + 1) ++ sides.take(currentSide + 1)
      val nextSide = searchSides.find(side => side != Option(EnumFacing.SOUTH) && (!rack.sides.contains(side) || side == None)) match {
        case Some(side) => side
        case _ => None
      }
      ClientPacketSender.sendServerSide(rack, number, nextSide)
    }
    if (button.id == 10) {
      ClientPacketSender.sendServerSwitchMode(rack, !rack.internalSwitch)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    for (i <- 0 to 3) {
      powerButtons(i).toggled = rack.isRunning(i)
      sideButtons(i).displayString = sideName(i)
    }
    switchButton.displayString = if (rack.internalSwitch) Localization.ServerRack.SwitchInternal else Localization.ServerRack.SwitchExternal
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    for (i <- 0 to 3) {
      powerButtons(i) = new ImageButton(i, guiLeft + 84, guiTop + 7 + i * 18, 18, 18, Textures.GUI.ButtonPower, canToggle = true)
      add(buttonList, powerButtons(i))
    }
    for (i <- 0 to 3) {
      sideButtons(i) = new ImageButton(4 + i, guiLeft + 126, guiTop + 7 + i * 18, 42, 18, Textures.GUI.ButtonSide, sideName(i))
      add(buttonList, sideButtons(i))
    }
    switchButton = new ImageButton(10, guiLeft + 8, guiTop + 17, 64, 18, Textures.GUI.ButtonSwitch, Localization.ServerRack.SwitchExternal, textIndent = 18)
    add(buttonList, switchButton)
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Prevents NEI render glitch.

    fontRendererObj.drawString(
      Localization.localizeImmediately(rack.getName),
      8, 6, 0x404040)

    for (i <- 0 to 3 if powerButtons(i).isMouseOver) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(if (rack.isRunning(i)) Localization.Computer.TurnOff else Localization.Computer.TurnOn)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }

    GL11.glPopAttrib()
  }
}
