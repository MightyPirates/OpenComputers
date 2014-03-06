package li.cil.oc.client.gui

import java.util
import li.cil.oc.Settings
import li.cil.oc.client.{PacketSender => ClientPacketSender, TexturePreloader}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.{GuiScreen, GuiButton}
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.StatCollector
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

class Rack(playerInventory: InventoryPlayer, val rack: tileentity.Rack) extends DynamicGuiContainer(new container.Rack(playerInventory, rack)) {
  protected var powerButtons = new Array[ImageButton](4)

  protected var sideButtons = new Array[GuiButton](4)

  protected var rangeButtons = new Array[GuiButton](2)

  def sideName(number: Int) = StatCollector.translateToLocal(Settings.namespace + (rack.sides(number) match {
    case ForgeDirection.UP => "gui.ServerRack.Top"
    case ForgeDirection.DOWN => "gui.ServerRack.Bottom"
    case ForgeDirection.EAST => "gui.ServerRack.Left"
    case ForgeDirection.WEST => "gui.ServerRack.Right"
    case ForgeDirection.NORTH => "gui.ServerRack.Back"
    case _ => "gui.ServerRack.None"
  }))

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected override def actionPerformed(button: GuiButton) {
    if (button.id >= 0 && button.id <= 3) {
      ClientPacketSender.sendServerPower(rack, button.id, !rack.isRunning(button.id))
    }
    if (button.id >= 4 && button.id <= 7) {
      val number = button.id - 4
      val sides = ForgeDirection.values
      val currentSide = rack.sides(number)
      val searchSides = sides.drop(currentSide.ordinal() + 1) ++ sides.take(currentSide.ordinal() + 1)
      val nextSide = searchSides.find(side => side != ForgeDirection.SOUTH && (!rack.sides.contains(side) || side == ForgeDirection.UNKNOWN)) match {
        case Some(side) => side
        case _ => ForgeDirection.UNKNOWN
      }
      ClientPacketSender.sendServerSide(rack, number, nextSide)
    }
    if (button.id >= 8 && button.id <= 9) {
      val step =
        if (GuiScreen.isShiftKeyDown) 32
        else if (GuiScreen.isCtrlKeyDown) 1
        else 8
      val range =
        if (button.id == 8) math.max(rack.range - step, 0)
        else math.min(rack.range + step, Settings.get.maxWirelessRange.toInt)
      if (range != rack.range) {
        ClientPacketSender.sendServerRange(rack, range)
      }
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
      powerButtons(i) = new ImageButton(i, guiLeft + 84, guiTop + 7 + i * 18, 18, 18, TexturePreloader.guiButtonPower, canToggle = true)
      add(buttonList, powerButtons(i))
    }
    for (i <- 0 to 3) {
      sideButtons(i) = new ImageButton(4 + i, guiLeft + 126, guiTop + 7 + i * 18, 42, 18, TexturePreloader.guiButtonSide, sideName(i))
      add(buttonList, sideButtons(i))
    }
    for (i <- 0 to 1) {
      rangeButtons(i) = new ImageButton(8 + i, guiLeft + 8 + i * 48, guiTop + 43, 16, 18, TexturePreloader.guiButtonRange, if (i == 0) "-" else "+")
      add(buttonList, rangeButtons(i))
    }
  }

  override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawGuiContainerForegroundLayer(mouseX, mouseY)
    GL11.glPushAttrib(0xFFFFFFFF) // Prevents NEI render glitch.

    fontRenderer.drawString(
      StatCollector.translateToLocal(rack.getInvName),
      8, 6, 0x404040)

    fontRenderer.drawString(
      StatCollector.translateToLocal(Settings.namespace + "gui.ServerRack.WirelessRange"),
      8, 31, 0x404040)

    {
      // Background for range value.
      val tx = 25
      val ty = 43
      val w = 30
      val h = 18
      val t = Tessellator.instance
      mc.getTextureManager.bindTexture(TexturePreloader.guiRange)
      GL11.glColor3f(1, 1, 1)
      GL11.glDepthMask(false)
      t.startDrawingQuads()
      t.addVertexWithUV(tx, ty + h, zLevel, 0, 1)
      t.addVertexWithUV(tx + w, ty + h, zLevel, 1, 1)
      t.addVertexWithUV(tx + w, ty, zLevel, 1, 0)
      t.addVertexWithUV(tx, ty, zLevel, 0, 0)
      t.draw()
      GL11.glDepthMask(true)
    }

    drawCenteredString(fontRenderer,
      rack.range.toString,
      40, 48, 0xFFFFFF)

    for (i <- 0 to 3 if powerButtons(i).func_82252_a) {
      val tooltip = new java.util.ArrayList[String]
      val which = if (rack.isRunning(i)) "gui.Robot.TurnOff" else "gui.Robot.TurnOn"
      tooltip.add(StatCollector.translateToLocal(Settings.namespace + which))
      drawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
    }

    GL11.glPopAttrib()
  }
}
