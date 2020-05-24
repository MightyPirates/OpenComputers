package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava.asJavaCollection

class Rack(playerInventory: InventoryPlayer, val rack: tileentity.Rack) extends DynamicGuiContainer(new container.Rack(playerInventory, rack)) {
  ySize = 210

  final val busMasterBlankUVs = (195, 14, 3, 5)
  final val busMasterPresentUVs = (194, 20, 5, 5)
  final val busSlaveBlankUVs = (195, 1, 3, 4)
  final val busSlavePresentUVs = (194, 6, 5, 4)

  final val connectorMasterUVs = (194, 26, 1, 3)
  final val connectorSlaveUVs = (194, 11, 1, 2)

  final val hoverMasterSize = (3, 3)
  final val hoverSlaveSize = (3, 2)

  final val wireMasterUVs = Array(
    (186, 16, 6, 3),
    (186, 20, 6, 3),
    (186, 24, 6, 3),
    (186, 28, 6, 3),
    (186, 32, 6, 3)
  )
  final val wireSlaveUVs = Array(
    (186, 1, 6, 2),
    (186, 4, 6, 2),
    (186, 7, 6, 2),
    (186, 10, 6, 2),
    (186, 13, 6, 2)
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

  final val busToSide = EnumFacing.values().filter(_ != EnumFacing.SOUTH)
  final val sideToBus = busToSide.zipWithIndex.toMap

  var relayButton: ImageButton = _

  // bus -> mountable -> connectable
  var wireButtons = Array.fill(rack.getSizeInventory)(Array.fill(4)(Array.fill(5)(null: ImageButton)))

  def sideName(side: EnumFacing) = side match {
    case EnumFacing.UP => Localization.Rack.Top
    case EnumFacing.DOWN => Localization.Rack.Bottom
    case EnumFacing.EAST => Localization.Rack.Left
    case EnumFacing.WEST => Localization.Rack.Right
    case EnumFacing.NORTH => Localization.Rack.Back
    case _ => Localization.Rack.None
  }

  def encodeButtonId(mountable: Int, connectable: Int, bus: Int) = {
    // +1 to offset for relay button
    1 + mountable * 4 * 5 + connectable * 5 + bus
  }

  def decodeButtonId(buttonId: Int) = {
    // -1 to offset for relay button
    val bus = (buttonId - 1) % 5
    val connectable = ((buttonId - 1) / 5) % 4
    val mountable = (buttonId - 1) / 5 / 4
    (mountable, connectable, bus)
  }

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0) {
      ClientPacketSender.sendRackRelayState(rack, !rack.isRelayEnabled)
    }
    else {
      val (mountable, connectable, bus) = decodeButtonId(button.id)
      if (rack.nodeMapping(mountable)(connectable).contains(busToSide(bus))) {
        ClientPacketSender.sendRackMountableMapping(rack, mountable, connectable, None)
      }
      else {
        ClientPacketSender.sendRackMountableMapping(rack, mountable, connectable, Option(busToSide(bus)))
      }
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    for (bus <- 0 until 5) {
      for (mountable <- 0 until rack.getSizeInventory) {
        val presence = inventoryContainer.nodePresence(mountable)
        for (connectable <- 0 until 4) {
          wireButtons(mountable)(connectable)(bus).visible = presence(connectable)
        }
      }
    }
    relayButton.displayString = if (rack.isRelayEnabled) Localization.Rack.RelayEnabled else Localization.Rack.RelayDisabled
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()

    relayButton = new ImageButton(0, guiLeft + 101, guiTop + 96, 65, 18, Textures.GUI.ButtonRelay, Localization.Rack.RelayDisabled, textIndent = 18)
    add(buttonList, relayButton)

    val (mw, mh) = hoverMasterSize
    val (sw, sh) = hoverSlaveSize
    val (_, _, _, mbh) = busMasterBlankUVs
    val (_, _, _, sbh) = busSlaveBlankUVs
    for (bus <- 0 until 5) {
      for (mountable <- 0 until rack.getSizeInventory) {
        val offset = mountable * (mbh + sbh * 3 + busGap)
        val (bx, by) = busStart(bus)

        {
          val button = new ImageButton(encodeButtonId(mountable, 0, bus), guiLeft + bx, guiTop + by + offset + 1, mw, mh)
          add(buttonList, button)
          wireButtons(mountable)(0)(bus) = button
        }

        for (connectable <- 0 until 3) {
          val button = new ImageButton(encodeButtonId(mountable, connectable + 1, bus), guiLeft + bx, guiTop + by + offset + 1 + mbh + sbh * connectable, sw, sh)
          add(buttonList, button)
          wireButtons(mountable)(connectable + 1)(bus) = button
        }
      }
    }
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    RenderState.pushAttrib() // Prevents NEI render glitch.

    fontRendererObj.drawString(
      Localization.localizeImmediately(rack.getName),
      8, 6, 0x404040)

    GlStateManager.color(1, 1, 1)
    mc.renderEngine.bindTexture(Textures.GUI.Rack)

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
        rack.nodeMapping(mountable)(0) match {
          case Some(side) =>
            val bus = sideToBus(side)
            val (mwx, mwy, mww, mwh) = wireMasterUVs(bus)
            for (i <- 0 to bus) {
              val xOffset = mcw + i * (mpw + mww)
              drawRect(cx + xOffset, cy, mww, mwh, mwx, mwy)
            }
          case _ =>
        }
        for (connectable <- 1 until 4) {
          rack.nodeMapping(mountable)(connectable) match {
            case Some(side) =>
              val bus = sideToBus(side)
              val (swx, swy, sww, swh) = wireSlaveUVs(bus)
              val yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1)
              for (i <- 0 to bus) {
                val xOffset = scw + i * (spw + sww)
                drawRect(cx + xOffset, cy + yOffset, sww, swh, swx, swy)
              }
            case _ =>
          }
        }
      }
      for (connectable <- 1 until 4) {
        if (presence(connectable)) {
          val yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1)
          drawRect(cx, cy + yOffset, scw, sch, scx, scy)
        }
      }

      // Draw connection points on buses.
      val yOffset = mountable * (mbh + sbh * 3 + busGap)
      for (bus <- 0 until 5) {
        val (bx, by) = busStart(bus)
        if (presence(0)) {
          drawRect(bx - 1, by + yOffset, mpw, mph, mpx, mpy)
        }
        else {
          drawRect(bx, by + yOffset, mbw, mbh, mbx, mby)
        }
        for (connectable <- 0 until 3) {
          if (presence(connectable + 1)) {
            drawRect(bx - 1, by + yOffset + mph + sph * connectable, spw, sph, spx, spy)
          }
          else {
            drawRect(bx, by + yOffset + mbh + sbh * connectable, sbw, sbh, sbx, sby)
          }
        }
      }
    }

    for (bus <- 0 until 5) {
      val x = 122
      val y = 20 + bus * 11

      fontRendererObj.drawString(
        Localization.localizeImmediately(sideName(busToSide(bus))),
        x, y, 0x404040)
    }

    if (relayButton.isMouseOver) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(Localization.Rack.RelayModeTooltip.lines.toIterable))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }

    RenderState.popAttrib()
  }

  override def drawSecondaryBackgroundLayer() {
    GlStateManager.color(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.GUI.Rack)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }

  private def drawRect(x: Int, y: Int, w: Int, h: Int, u: Int, v: Int): Unit = {
    val u0 = u / 256f
    val v0 = v / 256f
    val u1 = u0 + w / 256f
    val v1 = v0 + h / 256f
    val t = Tessellator.getInstance()
    val r = t.getBuffer
    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
    r.pos(x, y, windowZ).tex(u0, v0).endVertex()
    r.pos(x, y + h, windowZ).tex(u0, v1).endVertex()
    r.pos(x + w, y + h, windowZ).tex(u1, v1).endVertex()
    r.pos(x + w, y, windowZ).tex(u1, v0).endVertex()
    t.draw()
  }
}
