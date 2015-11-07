package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._

class Rack(playerInventory: InventoryPlayer, val rack: tileentity.Rack) extends DynamicGuiContainer(new container.Rack(playerInventory, rack)) {
  ySize = 210

  final val busSlaveBlankUVs = (195, 1, 3, 4)
  final val busSlavePresentUVs = (194, 6, 5, 4)
  final val busMasterBlankUVs = (195, 14, 3, 5)
  final val busMasterPresentUVs = (194, 20, 5, 5)

  final val connectorSlaveUVs = (194, 11, 1, 2)
  final val hoverSlaveUVs = (196, 11, 3, 2)
  final val connectorMasterUVs = (194, 26, 1, 3)
  final val hoverMasterUVs = (196, 26, 3, 3)

  final val wireSlaveUVs = Array(
    (186, 1, 6, 2),
    (186, 4, 6, 2),
    (186, 7, 6, 2),
    (186, 10, 6, 2),
    (186, 13, 6, 2)
  )
  final val wireMasterUVs = Array(
    (186, 16, 6, 3),
    (186, 20, 6, 3),
    (186, 24, 6, 3),
    (186, 28, 6, 3),
    (186, 32, 6, 3)
  )

  final val busStart = Array(
    (45, 22),
    (56, 22),
    (67, 22),
    (78, 22),
    (89, 22)
  )

  final val busGap = 3

  final val connectorStart = Array(
    (37, 23),
    (37, 43),
    (37, 63),
    (37, 83)
  )

  final val connectorGap = 2

  final val relayModeUVs = (195, 30, 4, 2)

  final val wireRelay = Array(
    (50, 104),
    (61, 104),
    (72, 104),
    (83, 104)
  )

  var switchButton: ImageButton = _

  var wireButtons = Array.fill(5)(Array.fill(rack.getSizeInventory * 4)(null: ImageButton))

//  protected var powerButtons = new Array[ImageButton](4)
//
//  protected var sideButtons = new Array[GuiButton](4)
//
//  protected var rangeButtons = new Array[GuiButton](2)
//
//  def sideName(number: Int) = rack.sides(number) match {
//    case Some(ForgeDirection.UP) => Localization.ServerRack.Top
//    case Some(ForgeDirection.DOWN) => Localization.ServerRack.Bottom
//    case Some(ForgeDirection.EAST) => Localization.ServerRack.Left
//    case Some(ForgeDirection.WEST) => Localization.ServerRack.Right
//    case Some(ForgeDirection.NORTH) => Localization.ServerRack.Back
//    case _ => Localization.ServerRack.None
//  }
//
//  protected override def actionPerformed(button: GuiButton) {
//    if (button.id >= 0 && button.id <= 3) {
//      ClientPacketSender.sendServerPower(rack, button.id, !rack.isRunning(button.id))
//    }
//    if (button.id >= 4 && button.id <= 7) {
//      val number = button.id - 4
//      val sides = ForgeDirection.VALID_DIRECTIONS.map(Option(_)) ++ Seq(None)
//      val currentSide = sides.indexOf(rack.sides(number))
//      val searchSides = sides.drop(currentSide + 1) ++ sides.take(currentSide + 1)
//      val nextSide = searchSides.find(side => side != Option(ForgeDirection.SOUTH) && (!rack.sides.contains(side) || side.isEmpty)) match {
//        case Some(side) => side
//        case _ => None
//      }
//      ClientPacketSender.sendServerSide(rack, number, nextSide)
//    }
//    if (button.id >= 8 && button.id <= 9) {
//      val step =
//        if (GuiScreen.isShiftKeyDown) 32
//        else if (GuiScreen.isCtrlKeyDown) 1
//        else 8
//      val range =
//        if (button.id == 8) math.max(rack.range - step, 0)
//        else math.min(rack.range + step, Settings.get.maxWirelessRange.toInt)
//      if (range != rack.range) {
//        ClientPacketSender.sendServerRange(rack, range)
//      }
//    }
//    if (button.id == 10) {
//      ClientPacketSender.sendServerSwitchMode(rack, !rack.internalSwitch)
//    }
//  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
//    for (i <- 0 to 3) {
//      powerButtons(i).toggled = rack.isRunning(i)
//      sideButtons(i).displayString = sideName(i)
//    }
    switchButton.displayString = if (rack.isRelayEnabled) Localization.ServerRack.SwitchInternal else Localization.ServerRack.SwitchExternal
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
//    for (i <- 0 to 3) {
//      powerButtons(i) = new ImageButton(i, guiLeft + 84, guiTop + 7 + i * 18, 18, 18, Textures.guiButtonPower, canToggle = true)
//      add(buttonList, powerButtons(i))
//    }
//    for (i <- 0 to 3) {
//      sideButtons(i) = new ImageButton(4 + i, guiLeft + 126, guiTop + 7 + i * 18, 42, 18, Textures.guiButtonSide, sideName(i))
//      add(buttonList, sideButtons(i))
//    }
//    for (i <- 0 to 1) {
//      rangeButtons(i) = new ImageButton(8 + i, guiLeft + 8 + i * 48, guiTop + 50, 16, 18, Textures.guiButtonRange, if (i == 0) "-" else "+")
//      add(buttonList, rangeButtons(i))
//    }
    switchButton = new ImageButton(0, guiLeft + 101, guiTop + 96, 65, 18, Textures.guiButtonSwitch, Localization.ServerRack.SwitchExternal, textIndent = 18)
    add(buttonList, switchButton)
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Prevents NEI render glitch.

    fontRendererObj.drawString(
      Localization.localizeImmediately(rack.getInventoryName),
      8, 6, 0x404040)
//
//    val rangeY = 39
//    fontRendererObj.drawString(Localization.ServerRack.WirelessRange, 8, rangeY, 0x404040)
//
//    {
//      // Background for range value.
//      val tx = 25
//      val ty = 50
//      val w = 30
//      val h = 18
//      val t = Tessellator.instance
//      mc.getTextureManager.bindTexture(Textures.guiRange)
//      GL11.glColor3f(1, 1, 1)
//      GL11.glDepthMask(false)
//      t.startDrawingQuads()
//      t.addVertexWithUV(tx, ty + h, zLevel, 0, 1)
//      t.addVertexWithUV(tx + w, ty + h, zLevel, 1, 1)
//      t.addVertexWithUV(tx + w, ty, zLevel, 1, 0)
//      t.addVertexWithUV(tx, ty, zLevel, 0, 0)
//      t.draw()
//      GL11.glDepthMask(true)
//    }
//
//    drawCenteredString(fontRendererObj,
//      rack.range.toString,
//      40, 56, 0xFFFFFF)
//
//    for (i <- 0 to 3 if powerButtons(i).func_146115_a) {
//      val tooltip = new java.util.ArrayList[String]
//      tooltip.addAll(asJavaCollection(if (rack.isRunning(i)) Localization.Computer.TurnOff.lines.toIterable else Localization.Computer.TurnOn.lines.toIterable))
//      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
//    }
//

    GL11.glColor3f(1, 1, 1)
    mc.renderEngine.bindTexture(Textures.guiRack)

    if (rack.isRelayEnabled) {
      val (left, top, w, h) = relayModeUVs
      for ((x, y) <- wireRelay) {
        drawRect(x, y, w, h, left, top)
      }
    }

    val (mcx, mcy, mcw, mch) = connectorMasterUVs
    val (mbx, mby, mbw, mbh) = busMasterBlankUVs
    val (mpx, mpy, mpw, mph) = busMasterPresentUVs
    val (scx, scy, scw, sch) = connectorSlaveUVs
    val (sbx, sby, sbw, sbh) = busSlaveBlankUVs
    val (spx, spy, spw, sph) = busSlavePresentUVs
    for (mountable <- 0 until rack.getSizeInventory) {
      val presence = inventoryContainer.nodePresence(mountable)

      // Draw connectable indicators next to item slots.
      val (cx, cy) = connectorStart(mountable)
      if (presence(0)) {
        drawRect(cx, cy, mcw, mch, mcx, mcy)
      }
      for (connectable <- 1 until 4) {
        if (presence(connectable)) {
          drawRect(cx, cy + (mch + connectorGap) + (sch + connectorGap) * (connectable - 1), scw, sch, scx, scy)
        }
      }

      // Draw connection points on buses.
      val offset = mountable * (mbh + sbh * 3 + busGap)
      for (bus <- 0 until 5) {
        val (bx, by) = busStart(bus)
        if (presence(0)) {
          drawRect(bx - 1, by + offset, mpw, mph, mpx, mpy)
        }
        else {
          drawRect(bx, by + offset, mbw, mbh, mbx, mby)
        }
        for (connectable <- 0 until 3) {
          if (presence(connectable + 1)) {
            drawRect(bx - 1, by + offset + mph + sph * connectable, spw, sph, spx, spy)
          }
          else {
            drawRect(bx, by + offset + mbh + sbh * connectable, sbw, sbh, sbx, sby)
          }
        }
      }

      // Draw active connections.
    }

    for (bus <- 0 until 5) {
      val x = 122
      val y = 20 + bus * 11

      fontRendererObj.drawString(
        Localization.localizeImmediately("TODO"),
        x, y, 0x404040)
    }

    GL11.glPopAttrib()
  }

  override def drawSecondaryBackgroundLayer() {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiRack)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  private def drawRect(x: Int, y: Int, w: Int, h: Int, u: Int, v: Int): Unit = {
    val u0 = u / 256f
    val v0 = v / 256f
    val u1 = u0 + w / 256f
    val v1 = v0 + h / 256f
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y, windowZ, u0, v0)
    t.addVertexWithUV(x, y + h, windowZ, u0, v1)
    t.addVertexWithUV(x + w, y + h, windowZ, u1, v1)
    t.addVertexWithUV(x + w, y, windowZ, u1, v0)
    t.draw()
  }
}
