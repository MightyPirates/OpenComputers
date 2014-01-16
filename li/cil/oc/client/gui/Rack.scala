package li.cil.oc.client.gui

import java.util
import li.cil.oc.Settings
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.{ResourceLocation, StatCollector}
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

class Rack(playerInventory: InventoryPlayer, val rack: tileentity.Rack) extends DynamicGuiContainer(new container.Rack(playerInventory, rack)) {
  protected val powerIcon = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_power.png")
  protected val sideIcon = new ResourceLocation(Settings.resourceDomain, "textures/gui/button_side.png")

  protected var powerButtons = new Array[ImageButton](4)

  protected var sideButtons = new Array[GuiButton](4)

  def sideName(number: Int) = StatCollector.translateToLocal(Settings.namespace + (rack.sides(number) match {
    case ForgeDirection.UP => "gui.ServerRack.Top"
    case ForgeDirection.DOWN => "gui.ServerRack.Bottom"
    case ForgeDirection.EAST => "gui.ServerRack.Left"
    case ForgeDirection.WEST => "gui.ServerRack.Right"
    case ForgeDirection.NORTH => "gui.ServerRack.Back"
    case _ => "gui.ServerRack.All"
  }))

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id >= 0 && button.id <= 3) {
      ClientPacketSender.sendServerPower(rack, button.id, !rack.isRunning(button.id))
    }
    if (button.id >= 4 && button.id <= 7) {
      val number = button.id - 4
      val nextSide = rack.sides(number) match {
        case ForgeDirection.UP => ForgeDirection.DOWN
        case ForgeDirection.DOWN => ForgeDirection.EAST
        case ForgeDirection.EAST => ForgeDirection.WEST
        case ForgeDirection.WEST => ForgeDirection.NORTH
        case ForgeDirection.NORTH => ForgeDirection.UNKNOWN
        case _ => ForgeDirection.UP
      }
      ClientPacketSender.sendServerSide(rack, number, nextSide)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    for (i <- 0 to 3) {
      powerButtons(i).toggled = rack.isRunning(i)
      sideButtons(i).displayString = sideName(i)
    }
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    for (i <- 0 to 3) {
      powerButtons(i) = new ImageButton(i, guiLeft + 84, guiTop + 7 + i * 18, 18, 18, powerIcon)
      add(buttonList, powerButtons(i))
    }
    for (i <- 0 to 3) {
      sideButtons(i) = new ImageButton(4 + i, guiLeft + 126, guiTop + 7 + i * 18, 42, 18, sideIcon, sideName(i), canToggle = false)
      add(buttonList, sideButtons(i))
    }
  }

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    GL11.glPushAttrib(0xFFFFFFFF) // Prevents NEI render glitch.
    fontRenderer.drawString(
      StatCollector.translateToLocal(rack.getInvName),
      8, 6, 0x404040)
    for (i <- 0 to 3 if powerButtons(i).func_82252_a) {
      val tooltip = new java.util.ArrayList[String]
      val which = if (rack.isRunning(i)) "gui.Robot.TurnOff" else "gui.Robot.TurnOn"
      tooltip.add(StatCollector.translateToLocal(Settings.namespace + which))
      drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }
    GL11.glPopAttrib()
  }
}
